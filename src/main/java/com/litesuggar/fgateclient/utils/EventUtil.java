package com.litesuggar.fgateclient.utils;

import com.litesuggar.fgateclient.FGateClient;
import org.bukkit.event.Listener;

public class EventUtil {

    public static void registerEvents(Listener... listeners) {
        for (Listener listener : listeners) {
            FGateClient.getInstance().getServer().getPluginManager().registerEvents(listener,
                    FGateClient.getInstance());
        }
    }
}