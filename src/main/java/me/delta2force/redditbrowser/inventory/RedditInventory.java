package me.delta2force.redditbrowser.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class RedditInventory {
	final ItemStack[] contents;
	final ItemStack[] armorContents;
	final ItemStack[] storageContents;
	final ItemStack[] extraContents;
	final ItemStack itemInOffHand;
	
	public RedditInventory(PlayerInventory inv) {
		contents = inv.getContents();
		armorContents = inv.getArmorContents();
		storageContents = inv.getStorageContents();
		extraContents = inv.getExtraContents();
		itemInOffHand = inv.getItemInOffHand();
	}
	
	public void apply(Player p) {
		p.getInventory().setArmorContents(armorContents);
        p.getInventory().setContents(contents);
        p.getInventory().setStorageContents(storageContents);
        p.getInventory().setExtraContents(extraContents);
        p.getInventory().setItemInOffHand(itemInOffHand);
	}
}
