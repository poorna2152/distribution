package io.siddhi.langserver.request;

public class GetStreamsRequest {
    private String siddhiApp;

    public GetStreamsRequest(String siddhiApp) {
        this.siddhiApp = siddhiApp;
    }

    public String getSiddhiApp() {
        return siddhiApp;
    }
}
