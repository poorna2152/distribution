package io.siddhi.langserver.extension;

import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethodProvider;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;

import java.util.HashMap;
import java.util.Map;

public abstract class ExtensionService implements JsonRpcMethodProvider {
    @Override
    public Map<String, JsonRpcMethod> supportedMethods() {
        return new HashMap<>(ServiceEndpoints.getSupportedMethods(getClass()));
    }

}
