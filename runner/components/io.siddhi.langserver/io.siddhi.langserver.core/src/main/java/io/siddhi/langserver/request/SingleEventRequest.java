package io.siddhi.langserver.request;

public class SingleEventRequest {
    private String singleEventConfig;

    public String getConfiguration() {
        return singleEventConfig;
    }

    public SingleEventRequest(String singleEventConfig) {
        this.singleEventConfig = singleEventConfig;
    }
}
