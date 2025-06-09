package com.litesuggar.fgateclient.manager;

import com.litesuggar.fgateclient.config.ConfigManager;
import com.litesuggar.fgateclient.handler.RequestDispatcher;
import com.litesuggar.fgateclient.handler.impl.ExecuteRconHandler;
import com.litesuggar.fgateclient.handler.impl.GetClientInfoHandler;
import com.litesuggar.fgateclient.handler.impl.KickPlayerHandler;
import com.litesuggar.fgateclient.service.PlayerManager;
import com.litesuggar.fgateclient.service.RconManager;
import com.litesuggar.fgateclient.service.WebSocketManager;
import com.tcoded.folialib.FoliaLib;

import java.util.logging.Logger;

/**
 * 服务管理器 - 负责创建和管理所有服务实例
 */
public class ServiceManager {

    private final Logger logger;
    private final FoliaLib foliaLib;
    private final ConfigManager configManager;
    private final String clientVersion;

    // 服务实例
    private RconManager rconManager;
    private PlayerManager playerManager;
    private WebSocketManager webSocketManager;
    private RequestDispatcher requestDispatcher;

    public ServiceManager(Logger logger, FoliaLib foliaLib, ConfigManager configManager, String clientVersion) {
        this.logger = logger;
        this.foliaLib = foliaLib;
        this.configManager = configManager;
        this.clientVersion = clientVersion;

        initializeServices();
    }

    private void initializeServices() {
        // 初始化基础服务
        rconManager = new RconManager(logger, foliaLib, configManager);
        playerManager = new PlayerManager(logger, foliaLib);

        // 初始化请求分发器
        requestDispatcher = new RequestDispatcher(logger, foliaLib);

        // 初始化 WebSocket 服务
        webSocketManager = new WebSocketManager(logger, foliaLib, configManager, requestDispatcher, clientVersion);

        // 注册请求处理器
        registerHandlers();

        logger.info("服务管理器初始化完成，共注册了 " + requestDispatcher.getHandlerCount() + " 个请求处理器");
    }

    private void registerHandlers() {
        requestDispatcher.registerHandler(new GetClientInfoHandler(webSocketManager, rconManager));
        requestDispatcher.registerHandler(new ExecuteRconHandler(webSocketManager, rconManager));
        requestDispatcher.registerHandler(new KickPlayerHandler(webSocketManager, playerManager));
    }

    /**
     * 启动所有服务
     */
    public void startServices() throws Exception {
        logger.info("正在启动服务...");

        // 验证配置
        configManager.validateConfig();

        // 连接 WebSocket
        webSocketManager.connect();

        logger.info("所有服务启动完成");
    }

    /**
     * 停止所有服务
     */
    public void stopServices() {
        logger.info("正在停止服务...");

        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }

        if (rconManager != null) {
            rconManager.close();
        }

        logger.info("所有服务已停止");
    }

    // Getter 方法
    public RconManager getRconManager() {
        return rconManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }

    public RequestDispatcher getRequestDispatcher() {
        return requestDispatcher;
    }
}
