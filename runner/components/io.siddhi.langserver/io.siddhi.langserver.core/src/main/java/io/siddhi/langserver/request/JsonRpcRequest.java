package io.siddhi.langserver.request;

public class JsonRpcRequest<T> {
    public String jsonrpc;
    public String method;
    public T params;
    public int id;
}