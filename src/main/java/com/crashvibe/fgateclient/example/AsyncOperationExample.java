package com.crashvibe.fgateclient.example;

import com.google.gson.JsonObject;
import com.crashvibe.fgateclient.FGateClient;
import com.crashvibe.fgateclient.manager.ServiceManager;
import com.crashvibe.fgateclient.service.RconManager;
import com.crashvibe.fgateclient.service.WebSocketManager;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * 异步操作示例 - 展示如何正确使用异步方法避免阻塞
 */
public class AsyncOperationExample {

    private final FGateClient plugin;
    private final Logger logger;

    public AsyncOperationExample(FGateClient plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 示例1: 异步初始化服务管理器
     */
    public void initializeServiceManagerExample() {
        CompletableFuture.runAsync(() -> {
            try {
                // 异步创建服务管理器（在异步线程中创建以避免阻塞主线程）
                ServiceManager serviceManager = plugin.getServiceManager();

                // 异步启动所有服务
                serviceManager.startServicesAsync()
                        .thenRun(() -> {
                            logger.info("All services started successfully!");
                            // 服务启动后执行其他初始化任务
                            postInitializationTasks();
                        })
                        .exceptionally(throwable -> {
                            logger.severe("Failed to start services: " + throwable.getMessage());
                            return null;
                        });

            } catch (Exception e) {
                logger.severe("Failed to initialize service manager: " + e.getMessage());
            }
        });
    }

    /**
     * 示例2: 异步执行 RCON 命令
     */
    public void executeRconCommandExample() {
        ServiceManager serviceManager = plugin.getServiceManager();
        if (serviceManager == null) {
            logger.warning("Service manager not initialized");
            return;
        }

        RconManager rconManager = serviceManager.getRconManager();

        // 使用异步方法执行命令，避免阻塞
        rconManager.executeCommandAsync("list")
                .thenAccept(result -> {
                    logger.info("RCON command result: " + result);
                    // 处理结果
                    processRconResult(result);
                })
                .exceptionally(throwable -> {
                    logger.warning("RCON command failed: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * 示例3: 链式异步操作
     */
    public void chainedAsyncOperationsExample() {
        ServiceManager serviceManager = plugin.getServiceManager();
        if (serviceManager == null) {
            return;
        }

        RconManager rconManager = serviceManager.getRconManager();
        WebSocketManager webSocketManager = serviceManager.getWebSocketManager();

        // 链式执行多个异步操作
        rconManager.executeCommandAsync("list")
                .thenCompose(playerList -> {
                    // 基于玩家列表创建统计信息
                    JsonObject stats = new JsonObject();
                    stats.addProperty("playerCount", playerList.split("\n").length);
                    stats.addProperty("timestamp", System.currentTimeMillis());

                    // 异步发送统计信息到服务器
                    return webSocketManager.sendRequestAsync("server.stats", stats);
                })
                .thenAccept(response -> {
                    if (response != null && !response.has("error")) {
                        logger.info("Server stats sent successfully");
                    } else {
                        logger.warning("Failed to send server stats");
                    }
                })
                .exceptionally(throwable -> {
                    logger.warning("Chained operations failed: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * 示例4: 并行异步操作
     */
    public void parallelAsyncOperationsExample() {
        ServiceManager serviceManager = plugin.getServiceManager();
        if (serviceManager == null) {
            return;
        }

        RconManager rconManager = serviceManager.getRconManager();
        WebSocketManager webSocketManager = serviceManager.getWebSocketManager();

        // 并行执行多个独立的异步操作
        CompletableFuture<String> rconFuture = rconManager.executeCommandAsync("tps");
        CompletableFuture<JsonObject> healthFuture = webSocketManager.healthCheckAsync()
                .thenApply(healthy -> {
                    JsonObject result = new JsonObject();
                    result.addProperty("healthy", healthy);
                    return result;
                });
        CompletableFuture<JsonObject> statsFuture = webSocketManager.getConnectionStatsAsync();

        // 等待所有操作完成
        CompletableFuture.allOf(rconFuture, healthFuture, statsFuture)
                .thenRun(() -> {
                    try {
                        String tpsInfo = rconFuture.get();
                        JsonObject healthInfo = healthFuture.get();
                        JsonObject statsInfo = statsFuture.get();

                        logger.info("TPS: " + tpsInfo);
                        logger.info("Health: " + healthInfo);
                        logger.info("Stats: " + statsInfo);

                        // 汇总所有信息
                        JsonObject summary = createSummary(tpsInfo, healthInfo, statsInfo);

                        // 异步发送汇总信息
                        webSocketManager.sendAsync(summary);

                    } catch (Exception e) {
                        logger.warning("Failed to process parallel results: " + e.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    logger.warning("Parallel operations failed: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * 示例5: 异步重连机制
     */
    public void asyncReconnectionExample() {
        ServiceManager serviceManager = plugin.getServiceManager();
        if (serviceManager == null) {
            return;
        }

        WebSocketManager webSocketManager = serviceManager.getWebSocketManager();

        // 定期检查连接状态并异步重连
        CompletableFuture<Void> reconnectionTask = CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 异步健康检查
                    webSocketManager.healthCheckAsync()
                            .thenCompose(healthy -> {
                                if (!healthy) {
                                    logger.warning("Connection unhealthy, attempting reset...");
                                    return webSocketManager.resetConnectionAsync();
                                }
                                return CompletableFuture.completedFuture(true);
                            })
                            .thenAccept(success -> {
                                if (success) {
                                    logger.info("Connection status: OK");
                                } else {
                                    logger.warning("Failed to reset connection");
                                }
                            })
                            .exceptionally(throwable -> {
                                logger.warning("Health check failed: " + throwable.getMessage());
                                return null;
                            });

                    // 等待30秒再次检查
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    logger.info("Reconnection task interrupted");
                    break;
                }
            }
        });

        // 可以通过 reconnectionTask.cancel(true) 来停止重连任务
    }

    /**
     * 示例6: 异步批量清理
     */
    public void asyncBatchCleanupExample() {
        ServiceManager serviceManager = plugin.getServiceManager();
        if (serviceManager == null) {
            return;
        }

        WebSocketManager webSocketManager = serviceManager.getWebSocketManager();

        // 批量清理操作
        CompletableFuture<Integer> cleanupFuture = webSocketManager.cleanupTimeoutRequestsAsync();
        CompletableFuture<Void> cacheClearFuture = serviceManager.getI18n().clearCacheAsync();

        CompletableFuture.allOf(cleanupFuture, cacheClearFuture)
                .thenRun(() -> {
                    try {
                        int cleanedRequests = cleanupFuture.get();
                        logger.info("Cleanup completed. Cleaned " + cleanedRequests + " timeout requests");
                        logger.info("I18n cache cleared");
                    } catch (Exception e) {
                        logger.warning("Failed to get cleanup results: " + e.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    logger.warning("Batch cleanup failed: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * 示例7: 异步配置热重载
     */
    public void asyncConfigReloadExample() {
        ServiceManager serviceManager = plugin.getServiceManager();
        if (serviceManager == null) {
            return;
        }

        // 异步重载配置
        serviceManager.getConfigManager().loadConfigAsync()
                .thenCompose(v -> {
                    // 验证新配置
                    return serviceManager.getConfigManager().validateConfigAsync();
                })
                .thenCompose(v -> {
                    // 重新初始化 I18n
                    return serviceManager.getI18n().clearCacheAsync()
                            .thenCompose(cleared -> serviceManager.getI18n().preloadLanguageFilesAsync());
                })
                .thenRun(() -> {
                    logger.info("Configuration reloaded successfully");
                })
                .exceptionally(throwable -> {
                    logger.severe("Failed to reload configuration: " + throwable.getMessage());
                    return null;
                });
    }

    // 辅助方法
    private void postInitializationTasks() {
        logger.info("Executing post-initialization tasks...");
        // 执行其他初始化任务
    }

    private void processRconResult(String result) {
        // 处理 RCON 结果
        logger.info("Processing RCON result: " + result);
    }

    private JsonObject createSummary(String tpsInfo, JsonObject healthInfo, JsonObject statsInfo) {
        JsonObject summary = new JsonObject();
        summary.addProperty("tps", tpsInfo);
        summary.add("health", healthInfo);
        summary.add("stats", statsInfo);
        summary.addProperty("timestamp", System.currentTimeMillis());
        return summary;
    }
}
