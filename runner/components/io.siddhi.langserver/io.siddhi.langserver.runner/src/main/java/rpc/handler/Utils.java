package rpc.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Utils {
    private static final Gson gson = new Gson();

    public static String createSuccessResponse(String result, Integer id) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", id);
        response.addProperty("result", result);
        return gson.toJson(response);
    }

    public static String createErrorResponse(String message, Integer id) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        if (id != null) {
            response.addProperty("id", id);
        } else {
            response.add("id", null);
        }
        JsonObject error = new JsonObject();
        error.addProperty("code", -32603);
        error.addProperty("message", message);
        response.add("error", error);
        return gson.toJson(response);
    }
}
