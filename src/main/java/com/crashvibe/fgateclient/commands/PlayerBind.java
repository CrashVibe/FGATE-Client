package com.crashvibe.fgateclient.commands;

import com.crashvibe.fgateclient.FGateClient;
import com.crashvibe.fgateclient.service.WebSocketManager;
import com.crashvibe.fgateclient.utils.I18n;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import com.crashvibe.fgateclient.manager.ServiceManager;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class PlayerBind implements CommandExecutor {
    private final I18n i18n = ServiceManager.getInstance().getI18n();
    private static WebSocketManager webSocketManager = null;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        FGateClient plugin = FGateClient.getInstance();
        switch (command.getName()) {
            case "fgate" -> {
                if (args.length == 0 || args.length == 1) {
                    sender.sendMessage(NamedTextColor.GOLD + "FLOW GATE" + NamedTextColor.GREEN + plugin.getPluginMeta().getVersion());
                    return true;
                }
            }
            case "bind" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(NamedTextColor.RED + i18n.get("player_only"));
                    return true;
                }
                JsonObject params = new JsonObject();
                AtomicReference<JsonObject> bindResult = new AtomicReference<>(new JsonObject());
                AtomicReference<JsonObject> data = new AtomicReference<>(new JsonObject());

                params.addProperty("playerName", player.getName());
                params.addProperty("playerUUID", player.getUniqueId().toString());

                webSocketManager.sendRequestAsync("player.bindQuery", params).thenAccept(bindResult::set);
                if (bindResult.get().getAsJsonObject("result").get("isBind").getAsBoolean()) {
                    sender.sendMessage(NamedTextColor.GOLD + i18n.get("already_bind"));
                    return true;
                } else {
                    webSocketManager.sendRequestAsync("player.bind", params).thenAccept(data::set);
                    String auth_code = data.get().getAsJsonObject("result").get("authCode").getAsString();
                    HashMap<String, String> formatParam = new HashMap<>();
                    formatParam.put("auth_code", auth_code);
                    sender.sendMessage(NamedTextColor.GOLD + i18n.format("got_auth_code", formatParam));

                }

            }
        }
        return false;
    }

    public static void initWebsocketManager() {
        if (webSocketManager != null) {
            throw new RuntimeException("WebSocketManager has been initialized");
        }
        webSocketManager = ServiceManager.getInstance().getWebSocketManager();
    }
}
