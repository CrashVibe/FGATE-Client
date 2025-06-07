package com.litesugar.fgateclient.LibWebSocket;

import com.litesugar.fgateclient.FGateClient;
import com.litesugar.fgateclient.LibWebSocket.Handlers.notification.Kick;
import com.litesugar.fgateclient.LibWebSocket.Handlers.request.ServerInfo;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class MessageRunner {
    Logger logger = FGateClient.getPlugin(FGateClient.class).getLogger();
    AsyncSocketClient client;
    public void runner(String message, AsyncSocketClient client) {
        Object json;
        this.client = client;
        try {
            try {
                json = new JSONObject(message);


            } catch (Exception e) {
                json = new JSONArray(message);

            }
        } catch (Exception e) {
            logger.warning("JSON解析错误：" + message + e.getMessage());
            JSONObject error = new JSONObject();
            JSONObject data = new JSONObject();
            error.put("code", -32700).put("message", "Parse error" + e.getMessage());
            data.put("jsonrpc", "2.0").put("error", error).put("id", Optional.empty());
            client.sendTextAsync(data.toString());
            return;
        }
        try {
            if (json instanceof JSONObject) {
                run_object((JSONObject) json);
            } else {
                run_array((JSONArray) json);
            }
        } catch (Exception e) {
            logger.severe("执行WebSocket消息响应时出现问题：" + message + "\n" + e.getMessage());
            JSONObject error = new JSONObject();
            JSONObject data = new JSONObject();
            data.put("jsonrpc", "2.0").put("error", error);
            data.put("id", Optional.empty());
            error.put("code", -32603).put("message", "Server error " + e.getMessage());

            e.printStackTrace();
        }

    }

    public static boolean validate(JSONObject json) {
        boolean is_validate = true;
        if (!json.has("jsonrpc")) {
            is_validate = false;
        } else if (!Objects.equals(json.getString("jsonrpc"), "2.0")) {
            is_validate = false;

        }

        return is_validate;
    }

    private void run_object(JSONObject json) {
        try {
            JSONObject data = new JSONObject();
            if (!validate(json)) {
                JSONObject error = new JSONObject();

                error.put("code", -32600).put("message", "Invalid Request");
                data.put("jsonrpc", "2.0").put("error", error);
                if (json.has("id")) {
                    data.put("id", json.get("id"));
                } else {
                    data.put("id", Optional.empty());
                }
                this.client.sendTextAsync(data.toString());
            String message = Objects.requireNonNull(to_object(json)).toString();
            if(message!=null)
                this.client.sendTextAsync(message);
            }
        } catch (Exception e) {
            JSONObject data = new JSONObject();
            JSONObject error = new JSONObject();
            error.put("code", -32603).put("message", "Server error" + e.getMessage());
            try {
                data.put("id", (json.get("id") == null || json.get("id") instanceof String) ? json.get("id") : null);
            } catch (JSONException e1) {
                data.put("id", Optional.empty());
            }
            data.put("jsonrpc", "2.0").put("error", error);
            this.client.sendTextAsync(data.toString());
        }
    }
    private void run_notification(JSONObject json) {

        switch (json.get("method").toString()){
            case "client.kick"-> new Kick().handle(json);
        }

    }
    private JSONObject run_request(JSONObject json) {
        JSONObject data = new JSONObject("{\"jsonrpc\": \"2.0\", \"error\": {\"code\": -32601, \"message\": \"Method not found\"}");
        data.put("id",json.get("id"));

        switch (json.get("method").toString()){
            case "client.info" -> data =  new ServerInfo().handle(json);
        }
        return data;
    }
    @Nullable
    private JSONObject to_object(JSONObject json) {
        if(json.has("id")){
            return run_request(json);
        }else{
            run_notification(json);
            return null;
        }
    }

    private void run_array(JSONArray json) {
        for (int i = 0; i < json.length(); i++) {
            run_object(json.getJSONObject(i));
        }
    }
}
