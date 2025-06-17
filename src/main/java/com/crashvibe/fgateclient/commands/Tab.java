package com.crashvibe.fgateclient.commands;

import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public final class Tab implements TabCompleter {
    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("fgate")) {
            switch (args.length) {
                case 1 -> {
                completions.add("info");
                }
            }
        }
        return completions;
    }
}
