package me.delta2force.redditbrowser.listeners;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.renderer.RedditRenderer;
import me.delta2force.redditbrowser.room.Room;
import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
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

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import static me.delta2force.redditbrowser.room.Room.createWritableBookStack;

public class EventListener implements Listener {

    private static final float VOLUME = 75f;
    private RedditBrowserPlugin reddit;

    public EventListener(RedditBrowserPlugin reddit) {
        this.reddit = reddit;
    }


    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null
                && Action.RIGHT_CLICK_BLOCK.equals(event.getAction())
                && event.getClickedBlock().hasMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM)) {
            List<MetadataValue> metadata = event.getClickedBlock().getMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM);
            if (metadataContains(metadata, InteractiveEnum.UPVOTE)) {
                String submissionID = event.getClickedBlock().getMetadata(RedditBrowserPlugin.SUBMISSION_ID).get(0).asString();
                Bukkit.getScheduler().runTaskAsynchronously(reddit, new Runnable() {
                    @Override
                    public void run() {
                        event.getPlayer().playSound(event.getClickedBlock().getLocation(), Sound.ENTITY_VILLAGER_YES, VOLUME, 1);
                        reddit.redditClient.submission(submissionID).upvote();
                        int karma = reddit.redditClient.submission(submissionID).inspect().getScore();
                        event.getPlayer().sendMessage(ChatColor.GREEN + "You have upvoted the post! It now has " + karma + " karma.");
                    }
                });
            } else if (metadataContains(metadata, InteractiveEnum.DOWNVOTE)) {
                String submissionID = event.getClickedBlock().getMetadata(RedditBrowserPlugin.SUBMISSION_ID).get(0).asString();
                Bukkit.getScheduler().runTaskAsynchronously(reddit, new Runnable() {
                    @Override
                    public void run() {
                        event.getPlayer().playSound(event.getClickedBlock().getLocation(), Sound.ENTITY_VILLAGER_NO, VOLUME, 1);
                        reddit.redditClient.submission(submissionID).downvote();
                        int karma = reddit.redditClient.submission(submissionID).inspect().getScore();
                        event.getPlayer().sendMessage(ChatColor.RED + "You have downvoted the post! It now has " + karma + " karma.");
                    }
                });
            } else if (metadataContains(metadata, InteractiveEnum.NEXT_ROOM)) {
                if(!event.getClickedBlock().getMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED).get(0).asBoolean()) {
                    event.getClickedBlock().setMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED, new FixedMetadataValue(reddit, true));
                    UUID roomId = (UUID) event.getClickedBlock().getMetadata(RedditBrowserPlugin.ROOM_ID).get(0).value();
                    if (reddit.roomMap.containsKey(roomId)) {
                        final Room room = reddit.roomMap.get(roomId);
                        room.nextPost();
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + "Room not found!");
                    }
                }
            } else if (metadataContains(metadata, InteractiveEnum.PREVIOUS_ROOM)) {
                if(!event.getClickedBlock().getMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED).get(0).asBoolean()) {
                    UUID roomId = (UUID) event.getClickedBlock().getMetadata(RedditBrowserPlugin.ROOM_ID).get(0).value();
                    event.getClickedBlock().setMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED, new FixedMetadataValue(reddit, true));
                    if (reddit.roomMap.containsKey(roomId)) {
                        final Room room = reddit.roomMap.get(roomId);
                        room.previousPost();
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + "Room not found!");
                    }
                }
            } else if (metadataContains(metadata, InteractiveEnum.LOAD_COMMENTS)) {
                if(!event.getClickedBlock().getMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED).get(0).asBoolean()) {
                    UUID roomId = (UUID) event.getClickedBlock().getMetadata(RedditBrowserPlugin.ROOM_ID).get(0).value();
                    event.getClickedBlock().setMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED, new FixedMetadataValue(reddit, true));
                    if (reddit.roomMap.containsKey(roomId)) {
                        final Room room = reddit.roomMap.get(roomId);
                        room.displayComments();
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + "Room not found!");
                    }
                }
            } else if (metadataContains(metadata, InteractiveEnum.REFRESH)) {
                if(!event.getClickedBlock().getMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED).get(0).asBoolean()) {
                    UUID roomId = (UUID) event.getClickedBlock().getMetadata(RedditBrowserPlugin.ROOM_ID).get(0).value();
                    event.getClickedBlock().setMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED, new FixedMetadataValue(reddit, true));
                    if (reddit.roomMap.containsKey(roomId)) {
                        final Room room = reddit.roomMap.get(roomId);
                        room.refresh();
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + "Room not found!");
                    }
                }
            } else if (metadataContains(metadata, InteractiveEnum.LEAVE)) {
                if(!event.getClickedBlock().getMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED).get(0).asBoolean()) {
                    event.getClickedBlock().setMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED, new FixedMetadataValue(reddit, true));
                    reddit.kickOut(event.getPlayer());
                }
            } else if (metadataContains(metadata, InteractiveEnum.WRITE_COMMENT)) {
                if(!event.getClickedBlock().getMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED).get(0).asBoolean()) {
                    event.getClickedBlock().setMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED, new FixedMetadataValue(reddit, true));
                    final ItemStack writableBookStack = createWritableBookStack();
                    event.getPlayer().getInventory().setItemInMainHand(writableBookStack);
                }
            } else if (metadataContains(metadata, InteractiveEnum.SHOW_URL)) {
                UUID roomId = (UUID) event.getClickedBlock().getMetadata(RedditBrowserPlugin.ROOM_ID).get(0).value();
                if (reddit.roomMap.containsKey(roomId)) {
                    final Room room = reddit.roomMap.get(roomId);
                    room.showURLtoPlayers(event.getPlayer());
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "Room not found!");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void interactInventory(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();

        if(event.getCurrentItem() != null &&
                event.getCurrentItem().getType().equals(Material.WRITTEN_BOOK)
                && event.getInventory().getType().equals(InventoryType.CHEST) && event.isRightClick()) {
            if(!event.getView().getTitle().startsWith("Comment ")) {
                if(reddit.roomMap.values()
                        .stream()
                        .noneMatch(o->o.hasPlayer(p.getUniqueId()))){
                    return ;
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
                    bookmeta.setDisplayName(Room.COMMENT_DISPLAY_NAME);
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
    public void closeInventory(InventoryCloseEvent event) {
//		redditClient.setKarma((Player) event.getPlayer()); Overloading the server

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
                            reddit.commentCache.get(commentID).getSubject().toReference(reddit.redditClient).reply(comment);
                            event.getPlayer().sendMessage(ChatColor.GREEN + "You have left a comment on a comment!");
                            event.getPlayer().getInventory().addItem(createWritableBookStack());
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
                                    reddit.redditClient.submission(submissionID).reply(comment);
                                    event.getPlayer().sendMessage(ChatColor.GREEN + "You have left a comment!");
                                    event.getPlayer().getInventory().addItem(createWritableBookStack());
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
        if(reddit.roomMap.values().stream()
                .anyMatch(o->o.hasPlayer(event.getPlayer().getUniqueId()))) {
            Player player = event.getPlayer();
            reddit.kickOut(player);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent blockBreakEvent) {
        final Location location = blockBreakEvent.getBlock().getLocation();
        if(reddit.roomMap.values().stream().anyMatch(o->o.isInside(location))) {
            blockBreakEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent blockPlaceEvent) {
        final Location location = blockPlaceEvent.getBlock().getLocation();
        if(reddit.roomMap.values().stream().anyMatch(o->o.isInside(location))) {
            blockPlaceEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(HangingBreakEvent hangingBreakEvent) {
        final Location location = hangingBreakEvent.getEntity().getLocation();
        if(reddit.roomMap.values().stream().anyMatch(o->o.isInside(location))) {
            hangingBreakEvent.setCancelled(true);
        }
    }

    private static boolean metadataContains(List<MetadataValue> values, Object value) {
        if (values != null && !values.isEmpty()) {
            return values.stream().anyMatch(o -> Objects.equals(o.value(), value));
        }
        return false;
    }

}
