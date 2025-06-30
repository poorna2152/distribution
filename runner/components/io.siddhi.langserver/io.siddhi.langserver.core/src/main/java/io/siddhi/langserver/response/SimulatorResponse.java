package io.siddhi.langserver.response;

public class SimulatorResponse {
    private String message;
    private boolean success;

    public SimulatorResponse(boolean success, String  message) {
        this.message = message;
        this.success = success;
    }
}
