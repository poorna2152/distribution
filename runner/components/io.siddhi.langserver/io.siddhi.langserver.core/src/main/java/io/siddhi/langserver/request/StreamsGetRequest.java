package io.siddhi.langserver.request;

public class StreamsGetRequest {
    private String siddhiApp;
    private String siddhiAppUri;


    public StreamsGetRequest(String siddhiApp, String siddhiAppUri) {
        this.siddhiApp = siddhiApp;
        this.siddhiAppUri = siddhiAppUri;
    }

    public String getSiddhiAppUri() {
        return siddhiAppUri;
    }

    public String getSiddhiApp() {
        return siddhiApp;
    }
}
