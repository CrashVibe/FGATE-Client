package com.litesuggar.fgateclient.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.litesuggar.fgateclient.FGateClient;
import com.litesuggar.fgateclient.config.ConfigManager;
import com.litesuggar.fgateclient.handler.RequestDispatcher;
import com.litesuggar.fgateclient.manager.ServiceManager;
import com.tcoded.folialib.FoliaLib;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket 管理器 - 负责WebSocket连接和消息处理
 */
public class WebSocketManager extends WebSocketClient {

    private final Logger logger = FGateClient.getInstance().logger;
    private final FoliaLib foliaLib = ServiceManager.getInstance().getFoliaLib();
    private final ConfigManager configManager = ServiceManager.getInstance().getConfigManager();
    private final RequestDispatcher requestDispatcher = ServiceManager.getInstance().getRequestDispatcher();
    private final Map<String, CompletableFuture<JsonObject>> pendingRequests = new ConcurrentHashMap<>();
    private final String clientVersion ;
    private final WebSocketClient client;
    private boolean connected = false;
    private boolean is_trying = false;


    public WebSocketManager(URI uri,  String token) {

        super(uri, new HashMap<>() {{
            put("Authorization", "Bearer " + token);
            put("X-API-Version", ServiceManager.getInstance().getClientVersion());
        }});


        this.uri = uri;
        this.clientVersion = ServiceManager.getInstance().getClientVersion();
        this.client = this;
    }
        @Override
        public void onOpen(ServerHandshake handshake) {
            logger.info("正在与远程服务器交换信息: " + uri.toString());
        }

        @Override
        public void onMessage(String message) {
            if (configManager.getConfig().getBoolean("debug.enable")) {
                logger.info("收到来自服务器的消息: " + message);
            }
            handleMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            connected = false;
            if (code == 1000 || code == 1006) {
                if (reason.isEmpty()) {
                    reason = "Connection closed.";
                }
                logger.warning("Connection has closed because " + reason + " (Code: " + code + ")");
            }

            if (remote) {
                scheduleReconnect();
            }
        }

        @Override
        public void onError(Exception ex) {
            if (ex instanceof java.net.ConnectException) {
                logger.warning("Connect failed(" + ex.getMessage() + ")Trying to connect again.....");
            } else {
                logger.log(Level.SEVERE, "WebSocket connect failed", ex);
            }
            connected = false;
            if (!is_trying) {
                scheduleReconnect();
            }
        }

    private void waitForConnection() {
        waitForConnection(10);
    }

    private void waitForConnection(int timeout) {
        this.is_trying = true;
        foliaLib.getScheduler().runLaterAsync(() -> {

            Socket socket = client.getSocket();
            if (socket != null) {
                if (socket.isConnected()) {
                    this.is_trying = false;
                    this.connected = true;
                    logger.info("WebSocket has connected");

                    return;
                }
            }
            if (timeout < 1) {
                logger.warning("Cannot reach WebSocket server.");

                this.is_trying = false;
            } else {
                waitForConnection(timeout - 1);
            }
        }, 10L);

    }
    @Override
    public void connect() {
        String wsUrl = configManager.getWebsocketUrl();
        String token = configManager.getWebsocketToken();

        if (wsUrl == null || token == null) {
            throw new IllegalArgumentException("WebSocket URL & Token cannot be null!");
        }

        super.connect();
        waitForConnection();

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

    public void sendResponse(String id, JsonObject result, String error) {
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

    public void send(JsonObject message) {
        if (configManager.getConfig().getBoolean("debug.enable")) {
            logger.info("Send message: " + message);
        }
        if (isConnected()) {
            client.send(message.toString());
        }
    }

    private void processMessage(JsonObject json) {
        try {
            if (json.has("type")) {
                handleSystemMessage(json);
            } else if (json.has("method")) {
                String method = json.get("method").getAsString();
                requestDispatcher.dispatch(method, json);
            } else if (json.has("id") && !json.get("id").isJsonNull()) {
                handleResponse(json);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Something wrong while processing WebSocket: " + json.toString(), e);
        }
    }

    private void processMessage(JsonArray array) {
        for (int i = 0; i < array.size(); i++) {
            var json = array.get(i).getAsJsonObject();
            foliaLib.getScheduler().runAsync(task -> processMessage(json));

        }
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            foliaLib.getScheduler().runAsync(task -> processMessage(json));
        } catch (Exception e) {
            try {
                JsonArray json = JsonParser.parseString(message).getAsJsonArray();
                processMessage(json);

            } catch (Exception e2) {
                throw new RuntimeException("Message received is not a json object or a json array!");
            }
        }

    }

    private void handleSystemMessage(JsonObject json) {
        String type = json.get("type").getAsString();
        if ("welcome".equals(type)) {
            String welcomeMsg = json.get("message").getAsString();
            String serverApiVersion = json.has("api_version") ? json.get("api_version").getAsString() : "unknown";

            if (serverApiVersion != null && compareVersions(serverApiVersion, clientVersion) < 0) {
                logger.warning("Server API version " + serverApiVersion +
                        " OOPS!Server API version is too low!I am on " + clientVersion + ".Closing connection......");
                client.close();
                return;
            }

            logger.info("🎉 Server welcome message: " + welcomeMsg + " (API v" + serverApiVersion + ")");
            connected = true;
            logger.info("API check passed: " + serverApiVersion);
        }
    }

    private void handleResponse(JsonObject json) {
        String id = json.get("id").getAsString();
        CompletableFuture<JsonObject> future = pendingRequests.remove(id);
        if (future != null) {
            future.complete(json);
        }
    }

    private int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1Part < v2Part)
                return -1;
            if (v1Part > v2Part)
                return 1;
        }

        return 0;
    }



    private void scheduleReconnect() {
        foliaLib.getScheduler().runLaterAsync(() -> {
            try {
                connect();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Reconnect failed.", e);
            }
        }, 10L);
    }
}
