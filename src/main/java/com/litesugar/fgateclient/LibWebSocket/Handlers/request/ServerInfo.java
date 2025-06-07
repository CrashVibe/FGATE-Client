package com.litesugar.fgateclient.LibWebSocket.Handlers.request;

import com.litesugar.fgateclient.FGateClient;
import com.litesugar.fgateclient.LibWebSocket.Handlers.Handler;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class ServerInfo implements Handler {
    @NotNull
    public JSONObject handle(@NotNull JSONObject message) {
        JSONObject result = new JSONObject();
        JSONObject json = new JSONObject();
        json.put("jsonrpc", "2.0").put("id", message.get("id"));

        try {
            JSONObject data = new JSONObject();
            result.put("status", "success").put("message", "获取成功");
            data
                    .put("server_version", FGateClient.getPlugin(FGateClient.class).getServer().getVersion())
                    .put("server_platform", System.getProperty("os.name"))
                    // .put("bukkit_version", FGateClient.getPlugin(FGateClient.class).getServer().getBukkitVersion())
                    .put("max_players", FGateClient.getPlugin(FGateClient.class).getServer().getMaxPlayers())
                    .put("online_players_count", FGateClient.getPlugin(FGateClient.class).getServer().getOnlinePlayers().size());
            result.put("data", data);
            json.put("result", result);
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("code", -32603).put("message", "Error!" + e.getMessage()).put("status", "error");
            json.put("result", error);
        }
        return json;
    }
}
