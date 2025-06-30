package io.siddhi.langserver.request;

import java.util.List;

public class SimulationsGetRequest {
    private final List<String> siddhiApps;

    public SimulationsGetRequest(List<String> siddhiApps) {
        this.siddhiApps = siddhiApps;
    }

    public List<String> getSiddhiApps() {
        return siddhiApps;
    }
}
