package me.delta2force.redditbrowser.room;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.renderer.TiledRenderer;
import me.delta2force.redditbrowser.repository.URLToImageRepository;
import net.dean.jraw.models.Submission;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.metadata.FixedMetadataValue;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Objects;

import static java.lang.Math.ceil;
import static me.delta2force.redditbrowser.RedditBrowserPlugin.INTERACTIVE_ENUM;
import static me.delta2force.redditbrowser.RedditBrowserPlugin.ROOM_ID;
import static me.delta2force.redditbrowser.room.Room.COMMENT_DISPLAY_NAME;

public class Screen {
    private final Room room;
    private TiledRenderer tiledRenderer = null;
    private final Location screenLocation;
    private final int screenWidth;
    private final int screenHeight;

    public Screen(
            Room room,
            Location screenLocation,
            int screenWidth,
            int screenHeight) {
        this.room = room;
        this.screenLocation = screenLocation;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void buildScreen(Submission submission) {
        if (submission != null && !submission.isSelfPost()) {
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
                            mapView.setLocked(true);
                            mapView.setUnlimitedTracking(false);
                            mapView.getRenderers().forEach(mapView::removeRenderer);
                            mapView.addRenderer(tiledRenderer.getRenderer(row, col));
                            mapMeta.setMapView(mapView);
                            map.setItemMeta(mapMeta);
                            itf.setItem(map);

                        }
                    }
                    updateRenderer(submission);

                }, 5);
            } else {
                updateRenderer(submission);
            }
        } else if (submission != null && submission.isSelfPost()) {
            buildSelfPost(submission);
        } else {
            cleanBackWall();
        }
    }

    private void updateRenderer(Submission submission) {
        Bukkit.getScheduler().runTaskAsynchronously(room.getRedditBrowserPlugin(), () -> {
            final BufferedImage image = URLToImageRepository.findImage(submission);
            if (tiledRenderer != null) {
                tiledRenderer.updateImage(submission.getUrl(), image);
            }
        });
    }

    public void clean() {
        cleanBackWall();
    }

    private void buildSelfPost(Submission submission) {
        cleanBackWall();
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookmeta = (BookMeta) book.getItemMeta();
        bookmeta.setTitle(submission.getTitle());
        bookmeta.setDisplayName(COMMENT_DISPLAY_NAME);
        bookmeta.setAuthor(submission.getAuthor());
        if (submission.getSelfText().length() > 255) {
            double f = ceil(((float) submission.getSelfText().length()) / 255f);
            for (int i = 0; i < f; i++) {
                if (submission.getSelfText().length() < (i + 1) * 255) {
                    bookmeta.addPage(submission.getSelfText().substring(i * 255));
                } else {
                    bookmeta.addPage(submission.getSelfText().substring(i * 255, (i + 1) * 255));
                }
            }
        } else {
            bookmeta.addPage(submission.getSelfText());
        }

        Bukkit.getScheduler().runTaskLater(room.getRedditBrowserPlugin(), () -> {
            final Location smallScreenLocation = screenLocation.clone().add(screenWidth / 2, -screenHeight + 1, 0);
            smallScreenLocation.getBlock().setType(Material.GLOWSTONE);
            ItemFrame itf = (ItemFrame) smallScreenLocation.getWorld().spawnEntity(smallScreenLocation.clone().add(0, 0, 1), EntityType.ITEM_FRAME);
            itf.setFacingDirection(BlockFace.SOUTH);
            book.setItemMeta(bookmeta);
            itf.setItem(book);
        }, 5);
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
}
