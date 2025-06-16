package com.crashvibe.fgateclient.example;

import com.google.gson.JsonObject;
import com.crashvibe.fgateclient.service.WebSocketManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * WebSocketManager 异步使用示例
 * 展示如何使用新的异步方法来避免阻塞主线程
 */
public class AsyncWebSocketExample {

    private final WebSocketManager webSocketManager;
    private final Logger logger;

    public AsyncWebSocketExample(WebSocketManager webSocketManager, Logger logger) {
        this.webSocketManager = webSocketManager;
        this.logger = logger;
    }

    /**
     * 示例1: 异步发送单个请求
     */
    public void sendSingleRequestExample() {
        JsonObject params = new JsonObject();
        params.addProperty("action", "getPlayerInfo");
        params.addProperty("playerName", "testPlayer");

        // 使用异步方法，不阻塞当前线程
        webSocketManager.sendRequestAsync("player.info", params)
                .thenAccept(response -> {
                    if (response != null && !response.has("error")) {
                        logger.info("Player info received: " + response);
                    } else {
                        logger.warning("Failed to get player info");
                    }
                })
                .exceptionally(throwable -> {
                    logger.severe("Error getting player info: " + throwable.getMessage());
                    return null;
                });

        logger.info("Request sent, continuing with other work...");
    }

    /**
     * 示例2: 批量发送多个请求
     */
    public void sendMultipleRequestsExample() {
        Map<String, JsonObject> requests = new HashMap<>();

        // 准备多个请求
        JsonObject playerParams = new JsonObject();
        playerParams.addProperty("playerName", "testPlayer");
        requests.put("player.info", playerParams);

        JsonObject serverParams = new JsonObject();
        requests.put("server.status", serverParams);

        JsonObject worldParams = new JsonObject();
        worldParams.addProperty("worldName", "world");
        requests.put("world.info", worldParams);

        // 批量发送并处理响应
        webSocketManager.sendMultipleRequestsAsync(requests)
                .thenAccept(responses -> {
                    logger.info("Received " + responses.size() + " responses");
                    for (Map.Entry<String, JsonObject> entry : responses.entrySet()) {
                        logger.info("Response for " + entry.getKey() + ": " + entry.getValue());
                    }
                })
                .exceptionally(throwable -> {
                    logger.severe("Error in batch requests: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * 示例3: 异步健康检查和连接管理
     */
    public void connectionManagementExample() {
        // 执行健康检查
        webSocketManager.healthCheckAsync()
                .thenCompose(isHealthy -> {
                    if (!isHealthy) {
                        logger.warning("Connection unhealthy, attempting reset...");
                        return webSocketManager.resetConnectionAsync();
                    } else {
                        logger.info("Connection is healthy");
                        return CompletableFuture.completedFuture(true);
                    }
                })
                .thenCompose(resetSuccess -> {
                    if (resetSuccess) {
                        // 获取连接统计信息
                        return webSocketManager.getConnectionStatsAsync();
                    } else {
                        throw new RuntimeException("Failed to reset connection");
                    }
                })
                .thenAccept(stats -> {
                    logger.info("Connection stats: " + stats);
                })
                .exceptionally(throwable -> {
                    logger.severe("Connection management failed: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * 示例4: 链式异步操作
     */
    public void chainedOperationsExample() {
        JsonObject loginParams = new JsonObject();
        loginParams.addProperty("username", "admin");
        loginParams.addProperty("password", "secret");

        // 链式执行多个异步操作
        webSocketManager.sendRequestAsync("auth.login", loginParams)
                .thenCompose(loginResponse -> {
                    if (loginResponse != null && !loginResponse.has("error")) {
                        logger.info("Login successful");
                        // 登录成功后获取用户信息
                        JsonObject userParams = new JsonObject();
                        return webSocketManager.sendRequestAsync("user.profile", userParams);
                    } else {
                        throw new RuntimeException("Login failed");
                    }
                })
                .thenCompose(profileResponse -> {
                    if (profileResponse != null && !profileResponse.has("error")) {
                        logger.info("Profile loaded: " + profileResponse);
                        // 加载用户设置
                        JsonObject settingsParams = new JsonObject();
                        return webSocketManager.sendRequestAsync("user.settings", settingsParams);
                    } else {
                        throw new RuntimeException("Failed to load profile");
                    }
                })
                .thenAccept(settingsResponse -> {
                    if (settingsResponse != null && !settingsResponse.has("error")) {
                        logger.info("Settings loaded: " + settingsResponse);
                    } else {
                        logger.warning("Failed to load settings");
                    }
                })
                .exceptionally(throwable -> {
                    logger.severe("Chained operations failed: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * 示例5: 定期清理和维护
     */
    public void maintenanceExample() {
        // 创建定期维护任务
        CompletableFuture<Void> maintenanceTask = CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    // 清理超时的请求
                    webSocketManager.cleanupTimeoutRequestsAsync()
                            .thenAccept(cleanedCount -> {
                                if (cleanedCount > 0) {
                                    logger.info("Cleaned up " + cleanedCount + " timeout requests");
                                }
                            });

                    // 检查连接状态
                    webSocketManager.ensureConnectionAsync()
                            .thenAccept(connected -> {
                                if (!connected) {
                                    logger.warning("Connection lost, attempting to reconnect...");
                                }
                            });

                    // 等待30秒再次执行
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    logger.info("Maintenance task interrupted");
                    break;
                } catch (Exception e) {
                    logger.severe("Maintenance task error: " + e.getMessage());
                }
            }
        });

        // 可以通过 maintenanceTask.cancel(true) 来停止维护任务
    }
}
