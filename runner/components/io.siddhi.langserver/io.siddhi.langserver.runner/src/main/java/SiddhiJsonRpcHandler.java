import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import rpc.handler.EventSimulatorRpcHandler;
import rpc.handler.RuntimeRpcHandler;
import rpc.handler.Utils;

import java.util.List;

public class SiddhiJsonRpcHandler {
    private final Gson gson = new Gson();
    private final EventSimulatorRpcHandler eventSimulatorRpcHandler = new EventSimulatorRpcHandler();
    private final RuntimeRpcHandler runtimeRpcHandler = new RuntimeRpcHandler();

    public String handleRequest(String jsonRequest) {
        try {
            JsonObject request = gson.fromJson(jsonRequest, JsonObject.class);
            JsonObject params = request.getAsJsonObject("params");
            int id = request.get("id").getAsInt();
            String method = request.get("method").getAsString();

            List<String> parts = List.of(method.split("/"));
            if (parts.size() != 2) {
                return Utils.createErrorResponse("Invalid method format", null);
            }

            String domain = parts.get(0);
            String action = parts.get(1);
            switch (domain) {
                case "runtime":
                    return runtimeRpcHandler.handleRequest(params, action, id);
                case "eventSimulator":
                    return eventSimulatorRpcHandler.handleRequest(params, action, id);
                default:
                    return Utils.createErrorResponse("Method not found", id);
            }
        } catch (JsonParseException e) {
            return Utils.createErrorResponse("Invalid JSON", null);
        }
    }
}
