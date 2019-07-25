package me.delta2force.redditbrowser.interaction;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class InteractiveLocation {
	private int x;
	private int y;
	private int z;
	private InteractiveEnum interaction;
	
	public InteractiveLocation(Location loc, InteractiveEnum interaction) {
		this.x=loc.getBlockX();
		this.y=loc.getBlockY();
		this.z=loc.getBlockZ();
		this.interaction = interaction;
	}
	
	public Location toLocation() {
		return new Location(Bukkit.getWorld("reddit"), x, y, z);
	}
	
	public InteractiveEnum getInteraction() {
		return interaction;
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
