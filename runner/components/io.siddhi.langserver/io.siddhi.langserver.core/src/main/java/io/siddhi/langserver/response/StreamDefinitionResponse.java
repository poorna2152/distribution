package io.siddhi.langserver.response;

import io.siddhi.query.api.definition.Attribute;

import java.util.List;

public class StreamDefinitionResponse {
    public String getName() {
        return name;
    }

    public List<Attribute> getAttributeList() {
        return attributeList;
    }

    private String name;
    private final List<Attribute> attributeList;

    public StreamDefinitionResponse(List<Attribute> attributeList, String name) {
        this.attributeList = attributeList;
        this.name = name;
    }
}
