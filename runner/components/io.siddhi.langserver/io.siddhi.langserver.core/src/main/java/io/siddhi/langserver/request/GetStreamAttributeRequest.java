package io.siddhi.langserver.request;

public class GetStreamAttributeRequest {
    private String siddhiAppString;
    private String streamName;

    public GetStreamAttributeRequest(String siddhiAppString, String streamName) {
        this.siddhiAppString = siddhiAppString;
        this.streamName = streamName;
    }

    public String getSiddhiAppString() {
        return siddhiAppString;
    }

    public String getStreamName() {
        return streamName;
    }
}
