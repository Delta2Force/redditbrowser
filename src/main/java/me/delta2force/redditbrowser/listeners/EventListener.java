package me.delta2force.redditbrowser.listeners;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.room.Room;
import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.*;

import static me.delta2force.redditbrowser.room.Room.createWritableBookStack;

public class EventListener implements Listener {

    private static final float VOLUME = 75f;
    private RedditBrowserPlugin reddit;

    public EventListener(RedditBrowserPlugin reddit) {
        this.reddit = reddit;
    }

    @EventHandler
    public void playerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() != null
                && event.getRightClicked().hasMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM)) {
            List<MetadataValue> metadata = event.getRightClicked().getMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM);

            if (metadataContains(metadata, InteractiveEnum.SHOW_URL)) {
                UUID roomId = (UUID) event.getRightClicked().getMetadata(RedditBrowserPlugin.ROOM_ID).get(0).value();
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
    public void closeInventory(InventoryCloseEvent event) {
         if (event.getInventory().getHolder() instanceof Hopper) {
             Hopper hopper = (Hopper) event.getInventory().getHolder();
            if (hopper.hasMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM)) {
                List<MetadataValue> metadata = hopper.getMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM);
                if (metadataContains(metadata, InteractiveEnum.COMMENT_HOPPER)) {
                    final ListIterator<ItemStack> iterator = hopper.getInventory().iterator();
                    while(iterator.hasNext()) {
                        final ItemStack itemStack = iterator.next();
                        itemAddedToHopper(hopper, itemStack);
                    }
                    hopper.getInventory().clear();
                }
            }
        }
    }

    private void itemAddedToHopper(Hopper hopper, ItemStack itemStack) {
        if(itemStack != null) {
            UUID roomId = (UUID) hopper.getMetadata(RedditBrowserPlugin.ROOM_ID).get(0).value();
            if (reddit.roomMap.containsKey(roomId)) {
                final Room room = reddit.roomMap.get(roomId);
                if (Material.WRITTEN_BOOK.equals(itemStack.getType())) {
                    BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
                    final String comment = String.join(" ", bookMeta.getPages());
                    room.replyComment(comment);
                }
            }
        }
    }

    @EventHandler
    public void inventoryPickedUp(InventoryPickupItemEvent inventoryPickupItemEvent) {
        final InventoryHolder holder = inventoryPickupItemEvent.getInventory().getHolder();
        if (holder instanceof Hopper) {
            final Hopper hopper = (Hopper) holder;
            if (hopper.hasMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM)) {
                final List<MetadataValue> metadata = hopper.getMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM);
                if (metadataContains(metadata, InteractiveEnum.COMMENT_HOPPER)) {
                    final Item item = inventoryPickupItemEvent.getItem();
                    final ItemStack itemStack = item.getItemStack();
                    itemAddedToHopper(hopper, itemStack);
                    item.remove();
                }
            }
        }
    }

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null
                && Action.RIGHT_CLICK_BLOCK.equals(event.getAction())
                && event.getClickedBlock().hasMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM)) {
            List<MetadataValue> metadata = event.getClickedBlock().getMetadata(RedditBrowserPlugin.INTERACTIVE_ENUM);
            UUID roomId = (UUID) event.getClickedBlock().getMetadata(RedditBrowserPlugin.ROOM_ID).get(0).value();
            if (reddit.roomMap.containsKey(roomId)) {
                final Room room = reddit.roomMap.get(roomId);
                if (!event.getClickedBlock().hasMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED) ||
                        !event.getClickedBlock().getMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED).get(0).asBoolean()) {
                    event.getClickedBlock().setMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED, new FixedMetadataValue(reddit, true));
                    if (metadataContains(metadata, InteractiveEnum.UPVOTE)) {
                        room.upvote();
                    } else if (metadataContains(metadata, InteractiveEnum.DOWNVOTE)) {
                        room.downvote();
                    } else if (metadataContains(metadata, InteractiveEnum.NEXT_ROOM)) {
                        room.nextPost();
                    } else if (metadataContains(metadata, InteractiveEnum.PREVIOUS_ROOM)) {
                        room.previousPost();
                    } else if (metadataContains(metadata, InteractiveEnum.SHOW_COMMENTS)) {
                        room.showComment();
                    } else if (metadataContains(metadata, InteractiveEnum.SHOW_POST)) {
                        room.showPost();
                    } else if (metadataContains(metadata, InteractiveEnum.REFRESH)) {
                        room.refresh();
                    } else if (metadataContains(metadata, InteractiveEnum.LEAVE)) {
                        reddit.kickOut(event.getPlayer());
                    } else if (metadataContains(metadata, InteractiveEnum.WRITE_COMMENT)) {
                        final ItemStack writableBookStack = createWritableBookStack();
                        event.getPlayer().getInventory().setItemInMainHand(writableBookStack);
                        event.getClickedBlock().setMetadata(RedditBrowserPlugin.BUTTON_ACTIVATED, new FixedMetadataValue(reddit, false));
                    } else if (metadataContains(metadata, InteractiveEnum.SCROLL_UP)) {
                        room.getScreenController().back();
                    } else if (metadataContains(metadata, InteractiveEnum.SCROLL_DOWN)) {
                        room.getScreenController().forward();
                    } else if (metadataContains(metadata, InteractiveEnum.PARENT_COMMENT)) {
                        room.getCommentsController().parent();
                    } else if (metadataContains(metadata, InteractiveEnum.CHILD_COMMENT)) {
                        room.getCommentsController().child();
                    } else if (metadataContains(metadata, InteractiveEnum.NEXT_COMMENT)) {
                        room.getCommentsController().next();
                    } else if (metadataContains(metadata, InteractiveEnum.PREVIOUS_COMMENT)) {
                        room.getCommentsController().previous();
                    } else if (metadataContains(metadata, InteractiveEnum.COMMENT_HOPPER)) {
                        event.setCancelled(true);
                    }
                }
            } else {
                event.getPlayer().sendMessage(ChatColor.RED + "Room not found!");
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        if (reddit.roomMap.values().stream()
                .anyMatch(o -> o.hasPlayer(event.getPlayer().getUniqueId()))) {
            Player player = event.getPlayer();
            reddit.kickOut(player);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent blockBreakEvent) {
        final Location location = blockBreakEvent.getBlock().getLocation();
        if (reddit.roomMap.values().stream().anyMatch(o -> o.isInside(location))) {
            blockBreakEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent blockPlaceEvent) {
        final Location location = blockPlaceEvent.getBlock().getLocation();
        if (reddit.roomMap.values().stream().anyMatch(o -> o.isInside(location))) {
            blockPlaceEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(HangingBreakEvent hangingBreakEvent) {
        final Location location = hangingBreakEvent.getEntity().getLocation();
        if (reddit.roomMap.values().stream().anyMatch(o -> o.isInside(location))) {
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
