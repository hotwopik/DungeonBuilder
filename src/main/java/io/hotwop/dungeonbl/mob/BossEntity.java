package io.hotwop.dungeonbl.mob;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public record BossEntity(
    LivingEntity entity,
    @Nullable BossBar bossbar,
    double max
){
    public boolean update(){
        if(bossbar!=null)bossbar.setProgress(entity.getHealth()/max);
        if(entity.isDead()){
            if(bossbar!=null)bossbar.removeAll();
            return true;
        }
        return false;
    }
    public void remove(){
        if(bossbar!=null)bossbar.removeAll();
        entity.remove();
    }
}
