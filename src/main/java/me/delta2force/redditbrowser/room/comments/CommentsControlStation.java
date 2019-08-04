package me.delta2force.redditbrowser.room.comments;

import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.room.Room;
import me.delta2force.redditbrowser.room.screen.ScreenController;
import me.delta2force.redditbrowser.room.screen.ScreenModelType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.metadata.FixedMetadataValue;

import static me.delta2force.redditbrowser.RedditBrowserPlugin.*;

public class CommentsControlStation {
    public static final String PARENT_COMMENT = "Parent comment";
    public static final String CHILD_COMMENT = "Child comment";
    public static final String NEXT_COMMENT = "Next comment";
    public static final String PREVIOUS_COMMENT = "Previous comment";
    private final Location location;
    private final CommentsController commentsController;
    private final ScreenController screenController;
    private final Room room;

    public CommentsControlStation(
            Location location,
            CommentsController commentsController,
            ScreenController screenController,
            Room room) {
        this.location = location;
        this.commentsController = commentsController;
        this.screenController = screenController;
        this.room = room;
    }

    public void build() {
        if(ScreenModelType.COMMENT.equals(screenController.getScreenModelType())) {
            buildParentButton();
            buildNextButton();
            buildPreviousButton();
            buildChildButton();
        } else {
            clean();
        }
    }

    public void clean() {
        location.clone().add(0, 1, 1).getBlock().setType(Material.AIR);
        location.clone().add(1, 1, 1).getBlock().setType(Material.AIR);
        location.clone().add(1, 2, 1).getBlock().setType(Material.AIR);
        location.clone().add(2, 1, 1).getBlock().setType(Material.AIR);

        location.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
        location.clone().add(1, 1, 0).getBlock().setType(Material.AIR);
        location.clone().add(1, 2, 0).getBlock().setType(Material.AIR);
        location.clone().add(2, 1, 0).getBlock().setType(Material.AIR);
    }

    private void buildParentButton() {
        final Location blockLocation = location.clone().add(0, 1, 0);
        final Block block = blockLocation.getBlock();
        block.setType(room.getRoomMaterial());
        Block upButton = blockLocation.getWorld().getBlockAt(blockLocation.clone().add(0, 0, 1));

        if(commentsController.canParent()) {
            upButton.setType(Material.OAK_WALL_SIGN);
            final Sign sign = (Sign) upButton.getState();
            sign.setLine(1, PARENT_COMMENT);
            sign.setEditable(false);
            sign.update();
            upButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(room.getRedditBrowserPlugin(), InteractiveEnum.PARENT_COMMENT));
            upButton.setMetadata(ROOM_ID, new FixedMetadataValue(room.getRedditBrowserPlugin(), room.getRoomId()));
            upButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(room.getRedditBrowserPlugin(), false));

            Directional downButtonDirection = (Directional) upButton.getBlockData();
            downButtonDirection.setFacing(BlockFace.SOUTH);
            upButton.setBlockData(downButtonDirection);

        } else {
            upButton.setType(Material.AIR);
        }
    }

    private void buildChildButton() {
        final Location blockLocation = location.clone().add(2, 1, 0);
        final Block block = blockLocation.getBlock();
        block.setType(room.getRoomMaterial());
        Block button = blockLocation.getWorld().getBlockAt(blockLocation.clone().add(0, 0, 1));

        if(commentsController.canChild()) {
            button.setType(Material.OAK_WALL_SIGN);
            final Sign sign = (Sign) button.getState();
            sign.setLine(1, CHILD_COMMENT);
            sign.setEditable(false);
            sign.update();
            button.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(room.getRedditBrowserPlugin(), InteractiveEnum.CHILD_COMMENT));
            button.setMetadata(ROOM_ID, new FixedMetadataValue(room.getRedditBrowserPlugin(), room.getRoomId()));
            button.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(room.getRedditBrowserPlugin(), false));

            Directional downButtonDirection = (Directional) button.getBlockData();
            downButtonDirection.setFacing(BlockFace.SOUTH);
            button.setBlockData(downButtonDirection);

        } else {
            button.setType(Material.AIR);
        }
    }

    private void buildNextButton() {
        final Location blockLocation = location.clone().add(1, 2, 0);
        final Block block = blockLocation.getBlock();
        block.setType(room.getRoomMaterial());
        Block button = blockLocation.getWorld().getBlockAt(blockLocation.clone().add(0, 0, 1));

        if(commentsController.canNext()) {
            button.setType(Material.OAK_WALL_SIGN);
            final Sign sign = (Sign) button.getState();
            sign.setLine(1, NEXT_COMMENT);
            sign.setEditable(false);
            sign.update();
            button.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(room.getRedditBrowserPlugin(), InteractiveEnum.NEXT_COMMENT));
            button.setMetadata(ROOM_ID, new FixedMetadataValue(room.getRedditBrowserPlugin(), room.getRoomId()));
            button.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(room.getRedditBrowserPlugin(), false));

            Directional downButtonDirection = (Directional) button.getBlockData();
            downButtonDirection.setFacing(BlockFace.SOUTH);
            button.setBlockData(downButtonDirection);

        } else {
            button.setType(Material.AIR);
        }
    }

    private void buildPreviousButton() {
        final Location blockLocation = location.clone().add(1, 1, 0);
        final Block block = blockLocation.getBlock();
        block.setType(room.getRoomMaterial());
        Block button = blockLocation.getWorld().getBlockAt(blockLocation.clone().add(0, 0, 1));

        if(commentsController.canPrevious()) {
            button.setType(Material.OAK_WALL_SIGN);
            final Sign sign = (Sign) button.getState();
            sign.setLine(1, PREVIOUS_COMMENT);
            sign.setEditable(false);
            sign.update();
            button.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(room.getRedditBrowserPlugin(), InteractiveEnum.PREVIOUS_COMMENT));
            button.setMetadata(ROOM_ID, new FixedMetadataValue(room.getRedditBrowserPlugin(), room.getRoomId()));
            button.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(room.getRedditBrowserPlugin(), false));

            Directional downButtonDirection = (Directional) button.getBlockData();
            downButtonDirection.setFacing(BlockFace.SOUTH);
            button.setBlockData(downButtonDirection);

        } else {
            button.setType(Material.AIR);
        }
    }
}
