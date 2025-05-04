package io.siddhi.langserver.response;

import java.util.Arrays;

public class DesignModelResponse {
    private String errorMsg;
    private String stacktrace;
    private String content;

    public void setError(Throwable e) {
        this.errorMsg = e.toString();
        this.stacktrace = Arrays.toString(e.getStackTrace());
    }

    public String errorMsg() {
        return errorMsg;
    }

    public String stackTrace() {
        return stacktrace;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
