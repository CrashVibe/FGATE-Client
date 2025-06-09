package com.litesuggar.fgateclient;

import com.litesuggar.fgateclient.config.ConfigManager;
import com.litesuggar.fgateclient.listeners.OnJoin;
import com.litesuggar.fgateclient.manager.ServiceManager;
import com.litesuggar.fgateclient.utils.EventUtil;
import com.tcoded.folialib.FoliaLib;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class FGateClient extends JavaPlugin {

    public final FoliaLib foliaLib = new FoliaLib(this);
    public static final Logger LOGGER = LogManager.getLogger();
    private static FGateClient instance;

    private ConfigManager configManager;
    private ServiceManager serviceManager;

    @Override
    public void onLoad() {
        instance = this;
        LOGGER.info("FGateClient plugin is loading...");
    }

    @Override
    public void onEnable() {
        try {
            // 初始化配置管理器
            configManager = new ConfigManager(this); // 初始化服务管理器
            serviceManager = new ServiceManager(getLogger(), foliaLib, configManager, getPluginMeta().getVersion());

            // 初始化 bStats
            new Metrics(this, 26085);
            LOGGER.info("bStats Hook Enabled!");

            // 注册事件监听器
            initListeners();

            // 异步启动服务
            foliaLib.getScheduler().runAsync(task -> {
                try {
                    serviceManager.startServices();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to start services", e);
                }
            });

            getLogger().info("FGateClient plugin enabled successfully!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable plugin", e);
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (serviceManager != null) {
            serviceManager.stopServices();
        }
        getLogger().info("FGateClient plugin disabled!");
    }

    private void initListeners() {
        EventUtil.registerEvents(
                new OnJoin(this));
    }

    public static FGateClient getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }
}
