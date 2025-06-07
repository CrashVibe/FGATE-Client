package com.litesugar.fgateclient;

import com.litesugar.fgateclient.Commands.FGateExecutor;
import com.litesugar.fgateclient.LibWebSocket.AsyncSocketClient;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class FGateClient extends JavaPlugin {
    public AsyncSocketClient client = new AsyncSocketClient();
    public Session session = null;
    Logger logger = getLogger();

    @Override
    public void onEnable() {
        String version = getDescription().getVersion();
        int pluginId = 26085;
        logger.info("\033[33mFGate-Client\033[32m  " + version + " \033[33m正在加载......\033[0m");
        logger.info("\033[33m正在加载配置文件......\033[0m");
        saveDefaultConfig();
        reloadConfig();
        new Metrics(this, pluginId);
        logger.info("正在注册事件......");
        // Objects.requireNonNull(Bukkit.getPluginCommand("fgate")).setExecutor(new FGateExecutor());
        // 指令下次再做~
        Bukkit.getPluginManager().registerEvents(new EventHandler(), this);
        String serverAddress = getConfig().getString("serverAddress");
        String authToken = getConfig().getString("authToken");
        logger.info("\033[33m管理面板地址：\033[32m" + serverAddress + "\033[0m");

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        URI uri = null;

        try {
            if (serverAddress != null) {
                uri = new URI(serverAddress);
            } else {
                throw new NullPointerException("请填写管理面板地址!URI不可为null！");
            }
            if (authToken == null) {
                logger.warning("\033[33m请填写鉴权Token！\033[0m");
                throw new NullPointerException("请填写鉴权Token！");
            }
        } catch (URISyntaxException e) {
            logger.warning("\033[33m管理面板地址格式错误，请检查配置文件！\033[0m");
            getServer().getPluginManager().disablePlugin(this);
        }

        // 连接服务器

        try {

            ClientEndpointConfig config = ClientEndpointConfig.Builder.create()
                    .configurator(new ClientEndpointConfig.Configurator() {
                        @Override
                        public void beforeRequest(Map<String, List<String>> headers) {
                            // 添加鉴权头
                            headers.put("Authorization", Collections.singletonList(authToken));
                            // API版本
                            headers.put("X-API-Version", Collections.singletonList(String.valueOf(AsyncSocketClient.API_VERSION)));
                        }
                    })
                    .build();
            this.session = container.connectToServer(client, config, uri);
            // 等待连接建立
            client.awaitConnection(5);
        } catch (DeploymentException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }


        // 异步发送文本消息
        client.sendTextAsync("Hello Server!")
                .thenAccept(success -> System.out.println("Text sent: " + success))
                .exceptionally(ex -> {
                    logger.warning("Text send failed: " + ex.getMessage());
                    return null;
                });


        // 异步发送Ping
        client.sendPingAsync()
                .thenAccept(success -> logger.info("Pinged " + success))
                .exceptionally(ex -> {
                    logger.warning("Ping failed: \033[33m" + ex.getMessage() + "\033[0m");
                    return null;
                });

    }

    @Override
    public void onDisable() {
        logger.info("\033[33mFGate-Client正在禁用......\033[0m");
        try {
            if (session != null) {
                logger.info("\033[33m正在释放连接......\033[0m");
                session.close();
            }
        } catch (IOException ignored) {
        }
    }
}
