package me.delta2force.redditbrowser.listeners;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.interaction.InteractiveLocation;
import net.dean.jraw.models.Comment;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class EventListener implements Listener {

    private RedditBrowserPlugin reddit;

    public EventListener(RedditBrowserPlugin reddit) {
        this.reddit = reddit;
    }


    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        // Check if the list contains the players UUID
        if (!reddit.getRedditBrowsers().contains(player.getUniqueId())) {
            return;
        }

        // Set the line to a variable
        String line = event.getLine(0);
        // Make sure the line exist
        if (line != null) {
            // Check if the sign's first line is what we want
            if (!line.startsWith("r/")) {
                return;
            }
            
            line = "";
            
            for(String s : event.getLines()) {
            	line+=s;
            }

            // Get the sub they want
            String sub = line.replaceFirst("r/", "");
            
            // Set the player to spectator mode
            player.setGameMode(GameMode.SPECTATOR);
            // Send them a message
            player.sendMessage(ChatColor.YELLOW + "Please wait...");
            // Teleport them
            player.teleport(player.getLocation().add(0, 400, 0));
            // Add new task
            reddit.getTask().add(reddit.getServer().getScheduler().runTaskAsynchronously(reddit, () -> reddit.createTowerAndTP(player, sub, player.getWorld())));
        }

    }
    
    public InteractiveLocation getInteractionAt(Location loc) {
    	int x = loc.getBlockX();
    	int y = loc.getBlockY();
    	int z = loc.getBlockZ();
    	for(InteractiveLocation inloc : reddit.interactiveSubmissionID.keySet()) {
    		if(inloc.getX() == x && inloc.getY() == y && inloc.getZ() == z) {
    			return inloc;
    		}
    	}
    	return null;
    }
    
    @EventHandler
    public void interact(PlayerInteractEvent event) {
    	 if (!reddit.getRedditBrowsers().contains(event.getPlayer().getUniqueId())) {
             return;
         }
    	 reddit.setKarma(event.getPlayer());
    	if(getInteractionAt(event.getClickedBlock().getLocation()) != null) {
    		InteractiveLocation inLoc = getInteractionAt(event.getClickedBlock().getLocation());
    		if(reddit.interactiveSubmissionID.get(inLoc) == InteractiveEnum.UPVOTE) {
        		String submissionID = inLoc.getSubmissionId();
        		reddit.reddit.submission(submissionID).upvote();
        		event.getPlayer().sendMessage(ChatColor.GREEN + "You have upvoted the post!");
    		}else if(reddit.interactiveSubmissionID.get(inLoc) == InteractiveEnum.DOWNVOTE) {
    			String submissionID = inLoc.getSubmissionId();
        		reddit.reddit.submission(submissionID).downvote();
        		event.getPlayer().sendMessage(ChatColor.RED + "You have downvoted the post!");
        	}
    	}
    }
    
    @EventHandler
    public void closeInventory(InventoryCloseEvent event) {
    	 if (!reddit.getRedditBrowsers().contains(event.getPlayer().getUniqueId())) {
             return;
         }
    	 reddit.setKarma((Player) event.getPlayer());
    	Location blockLocation = event.getInventory().getLocation();
    	if(getInteractionAt(blockLocation) != null) {
    		InteractiveLocation inloc = getInteractionAt(blockLocation);
    		if(reddit.interactiveSubmissionID.get(inloc) == InteractiveEnum.COMMENT_CHEST) {
        		String submissionID = inloc.getSubmissionId();
        		for(ItemStack is : event.getInventory().getContents()) {
        			if(is != null) {
        				if(is.getType() == Material.WRITTEN_BOOK) {
        					BookMeta bm = (BookMeta) is.getItemMeta();
        					if(bm.getAuthor().equals(event.getPlayer().getName())) {
        						String comment = "";
        						for(String page : bm.getPages()) {
        							comment+=page + " ";
        						}
        						Comment redditComment = reddit.reddit.submission(submissionID).reply(comment);
        						event.getPlayer().sendMessage(ChatColor.GREEN + "You have left a comment!");
        						event.getPlayer().getInventory().addItem(new ItemStack(Material.WRITABLE_BOOK));
        						event.getInventory().remove(is);
        					}
        				}
        			}
        		}
    		}
    	}
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
    	if (!reddit.getRedditBrowsers().contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Player player = event.getPlayer();
        reddit.kickOut(player);
    }

}
