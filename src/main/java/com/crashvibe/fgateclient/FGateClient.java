package com.crashvibe.fgateclient;

import com.crashvibe.fgateclient.commands.PlayerBind;
import com.crashvibe.fgateclient.commands.Tab;
import com.crashvibe.fgateclient.config.ConfigManager;
import com.crashvibe.fgateclient.listeners.OnChatMessage;
import com.crashvibe.fgateclient.listeners.OnJoin;
import com.crashvibe.fgateclient.manager.ServiceManager;
import com.crashvibe.fgateclient.utils.EventUtil;
import com.crashvibe.fgateclient.utils.I18n;
import com.tcoded.folialib.FoliaLib;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class FGateClient extends JavaPlugin {

    private static FGateClient instance;
    public final FoliaLib foliaLib = new FoliaLib(this);
    public final Logger logger = getLogger();
    private ServiceManager serviceManager;

    public static FGateClient getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        logger.info("FGateClient plugin is loading...");
    }

    @Override
    public void onEnable() {
        // 异步初始化插件，避免阻塞服务器启动
        CompletableFuture.runAsync(() -> {
            try {
                // 异步加载配置文件
                saveDefaultConfig();
                reloadConfig();

                logger.info("Configuration loaded, initializing services...");

                // 异步创建服务管理器
                serviceManager = new ServiceManager(logger, foliaLib, new ConfigManager(this),
                        getPluginMeta().getVersion(), new I18n(getDataFolder()));

                // 初始化 bStats
                new Metrics(this, 26085);
                logger.info("bStats Hook Enabled!");

                // 在主线程注册事件监听器（必须在主线程）
                getServer().getScheduler().runTask(this, this::initListeners);


                // 异步启动服务
                serviceManager.startServicesAsync()
                        .thenRun(() -> logger.info("FGateClient plugin enabled successfully!"))
                        .exceptionally(throwable -> {
                            logger.log(Level.SEVERE, "Failed to start services", throwable);
                            // 在主线程禁用插件
                            getServer().getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
                            return null;
                        });

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to enable plugin", e);
                // 在主线程禁用插件
                getServer().getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
            }
        }).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "Failed to initialize plugin asynchronously", throwable);
            return null;
        });

        // 立即返回，不阻塞服务器启动
        logger.info("FGateClient plugin initialization started asynchronously...");
    }

    @Override
    public void onDisable() {
        if (serviceManager != null) {
            // 异步停止服务以避免阻塞服务器关闭
            serviceManager.stopServicesAsync()
                    .thenRun(() -> logger.info("FGateClient plugin disabled successfully!"))
                    .exceptionally(throwable -> {
                        logger.warning("Error during async shutdown: " + throwable.getMessage());
                        return null;
                    });

            // 给异步操作一些时间完成，但不无限等待
            try {
                Thread.sleep(1000); // 最多等待1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            logger.info("FGateClient plugin disabled!");
        }
    }

    private void initListeners() {
        EventUtil.registerEvents(this,
                new OnJoin(this),
                new OnChatMessage(this));
        Objects.requireNonNull(Bukkit.getPluginCommand("fgate")).setExecutor(new PlayerBind());
        Bukkit.getPluginCommand("fgate").setTabCompleter(new Tab());
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

}
