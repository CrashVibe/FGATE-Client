package com.crashvibe.fgateclient.manager;

import com.crashvibe.fgateclient.commands.PlayerBind;
import com.crashvibe.fgateclient.config.ConfigManager;
import com.crashvibe.fgateclient.handler.RequestDispatcher;
import com.crashvibe.fgateclient.handler.impl.ExecuteRconHandler;
import com.crashvibe.fgateclient.handler.impl.GetClientInfoHandler;
import com.crashvibe.fgateclient.handler.impl.KickPlayerHandler;
import com.crashvibe.fgateclient.service.PlayerManager;
import com.crashvibe.fgateclient.service.RconManager;
import com.crashvibe.fgateclient.service.WebSocketManager;
import com.tcoded.folialib.FoliaLib;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * 服务管理器 - 负责创建和管理所有服务实例
 */
@SuppressWarnings("unused")
public class ServiceManager {

    private static ServiceManager instance;
    private final Logger logger;
    private final FoliaLib foliaLib;
    private final ConfigManager configManager;
    private final String clientVersion;
    private final com.crashvibe.fgateclient.utils.I18n i18n;
    // 服务实例
    private RconManager rconManager;
    private PlayerManager playerManager;
    private WebSocketManager webSocketManager;
    private RequestDispatcher requestDispatcher;
    private final AtomicReference<ExecutorService> executorService = new AtomicReference<>();

    public ServiceManager(Logger logger, FoliaLib foliaLib, ConfigManager configManager, String clientVersion,
                          com.crashvibe.fgateclient.utils.I18n i18n) {
        this.logger = logger;
        this.foliaLib = foliaLib;
        this.configManager = configManager;
        this.clientVersion = clientVersion;
        this.i18n = i18n;
        instance = this;
        
        // 创建专用线程池用于插件任务
        executorService.set(Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "FGateClient-Worker-" + r.hashCode());
            t.setDaemon(true);
            return t;
        }));
        
        // 异步初始化I18n的配置管理器引用和预加载
        i18n.initializeAsync(configManager)
                .thenCompose(v -> i18n.preloadLanguageFilesAsync())
                .exceptionally(throwable -> {
                    logger.warning("Failed to initialize I18n asynchronously: " + throwable.getMessage());
                    return null;
                });

        initializeServices();
    }

    public static ServiceManager getInstance() {
        return instance;
    }

    private void initializeServices() {
        // 初始化基础服务
        rconManager = new RconManager(logger, foliaLib, configManager);
        playerManager = new PlayerManager(logger, foliaLib);

        // 初始化请求分发器
        requestDispatcher = new RequestDispatcher(logger, foliaLib);

        // 初始化 WebSocket 服务
        try {
            webSocketManager = new WebSocketManager(
                    new URI(configManager.getWebsocketUrl()),
                    configManager.getWebsocketToken(),
                    logger,
                    foliaLib,
                    configManager,
                    requestDispatcher,
                    clientVersion);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // 注册请求处理器
        registerHandlers();
        PlayerBind.initWebsocketManager();
        logger.info("Init done, " + requestDispatcher.getHandlerCount() + " handlers has been enabled");
    }

    private void registerHandlers() {
        requestDispatcher
                .registerHandler(new GetClientInfoHandler(webSocketManager, rconManager, logger))
                .registerHandler(new ExecuteRconHandler(webSocketManager, rconManager))
                .registerHandler(new KickPlayerHandler(webSocketManager, playerManager))
                .registerHandler(new com.crashvibe.fgateclient.handler.impl.BroadcastMessageHandler(webSocketManager,
                        logger, foliaLib));
    }

    /**
     * 异步启动所有服务
     */
    public CompletableFuture<Void> startServicesAsync() {
        return CompletableFuture.runAsync(() -> {
                    logger.info("Starting services......");
                })
                .thenCompose(v -> {
                    // 异步验证配置
                    return configManager.validateConfigAsync();
                })
                .thenRun(() -> {
                    // 连接 WebSocket
                    webSocketManager.connect();
                    logger.info("Done!");
                });
    }

    /**
     * 启动所有服务（同步版本，保持兼容性）
     */

    public void startServices() {
        logger.info("Starting services......");

        // 验证配置
        configManager.validateConfig();

        // 连接 WebSocket
        webSocketManager.connect();

        logger.info("Done!");
    }

    /**
     * 异步停止所有服务
     */
    public CompletableFuture<Void> stopServicesAsync() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Stopping services......");

            if (webSocketManager != null) {
                webSocketManager.close(1001);
            }

            if (rconManager != null) {
                rconManager.close();
            }
            
            // 关闭线程池
            ExecutorService es = executorService.getAndSet(null);
            if (es != null) {
                try {
                    es.shutdown();
                    if (!es.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                        es.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    es.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            logger.info("ALL SERVICES HAS STOPPED");
        });
    }

    /**
     * 停止所有服务
     */

    public void stopServices() {
        logger.info("Stopping services......");

        if (webSocketManager != null) {
            webSocketManager.close(1001);
        }

        if (rconManager != null) {
            rconManager.close();
        }

        logger.info("ALL SERVICES HAS STOPPED");
    }

    // Getter 方法

    public Logger getLogger() {
        return logger;
    }


    public RconManager getRconManager() {
        return rconManager;
    }


    public ConfigManager getConfigManager() {
        return configManager;
    }


    public PlayerManager getPlayerManager() {
        return playerManager;
    }


    public WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }


    public FoliaLib getFoliaLib() {
        return foliaLib;
    }


    public RequestDispatcher getRequestDispatcher() {
        return requestDispatcher;
    }


    public String getClientVersion() {
        return clientVersion;
    }


    public com.crashvibe.fgateclient.utils.I18n getI18n() {
        return i18n;
    }
}
