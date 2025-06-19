package io.siddhi.langserver.response;

public class SimulatorResponse {
    private String errorMessage;
    private boolean success;

    public SimulatorResponse(boolean success, String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = success;
    }
}
