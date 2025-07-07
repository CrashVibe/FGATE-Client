package com.crashvibe.fgateclient;

import com.crashvibe.fgateclient.commands.PaperCommand;
import com.crashvibe.fgateclient.listeners.OnChatMessage;
import com.crashvibe.fgateclient.listeners.OnJoin;
import com.crashvibe.fgateclient.utils.EventUtil;
import com.crashvibe.fgateclient.utils.I18n;
import com.tcoded.folialib.FoliaLib;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

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
        saveDefaultConfig();
        reloadConfig();

        logger.info("Configuration loaded, initializing services...");
        serviceManager = new ServiceManager(logger, foliaLib, new ConfigManager(this),
                getPluginMeta().getVersion(), new I18n(getDataFolder()));

        new Metrics(this, 26085);
        logger.info("bStats Hook Enabled!");
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(PaperCommand.createCommand(), "Flow Gate主命令"));
        CompletableFuture.runAsync(() -> {
            try {
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
        } else {
            logger.info("FGateClient plugin disabled!");
        }
    }

    private void initListeners() {
        EventUtil.registerEvents(this,
                new OnJoin(this),
                new OnChatMessage(this));
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

}