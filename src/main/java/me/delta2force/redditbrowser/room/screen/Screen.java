package me.delta2force.redditbrowser.room.screen;

import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.room.screen.renderer.TiledRenderer;
import me.delta2force.redditbrowser.room.Room;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.metadata.FixedMetadataValue;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Objects;

import static me.delta2force.redditbrowser.RedditBrowserPlugin.INTERACTIVE_ENUM;
import static me.delta2force.redditbrowser.RedditBrowserPlugin.ROOM_ID;

public class Screen {
    public static final int BLOCK_PIXELS = 128;

    private final Room room;
    private TiledRenderer tiledRenderer = null;
    private final Location screenLocation;
    private final int screenWidth;
    private final int screenHeight;
    private final ScreenController screenController;

    public Screen(
            Room room,
            Location screenLocation,
            int screenWidth,
            int screenHeight,
            ScreenController screenController) {
        this.room = room;
        this.screenLocation = screenLocation;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.screenController = screenController;
    }

    public void buildScreen(BufferedImage bufferedImage) {
        if (bufferedImage != null) {
            if (tiledRenderer == null) {
                cleanBackWall();
                final World world = screenLocation.getWorld();
                tiledRenderer = new TiledRenderer(room.getRedditBrowserPlugin(), screenWidth, screenHeight);
                Bukkit.getScheduler().runTaskLater(room.getRedditBrowserPlugin(), () -> {
                    for (int row = 0; row < screenHeight; row++) {
                        for (int col = 0; col < screenWidth; col++) {
                            final Location blockLocation = screenLocation.clone().add(+col, -row, 0);
                            blockLocation.getBlock().setType(Material.GLOWSTONE);
                            ItemFrame itf = (ItemFrame) world.spawnEntity(blockLocation.clone().add(0, 0, 1), EntityType.ITEM_FRAME);
                            itf.setFacingDirection(BlockFace.SOUTH);
                            itf.setInvulnerable(true);
                            itf.setMetadata(ROOM_ID, new FixedMetadataValue(room.getRedditBrowserPlugin(), room.getRoomId()));
                            itf.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(room.getRedditBrowserPlugin(), InteractiveEnum.SHOW_URL));
                            itf.setRotation(Rotation.NONE);
                            ItemStack map = new ItemStack(Material.FILLED_MAP);
                            MapMeta mapMeta = (MapMeta) map.getItemMeta();
                            MapView mapView = Bukkit.createMap(world);
                            mapView.setTrackingPosition(false);
                            mapView.getRenderers().forEach(mapView::removeRenderer);
                            mapView.addRenderer(tiledRenderer.getRenderer(row, col));
                            mapMeta.setMapView(mapView);
                            map.setItemMeta(mapMeta);
                            itf.setItem(map);

                        }
                    }
                    updateRenderer(bufferedImage);

                }, 50);
            } else {
                updateRenderer(bufferedImage);
            }
        } else {
            cleanBackWall();
        }
    }

    private void updateRenderer(BufferedImage bufferedImage) {
        Bukkit.getScheduler().runTaskAsynchronously(room.getRedditBrowserPlugin(), () -> {
            if (tiledRenderer != null) {
                tiledRenderer.updateImage(bufferedImage);
            }
        });
    }

    public void clean() {
        cleanBackWall();
    }

    private void cleanBackWall() {
        clearItemFrames();
        tiledRenderer = null;

        for (int row = 0; row < screenHeight; row++) {
            for (int col = 0; col < screenWidth; col++) {
                final Location blockLocation = screenLocation.clone().add(+col, -row, 0);
                blockLocation.getBlock().setType(room.getRoomMaterial());
            }
        }
    }

    private void clearItemFrames() {
        final Collection<Entity> nearbyEntities = screenLocation.getWorld()
                .getNearbyEntities(screenLocation,
                        screenWidth,
                        -screenHeight,
                        2,
                        o -> Objects.equals(EntityType.ITEM_FRAME, o.getType()));
        nearbyEntities.forEach(Entity::remove);
    }

    public int getBlockPixels() {
        return BLOCK_PIXELS;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public ScreenController getScreenController() {
        return screenController;
    }

    public Room getRoom() {
        return room;
    }
}
