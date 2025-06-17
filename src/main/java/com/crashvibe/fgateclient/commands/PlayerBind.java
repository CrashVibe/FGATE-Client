package com.crashvibe.fgateclient.commands;

import com.crashvibe.fgateclient.FGateClient;
import com.crashvibe.fgateclient.utils.I18n;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import com.crashvibe.fgateclient.manager.ServiceManager;
public final class PlayerBind implements CommandExecutor {
    I18n i18n = ServiceManager.getInstance().getI18n();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args) {
        FGateClient plugin = FGateClient.getInstance();
        switch (command.getName()){
            case "fgate" ->{
                if(args.length==0 || args.length==1){
                    sender.sendMessage(NamedTextColor.GOLD+"FLOW GATE"+NamedTextColor.GREEN+plugin.getPluginMeta().getVersion());
                    return true;
                }
            }case "bind"->{
                sender.sendMessage(NamedTextColor.RED+i18n.get("unavailable"));
            }
        }
        return false;
    }
}
