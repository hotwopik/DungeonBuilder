package io.hotwop.dungeonbl.mark.place;

import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import org.bukkit.potion.PotionEffectType;

public record PotionData(
    PotionEffectType effectType,
    int duration,
    int amplifier
) implements MarkPlaceData {
    public PotionData(MarkCodec.Info info) {
        this(
            info.get("effect", PotionEffectType.class),
            info.get("duration", int.class),
            info.get("amplifier", int.class)
        );
    }
}
