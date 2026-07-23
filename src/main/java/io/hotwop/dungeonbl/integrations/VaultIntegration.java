package io.hotwop.dungeonbl.integrations;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultIntegration{
    private final Economy economyService;

    public VaultIntegration(){
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if(rsp==null)throw new RuntimeException("Economy service not found!");
        economyService=rsp.getProvider();
    }

    public void deposit(Player player,double amount){
        if(economyService.isEnabled()){
            economyService.depositPlayer(player,amount);
        }
    }
}
