package com.crashvibe.fgateclient.commands;

import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class Tab implements TabCompleter {
    @Override
    public List<String> onTabComplete(org.bukkit.command.@NotNull CommandSender sender, org.bukkit.command.Command command, @NotNull String alias, String @NotNull [] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("fgate")) {
            switch (args.length) {
                case 1 -> {
                    completions.add("info");
                    if (sender.hasPermission("fgate.admin.bind")) {
                        completions.add("bind");
                    }
                    if (sender.hasPermission("fgate.admin.unbind")) {
                        completions.add("unbind");
                    }
                }
            }
        }
        return completions;
    }
}
