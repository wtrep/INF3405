package protocol;

import com.google.gson.Gson;

import java.util.Map;

public class Response {
    private final String response;
    private final Map<String, String> payload;

    public Response(String response, Map<String, String> payload) {
        this.response = response;
        this.payload = payload;
    }

    public String getResponse() {
        return response;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public String encodeResponse() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Response decodeResponse(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Response.class);
    }
}
