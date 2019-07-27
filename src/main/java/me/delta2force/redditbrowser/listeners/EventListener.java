package me.delta2force.redditbrowser.listeners;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.renderer.RedditRenderer;
import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;

import org.bukkit.metadata.MetadataValue;
import org.bukkit.scoreboard.*;

public class EventListener implements Listener {

    private static final float VOLUME = 75f;
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

            for (String s : event.getLines()) {
                line += s;
            }

            // Get the sub they want
            String sub = line.replaceFirst("r/", "");

            // If it's pewds' subreddit, display LWIAY title
            if (sub.equalsIgnoreCase("pewdiepiesubmissions")) {
                player.sendTitle(reddit.colorCode("4") + "L" + reddit.colorCode("a") + "W" + reddit.colorCode("1") + "I" + reddit.colorCode("d") + "A" + reddit.colorCode("e") + "Y", "", 10, 70, 20);
            }

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

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if (!reddit.getRedditBrowsers().contains(event.getPlayer().getUniqueId())) {
            return;
        }
        //reddit.setKarma(event.getPlayer()); Overloading the server
        if (event.getClickedBlock() != null && event.getClickedBlock().hasMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM)) {
            List<MetadataValue> metadata = event.getClickedBlock().getMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM);

            if (metadataContains(metadata, InteractiveEnum.UPVOTE)) {
                String submissionID = event.getClickedBlock().getMetadata(RedditBrowserPlugin.SUBMISSION_ID).get(0).asString();

                Bukkit.getScheduler().runTaskAsynchronously(reddit, new Runnable() {
                    @Override
                    public void run() {
                        event.getPlayer().playSound(event.getClickedBlock().getLocation(), Sound.ENTITY_VILLAGER_YES, VOLUME, 1);
                        reddit.reddit.submission(submissionID).upvote();
                        int karma = reddit.reddit.submission(submissionID).inspect().getScore();
                        event.getPlayer().sendMessage(ChatColor.GREEN + "You have upvoted the post! It now has " + karma + " karma.");
                    }
                });
            } else if (metadataContains(metadata, InteractiveEnum.DOWNVOTE)) {
                String submissionID = event.getClickedBlock().getMetadata(RedditBrowserPlugin.SUBMISSION_ID).get(0).asString();

                Bukkit.getScheduler().runTaskAsynchronously(reddit, new Runnable() {
                    @Override
                    public void run() {
                        event.getPlayer().playSound(event.getClickedBlock().getLocation(), Sound.ENTITY_VILLAGER_NO, VOLUME, 1);
                        reddit.reddit.submission(submissionID).downvote();
                        int karma = reddit.reddit.submission(submissionID).inspect().getScore();
                        event.getPlayer().sendMessage(ChatColor.RED + "You have downvoted the post! It now has " + karma + " karma.");
                    }
                });
            } else if (metadataContains(metadata, InteractiveEnum.ROOM_ENTERED)) {
                Bukkit.getScheduler().runTask(reddit, new Runnable() {
                    @Override
                    public void run() {
                        String title = event.getClickedBlock().getMetadata(RedditBrowserPlugin.REDDIT_POST_TITLE).get(0).asString();
                        Integer score = event.getClickedBlock().getMetadata(RedditBrowserPlugin.REDDIT_POST_SCORE).get(0).asInt();
                        String author = event.getClickedBlock().getMetadata(RedditBrowserPlugin.REDDIT_POST_AUTHOR).get(0).asString();
                        Objective objective = event.getPlayer().getScoreboard().getObjective("reddit");
                        if(objective == null || !StringUtils.equalsIgnoreCase(RedditBrowserPlugin.chopOffTitle(title), objective.getDisplayName())) {
                            reddit.updateTitle(title, author, score, event.getPlayer());
                        }
                    }
                });
            }
        }
    }



    @EventHandler
    public void interactInventory(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        if (!reddit.getRedditBrowsers().contains(p.getUniqueId())) {
            return;
        }
        if (event.getCurrentItem() != null
                && Material.WRITTEN_BOOK.equals(event.getCurrentItem().getType())
                && InventoryType.CHEST.equals(event.getInventory().getType())
                && event.isRightClick()) {
            if (!event.getView().getTitle().startsWith("Comment ")) {
                if (event.getClickedInventory() != null) {
                    return;
                }
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
                    bookmeta.setLore(Arrays.asList(c.getId(), c.getBody()));
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
    public void interactEntity(PlayerInteractAtEntityEvent event) {
        Player p = event.getPlayer();
        if (event.getRightClicked() instanceof ItemFrame) {
            ItemFrame itemframe = (ItemFrame) event.getRightClicked();
            if (Material.FILLED_MAP.equals(itemframe.getItem().getType())) {
                ItemStack map = itemframe.getItem();
                if (map.getItemMeta() != null) {
                    MapMeta mapMeta = (MapMeta) map.getItemMeta();
                    if (mapMeta.getMapView() != null && mapMeta.getMapView().getRenderers() != null) {
                        for (MapRenderer mr : mapMeta.getMapView().getRenderers()) {
                            if (mr instanceof RedditRenderer) {
                                //Bingo!
                                RedditRenderer rr = (RedditRenderer) mr;
                                p.sendMessage(ChatColor.YELLOW + "Image/Video link: " + ChatColor.BLUE + ChatColor.UNDERLINE + rr.url);
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
        }
    }


    @EventHandler
    public void closeInventory(InventoryCloseEvent event) {
        if (!reddit.getRedditBrowsers().contains(event.getPlayer().getUniqueId())) {
            return;
        }
//		reddit.setKarma((Player) event.getPlayer()); Overloading the server

        if (event.getView().getTitle().startsWith("Comment ")) {
            String commentID = event.getView().getTitle().split(" ")[1];
            for (ItemStack is : event.getInventory().getContents()) {
                if (is != null) {
                    if (is.getType() == Material.WRITTEN_BOOK) {
                        BookMeta bm = (BookMeta) is.getItemMeta();
                        if (bm.getAuthor().equals(event.getPlayer().getName())) {
                            String comment = "";
                            for (String page : bm.getPages()) {
                                comment += page + " ";
                            }
                            reddit.commentCache.get(commentID).getSubject().toReference(reddit.reddit).reply(comment);
                            event.getPlayer().sendMessage(ChatColor.GREEN + "You have left a comment on a comment!");
                            event.getPlayer().getInventory().addItem(new ItemStack(Material.WRITABLE_BOOK));
                            event.getInventory().remove(is);
                        }
                    }
                }
            }
        } else if (event.getInventory().getHolder() instanceof Chest) {
            Chest chest = (Chest) event.getInventory().getHolder();

            if (chest.hasMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM)) {
                List<MetadataValue> metadata = chest.getMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM);
                if (metadataContains(metadata, InteractiveEnum.COMMENT_CHEST)) {
                    String submissionID = chest.getMetadata(RedditBrowserPlugin.SUBMISSION_ID).get(0).asString();
                    for (ItemStack is : event.getInventory().getContents()) {
                        if (is != null) {
                            if (is.getType() == Material.WRITTEN_BOOK) {
                                BookMeta bm = (BookMeta) is.getItemMeta();
                                if (bm.getAuthor().equals(event.getPlayer().getName())) {
                                    String comment = "";
                                    for (String page : bm.getPages()) {
                                        comment += page + " ";
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
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (!reddit.getRedditBrowsers().contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Player player = event.getPlayer();
        reddit.kickOut(player);
    }

    private static boolean metadataContains(List<MetadataValue> values, Object value) {
        if (values != null && !values.isEmpty()) {
            return values.stream().anyMatch(o -> Objects.equals(o.value(), value));
        }
        return false;
    }

}
