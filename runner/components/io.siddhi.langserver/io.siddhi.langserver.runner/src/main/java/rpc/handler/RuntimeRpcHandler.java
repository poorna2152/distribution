package rpc.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import event.simulator.LSEventStreamService;
import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.deployment.engine.Artifact;
import org.wso2.carbon.deployment.engine.exception.CarbonDeploymentException;
import org.wso2.carbon.event.simulator.core.internal.generator.EventGenerator;
import org.wso2.carbon.event.simulator.core.internal.util.EventSimulatorConstants;
import org.wso2.carbon.event.simulator.core.service.CSVFileDeployer;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorDataHolder;
import org.wso2.carbon.event.simulator.core.service.SimulationConfigDeployer;
import request.StartParams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RuntimeRpcHandler {
    private final SiddhiManager siddhiManager = new SiddhiManager();
    private SiddhiAppRuntime siddhiAppRuntime = null;
    private final Gson gson = new Gson();

    public String handleRequest(JsonObject params, String action, int id) {
        try {
            switch (action) {
                case "start":
                    return handleStart(gson.fromJson(params, StartParams.class), id);
                case "stop":
                    return handleStop(id);
                default:
                    return Utils.createErrorResponse("Method not found", id);
            }
        } catch (JsonParseException e) {
            return Utils.createErrorResponse("Invalid JSON", null);
        }
    }

    private String handleStart(StartParams startParams, int id) {
        if (startParams == null || startParams.path == null) {
            return Utils.createErrorResponse("Missing parameter 'path'", id);
        }

        try {
            String siddhiApp = Files.readString(Paths.get(startParams.path));
            siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);
            siddhiAppRuntime.start();
            EventSimulatorDataHolder eventSimulatorDataHolder = EventSimulatorDataHolder.getInstance();
            eventSimulatorDataHolder.setEventStreamService(new LSEventStreamService(siddhiAppRuntime));

            SimulationConfigDeployer simulationConfigDeployer = new SimulationConfigDeployer();
            File folder = new File(Paths.get(org.wso2.carbon.utils.Utils.getRuntimePath().toString(),
                    EventSimulatorConstants.DIRECTORY_DEPLOYMENT,
                    EventSimulatorConstants.DIRECTORY_SIMULATION_CONFIGS).toUri());
            File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            assert jsonFiles != null;
            for (File jsonFile: jsonFiles) {
                String jsonContent = Files.readString(jsonFile.toPath());
                JSONObject jsonObject = new JSONObject(jsonContent);
                JSONArray sourcesArray = jsonObject.getJSONArray("sources");
                for (int i = 0; i < sourcesArray.length(); i++) {
                    JSONObject sourceObject = sourcesArray.getJSONObject(i);
                    EventGenerator.GeneratorType generatorType = EventGenerator.GeneratorType.valueOf(sourceObject.getString(EventSimulatorConstants.EVENT_SIMULATION_TYPE));
                    if (generatorType == EventGenerator.GeneratorType.CSV_SIMULATION) {
                        CSVFileDeployer csvFileDeployer = new CSVFileDeployer();
                        csvFileDeployer.deploy(new Artifact(new File(sourceObject.getString("fileName"))));
                    }
                }
                simulationConfigDeployer.deploy(new Artifact(jsonFile));
            }
            return Utils.createSuccessResponse("Siddhi app started", id);
        } catch (IOException | CarbonDeploymentException e) {
            return Utils.createErrorResponse("Failed to read Siddhi file: " + e.getMessage(), id);
        }
    }

    private String handleStop(int id) {
        if (siddhiAppRuntime != null) {
            siddhiAppRuntime.shutdown();
            siddhiAppRuntime = null;
            return Utils.createSuccessResponse("Siddhi app stopped", id);
        }
        return Utils.createErrorResponse("No Siddhi app is currently running", id);
    }

}
