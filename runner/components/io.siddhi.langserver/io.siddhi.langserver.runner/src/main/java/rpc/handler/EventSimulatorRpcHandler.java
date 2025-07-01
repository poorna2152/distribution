package rpc.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.siddhi.langserver.LSEventSimulatorDataHolder;
import org.wso2.carbon.deployment.engine.exception.CarbonDeploymentException;
import org.wso2.carbon.event.simulator.core.api.NotFoundException;
import org.wso2.carbon.event.simulator.core.api.SingleApiService;
import org.wso2.carbon.event.simulator.core.factories.FeedApiServiceFactory;
import org.wso2.carbon.event.simulator.core.factories.SingleApiServiceFactory;
import org.wso2.carbon.event.simulator.core.impl.FeedApiServiceImpl;
import request.*;

import java.io.IOException;


public class EventSimulatorRpcHandler {
    private final Gson gson = new Gson();

    public String handleRequest(JsonObject params, String method, int id) {
        try {
            switch (method) {
                case "singleEvent":
                    return simulateSingleEvent(gson.fromJson(params, SingleEventParams.class), id);
                case "feedSimulation":
                    return simulateEventFeed(gson.fromJson(params, FeedSimulationParams.class), id);
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
            LSEventSimulatorDataHolder.INSTANCE.activateSimulationConfig(feedSimulationParams.simulationName);
            feedApiService.operateFeedSimulation(feedSimulationParams.action, feedSimulationParams.simulationName);
            return Utils.createSuccessResponse("Simulation completed", id);
        } catch (NotFoundException | CarbonDeploymentException | IOException e) {
            return Utils.createErrorResponse(e.getMessage(), id);
        }
    }
}
