package com.litesugar.fgateclient.LibWebSocket.Handlers.notification;

import com.litesugar.fgateclient.FGateClient;
import com.litesugar.fgateclient.LibWebSocket.Handlers.NoticeHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.logging.Logger;

public class Kick implements NoticeHandler {

    public void handle(@NotNull JSONObject message){
        Logger logger = FGateClient.getPlugin(FGateClient.class).getLogger();
        try {
            Player player = FGateClient.getPlugin(FGateClient.class)
                    .getServer()
                    .getPlayer(message
                            .getJSONObject("params")
                            .getString("player"));
            String reason = "你被移出服务器。";
            if(message.getJSONObject("params").has("reason")){
                reason = message
                        .getJSONObject("params")
                        .getString("reason");
            }
            player.kickPlayer(reason);

        }catch (Exception e){
            logger.warning("Player "+message.getJSONObject("params").getString("player")+" not found!");


        }
    }
}
