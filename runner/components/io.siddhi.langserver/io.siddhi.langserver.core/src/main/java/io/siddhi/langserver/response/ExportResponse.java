package io.siddhi.langserver.response;

public class ExportResponse {
    private String errorMessage;
    private boolean success;

    public ExportResponse(boolean success, String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = success;
    }
}
