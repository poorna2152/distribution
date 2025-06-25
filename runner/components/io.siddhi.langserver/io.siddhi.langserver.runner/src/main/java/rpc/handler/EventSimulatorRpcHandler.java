package rpc.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.siddhi.langserver.request.SimulationRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.deployment.engine.Artifact;
import org.wso2.carbon.deployment.engine.exception.CarbonDeploymentException;
import org.wso2.carbon.event.simulator.core.api.NotFoundException;
import org.wso2.carbon.event.simulator.core.api.SingleApiService;
import org.wso2.carbon.event.simulator.core.exception.FileOperationsException;
import org.wso2.carbon.event.simulator.core.exception.SimulationValidationException;
import org.wso2.carbon.event.simulator.core.factories.DatabaseApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.FeedApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.FilesApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.SingleApiServiceFactory;
import org.wso2.carbon.event.simulator.core.impl.DatabaseApiServiceImpl;
import org.wso2.carbon.event.simulator.core.impl.FeedApiServiceImpl;
import org.wso2.carbon.event.simulator.core.impl.FilesApiServiceImpl;
import org.wso2.carbon.event.simulator.core.internal.generator.EventGenerator;
import org.wso2.carbon.event.simulator.core.internal.util.EventSimulatorConstants;
import org.wso2.carbon.event.simulator.core.internal.util.SimulationConfigUploader;
import org.wso2.carbon.event.simulator.core.model.DBConnectionModel;
import org.wso2.carbon.event.simulator.core.service.CSVFileDeployer;
import org.wso2.carbon.event.simulator.core.service.EventSimulator;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorMap;
import org.wso2.carbon.event.simulator.core.service.bean.ActiveSimulatorData;
import org.wso2.carbon.streaming.integrator.common.exception.ResponseMapper;
import org.wso2.msf4j.formparam.FileInfo;
import request.*;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class EventSimulatorRpcHandler {
    private final Gson gson = new Gson();
    private final FeedApiServiceImpl feedApi = (FeedApiServiceImpl) FeedApiServiceFactory.getFeedApi();
    private final FilesApiServiceImpl filesApiService = (FilesApiServiceImpl) FilesApiServiceFactory.getFilesApi();
    private final DatabaseApiServiceImpl databaseApiService = (DatabaseApiServiceImpl) DatabaseApiServiceFactory.getConnectToDatabaseApi();

    public String handleRequest(JsonObject params, String method, int id) {
        try {
            switch (method) {
                case "singleEvent":
                    return simulateSingleEvent(gson.fromJson(params, SingleEventParams.class), id);
                case "feedSimulation":
                    return simulateEventFeed(gson.fromJson(params, FeedSimulationParams.class), id);
                case "addFeedSimulation":
                    return addFeedSimulation(gson.fromJson(params, SimulationRequest.class), id);
                case "deleteFeedSimulation":
                    return deleteFeedSimulation(gson.fromJson(params, SimulationUpdateParam.class), id);
                case "getFeedSimulation":
                    return getFeedSimulation(gson.fromJson(params, SimulationUpdateParam.class), id);
                case "getFeedSimulations":
                    return getFeedSimulations(id);
                case "updateFeedSimulation":
                    return updateFeedSimulation(gson.fromJson(params, SimulationUpdateParam.class), id);
                case "getFeedSimulationStatus":
                    return getFeedSimulationStatus(gson.fromJson(params, SimulationUpdateParam.class), id);
                case "testDBConnection":
                    return testDBConnection(gson.fromJson(params, DBConnectionModel.class), id);
                case "getDatabaseTables":
                    return getDatabaseTables(gson.fromJson(params, DBConnectionModel.class), id);
                case "getDatabaseTableColumns":
                    return getDatabaseTableColumns(gson.fromJson(params, DBTableParam.class), id);
                case "uploadFile":
                    return uploadFile(gson.fromJson(params, FileInfo.class), id);
                case "updateFile":
                    return updateFile(gson.fromJson(params, FileInfo.class), id);
                case "deleteFile":
                    return deleteFile(gson.fromJson(params, DeleteFileParam.class), id);
                case "getFileNames":
                    return getFileNames(id);
                default:
                    return Utils.createErrorResponse("Method not found", id);
            }
        } catch (JsonParseException e) {
            return Utils.createErrorResponse("Invalid JSON", null);
        }
    }

    private String simulateSingleEvent(SingleEventParams singleEventParams, int id) {
        SingleApiService singleApiService = SingleApiServiceFactory.getSingleApi();
        try {
            singleApiService.runSingleSimulation(singleEventParams.body);
            return Utils.createSuccessResponse("Simulation completed", id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    private String simulateEventFeed(FeedSimulationParams feedSimulationParams, int id) {
        FeedApiServiceImpl feedApiService = (FeedApiServiceImpl) FeedApiServiceFactory.getFeedApi();
        try {
            feedApiService.operateFeedSimulation(feedSimulationParams.action, feedSimulationParams.simulationName);
            return Utils.createSuccessResponse("Simulation completed", id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    private String addFeedSimulation(SimulationRequest simulationRequest, int id) {
        try {
            String simulationConfig = simulationRequest.simulationConfig;
            feedApi.addFeedSimulation(simulationConfig);
            String simulationName = SimulationConfigUploader.getConfigUploader().getSimulationName(simulationConfig);
            JSONObject jsonObject = new JSONObject(simulationRequest.simulationConfig);
            JSONArray sourcesArray = jsonObject.getJSONArray("sources");
            for (int i = 0; i < sourcesArray.length(); i++) {
                JSONObject sourceObject = sourcesArray.getJSONObject(i);
                EventGenerator.GeneratorType generatorType = EventGenerator.GeneratorType.valueOf(sourceObject.getString(EventSimulatorConstants.EVENT_SIMULATION_TYPE));
                if (generatorType == EventGenerator.GeneratorType.CSV_SIMULATION) {
                    CSVFileDeployer csvFileDeployer = new CSVFileDeployer();
                    csvFileDeployer.deploy(new Artifact(new File(sourceObject.getString("fileName"))));
                }
            }
            EventSimulator eventSimulator = new EventSimulator(simulationName, simulationConfig, false);
            EventSimulatorMap.getInstance().getActiveSimulatorMap().put(simulationName, new ActiveSimulatorData(eventSimulator, simulationConfig));
            JsonObject response = new JsonObject();
            response.addProperty("message", "Successfully uploaded simulation configuration" + simulationName);
            response.addProperty("status", "CREATED");
            return Utils.createSuccessResponse(response.toString(), id);
        } catch (NotFoundException | SimulationValidationException | CarbonDeploymentException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    public String deleteFeedSimulation(SimulationUpdateParam simulationUpdateParam, int id) {
        try {
            feedApi.deleteFeedSimulation(simulationUpdateParam.simulationName);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
        return Utils.createSuccessResponse("Feed Simulation deleted successfully", id);
    }

    public String getFeedSimulation(SimulationUpdateParam simulationUpdateParam, int id) {
        try {
            Response feedSimulation = feedApi.getFeedSimulation(simulationUpdateParam.simulationName);
            ResponseMapper entity = (ResponseMapper) feedSimulation.getEntity();
            return Utils.createSuccessResponse(entity.getMessage(), id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    public String getFeedSimulations(int id) {
        try {
            Response feedSimulation = feedApi.getFeedSimulations();
            ResponseMapper entity = (ResponseMapper) feedSimulation.getEntity();
            return Utils.createSuccessResponse(entity.getMessage(), id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    public String updateFeedSimulation(SimulationUpdateParam simulationUpdateParam, int id) {
        try {
            feedApi.updateFeedSimulation(simulationUpdateParam.simulationName, simulationUpdateParam.body);
            return Utils.createSuccessResponse("Feed simulation updated succesfully", id);
        } catch (NotFoundException | FileOperationsException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    public String getFeedSimulationStatus(SimulationUpdateParam simulationUpdateParam, int id) {
        try {
            Response response = feedApi.getFeedSimulationStatus(simulationUpdateParam.simulationName);
            ResponseMapper entity = (ResponseMapper) response.getEntity();
            return Utils.createSuccessResponse(entity.getMessage(), id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    private String deleteFile(DeleteFileParam deleteFileParam, int id) {
        try {
            filesApiService.deleteFile(deleteFileParam.fileName);
            return Utils.createSuccessResponse("File deleted", id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        } catch (FileOperationsException e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileNames(int id) {
        try {
            Response response = filesApiService.getFileNames();
            return Utils.createSuccessResponse((response.getEntity()).toString(), id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    private String updateFile(FileInfo fileDetail, int id) {
        try {
            filesApiService.updateFile(fileDetail.getFileName(), Files.newInputStream(Paths.get(fileDetail.getFileName())), fileDetail);
            return Utils.createSuccessResponse("File updated", id);
        } catch (NotFoundException | FileOperationsException | IOException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    private String uploadFile(FileInfo fileDetail, int id) {
        try {
            Path fileAbsolutePath = Paths.get(fileDetail.getFileName());
            fileDetail.setFileName(fileAbsolutePath.getFileName().toString());
            filesApiService.uploadFile(Files.newInputStream(fileAbsolutePath), fileDetail);
            return Utils.createSuccessResponse("File uploaded", id);
        } catch (NotFoundException | IOException | FileOperationsException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    private String testDBConnection(DBConnectionModel dbConnectionModel, int id) {
        try {
            databaseApiService.testDBConnection(dbConnectionModel);
            return Utils.createSuccessResponse("Connection successful", id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    private String getDatabaseTables(DBConnectionModel body, int id) {
        try {
            Response response = databaseApiService.getDatabaseTables(body);
            return Utils.createSuccessResponse((response.getEntity()).toString(), id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

    private String getDatabaseTableColumns(DBTableParam dbTableParam, int id) {
        try {
            DBConnectionModel dbConnectionModel = new DBConnectionModel();
            dbConnectionModel = dbConnectionModel.password(dbTableParam.password);
            dbConnectionModel = dbConnectionModel.username(dbTableParam.username);
            dbConnectionModel = dbConnectionModel.driver(dbTableParam.driver);
            dbConnectionModel = dbConnectionModel.dataSourceLocation(dbTableParam.dataSourceLocation);
            Response response = databaseApiService.getDatabaseTableColumns(dbConnectionModel, dbTableParam.tableName);
            return Utils.createSuccessResponse((response.getEntity()).toString(), id);
        } catch (NotFoundException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }

}
