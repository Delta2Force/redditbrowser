package me.delta2force.redditbrowser.math;

import org.bukkit.Location;
import org.bukkit.World;

public class InteractiveLocation {
	private int x;
	private int y;
	private int z;
	private World world;
	
	public InteractiveLocation(Location loc) {
		this.x=loc.getBlockX();
		this.y=loc.getBlockY();
		this.z=loc.getBlockZ();
		this.world=loc.getWorld();
	}
	
	public Location toLocation() {
		return new Location(world, x, y, z);
	}
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getZ() {
		return z;
	}
}
