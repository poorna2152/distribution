package io.siddhi.langserver;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.carbon.deployment.engine.Artifact;
import org.wso2.carbon.deployment.engine.exception.CarbonDeploymentException;
import org.wso2.carbon.event.simulator.core.internal.generator.EventGenerator;
import org.wso2.carbon.event.simulator.core.internal.util.EventSimulatorConstants;
import org.wso2.carbon.event.simulator.core.service.CSVFileDeployer;
import org.wso2.carbon.event.simulator.core.service.EventSimulatorDataHolder;
import org.wso2.carbon.event.simulator.core.service.SimulationConfigDeployer;
import org.wso2.carbon.streaming.integrator.common.EventStreamService;
import org.wso2.carbon.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class LSEventSimulatorDataHolder {
    private static final String JSON_EXTENSION = ".json";
    private static final long DEFAULT_MAX_FILE_SIZE = 8388608L; // 8MB
    private final String DEPLOYMENT_DIR = Paths.get(Utils.getRuntimePath().toString(), EventSimulatorConstants.DIRECTORY_DEPLOYMENT).toString();
    private final Path SIMULATION_CONFIG_DIR = Paths.get(DEPLOYMENT_DIR, EventSimulatorConstants.DIRECTORY_SIMULATION_CONFIGS);

    private final Map<String, List<String>> siddhiAppSimulationConfigs;
    private final Set<String> activatedSimulationConfigs;
    private final CSVFileDeployer csvFileDeployer;
    private final SimulationConfigDeployer simulationConfigDeployer;
    public static final LSEventSimulatorDataHolder INSTANCE = new LSEventSimulatorDataHolder();

    public LSEventSimulatorDataHolder() {
        this.siddhiAppSimulationConfigs = new HashMap<>();
        this.activatedSimulationConfigs = new HashSet<>();
        this.simulationConfigDeployer = new SimulationConfigDeployer();
        this.csvFileDeployer = new CSVFileDeployer();
    }

    public void initializeEventSimulatorDataHolder(EventStreamService eventStreamService) {
        EventSimulatorDataHolder dataHolder = EventSimulatorDataHolder.getInstance();
        dataHolder.setEventStreamService(eventStreamService);
        dataHolder.setMaximumFileSize(DEFAULT_MAX_FILE_SIZE);
        dataHolder.setCsvFileDirectory(Paths.get(DEPLOYMENT_DIR, EventSimulatorConstants.DIRECTORY_CSV_FILES).toString());
    }

    public void loadSimulationConfigurations() throws IOException {
        File folder = SIMULATION_CONFIG_DIR.toFile();
        File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(JSON_EXTENSION));

        if (jsonFiles == null) {
            return;
        }

        for (File jsonFile : jsonFiles) {
            processSimulationConfigFile(jsonFile);
        }
    }

    public static String getSiddhiAppName(JSONArray sourcesArray) {
        JSONObject sourceObject = sourcesArray.getJSONObject(0);
        return sourceObject.getString("siddhiAppName");
    }

    public void addConfigToSiddhiAppConfigs(String siddhiAppName, String simulationName) {
        siddhiAppSimulationConfigs.computeIfAbsent(siddhiAppName, k -> new ArrayList<>())
                .add(simulationName);
    }

    public void activateSimulationConfig(String simulationName) throws CarbonDeploymentException, IOException {
        if (this.activatedSimulationConfigs.contains(simulationName)) {
            return;
        }
        Path path = SIMULATION_CONFIG_DIR.resolve(simulationName + ".json");
        File configurationFile = new File(path.toUri());
        if (configurationFile.isFile()) {
            deploySimulationConfig(new JSONObject(readFile(configurationFile)));
            this.simulationConfigDeployer.deploy(new Artifact(configurationFile));
        }
        this.activatedSimulationConfigs.add(simulationName);
    }

    public List<JSONObject> getSimulationConfigs(List<String> siddhiApps) throws IOException {
        ArrayList<JSONObject> configs = new ArrayList<>();
        for (String siddhiApp : siddhiApps) {
            if (!this.siddhiAppSimulationConfigs.containsKey(siddhiApp)) {
                continue;
            }
            List<String> configurationNames = this.siddhiAppSimulationConfigs.get(siddhiApp);
            for (String configurationName : configurationNames) {
                File configurationFile = new File(SIMULATION_CONFIG_DIR.resolve(configurationName + ".json").toUri());
                if (configurationFile.isFile()) {
                    configs.add(new JSONObject(readFile(configurationFile)));
                }
            }
        }
        return configs;
    }

    private void processSimulationConfigFile(File jsonFile) throws IOException {
        String content = readFile(jsonFile);
        JSONObject simulationConfig = new JSONObject(content);

        JSONArray sourcesArray = simulationConfig.optJSONArray("sources");
        if (sourcesArray == null || sourcesArray.length() == 0) {
            return;
        }

        JSONObject sourceObject = sourcesArray.getJSONObject(0);
        String siddhiAppName = sourceObject.getString("siddhiAppName");
        String simulationName = simulationConfig.getJSONObject("properties").getString("simulationName");

        siddhiAppSimulationConfigs.computeIfAbsent(siddhiAppName, k -> new ArrayList<>())
                .add(simulationName);
    }

    private String readFile(File file) throws IOException {
        return String.join(System.lineSeparator(), Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
    }

    private void deploySimulationConfig(JSONObject simulationConfiguration) throws CarbonDeploymentException {
        JSONArray sourcesArray = simulationConfiguration.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            JSONObject sourceObject = sourcesArray.getJSONObject(i);
            String simulationType = sourceObject.getString(EventSimulatorConstants.EVENT_SIMULATION_TYPE);
            EventGenerator.GeneratorType generatorType = EventGenerator.GeneratorType.valueOf(simulationType);
            if (generatorType == EventGenerator.GeneratorType.CSV_SIMULATION) {
                this.csvFileDeployer.deploy(new Artifact(new File(sourceObject.getString("fileName"))));
            }
        }
    }

}
