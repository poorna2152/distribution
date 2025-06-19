package io.siddhi.langserver.request;

public class StartAppRequest {
    private String siddhiApp;
    private String path;

    public StartAppRequest(String siddhiApp, String path) {
        this.siddhiApp = siddhiApp;
        this.path = path;
    }

    public String getSiddhiApp() {
        return siddhiApp;
    }

    public String getPath() {
        return path;
    }
}
