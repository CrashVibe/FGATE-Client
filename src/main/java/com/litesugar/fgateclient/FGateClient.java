package com.litesugar.fgateclient;

import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.server.WebSocketServer;

import java.util.logging.Logger;

public final class FGateClient extends JavaPlugin {
    Logger logger = getLogger();
    private WebSocketServer webSocketServer = null;
    @Override
    public void onEnable() {
        String version = getDescription().getVersion();
        int pluginId = 26085;
        logger.info("\033[33mFGate-Client\033[32m  " + version + " \033[33m正在加载......\033[0m");
        logger.info("\033[33m正在加载配置文件......\033[0m");
        saveDefaultConfig();
        reloadConfig();
        new Metrics(this, pluginId);
        String listenAddress = getConfig().getString("bind_address");
        int listenPort = getConfig().getInt("bind_port");
        logger.info("\033[33m正在监听地址：\033[32m" + listenAddress + ":" + listenPort + "\033[0m");
        try {
            webSocketServer = new SocketServer(listenAddress, listenPort);
            logger.info("\033[33m正在启动WS服务器......\033[0m");
            webSocketServer.start();
        } catch (Exception e) {
            logger.severe("\033[31m启动失败！请检查端口是否被占用！\033[0m");
            e.printStackTrace();

            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        logger.info("\033[33mFGate-Client正在禁用......\033[0m");
        try {
            webSocketServer.stop(100,"Server closed.");
        } catch (InterruptedException ignored) {}
    }
}
