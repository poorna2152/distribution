package io.siddhi.langserver.extension;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.langserver.LSEventSimulatorDataHolder;
import io.siddhi.langserver.LSEventStreamService;
import io.siddhi.langserver.LSOperationContext;
import io.siddhi.langserver.request.*;
import io.siddhi.langserver.response.SimulationConfigResponse;
import io.siddhi.langserver.response.SimulatorResponse;
import io.siddhi.langserver.response.StreamDefinitionResponse;
import io.siddhi.langserver.response.StreamResponse;
import io.siddhi.query.api.definition.StreamDefinition;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.json.JSONObject;
import org.wso2.carbon.deployment.engine.exception.CarbonDeploymentException;
import org.wso2.carbon.event.simulator.core.api.*;
import org.wso2.carbon.event.simulator.core.exception.FileOperationsException;
import org.wso2.carbon.event.simulator.core.exception.SimulationValidationException;
import org.wso2.carbon.event.simulator.core.factories.DatabaseApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.FeedApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.FilesApiServiceFactory;
import org.wso2.carbon.event.simulator.core.impl.DatabaseApiServiceImpl;
import org.wso2.carbon.event.simulator.core.impl.FeedApiServiceImpl;
import org.wso2.carbon.event.simulator.core.impl.FilesApiServiceImpl;
import org.wso2.carbon.event.simulator.core.internal.util.SimulationConfigUploader;
import org.wso2.carbon.event.simulator.core.model.DBConnectionModel;
import org.wso2.carbon.streaming.integrator.common.exception.ResponseMapper;
import org.wso2.msf4j.formparam.FileInfo;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@JsonSegment("eventSimulator")
public class EventSimulatorService extends ExtensionService {
    private final FilesApiServiceImpl filesApiService = (FilesApiServiceImpl) FilesApiServiceFactory.getFilesApi();
    private final DatabaseApiServiceImpl databaseApiService = (DatabaseApiServiceImpl) DatabaseApiServiceFactory.getConnectToDatabaseApi();
    private final FeedApiServiceImpl feedApi = (FeedApiServiceImpl) FeedApiServiceFactory.getFeedApi();

    public EventSimulatorService() {
        try {
            LSEventSimulatorDataHolder.INSTANCE.initialize(new LSEventStreamService());
        } catch (CarbonDeploymentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonRequest
    public CompletableFuture<StreamResponse> getStreams(StreamsGetRequest getStreamsRequest) {
        return CompletableFuture.supplyAsync(() -> {
            SiddhiAppRuntime siddhiAppRuntime = getSiddhiAppRuntime(getStreamsRequest.getSiddhiAppUri(), getStreamsRequest.getSiddhiApp());
            Map<String, StreamDefinition> streamDefinitionMap = siddhiAppRuntime.getStreamDefinitionMap();
            StreamResponse streamResponse = new StreamResponse();
            streamDefinitionMap.forEach((key, value) -> {
                streamResponse.addStream(key);
            });
            return streamResponse;
        });
    }

    @JsonRequest
    public CompletableFuture<StreamDefinitionResponse> getStreamAttributes(StreamAttributeGetRequest getStreamAttributeRequest) {
        return CompletableFuture.supplyAsync(() -> {
            SiddhiAppRuntime siddhiAppRuntime = getSiddhiAppRuntime(getStreamAttributeRequest.getSiddhiAppUri(), getStreamAttributeRequest.getSiddhiAppString());
            String streamName = getStreamAttributeRequest.getStreamName();
            StreamDefinition streamDefinition = siddhiAppRuntime.getStreamDefinitionMap().get(streamName);
            return new StreamDefinitionResponse(streamDefinition.getAttributeList(), streamName);
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> addFeedSimulation(SimulationAddRequest simulationAddRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String simulationConfig = simulationAddRequest.simulationConfig;
                Response response = feedApi.addFeedSimulation(simulationConfig);
                String simulationName = SimulationConfigUploader.getConfigUploader().getSimulationName(simulationConfig);
                String siddhiAppName = LSEventSimulatorDataHolder.getSiddhiAppName((new JSONObject(simulationConfig)).getJSONArray("sources"));
                LSEventSimulatorDataHolder.INSTANCE.addConfigToSiddhiAppConfigs(siddhiAppName, simulationName);
                return buildSimulatorResponse(response);
            } catch (NotFoundException | SimulationValidationException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulationConfigResponse> getFeedSimulation(SimulationIdRequest simulationIdRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Response feedSimulation = feedApi.getFeedSimulation(simulationIdRequest.simulationName);
                ResponseMapper entity = (ResponseMapper) feedSimulation.getEntity();
                return new SimulationConfigResponse(entity.getMessage(), buildSimulatorResponse(feedSimulation));
            } catch (NotFoundException e) {
                return new SimulationConfigResponse(null, new SimulatorResponse(false, e.getMessage()));
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulationConfigResponse> getFeedSimulations(SimulationsGetRequest simulationsGetRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<JSONObject> activeSimulations = LSEventSimulatorDataHolder.INSTANCE.getSimulationConfigs(simulationsGetRequest.getSiddhiApps());
                JSONObject response = new JSONObject();
                response.put("activeSimulations", activeSimulations);
                response.put("inactiveSimulations", Collections.emptyList());
                return new SimulationConfigResponse(response.toString(), new SimulatorResponse(true, ""));
            } catch (IOException e) {
                return new SimulationConfigResponse(null, new SimulatorResponse(false, e.getMessage()));
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> updateFeedSimulation(SimulationUpdateRequest simulationUpdateRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return buildSimulatorResponse(feedApi.updateFeedSimulation(simulationUpdateRequest.simulationName, simulationUpdateRequest.simulationConfig));
            } catch (NotFoundException | FileOperationsException e) {
                return new SimulatorResponse(false, "");
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> deleteFeedSimulation(SimulationIdRequest simulationIdRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return buildSimulatorResponse(feedApi.deleteFeedSimulation(simulationIdRequest.simulationName));
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, "");
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulationConfigResponse> getFeedSimulationStatus(SimulationIdRequest simulationIdRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Response response = feedApi.getFeedSimulationStatus(simulationIdRequest.simulationName);
                ResponseMapper entity = (ResponseMapper) response.getEntity();
                return new SimulationConfigResponse(entity.getMessage(), buildSimulatorResponse(response));
            } catch (NotFoundException e) {
                return new SimulationConfigResponse(null, new SimulatorResponse(false, e.getMessage()));
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> uploadFile(FileInfo fileDetail) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path fileAbsolutePath = Paths.get(fileDetail.getFileName());
                fileDetail.setFileName(fileAbsolutePath.getFileName().toString());
                return buildSimulatorResponse(filesApiService.uploadFile(Files.newInputStream(fileAbsolutePath), fileDetail));
            } catch (NotFoundException | IOException | FileOperationsException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulationConfigResponse> getFileNames() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Response response = filesApiService.getFileNames();
                return new SimulationConfigResponse((response.getEntity()).toString(), buildSimulatorResponse(response));
            } catch (NotFoundException e) {
                return new SimulationConfigResponse(null, new SimulatorResponse(false, e.getMessage()));
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> updateFile(FileInfo fileDetail) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String fileName = fileDetail.getFileName();
                return buildSimulatorResponse(filesApiService.updateFile(fileName, Files.newInputStream(Paths.get(fileName)), fileDetail));
            } catch (NotFoundException | FileOperationsException | IOException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> deleteFile(DeleteFileRequest deleteFileRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return buildSimulatorResponse(filesApiService.deleteFile(deleteFileRequest.fileName));
            } catch (NotFoundException | FileOperationsException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulatorResponse> testDBConnection(DBConnectionModel dbConnectionModel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return buildSimulatorResponse(databaseApiService.testDBConnection(dbConnectionModel));
            } catch (NotFoundException e) {
                return new SimulatorResponse(false, e.getMessage());
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulationConfigResponse> getDatabaseTableColumns(DBColumnGetRequest dbColumnGetRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DBConnectionModel dbConnectionModel = new DBConnectionModel().password(dbColumnGetRequest.password).
                        username(dbColumnGetRequest.username).driver(dbColumnGetRequest.driver)
                        .dataSourceLocation(dbColumnGetRequest.dataSourceLocation);
                Response response = databaseApiService.getDatabaseTableColumns(dbConnectionModel, dbColumnGetRequest.tableName);
                return new SimulationConfigResponse(response.getEntity().toString(), buildSimulatorResponse(response));
            } catch (NotFoundException e) {
                return new SimulationConfigResponse(null, new SimulatorResponse(false, e.getMessage()));
            }
        });
    }

    @JsonRequest
    public CompletableFuture<SimulationConfigResponse> getDatabaseTables(DBConnectionModel dbConnectionModel) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Response response = databaseApiService.getDatabaseTables(dbConnectionModel);
                return new SimulationConfigResponse(response.getEntity().toString(), buildSimulatorResponse(response));
            } catch (NotFoundException e) {
                return new SimulationConfigResponse(null, new SimulatorResponse(false, e.getMessage()));
            }
        });
    }

    private SimulatorResponse buildSimulatorResponse(Response response) {
        int statusCode = response.getStatus();
        boolean isSuccess = statusCode >= 200 && statusCode < 300;

        String message;
        Object entity = response.getEntity();

        if (entity instanceof ResponseMapper) {
            message = ((ResponseMapper) entity).getMessage();
        } else if (entity != null) {
            message = entity.toString();
        } else {
            message = isSuccess ? "Operation successful." : "Unknown error occurred.";
        }

        return new SimulatorResponse(isSuccess, message);
    }

    private SiddhiAppRuntime getSiddhiAppRuntime(String siddhiAppUri, String encodedSiddhiApp) {
        String siddhiAppName = Paths.get(siddhiAppUri).getFileName().toString();
        if (LSOperationContext.INSTANCE.checkIfSiddhiAppRuntimeExists(siddhiAppName)) {
            return LSOperationContext.INSTANCE.getSiddhiAppRuntime(siddhiAppName);
        }
        String siddhiAppString = new String(Base64.getDecoder().decode(encodedSiddhiApp), StandardCharsets.UTF_8);
        SiddhiAppRuntime siddhiAppRuntime = LSOperationContext.INSTANCE.getSiddhiManager().createSiddhiAppRuntime(siddhiAppString);
        LSOperationContext.INSTANCE.addSiddhiAppRuntime(siddhiAppName, siddhiAppRuntime);
        return siddhiAppRuntime;
    }
}
