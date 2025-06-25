package io.siddhi.langserver.extension;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.langserver.request.GetStreamAttributeRequest;
import io.siddhi.langserver.request.GetStreamsRequest;
import io.siddhi.langserver.request.SimulationRequest;
import io.siddhi.langserver.request.SingleEventRequest;
import io.siddhi.langserver.response.SimulatorResponse;
import io.siddhi.langserver.response.StreamDefinitionResponse;
import io.siddhi.langserver.response.StreamResponse;
import io.siddhi.query.api.definition.StreamDefinition;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.wso2.carbon.event.simulator.core.api.*;
import org.wso2.carbon.event.simulator.core.exception.FileOperationsException;
import org.wso2.carbon.event.simulator.core.exception.InvalidConfigException;
import org.wso2.carbon.event.simulator.core.exception.SimulationValidationException;
import org.wso2.carbon.event.simulator.core.factories.DatabaseApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.FeedApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.FilesApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.SingleApiServiceFactory;
import org.wso2.carbon.event.simulator.core.impl.DatabaseApiServiceImpl;
import org.wso2.carbon.event.simulator.core.impl.FeedApiServiceImpl;
import org.wso2.carbon.event.simulator.core.impl.FilesApiServiceImpl;
import org.wso2.carbon.event.simulator.core.internal.util.EventSimulatorConstants;
import org.wso2.carbon.event.simulator.core.internal.util.SimulationConfigUploader;
import org.wso2.carbon.event.simulator.core.model.DBConnectionModel;
import org.wso2.carbon.event.simulator.core.service.EventSimulator;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorDataHolder;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorMap;
import org.wso2.carbon.event.simulator.core.service.bean.ActiveSimulatorData;
import org.wso2.carbon.event.simulator.core.service.bean.ResourceDependencyData;
import org.wso2.carbon.siddhi.editor.core.internal.EditorDataHolder;
import org.wso2.carbon.streaming.integrator.core.services.PublicCarbonEventStreamService;
import org.wso2.carbon.streaming.integrator.core.services.PublicStreamProcessorDataHolder;
import org.wso2.carbon.streaming.integrator.core.services.PublicStreamProcessorService;
import org.wso2.carbon.utils.Utils;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.formparam.FileInfo;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@JsonSegment("eventSimulator")
public class EventSimulatorService extends ExtensionService {
    private final FilesApiServiceImpl filesApiService = (FilesApiServiceImpl)FilesApiServiceFactory.getFilesApi();
    private final SingleApiService singleApiService = SingleApiServiceFactory.getSingleApi();
    private final DatabaseApiServiceImpl databaseApiService = (DatabaseApiServiceImpl)DatabaseApiServiceFactory.getConnectToDatabaseApi();
    private final FeedApiServiceImpl feedApi = (FeedApiServiceImpl)FeedApiServiceFactory.getFeedApi();

    @JsonRequest
    public CompletableFuture<StreamResponse> getStreams(GetStreamsRequest getStreamsRequest) {
        return CompletableFuture.supplyAsync(() -> {
            StreamResponse streamResponse = new StreamResponse();
            EditorDataHolder.setSiddhiManager(new SiddhiManager());
            SiddhiManager siddhiManager = new SiddhiManager();
            String siddhiAppString = new String(Base64.getDecoder().decode(getStreamsRequest.getSiddhiApp()), StandardCharsets.UTF_8);
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiAppString);
            Map<String, StreamDefinition> streamDefinitionMap = siddhiAppRuntime.getStreamDefinitionMap();
            streamDefinitionMap.forEach((key, value) -> {
                streamResponse.addStream(key);
            });
            return streamResponse;
        });
    }

    @JsonRequest
    public CompletableFuture<StreamDefinitionResponse> getStreamAttributes(GetStreamAttributeRequest getStreamAttributeRequest) {
        return CompletableFuture.supplyAsync(() -> {
            EditorDataHolder.setSiddhiManager(new SiddhiManager());
            SiddhiManager siddhiManager = new SiddhiManager();
            String siddhiAppString = new String(Base64.getDecoder().decode(getStreamAttributeRequest.getSiddhiAppString()), StandardCharsets.UTF_8);
            String streamName = getStreamAttributeRequest.getStreamName();
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiAppString);
            StreamDefinition streamDefinition = siddhiAppRuntime.getStreamDefinitionMap().get(streamName);
            return new StreamDefinitionResponse(streamDefinition.getAttributeList(), streamName);
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> deleteFile(String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                filesApiService.deleteFile(fileName);
                return new SimulatorResponse(true, "");
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, e.getMessage());
            } catch (FileOperationsException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> getFileNames(String fileName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                filesApiService.getFileNames();
                return new SimulatorResponse(true, "");
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> updateFile(String fileName, InputStream fileInputStream, FileInfo fileDetail) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                filesApiService.updateFile(fileName, fileInputStream, fileDetail);
                return new SimulatorResponse(true, "");
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, e.getMessage());
            } catch (FileOperationsException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> uploadFile(InputStream fileInputStream, FileInfo fileDetail) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                filesApiService.uploadFile(fileInputStream, fileDetail);
                return new SimulatorResponse(true, "");
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, e.getMessage());
            } catch (FileOperationsException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @JsonRequest
    public CompletableFuture<SimulatorResponse> getDatabaseTableColumns(DBConnectionModel body, String tableName, Request request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Replace with actual service call
                databaseApiService.getDatabaseTableColumns(body, tableName, request);
                return new SimulatorResponse(true, "");
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> getDatabaseTables(DBConnectionModel body, Request request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Replace with actual service call
                databaseApiService.getDatabaseTables(body, request);
                return new SimulatorResponse(true, "");
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> testDBConnection(DBConnectionModel body, Request request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Replace with actual service call
                databaseApiService.testDBConnection(body, request);
                return new SimulatorResponse(true, "");
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }
}
