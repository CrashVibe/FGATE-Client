package com.litesugar.fgateclient.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import com.litesugar.fgateclient.FGateClient;

import java.util.logging.Logger;

public final class FGateExecutor implements CommandExecutor {
    Logger logger = FGateClient.getPlugin(FGateClient.class).getLogger();

    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("用法: /fgate <command>");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            try {
                com.litesugar.fgateclient.FGateClient.getPlugin(com.litesugar.fgateclient.FGateClient.class).reloadConfig();
                sender.sendMessage(ChatColor.GREEN+"已重载配置文件.");
            } catch (Exception e) {
                String msg = ChatColor.RED + "重载配置文件失败： " + e.getMessage();
                sender.sendMessage(msg);
                logger.warning(msg);

            }
            return true;
        }
        return false;
    }
}
