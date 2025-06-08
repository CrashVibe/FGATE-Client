package com.litesuggar.fgateclient;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class WebSocketManager {

    private final FGateClient plugin;
    private final RconManager rconManager;
    private final Gson gson = new Gson();
    private final Map<String, CompletableFuture<JsonObject>> pendingRequests = new ConcurrentHashMap<>();

    private WebSocketClient client;
    private boolean connected = false;
    private String serverApiVersion;
    private static String requiredServerVersion;


    public WebSocketManager(FGateClient plugin, RconManager rconManager) {
        this.plugin = plugin;
        this.rconManager = rconManager;
    }

    public void connect() throws Exception {
        var config = plugin.getConfig();
        String wsUrl = config.getString("websocket.url");
        String token = config.getString("websocket.token");
        String clientVersion = requiredServerVersion = plugin.getDescription().getVersion();

        if (wsUrl == null || token == null) {
            throw new IllegalArgumentException("WebSocket URL and token must be configured");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-API-Version", clientVersion);

        URI uri = new URI(wsUrl);

        client = createClient(uri, headers);

        client.connect();

        int timeout = 10;
        while (!client.isOpen() && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        if (!client.isOpen()) {
            plugin.getLogger().warning("Websocket 连接超时");
            return;
        }
    }

    public void disconnect() {
        connected = false;
        if (client != null) {
            client.close();
        }
    }

    public boolean isConnected() {
        return connected && client != null && client.isOpen();
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            if (json.has("type")) {
                String type = json.get("type").getAsString();
                if ("welcome".equals(type)) {
                    String welcomeMsg = json.get("message").getAsString();
                    String serverApiVersion = json.has("api_version") ? json.get("api_version").getAsString() : "unknown";
                    if (serverApiVersion != null && compareVersions(serverApiVersion, requiredServerVersion) < 0) {
                        plugin.getLogger().warning("Server API version " + serverApiVersion +
                                " is lower than required " + requiredServerVersion + ". Closing connection.");
                        client.close();
                        return;
                    }
                    plugin.getLogger().info("🎉 Welcome from server: " + welcomeMsg + " (API v" + serverApiVersion + ")");

                    connected = true;
                    plugin.getLogger().info("服务器 API 版本检查通过。版本: " + serverApiVersion);
                }
            }
            if (json.has("method")) {
                String method = json.get("method").getAsString();
                handleRequest(method, json);
            }else if (json.has("id") && !json.get("id").isJsonNull()) {
                String id = json.get("id").getAsString();
                CompletableFuture<JsonObject> future = pendingRequests.remove(id);
                if (future != null) {
                    future.complete(json);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling WebSocket message: " + message, e);
        }
    }

    private void handleRequest(String method, JsonObject request) {
        plugin.foliaLib.getScheduler().runAsync(task -> {
            try {
                switch (method) {
                    case "get.client.info":
                        handleGetClientInfo(request);
                        break;
                    case "execute.rcon":
                        handleExecuteRcon(request);
                        break;
                    case "kick.player":
                        handleKickPlayer(request);
                        break;
                    default:
                        plugin.getLogger().warning("Unknown method: " + method);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error handling request: " + method, e);
            }
        });
    }

    private void handleGetClientInfo(JsonObject request) {
        var config = plugin.getConfig();

        JsonObject data = new JsonObject();
        data.addProperty("minecraft_version", Bukkit.getVersion());
        data.addProperty("minecraft_software", Bukkit.getName());
        data.addProperty("supports_papi", Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null);
        data.addProperty("supports_rcon", rconManager.isRconAvailable());

        JsonObject result = new JsonObject();
        result.add("data", data);

        sendResponse(request.get("id").getAsString(), result, null);
    }

    private void handleExecuteRcon(JsonObject request) {
        if (!request.has("params") || !request.getAsJsonObject("params").has("command")) {
            sendResponse(request.get("id").getAsString(), null, "Missing command parameter");
            return;
        }

        String command = request.getAsJsonObject("params").get("command").getAsString();

        try {
            String output = rconManager.executeCommand(command);
            JsonObject result = new JsonObject();
            result.addProperty("commandoutput", output);
            sendResponse(request.get("id").getAsString(), result, null);
        } catch (Exception e) {
            sendResponse(request.get("id").getAsString(), null, "RCON execution failed: " + e.getMessage());
        }
    }

    private void handleKickPlayer(JsonObject request) {
        if (!request.has("params")) {
            return;
        }

        JsonObject params = request.getAsJsonObject("params");
        String playerIdentifier = params.has("player") ? params.get("player").getAsString() :
                params.has("uuid") ? params.get("uuid").getAsString() : null;
        String reason = params.has("reason") ? params.get("reason").getAsString() : "You have been kicked";

        if (playerIdentifier != null) {
            plugin.kickPlayer(playerIdentifier, reason);
        }
    }

    private void sendResponse(String id, JsonObject result, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("id", id);
        response.addProperty("jsonrpc", "2.0");

        if (error != null) {
            JsonObject errorObj = new JsonObject();
            errorObj.addProperty("code", -1);
            errorObj.addProperty("message", error);
            response.add("error", errorObj);
        } else {
            response.add("result", result);
        }

        send(response);
    }

    public void sendPlayerJoinEvent(String playerName, String uuid, long timestamp) {
        JsonObject params = new JsonObject();
        params.addProperty("player", playerName);
        params.addProperty("uuid", uuid);
        params.addProperty("timestamp", timestamp);

        JsonObject notification = new JsonObject();
        notification.addProperty("jsonrpc", "2.0");
        notification.addProperty("method", "player.join");
        notification.add("params", params);

        send(notification);
    }

    private void send(JsonObject message) {
        if (isConnected()) {
            client.send(gson.toJson(message));
        }
    }

    private int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Part < v2Part) return -1;
            if (v1Part > v2Part) return 1;
        }

        return 0;
    }
    private WebSocketClient createClient(URI uri, Map<String, String> headers) {
        return new WebSocketClient(uri, headers) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                plugin.getLogger().info("正在与远程服务器交换信息: " + uri);
            }

            @Override
            public void onMessage(String message) {
                handleMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                connected = false;
                if (code == 1000 || code == 1006) {
                    if (reason.isEmpty()) {
                        reason = "正常关闭";
                    }
                    plugin.getLogger().warning("连接关闭了: " + reason + " (Code: " + code + ")");
                }


                if (remote) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onError(Exception ex) {
                if (ex instanceof java.net.ConnectException) {
                    plugin.getLogger().warning("连接失败（Connection refused），将尝试重连...");
                } else {
                    plugin.getLogger().log(Level.SEVERE, "WebSocket 连接错误", ex);
                }
                connected = false;

                scheduleReconnect();
            }
        };
    }
    private void scheduleReconnect() {
        plugin.foliaLib.getScheduler().runLaterAsync(() -> {
            try {
                connect();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reconnect", e);
            }
        }, 100L);
    }

}