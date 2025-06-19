package io.siddhi.langserver.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.langserver.request.Base64String;
import io.siddhi.langserver.response.DesignModelResponse;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.wso2.carbon.siddhi.editor.core.commons.metadata.MetaData;
import org.wso2.carbon.siddhi.editor.core.commons.response.MetaDataResponse;
import org.wso2.carbon.siddhi.editor.core.commons.response.Status;
import org.wso2.carbon.siddhi.editor.core.internal.EditorDataHolder;
import org.wso2.carbon.siddhi.editor.core.util.SourceEditorUtils;
import org.wso2.carbon.siddhi.editor.core.util.designview.beans.EventFlow;
import org.wso2.carbon.siddhi.editor.core.util.designview.codegenerator.CodeGenerator;
import org.wso2.carbon.siddhi.editor.core.util.designview.deserializers.DeserializersRegisterer;
import org.wso2.carbon.siddhi.editor.core.util.designview.designgenerator.DesignGenerator;
import org.wso2.carbon.siddhi.editor.core.util.designview.exceptions.CodeGenerationException;
import org.wso2.carbon.siddhi.editor.core.util.designview.exceptions.DesignGenerationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@JsonSegment("flowDesignService")
public class DesignModelGeneratorService extends ExtensionService {


    @JsonRequest
    public CompletableFuture<DesignModelResponse> getDesignView(Base64String siddhiAppBase64) {
        return CompletableFuture.supplyAsync(() -> {
            DesignModelResponse designModelResponse = new DesignModelResponse();
            try {
                SiddhiManager siddhiManager = new SiddhiManager();
                DesignGenerator designGenerator = new DesignGenerator();
                designGenerator.setSiddhiManager(siddhiManager);
                String siddhiAppString = new String(Base64.getDecoder().decode(siddhiAppBase64.getValue()), StandardCharsets.UTF_8);
                EventFlow eventFlow = designGenerator.getEventFlow(siddhiAppString);
                Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                String eventFlowJson = gson.toJson(eventFlow);
                byte[] encodedBytes = Base64.getEncoder().encode(eventFlowJson.getBytes(StandardCharsets.UTF_8));
                String encodedString = new String(encodedBytes, StandardCharsets.UTF_8);
                designModelResponse.setContent(encodedString);
                return designModelResponse;
            } catch (SiddhiAppCreationException e) {
                designModelResponse.setError(e);
            } catch (DesignGenerationException e) {
                designModelResponse.setError(e);
            }
            return designModelResponse;
        });
    }

    @JsonRequest
    public CompletableFuture<DesignModelResponse> getSourceCode(Base64String encodedEventFlowJson) {
        return CompletableFuture.supplyAsync(() -> {
            DesignModelResponse designModelResponse = new DesignModelResponse();
            try {
                String eventFlowJson =
                        new String(Base64.getDecoder().decode(encodedEventFlowJson.getValue()), StandardCharsets.UTF_8);
                Gson gson = DeserializersRegisterer.getGsonBuilder().disableHtmlEscaping().create();
                EventFlow eventFlow = gson.fromJson(eventFlowJson, EventFlow.class);
                CodeGenerator codeGenerator = new CodeGenerator();
                String siddhiAppCode = codeGenerator.generateSiddhiAppCode(eventFlow);

                String encodedSiddhiAppString =
                        new String(Base64.getEncoder().encode(siddhiAppCode.getBytes(StandardCharsets.UTF_8)),
                                StandardCharsets.UTF_8);
                designModelResponse.setContent(encodedSiddhiAppString);
            } catch (CodeGenerationException e) {
                designModelResponse.setError(e);
            }
            return designModelResponse;
        });
    }

    @JsonRequest
    public CompletableFuture<DesignModelResponse> getMetaData() {
        return CompletableFuture.supplyAsync(() -> {
            DesignModelResponse designModelResponse = new DesignModelResponse();
            EditorDataHolder.setSiddhiManager(new SiddhiManager());
            MetaDataResponse response = new MetaDataResponse(Status.SUCCESS);
            Map<String, MetaData> extensions = SourceEditorUtils.getExtensionProcessorMetaData();
            response.setInBuilt(extensions.remove(""));
            response.setExtensions(extensions);
            String jsonString = new Gson().toJson(response);
            designModelResponse.setContent(jsonString);
            return designModelResponse;
        });
    }
}
