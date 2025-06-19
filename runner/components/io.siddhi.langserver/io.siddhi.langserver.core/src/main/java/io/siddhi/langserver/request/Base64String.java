package io.siddhi.langserver.request;

public class Base64String {
    private String value;

    public Base64String(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
