package io.siddhi.langserver.response;

import io.siddhi.query.api.definition.Attribute;

import java.util.List;

public class DeployAppResponse {

    private boolean success;
    private List<String> message;

    public DeployAppResponse(boolean success, List<String> message) {
        this.success = success;
        this.message = message;
    }
}
