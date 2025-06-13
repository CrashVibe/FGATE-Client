package com.litesuggar.fgateclient;

import com.litesuggar.fgateclient.config.ConfigManager;
import com.litesuggar.fgateclient.listeners.OnJoin;
import com.litesuggar.fgateclient.manager.ServiceManager;
import com.litesuggar.fgateclient.utils.EventUtil;
import com.tcoded.folialib.FoliaLib;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

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
        try {
            // 初始化配置管理器
            ConfigManager configManager = new ConfigManager(this); // 初始化服务管理器
            serviceManager = new ServiceManager(logger, foliaLib, configManager, getPluginMeta().getVersion());

            // 初始化 bStats
            new Metrics(this, 26085);
            logger.info("bStats Hook Enabled!");

            // 注册事件监听器
            initListeners();

            // 异步启动服务
            foliaLib.getScheduler().runAsync(task -> {
                try {
                    serviceManager.startServices();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to start services", e);
                }
            });

            logger.info("FGateClient plugin enabled successfully!");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to enable plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (serviceManager != null) {
            serviceManager.stopServices();
        }
        logger.info("FGateClient plugin disabled!");
    }

    private void initListeners() {
        EventUtil.registerEvents(
                new OnJoin(this));
    }

    /*
        public ConfigManager getConfigManager() {
            return configManager;
        }
    */
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

}
