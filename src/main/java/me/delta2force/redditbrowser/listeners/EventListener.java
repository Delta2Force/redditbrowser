package me.delta2force.redditbrowser.listeners;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.interaction.InteractiveLocation;
import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.google.gson.Gson;

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
    public void interactInventory(InventoryClickEvent event) {
    	Player p = (Player) event.getWhoClicked();
    	if (!reddit.getRedditBrowsers().contains(p.getUniqueId())) {
            return;
        }
    	if(event.getCurrentItem().getType().equals(Material.WRITTEN_BOOK) && event.getInventory().getType().equals(InventoryType.CHEST) && event.isRightClick()) {
    		if(!event.getView().getTitle().startsWith("Comment ")) {
    			if(getInteractionAt(event.getClickedInventory().getLocation()) == null) {
    				return;
    			}
    			return;
    		}
    		String commentID = event.getCurrentItem().getItemMeta().getLore().get(0);
    		List<CommentNode<Comment>> replies = reddit.commentCache.get(commentID).getReplies();
    		Inventory commentInventory = reddit.getServer().createInventory(event.getClickedInventory().getHolder(), InventoryType.CHEST, "Comment " + commentID);
    		int in = 0;
            for (CommentNode<Comment> cn : replies) {
                Comment c = cn.getSubject();
                if (in < 26) {
                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta bookmeta = (BookMeta) book.getItemMeta();
                    bookmeta.setTitle("Comment");
                    bookmeta.setAuthor("u/" + c.getAuthor());
                    if (c.getBody().length() > 255) {
                        double f = Math.ceil(((float) c.getBody().length()) / 255f);
                        for (int i = 0; i < f; i++) {
                            if (c.getBody().length() < (i + 1) * 255) {
                                bookmeta.addPage(c.getBody().substring(i * 255));
                            } else {
                                bookmeta.addPage(c.getBody().substring(i * 255, (i + 1) * 255));
                            }
                        }
                    } else {
                        bookmeta.addPage(c.getBody());
                    }
                    bookmeta.setLore(Arrays.asList(c.getId()));
                    book.setItemMeta(bookmeta);
                    reddit.commentCache.put(c.getId(), cn);
                    commentInventory.addItem(book);
                } else {
                    break;
                }
                in++;
            }
            p.openInventory(commentInventory);
    	}
    }
    
    @EventHandler
    public void closeInventory(InventoryCloseEvent event) {
    	 if (!reddit.getRedditBrowsers().contains(event.getPlayer().getUniqueId())) {
             return;
         }
    	 reddit.setKarma((Player) event.getPlayer());
    	 if(event.getView().getTitle().startsWith("Comment ")) {
     		String commentID = event.getView().getTitle().split(" ")[1];
     		for(ItemStack is : event.getInventory().getContents()) {
     			if(is != null) {
     				if(is.getType() == Material.WRITTEN_BOOK) {
     					BookMeta bm = (BookMeta) is.getItemMeta();
     					if(bm.getAuthor().equals(event.getPlayer().getName())) {
     						String comment = "";
     						for(String page : bm.getPages()) {
     							comment+=page + " ";
     						}
     						reddit.commentCache.get(commentID).getSubject().toReference(reddit.reddit).reply(comment);
     						event.getPlayer().sendMessage(ChatColor.GREEN + "You have left a comment on a comment!");
     						event.getPlayer().getInventory().addItem(new ItemStack(Material.WRITABLE_BOOK));
     						event.getInventory().remove(is);
     					}
     				}
     			}
     		}
 		}else if(getInteractionAt(event.getInventory().getLocation()) != null) {
    		InteractiveLocation inloc = getInteractionAt(event.getInventory().getLocation());
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
            						reddit.reddit.submission(submissionID).reply(comment);
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
