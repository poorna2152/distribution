package io.siddhi.langserver.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.langserver.response.DesignModelResponse;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethodProvider;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.siddhi.editor.core.util.designview.beans.EventFlow;
import org.wso2.carbon.siddhi.editor.core.util.designview.codegenerator.CodeGenerator;
import org.wso2.carbon.siddhi.editor.core.util.designview.deserializers.DeserializersRegisterer;
import org.wso2.carbon.siddhi.editor.core.util.designview.designgenerator.DesignGenerator;
import org.wso2.carbon.siddhi.editor.core.util.designview.exceptions.CodeGenerationException;
import org.wso2.carbon.siddhi.editor.core.util.designview.exceptions.DesignGenerationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DesignModelGeneratorService implements JsonRpcMethodProvider {

    private static final Logger log = LoggerFactory.getLogger(DesignModelGeneratorService.class);

    @Override
    public Map<String, JsonRpcMethod> supportedMethods() {
        return new HashMap<>(ServiceEndpoints.getSupportedMethods(getClass()));
    }

    @JsonRequest
    public CompletableFuture<DesignModelResponse> getDesignView(String siddhiAppBase64) {
        return CompletableFuture.supplyAsync(() -> {
            DesignModelResponse designModelResponse = new DesignModelResponse();
            try {
                SiddhiManager siddhiManager = new SiddhiManager();
                DesignGenerator designGenerator = new DesignGenerator();
                designGenerator.setSiddhiManager(siddhiManager);
                String siddhiAppString = new String(Base64.getDecoder().decode(siddhiAppBase64), StandardCharsets.UTF_8);
                EventFlow eventFlow = designGenerator.getEventFlow(siddhiAppString);
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String eventFlowJson = gson.toJson(eventFlow);
                designModelResponse.setContent(new String(Base64.getEncoder().encode(eventFlowJson.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
                return designModelResponse;
            } catch (SiddhiAppCreationException e) {
                designModelResponse.setError(e);
                log.error("Unable to generate design view", e);
            } catch (DesignGenerationException e) {
                designModelResponse.setError(e);
                log.error("Failed to convert Siddhi app code to design view", e);
            }
            return designModelResponse;
        });
    }

    @JsonRequest
    public CompletableFuture<DesignModelResponse> getSourceCode(String encodedEventFlowJson) {
        return CompletableFuture.supplyAsync(() -> {
            DesignModelResponse designModelResponse = new DesignModelResponse();
            try {
                String eventFlowJson =
                        new String(Base64.getDecoder().decode(encodedEventFlowJson), StandardCharsets.UTF_8);
                Gson gson = DeserializersRegisterer.getGsonBuilder().disableHtmlEscaping().create();
                EventFlow eventFlow = gson.fromJson(eventFlowJson, EventFlow.class);
                CodeGenerator codeGenerator = new CodeGenerator();
                String siddhiAppCode = codeGenerator.generateSiddhiAppCode(eventFlow);

                String encodedSiddhiAppString =
                        new String(Base64.getEncoder().encode(siddhiAppCode.getBytes(StandardCharsets.UTF_8)),
                                StandardCharsets.UTF_8);
                designModelResponse.setContent(encodedSiddhiAppString);
            } catch (CodeGenerationException e) {
                log.error("Unable to generate code view", e);
                designModelResponse.setError(e);
            }
            return designModelResponse;
        });
    }
}
