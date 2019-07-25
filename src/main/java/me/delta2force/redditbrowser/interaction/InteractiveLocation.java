package me.delta2force.redditbrowser.interaction;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class InteractiveLocation {
	private int x;
	private int y;
	private int z;
	private String submissionId;
	
	public InteractiveLocation(Location loc, String submissionId) {
		this.x=loc.getBlockX();
		this.y=loc.getBlockY();
		this.z=loc.getBlockZ();
		this.submissionId = submissionId;
	}
	
	public Location toLocation() {
		return new Location(Bukkit.getWorld("reddit"), x, y, z);
	}
	
	public String getSubmissionId() {
		return submissionId;
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
