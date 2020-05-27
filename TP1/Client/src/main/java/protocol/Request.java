package protocol;

import com.google.gson.Gson;

import java.util.Map;

public class Request {
    private final String request;
    private final String token;
    private final Map<String, String> payload;

    public Request(String request, String token, Map<String, String> payload) {
        this.request = request;
        this.token = token;
        this.payload = payload;
    }

    public String getRequest() {
        return request;
    }

    public String getToken() {
        return token;
    }

    public Map<String, String> getPayload() {
        return payload;
    }

    public String encodeRequest() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Request decodeRequest(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Request.class);
    }
}





