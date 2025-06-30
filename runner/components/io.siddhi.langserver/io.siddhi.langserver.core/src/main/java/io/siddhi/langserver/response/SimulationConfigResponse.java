package io.siddhi.langserver.response;


public class SimulationConfigResponse {
    private String result;
    private SimulatorResponse simulatorResponse;

    public SimulationConfigResponse(String jsonConfig, SimulatorResponse simulatorResponse) {
        this.result = jsonConfig;
        this.simulatorResponse = simulatorResponse;
    }
}
