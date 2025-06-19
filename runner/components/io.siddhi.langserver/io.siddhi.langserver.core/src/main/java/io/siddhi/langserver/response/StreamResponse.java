package io.siddhi.langserver.response;

import java.util.ArrayList;
import java.util.List;

public class StreamResponse {
    public List<String> streamNames;

    public StreamResponse() {
        this.streamNames = new ArrayList<>();
    }

    public void addStream(String stream) {
        this.streamNames.add(stream);
    }
}
