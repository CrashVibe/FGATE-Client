package com.litesuggar.fgateclient.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.litesuggar.fgateclient.FGateClient;
import com.litesuggar.fgateclient.WebSocketManager;

public class OnJoin implements Listener {
    private final FGateClient plugin;
    private final WebSocketManager webSocketManager;

    public OnJoin(FGateClient plugin) {
        this.plugin = plugin;
        this.webSocketManager = plugin.getWebSocketManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.foliaLib.getScheduler().runAsync(task -> {
            try {
                if (webSocketManager != null && webSocketManager.isConnected()) {
                    plugin.getLogger().info("发送玩家加入事件: " + event.getPlayer().getName());
                    webSocketManager.sendPlayerJoinEvent(
                            event.getPlayer().getName(),
                            event.getPlayer().getUniqueId().toString(),
                            System.currentTimeMillis()
                    );
                }
            } catch (Exception e) {
                plugin.getLogger().warning("发送玩家加入事件失败：" + e.getMessage());
            }
        });
    }
}
