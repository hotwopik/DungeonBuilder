package io.hotwop.dungeonbl.util.math;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public sealed interface AsVec permits Vec3, Vec3d{
    Location toLocation(World world);
    Vector toVector();

    double length();

    AsVec add(AsVec other);
    AsVec sub(AsVec other);
}
