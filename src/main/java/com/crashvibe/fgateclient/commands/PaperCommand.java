package com.crashvibe.fgateclient.commands;

import com.crashvibe.fgateclient.FGateClient;
import com.crashvibe.fgateclient.service.WebSocketManager;
import com.crashvibe.fgateclient.utils.I18n;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public class PaperCommand {
    private static final I18n i18n = FGateClient.getInstance().getServiceManager().getI18n();
    private static final WebSocketManager webSocketManager = FGateClient.getInstance().getServiceManager().getWebSocketManager();

    private static int unbind_player(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(NamedTextColor.RED + i18n.get("player_only"));
            return Command.SINGLE_SUCCESS;
        }
        JsonObject params = new JsonObject();
        AtomicReference<JsonObject> bindResult = new AtomicReference<>(new JsonObject());
        AtomicReference<JsonObject> data = new AtomicReference<>(new JsonObject());

        params.addProperty("playerName", player.getName());
        params.addProperty("playerUUID", player.getUniqueId().toString());

        webSocketManager.sendRequestAsync("player.bindQuery", params).thenAccept(bindResult::set);
        if (!bindResult.get().getAsJsonObject("result").get("isBind").getAsBoolean()) {
            sender.sendMessage(NamedTextColor.GOLD + i18n.get("not_bind_yet"));
        } else {
            webSocketManager.sendRequestAsync("player.unbind", params).thenAccept(data::set);
            if (!data.get().getAsJsonObject("result").get("isSuccess").getAsBoolean()) {
                sender.sendMessage(NamedTextColor.RED + i18n.get("unbind_fail"));
                sender.sendMessage(NamedTextColor.RED + data.get().getAsJsonObject("result").get("message").getAsString());
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(NamedTextColor.GOLD + i18n.get("unbind_success"));
            player.kickPlayer(i18n.get("player_unbind"));

        }
        return Command.SINGLE_SUCCESS;
    }

    private static int bind_player(CommandSender sender) {
        JsonObject params = new JsonObject();
        AtomicReference<JsonObject> bindResult = new AtomicReference<>(new JsonObject());
        AtomicReference<JsonObject> data = new AtomicReference<>(new JsonObject());
        if (sender instanceof Player player) {
            params.addProperty("playerName", player.getName());
            params.addProperty("playerUUID", player.getUniqueId().toString());
        } else {
            sender.sendMessage(NamedTextColor.RED + i18n.get("player_only"));
            return Command.SINGLE_SUCCESS;
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
        return Command.SINGLE_SUCCESS;

    }
    public static LiteralCommandNode<CommandSourceStack> bindCommand(){
        return Commands.literal("bind").requires(source -> source.getSender().hasPermission("fgate.player.bind")).executes(context -> bind_player(context.getSource().getSender()))

                .build();

    }
    public static LiteralCommandNode<CommandSourceStack> unbindCommand(){
        return Commands.literal("unbind").requires(source -> source.getSender().hasPermission("fgate.player.unbind")).executes(context -> unbind_player(context.getSource().getSender()))

                .build();

    }
    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("fgate")
                .then(Commands.literal("info")
                        .executes(context -> {
                            sendInfoMessage(context.getSource().getSender());
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("bind")
                        .requires(source -> source.getSender().hasPermission("fgate.admin.bind"))
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            if (!(sender instanceof Player)) {
                                sender.sendMessage(text(i18n.get("player_only"), RED));
                                return Command.SINGLE_SUCCESS;
                            }

                            return bind_player(sender);
                        }))
                .then(Commands.literal("unbind")
                        .requires(source -> source.getSender().hasPermission("fgate.admin.unbind"))
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            if (!(sender instanceof Player)) {
                                sender.sendMessage(text(i18n.get("player_only"), RED));
                                return Command.SINGLE_SUCCESS;
                            }

                            return unbind_player(sender);
                        }))
                .executes(context -> {
                    sendInfoMessage(context.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }

    private static void sendInfoMessage(CommandSender sender) {
        FGateClient plugin = FGateClient.getInstance();
        sender.sendMessage(text(
                "FLOW GATE" + plugin.getPluginMeta().getVersion(),
                GOLD
        ));
    }
}
