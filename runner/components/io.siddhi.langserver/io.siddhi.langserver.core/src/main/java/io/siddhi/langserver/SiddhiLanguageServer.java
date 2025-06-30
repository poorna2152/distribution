/*
 * Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.siddhi.langserver;

import com.google.gson.JsonParser;
import io.siddhi.core.SiddhiManager;
import io.siddhi.langserver.extension.DesignModelGeneratorService;
import io.siddhi.langserver.extension.EventSimulatorService;
import io.siddhi.langserver.extension.ExportService;
import io.siddhi.langserver.extension.RuntimeService;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethodProvider;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorDataHolder;
import org.wso2.carbon.siddhi.editor.core.internal.DebugProcessorService;
import org.wso2.carbon.siddhi.editor.core.internal.EditorDataHolder;
import org.wso2.carbon.streaming.integrator.common.utils.config.FileConfigManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;

/**
 * Siddhi Language Server implementation for Siddhi which  provides language analytic capabilities for Siddhi
 * application development.
 */
public class SiddhiLanguageServer implements LanguageServer, Endpoint, JsonRpcMethodProvider {

    private LanguageClient client;
    private SiddhiTextDocumentService textDocumentService;
    private SiddhiWorkspaceService workspaceService;
    private int shutDownStatus = 1;
    private final DesignModelGeneratorService designModelGeneratorService;
    private final EventSimulatorService eventSimulatorService;
    private final RuntimeService runtimeService;
    private final ExportService exportService;
    private Map<String, JsonRpcMethod> supportedMethods;
    private final Map<String, Endpoint> extensionServices = new HashMap<>();

    public SiddhiLanguageServer() {
        LSOperationContext.INSTANCE.setSiddhiLanguageServer(this);
        EditorDataHolder.setSiddhiManager(LSOperationContext.INSTANCE.getSiddhiManager());
        this.textDocumentService = new SiddhiTextDocumentService();
        this.workspaceService = new SiddhiWorkspaceService();
        this.designModelGeneratorService = new DesignModelGeneratorService();
        this.eventSimulatorService = new EventSimulatorService();
        this.runtimeService = new RuntimeService();
        this.exportService = new ExportService();

    }

    /**
     * Set the client instance to which the diagnostics are pushed.
     *
     * @param languageClient
     */
    public void connect(LanguageClient languageClient) {
        this.client = languageClient;
        //todo: Initiate loggers once the logging framework is implemented.
    }

    /**
     * This method binds the language server with server options.
     *
     * @param initializeParams an object which comprises of initialization options for the language server.
     * @return {@link InitializeResult} object which comprises of the capabilities of the language server.
     */
    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
        final CompletionOptions completionOptions = new CompletionOptions();
        completionOptions.setTriggerCharacters(Arrays.asList("#", "@"));
        final InitializeResult initializedResult = new InitializeResult(new ServerCapabilities());
        initializedResult.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
        initializedResult.getCapabilities().setCompletionProvider(completionOptions);
        return CompletableFuture.supplyAsync(() -> initializedResult);
    }

    /**
     * Shuts the language server down.
     *
     * @return private LSCompletionContext completionContext; {@link Object} a new Object instance.
     */
    @Override
    public CompletableFuture<Object> shutdown() {
        this.shutDownStatus = 0;
        return CompletableFuture.supplyAsync(Object::new);
    }

    @Override
    public void exit() {
        System.exit(shutDownStatus);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this.textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this.workspaceService;
    }

    public LanguageClient getClient() {
        return this.client;
    }

    @Override
    public Map<String, JsonRpcMethod> supportedMethods() {
        if (this.supportedMethods != null) {
            return this.supportedMethods;
        }
        synchronized (this.extensionServices) {
            Map<String, JsonRpcMethod> supportedMethods = new LinkedHashMap<>(ServiceEndpoints.getSupportedMethods(getClass()));
            Map<String, JsonRpcMethod> supportedExtensions = designModelGeneratorService.supportedMethods();
            Endpoint designModelEndpoint = ServiceEndpoints.toEndpoint(designModelGeneratorService);
            for (Map.Entry<String, JsonRpcMethod> entry : supportedExtensions.entrySet()) {
                this.extensionServices.put(entry.getKey(), designModelEndpoint);
                supportedMethods.put(entry.getKey(), entry.getValue());
            }
            supportedExtensions = eventSimulatorService.supportedMethods();
            Endpoint eventSimulatorEndpoint = ServiceEndpoints.toEndpoint(eventSimulatorService);
            for (Map.Entry<String, JsonRpcMethod> entry : supportedExtensions.entrySet()) {
                this.extensionServices.put(entry.getKey(), eventSimulatorEndpoint);
                supportedMethods.put(entry.getKey(), entry.getValue());
            }
            supportedExtensions = runtimeService.supportedMethods();
            Endpoint runtimeEndpoint = ServiceEndpoints.toEndpoint(runtimeService);
            for (Map.Entry<String, JsonRpcMethod> entry : supportedExtensions.entrySet()) {
                this.extensionServices.put(entry.getKey(), runtimeEndpoint);
                supportedMethods.put(entry.getKey(), entry.getValue());
            }
            supportedExtensions = exportService.supportedMethods();
            Endpoint exportEndpoint = ServiceEndpoints.toEndpoint(exportService);
            for (Map.Entry<String, JsonRpcMethod> entry : supportedExtensions.entrySet()) {
                this.extensionServices.put(entry.getKey(), exportEndpoint);
                supportedMethods.put(entry.getKey(), entry.getValue());
            }
            this.supportedMethods = supportedMethods;
            return supportedMethods;
        }
    }

    @Override
    public CompletableFuture<?> request(String method, Object parameter) {
        try (PrintWriter out = new PrintWriter("method.txt")) {
            out.println("Loaded JsonParser from: " + method);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!extensionServices.containsKey(method)) {
            throw new UnsupportedOperationException("The json request '" + method + "' is unknown.");
        }
        return extensionServices.get(method).request(method, parameter);
    }

    @Override
    public void notify(String method, Object parameter) {
        if (extensionServices.containsKey(method)) {
            extensionServices.get(method).notify(method, parameter);
        }
    }

}

