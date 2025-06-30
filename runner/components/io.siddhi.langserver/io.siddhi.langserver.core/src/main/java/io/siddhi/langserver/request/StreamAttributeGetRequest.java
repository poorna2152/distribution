package io.siddhi.langserver.request;

public class StreamAttributeGetRequest {
    private String siddhiAppString;
    private String streamName;
    private String siddhiAppUri;

    public StreamAttributeGetRequest(String siddhiAppString, String streamName, String siddhiAppUri) {
        this.siddhiAppString = siddhiAppString;
        this.streamName = streamName;
        this.siddhiAppUri = siddhiAppUri;
    }

    public String getSiddhiAppString() {
        return siddhiAppString;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getSiddhiAppUri() {
        return siddhiAppUri;
    }
}
