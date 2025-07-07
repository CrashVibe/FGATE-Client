package com.crashvibe.fgateclient.commands;

import com.crashvibe.fgateclient.FGateClient;
import com.crashvibe.fgateclient.ServiceManager;
import com.crashvibe.fgateclient.service.WebSocketManager;
import com.crashvibe.fgateclient.utils.I18n;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class PlayerBind implements CommandExecutor {
    private static WebSocketManager webSocketManager = null;
    private final I18n i18n = ServiceManager.getInstance().getI18n();

    public static void initWebsocketManager() {
        if (webSocketManager != null) {
            throw new RuntimeException("WebSocketManager has been initialized");
        }
        webSocketManager = ServiceManager.getInstance().getWebSocketManager();
    }

    private boolean unbind_player(CommandSender sender) {
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
        if (!bindResult.get().getAsJsonObject("result").get("isBind").getAsBoolean()) {
            sender.sendMessage(NamedTextColor.GOLD + i18n.get("not_bind_yet"));
            return true;
        } else {
            webSocketManager.sendRequestAsync("player.unbind", params).thenAccept(data::set);
            if (!data.get().getAsJsonObject("result").get("isSuccess").getAsBoolean()) {
                sender.sendMessage(NamedTextColor.RED + i18n.get("unbind_fail"));
                sender.sendMessage(NamedTextColor.RED + data.get().getAsJsonObject("result").get("message").getAsString());
                return true;
            }
            sender.sendMessage(NamedTextColor.GOLD + i18n.get("unbind_success"));
            player.kickPlayer(i18n.get("player_unbind"));
            return true;

        }
    }

    private boolean bind_player(CommandSender sender) {
        JsonObject params = new JsonObject();
        AtomicReference<JsonObject> bindResult = new AtomicReference<>(new JsonObject());
        AtomicReference<JsonObject> data = new AtomicReference<>(new JsonObject());
        if (sender instanceof Player player) {
            params.addProperty("playerName", player.getName());
            params.addProperty("playerUUID", player.getUniqueId().toString());
        } else {
            sender.sendMessage(NamedTextColor.RED + i18n.get("player_only"));
            return true;
        }


        webSocketManager.sendRequestAsync("player.bindQuery", params).thenAccept(bindResult::set);
        if (bindResult.get().getAsJsonObject("result").get("isBind").getAsBoolean()) {
            sender.sendMessage(NamedTextColor.GOLD + i18n.get("already_bind"));
        } else {
            webSocketManager.sendRequestAsync("player.bind", params).thenAccept(data::set);
            String auth_code = data.get().getAsJsonObject("result").get("authCode").getAsString();
            HashMap<String, String> formatParam = new HashMap<>();
            formatParam.put("auth_code", auth_code);
            sender.sendMessage(NamedTextColor.GOLD + i18n.format("got_auth_code", formatParam));

        }
        return true;

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        FGateClient plugin = FGateClient.getInstance();
        String fgate_info = NamedTextColor.GOLD + "FLOW GATE" + NamedTextColor.GREEN + plugin.getPluginMeta().getVersion();
        switch (command.getName()) {
            case "fgate" -> {
                if (args.length == 0) {
                    sender.sendMessage(fgate_info);
                    return true;
                } else {
                    if (args[0].equals("info")) {
                        sender.sendMessage(fgate_info);
                        return true;
                    }


                }
            }
            case "bind" -> {
                return bind_player(sender);

            }
            case ("unbind") -> {
                return unbind_player(sender);
            }
        }
        return false;
    }
}
