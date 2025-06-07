package com.litesugar.fgateclient.LibWebSocket.Handlers;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public interface Handler {
    @NotNull
    JSONObject handle(@NotNull JSONObject message);
}
