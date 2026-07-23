package io.hotwop.dungeonbl.versioned;

import org.bukkit.attribute.Attribute;

public record DBlAttributeMapping(
    Attribute movementSpeed,
    Attribute attackDamage,
    Attribute jumpStrength
){
}
