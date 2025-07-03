package com.crashvibe.fgateclient.listeners;

import com.crashvibe.fgateclient.FGateClient;
import com.crashvibe.fgateclient.manager.ServiceManager;
import com.crashvibe.fgateclient.service.WebSocketManager;
import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 聊天消息监听器 - 监听玩家聊天消息并发送到主机端
 */
public class OnChatMessage implements Listener {
    private final FGateClient plugin;

    public OnChatMessage(FGateClient plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        ServiceManager serviceManager = plugin.getServiceManager();
        if (serviceManager == null) {
            return;
        }

        WebSocketManager webSocketManager = serviceManager.getWebSocketManager();
        if (webSocketManager == null || !webSocketManager.isConnected()) {
            return;
        }

        String playerName = event.getPlayer().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        long timestamp = System.currentTimeMillis();

        // 构建参数 JSON 对象
        JsonObject paramsJson = new JsonObject();
        paramsJson.addProperty("player", playerName);
        paramsJson.addProperty("message", message);
        paramsJson.addProperty("timestamp", timestamp);
        paramsJson.addProperty("uuid", event.getPlayer().getUniqueId().toString());


        try {
            webSocketManager.sendNotificationAsync("mc.chat", paramsJson);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to send chat notification: " + ex.getMessage());
        }

        if (serviceManager.getConfigManager().getConfig().getBoolean("debug.enable")) {
            plugin.getLogger().info("Chat notification sent to host: " + playerName + " -> " + message);
        }
    }
}
