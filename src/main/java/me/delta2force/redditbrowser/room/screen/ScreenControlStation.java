package me.delta2force.redditbrowser.room.screen;

import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.room.Room;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.metadata.FixedMetadataValue;

import static me.delta2force.redditbrowser.RedditBrowserPlugin.*;

public class ControlStation {
    private static final String SCROLL_DOWN = "Scroll down";
    private static final String SCROLL_UP = "Scroll up";
    private ScreenController screenController;
    private final Room room;
    private final Location location;


    public ControlStation(ScreenController screenController, Room room, Location location) {
        this.screenController = screenController;
        this.room = room;
        this.location = location;
    }

    public void build() {
        buildScrollDownButton();
        buildScrollUpButton();
    }

    private void buildScrollDownButton() {
        final Location blockLocation = location.clone();
        final Block block = blockLocation.getBlock();
        block.setType(Material.GOLD_BLOCK);
        Block downButton = blockLocation.getWorld().getBlockAt(blockLocation.clone().add(0, 0, 1));

        if(screenController.canForward()) {
            downButton.setType(Material.OAK_WALL_SIGN);
            final Sign sign = (Sign) downButton.getState();
            sign.setLine(0, SCROLL_DOWN);
            sign.setEditable(false);
            sign.update();
            downButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(room.getRedditBrowserPlugin(), InteractiveEnum.SCROLL_DOWN));
            downButton.setMetadata(ROOM_ID, new FixedMetadataValue(room.getRedditBrowserPlugin(), room.getRoomId()));
            downButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(room.getRedditBrowserPlugin(), false));

            Directional downButtonDirection = (Directional) downButton.getBlockData();
            downButtonDirection.setFacing(BlockFace.SOUTH);
            downButton.setBlockData(downButtonDirection);

        } else {
            downButton.setType(Material.AIR);
        }
    }

    private void buildScrollUpButton() {
        final Location blockLocation = location.clone().add(0, 1, 0);
        final Block block = blockLocation.getBlock();
        block.setType(Material.GOLD_BLOCK);
        Block upButton = blockLocation.getWorld().getBlockAt(blockLocation.clone().add(0, 0, 1));

        if(screenController.canBack()) {
            upButton.setType(Material.OAK_WALL_SIGN);
            final Sign sign = (Sign) upButton.getState();
            sign.setLine(0, SCROLL_UP);
            sign.setEditable(false);
            sign.update();
            upButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(room.getRedditBrowserPlugin(), InteractiveEnum.SCROLL_UP));
            upButton.setMetadata(ROOM_ID, new FixedMetadataValue(room.getRedditBrowserPlugin(), room.getRoomId()));
            upButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(room.getRedditBrowserPlugin(), false));

            Directional downButtonDirection = (Directional) upButton.getBlockData();
            downButtonDirection.setFacing(BlockFace.SOUTH);
            upButton.setBlockData(downButtonDirection);

        } else {
            upButton.setType(Material.AIR);
        }
    }

    public void clean() {
        final Location blockLocation = location.clone().add(0, 1, 0);
        blockLocation.getBlock().setType(Material.AIR);
        blockLocation.clone().add(0, 0, 1).getBlock().setType(Material.AIR);
        final Location blockLocation2 = location.clone().add(0, 2, 0);
        blockLocation2.getBlock().setType(Material.AIR);
        blockLocation2.clone().add(0, 0, 1).getBlock().setType(Material.AIR);
    }
}
