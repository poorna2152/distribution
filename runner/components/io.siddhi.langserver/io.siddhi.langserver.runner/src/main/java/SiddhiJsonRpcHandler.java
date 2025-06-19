import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import event.simulator.LSEventStreamService;
import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.distribution.event.simulator.core.api.NotFoundException;
import io.siddhi.distribution.event.simulator.core.api.SingleApiService;
import io.siddhi.distribution.event.simulator.core.factories.FeedApiServiceFactory;
import io.siddhi.distribution.event.simulator.core.factories.SingleApiServiceFactory;
import io.siddhi.distribution.event.simulator.core.impl.FeedApiServiceImpl;
import io.siddhi.distribution.event.simulator.core.service.EventSimulatorDataHolder;
import request.FeedSimulationParams;
import request.SingleEventParams;
import request.StartParams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SiddhiJsonRpcHandler {

    private final SiddhiManager siddhiManager = new SiddhiManager();
    private SiddhiAppRuntime siddhiAppRuntime = null;
    private final Gson gson = new Gson();

    public String handleRequest(String jsonRequest) {
        try {
            JsonObject request = gson.fromJson(jsonRequest, JsonObject.class);
            String method = request.get("method").getAsString();
            JsonObject params = request.getAsJsonObject("params");
            int id = request.get("id").getAsInt();

            switch (method) {
                case "start":
                    return handleStart(gson.fromJson(params, StartParams.class), id);
                case "stop":
                    return handleStop(id);
                case "singleEvent":
                    return handleEventSimulation(gson.fromJson(params, SingleEventParams.class), id);
                case "feedSimulation":
                    return handleFeedEventSimulation(gson.fromJson(params, FeedSimulationParams.class), id);
                default:
                    return createErrorResponse("Method not found", id);
            }
        } catch (JsonParseException e) {
            return createErrorResponse("Invalid JSON", null);
        }
    }

    private String handleStart(StartParams startParams, int id) {
        if (startParams == null || startParams.path == null) {
            return createErrorResponse("Missing parameter 'path'", id);
        }

        try {
            String siddhiApp = Files.readString(Paths.get(startParams.path));
            siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);
            siddhiAppRuntime.start();
            EventSimulatorDataHolder eventSimulatorDataHolder = EventSimulatorDataHolder.getInstance();
            eventSimulatorDataHolder.setEventStreamService(new LSEventStreamService(siddhiAppRuntime));
            return createSuccessResponse("Siddhi app started", id);
        } catch (IOException e) {
            return createErrorResponse("Failed to read Siddhi file: " + e.getMessage(), id);
        }
    }

    private String handleStop(int id) {
        if (siddhiAppRuntime != null) {
            siddhiAppRuntime.shutdown();
            siddhiAppRuntime = null;
            return createSuccessResponse("Siddhi app stopped", id);
        }
        return createErrorResponse("No Siddhi app is currently running", id);
    }

    private String handleEventSimulation(SingleEventParams singleEventParams, int id) {
        SingleApiService singleApiService = SingleApiServiceFactory.getSingleApi();
        try {
            singleApiService.runSingleSimulation(singleEventParams.body);
            return createSuccessResponse("Simulation completed", 1);
        } catch (NotFoundException e) {
            return createErrorResponse(e.getMessage(), id);
        }
    }

    private String handleFeedEventSimulation(FeedSimulationParams feedSimulationParams, int id) {
        FeedApiServiceImpl feedApiService = (FeedApiServiceImpl)FeedApiServiceFactory.getFeedApi();
        try {
            feedApiService.operateFeedSimulation(feedSimulationParams.action, feedSimulationParams.simulationName);
            return createSuccessResponse("Simulation completed", id);
        } catch (NotFoundException e) {
            return createErrorResponse(e.getMessage(), id);
        }
    }

    private String createSuccessResponse(String result, Integer id) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", id);
        response.addProperty("result", result);
        return gson.toJson(response);
    }

    private String createErrorResponse(String message, Integer id) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (id != null) {
            response.addProperty("id", id);
        } else {
            response.add("id", null);
        }
        JsonObject error = new JsonObject();
        error.addProperty("code", -32603);
        error.addProperty("message", message);
        response.add("error", error);
        return gson.toJson(response);
    }
}
