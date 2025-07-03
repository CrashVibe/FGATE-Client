package com.crashvibe.fgateclient.service;

import com.crashvibe.fgateclient.config.ConfigManager;
import com.crashvibe.fgateclient.handler.RequestDispatcher;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tcoded.folialib.FoliaLib;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

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

    public final Map<String, CompletableFuture<JsonObject>> pendingRequests = new ConcurrentHashMap<>();
    private final Logger logger;
    private final FoliaLib foliaLib;
    private final ConfigManager configManager;
    private final RequestDispatcher requestDispatcher;
    private final String clientVersion;
    private final WebSocketClient client;
    private boolean connected = false;
    private int retryCount = 0;

    public WebSocketManager(URI uri, String token, Logger logger, FoliaLib foliaLib,
                            ConfigManager configManager, RequestDispatcher requestDispatcher,
                            String clientVersion) {

        super(uri, new HashMap<>() {
            {
                put("Authorization", "Bearer " + token);
                put("X-API-Version", clientVersion);
            }
        });

        this.uri = uri;
        this.logger = logger;
        this.foliaLib = foliaLib;
        this.configManager = configManager;
        this.requestDispatcher = requestDispatcher;
        this.clientVersion = clientVersion;
        this.client = this;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        CompletableFuture.runAsync(() -> {
            logger.info("Exchange message: " + uri.toString());
            String wsUrl = configManager.getWebsocketUrl();
            String token = configManager.getWebsocketToken();

            if (wsUrl == null || token == null) {
                logger.severe("WebSocket URL & Token cannot be null!");
                disconnect();
            }
        });
    }

    @Override
    public void onMessage(String message) {
        CompletableFuture.runAsync(() -> {
            if (configManager.getConfig().getBoolean("debug.enable")) {
                logger.info("收到来自服务器的消息: " + message);
            }

            try {
                handleMessage(message);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "处理消息时发生错误: " + message, e);
            }
        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        connected = false;
        final String finalReason = reason.isEmpty() ? "Connection closed." : reason;
        final boolean shouldReconnect = remote;

        CompletableFuture.runAsync(() -> {
            if (code == 1000 || code == 1006) {
                logger.warning("Connection has closed because " + finalReason + " (Code: " + code + ")");
            }

            if (shouldReconnect) {
                scheduleReconnect();
            }
        });
    }

    @Override
    public void onError(Exception ex) {
        connected = false;
        retryCount += 1;

        CompletableFuture.runAsync(() -> {
            if (ex instanceof java.net.ConnectException) {
                logger.warning("Connect failed(" + ex.getMessage() + ")Trying to connect again.....");
            } else {
                logger.log(Level.SEVERE, "WebSocket connect failed", ex);
            }

            if (retryCount < 10) {
                scheduleReconnect();
            } else if (retryCount == 10) {
                logger.severe("Reach max retry count!Please restart your server for try again!");
            }
        });
    }

    /**
     * 异步断开连接
     */
    public CompletableFuture<Void> disconnectAsync() {
        return CompletableFuture.runAsync(() -> {
            connected = false;
            if (client != null) {
                client.close();
            }
        });
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

    /**
     * 异步发送响应消息
     */
    @SuppressWarnings("unused")
    public void sendResponseAsync(String id, JsonObject result, String error) {
        CompletableFuture.runAsync(() -> {
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
        });
    }

    @SuppressWarnings("unused")
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

    /**
     * 异步发送消息（不阻塞调用线程）
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Void> sendAsync(JsonObject message) {
        return CompletableFuture.runAsync(() -> {
            if (configManager.getConfig().getBoolean("debug.enable")) {
                logger.info("Send message: " + message);
            }
            if (isConnected()) {
                client.send(message.toString());
            } else {
                throw new RuntimeException("WebSocket is not connected");
            }
        });
    }

    @SuppressWarnings("unused")
    public void send(JsonObject message) {
        if (configManager.getConfig().getBoolean("debug.enable")) {
            logger.info("Send message: " + message);
        }
        if (isConnected()) {
            client.send(message.toString());
        }
    }

    @SuppressWarnings("unused")
    private void processMessage(JsonObject json) {
        try {
            if (configManager.getConfig().getBoolean("debug.enable")) {
                logger.info("Processing WebSocket message: " + json.toString());
            }

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

    @SuppressWarnings("unused")
    private void processMessage(JsonArray array) {
        for (int i = 0; i < array.size(); i++) {
            var json = array.get(i).getAsJsonObject();
            CompletableFuture.runAsync(() -> processMessage(json));
        }
    }

    private void handleMessage(String message) {
        try {
            if (configManager.getConfig().getBoolean("debug.enable")) {
                logger.info("收到来自服务器的原始消息: " + message);
            }

            JsonObject json = JsonParser.parseString(message).getAsJsonObject();

            if (json.has("id") && !json.get("id").isJsonNull() &&
                    (json.has("result") || json.has("error"))) {
                processMessage(json);
            } else {
                CompletableFuture.runAsync(() -> processMessage(json));
            }
        } catch (Exception e) {
            try {
                JsonArray json = JsonParser.parseString(message).getAsJsonArray();
                processMessage(json);
            } catch (Exception e2) {
                logger.severe("消息格式错误，既不是JSON对象也不是JSON数组: " + message);
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

        if (configManager.getConfig().getBoolean("debug.enable")) {
            logger.info("Handling response for request ID: " + id);
        }

        CompletableFuture<JsonObject> future = pendingRequests.remove(id);
        if (future != null) {
            future.complete(json);
            if (configManager.getConfig().getBoolean("debug.enable")) {
                logger.info("Response completed for request ID: " + id);
            }
        } else {
            logger.warning("Received response for unknown request ID: " + id);
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
        CompletableFuture.runAsync(() -> {
            try {
                // 等待一段时间再重连
                Thread.sleep(1000); // 1秒延迟
                reconnect();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Reconnect failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 异步发送RPC请求，返回CompletableFuture（推荐使用，不阻塞调用线程）
     *
     * @param method 方法名
     * @param params 参数对象
     * @return 响应JsonObject的CompletableFuture
     */
    @SuppressWarnings("unused")
    public CompletableFuture<JsonObject> sendRequestAsync(String method, JsonObject params) {
        String requestId = java.util.UUID.randomUUID().toString();
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("method", method);
        request.addProperty("id", requestId);
        if (params != null) {
            request.add("params", params);
        }

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        this.pendingRequests.put(requestId, future);

        if (configManager.getConfig().getBoolean("debug.enable")) {
            logger.info("Sending async request " + requestId + " for method: " + method);
        }

        this.send(request);

        // 设置超时处理
        CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> {
                    if (!future.isDone()) {
                        this.pendingRequests.remove(requestId);
                        future.completeExceptionally(new java.util.concurrent.TimeoutException(
                                method + " request " + requestId + " timed out after 5 seconds"));
                    }
                });

        return future;
    }

    // 弃用-保留
    // /**
    // * 通用工具方法：发送RPC请求并等待响应（同步版本，可能阻塞线程）
    // * 建议使用 sendRequestAsync 方法代替此方法
    // *
    // * @param method 方法名
    // * @param params 参数对象
    // * @return 响应JsonObject，超时或异常返回null
    // */
    // public JsonObject sendRequest(String method, JsonObject params) {
    // try {
    // return sendRequestAsync(method, params).get(5,
    // java.util.concurrent.TimeUnit.SECONDS);
    // } catch (java.util.concurrent.TimeoutException e) {
    // logger.warning(method + " request timed out after 5 seconds");
    // return null;
    // } catch (Exception e) {
    // String errorMsg = e.getMessage();
    // if (errorMsg == null) {
    // errorMsg = e.getClass().getSimpleName();
    // }
    // logger.warning(method + " request exception: " + errorMsg);
    // return null;
    // }
    // }

    /**
     * 获取当前待处理的请求数量（用于调试）
     */
    @SuppressWarnings("unused")
    public int getPendingRequestsCount() {
        return pendingRequests.size();
    }

    /**
     * 获取待处理请求的ID列表（用于调试）
     */
    @SuppressWarnings("unused")
    public java.util.Set<String> getPendingRequestIds() {
        return new java.util.HashSet<>(pendingRequests.keySet());
    }

    /**
     * 异步清理超时的待处理请求
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Integer> cleanupTimeoutRequestsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            int cleanedCount = 0;

            // 副本避免并发修改异常
            Map<String, CompletableFuture<JsonObject>> requestsCopy = new HashMap<>(pendingRequests);

            for (Map.Entry<String, CompletableFuture<JsonObject>> entry : requestsCopy.entrySet()) {
                CompletableFuture<JsonObject> future = entry.getValue();
                if (future.isDone() || future.isCancelled()) {
                    pendingRequests.remove(entry.getKey());
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0 && configManager.getConfig().getBoolean("debug.enable")) {
                logger.info("Cleaned up " + cleanedCount + " completed/cancelled requests");
            }

            return cleanedCount;
        });
    }

    /**
     * 异步检查连接状态并尝试重连
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Boolean> ensureConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (isConnected()) {
                return true;
            }

            try {
                reconnect();
                // 等待一小段时间检查连接状态
                Thread.sleep(1000);
                return isConnected();
            } catch (Exception e) {
                logger.warning("Failed to ensure connection: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 批量发送多个异步请求
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Map<String, JsonObject>> sendMultipleRequestsAsync(Map<String, JsonObject> methodParams) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, CompletableFuture<JsonObject>> futures = new HashMap<>();

            // 发送所有请求
            for (Map.Entry<String, JsonObject> entry : methodParams.entrySet()) {
                futures.put(entry.getKey(), sendRequestAsync(entry.getKey(), entry.getValue()));
            }

            // 等待所有响应
            Map<String, JsonObject> results = new HashMap<>();
            for (Map.Entry<String, CompletableFuture<JsonObject>> entry : futures.entrySet()) {
                try {
                    JsonObject result = entry.getValue().get(10, java.util.concurrent.TimeUnit.SECONDS);
                    if (result != null) {
                        results.put(entry.getKey(), result);
                    }
                } catch (Exception e) {
                    logger.warning("Failed to get response for method " + entry.getKey() + ": " + e.getMessage());
                }
            }

            return results;
        });
    }

    /**
     * 异步获取连接统计信息
     */
    @SuppressWarnings("unused")
    public CompletableFuture<JsonObject> getConnectionStatsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject stats = new JsonObject();
            stats.addProperty("connected", isConnected());
            stats.addProperty("retryCount", retryCount);
            stats.addProperty("pendingRequests", getPendingRequestsCount());
            stats.addProperty("clientVersion", clientVersion);
            stats.addProperty("uri", uri.toString());
            stats.addProperty("timestamp", System.currentTimeMillis());
            return stats;
        });
    }

    /**
     * 异步健康检查
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Boolean> healthCheckAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected()) {
                return false;
            }

            // 发送ping请求测试连接
            JsonObject pingParams = new JsonObject();
            pingParams.addProperty("timestamp", System.currentTimeMillis());

            try {
                JsonObject response = sendRequestAsync("ping", pingParams)
                        .get(3, java.util.concurrent.TimeUnit.SECONDS);
                return response != null && !response.has("error");
            } catch (Exception e) {
                logger.warning("Health check failed: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 异步重置连接（断开并重新连接）
     */
    @SuppressWarnings("unused")
    public CompletableFuture<Boolean> resetConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 先断开连接
                disconnect();
                Thread.sleep(1000); // 等待1秒

                // 重置重试计数
                retryCount = 0;

                // 重新连接
                reconnect();
                Thread.sleep(2000); // 等待2秒让连接建立

                return isConnected();
            } catch (Exception e) {
                logger.warning("Reset connection failed: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * @param params 参数对象
     */
    public void sendNotificationAsync(String method, JsonObject params) {
        CompletableFuture.runAsync(() -> {
            JsonObject notification = new JsonObject();
            notification.addProperty("jsonrpc", "2.0");
            notification.addProperty("method", method);
            if (params != null) {
                notification.add("params", params);
            }
            // 通知消息不包含 id 字段，表示不需要响应

            if (configManager.getConfig().getBoolean("debug.enable")) {
                logger.info("Sending notification for method: " + method);
            }

            send(notification);
        });
    }

    // 弃用-保留
    // /**
    // * 发送通知消息（同步版本，用于事件通知）
    // *
    // * @param method 方法名
    // * @param params 参数对象
    // */
    // ublic void sendNotification(String method, JsonObject params) {
    // JsonObject notification = new JsonObject();
    // notification.addProperty("jsonrpc", "2.0");
    // notification.addProperty("method", method);
    // if (params != null) {
    // notification.add("params", params);
    // }
    // // 通知消息不包含 id 字段，表示不需要响应

    // configManager.getConfig().getBoolean("debug.enable")) {
    // logger.info("Sending notification for method: " + method);
    // }

    // send(notification);
    //
}
