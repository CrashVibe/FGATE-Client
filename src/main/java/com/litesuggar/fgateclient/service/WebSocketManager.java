package com.litesuggar.fgateclient.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.litesuggar.fgateclient.config.ConfigManager;
import com.litesuggar.fgateclient.handler.RequestDispatcher;
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
public class WebSocketManager {

    private final Logger logger;
    private final FoliaLib foliaLib;
    private final ConfigManager configManager;
    private final RequestDispatcher requestDispatcher;
    private final Gson gson = new Gson();
    private final Map<String, CompletableFuture<JsonObject>> pendingRequests = new ConcurrentHashMap<>();
    private final String clientVersion;
    private WebSocketClient client;
    private boolean connected = false;
    private String serverApiVersion;
    private boolean is_trying = false;

    public WebSocketManager(Logger logger, FoliaLib foliaLib, ConfigManager configManager,
                            RequestDispatcher requestDispatcher, String clientVersion) {
        this.logger = logger;
        this.foliaLib = foliaLib;
        this.configManager = configManager;
        this.requestDispatcher = requestDispatcher;
        this.clientVersion = clientVersion;
    }

    private void waitForConnection() {
        waitForConnection( 5);
    }

    private void waitForConnection( int timeout) {
        this.is_trying  = true;
        Socket socket = client.getSocket();
        if (socket != null ) {
            if(socket.isConnected()){
                this.is_trying = false;
                this.connected = true;
                logger.info("WebSocket 连接已建立");
                return;
            }
        }
        foliaLib.getScheduler().runLaterAsync(() -> {

            if (timeout < 1) {
                logger.warning("无法连接到 WebSocket 服务器");
                this.is_trying = false;
            } else {
                if(timeout%2==0){
                    logger.info("正在等待 WebSocket 连接...");
                }
                waitForConnection( timeout - 1);
            }
        }, 100L);

    }

    public void connect() throws Exception {
        String wsUrl = configManager.getWebsocketUrl();
        String token = configManager.getWebsocketToken();

        if (wsUrl == null || token == null) {
            throw new IllegalArgumentException("WebSocket URL 和 Token 必须配置");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-API-Version", clientVersion);

        URI uri = new URI(wsUrl);
        client = createClient(uri, headers);
        client.connect();
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
        if(configManager.getConfig().getBoolean("debug.enable")){
            logger.info("发送消息: " + message);
        }
        if (isConnected()) {
            client.send(gson.toJson(message));
        }
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            if (json.has("type")) {
                handleSystemMessage(json);
            } else if (json.has("method")) {
                String method = json.get("method").getAsString();
                requestDispatcher.dispatch(method, json);
            } else if (json.has("id") && !json.get("id").isJsonNull()) {
                handleResponse(json);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "处理 WebSocket 消息时出错: " + message, e);
        }
    }

    private void handleSystemMessage(JsonObject json) {
        String type = json.get("type").getAsString();
        if ("welcome".equals(type)) {
            String welcomeMsg = json.get("message").getAsString();
            serverApiVersion = json.has("api_version") ? json.get("api_version").getAsString() : "unknown";

            if (serverApiVersion != null && compareVersions(serverApiVersion, clientVersion) < 0) {
                logger.warning("服务器 API 版本 " + serverApiVersion +
                        " 低于所需版本 " + clientVersion + "。正在关闭连接。");
                client.close();
                return;
            }

            logger.info("🎉 服务器欢迎消息: " + welcomeMsg + " (API v" + serverApiVersion + ")");
            connected = true;
            logger.info("服务器 API 版本检查通过。版本: " + serverApiVersion);
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

    private WebSocketClient createClient(URI uri, Map<String, String> headers) {
        return new WebSocketClient(uri, headers) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                logger.info("正在与远程服务器交换信息: " + uri);
            }

            @Override
            public void onMessage(String message) {
                if(configManager.getConfig().getBoolean("debug.enable")){
                    logger.info("收到来自服务器的消息: " + message);
                }
                handleMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                connected = false;
                if (code == 1000 || code == 1006) {
                    if (reason.isEmpty()) {
                        reason = "正常关闭";
                    }
                    logger.warning("连接关闭了: " + reason + " (Code: " + code + ")");
                }

                if (remote) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onError(Exception ex) {
                if (ex instanceof java.net.ConnectException) {
                    logger.warning("连接失败（"+ ex.getMessage()+"），将尝试重连...");
                } else {
                    logger.log(Level.SEVERE, "WebSocket 连接错误", ex);
                }
                connected = false;
                if(!is_trying){
                    scheduleReconnect();
                }
            }
        };
    }

    private void scheduleReconnect() {
        foliaLib.getScheduler().runLaterAsync(() -> {
            try {
                connect();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "重连失败", e);
            }
        }, 100L);
    }
}
