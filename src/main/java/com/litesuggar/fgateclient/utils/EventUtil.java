package com.litesuggar.fgateclient.utils;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class EventUtil {

    public static void registerEvents(JavaPlugin plugin, Listener... listeners) {
        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}