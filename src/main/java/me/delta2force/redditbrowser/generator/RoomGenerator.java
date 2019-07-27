package me.delta2force.redditbrowser.generator;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.renderer.TiledRenderer;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;
import java.util.UUID;

import static java.lang.Math.ceil;
import static me.delta2force.redditbrowser.RedditBrowserPlugin.*;

public class RoomGenerator {
    private RedditBrowserPlugin redditBrowserPlugin;

    public RoomGenerator(RedditBrowserPlugin redditBrowserPlugin) {
        this.redditBrowserPlugin = redditBrowserPlugin;
    }

    public void createRoom(
            Location location,
            Submission submission,
            RootCommentNode comments,
            boolean firstRoom,
            RoomDimensions roomDimensions) {
        buildEmptyRoom(location, roomDimensions);
        buildDoors(location, submission, firstRoom, roomDimensions);
        buildVoteButtons(location, submission, roomDimensions);

        if (submission.isSelfPost()) {
            createSelfPost(location, submission, roomDimensions);
        } else {
            try {
                createTiledMapView(location, submission.getUrl(), roomDimensions);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        buildCommentsChest(location, submission, comments, roomDimensions);
    }

    private void buildCommentsChest(Location location, Submission submission, RootCommentNode comments, RoomDimensions roomDimensions) {
        if (comments != null) {
            Location chestLocation = location.clone().add(-roomDimensions.getRoomWidth() / 2, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1);
            Block b = chestLocation.getBlock();

            b.setType(Material.CHEST);
            Directional chestDirection = (Directional) b.getBlockData();
            chestDirection.setFacing(BlockFace.SOUTH);
            b.setBlockData(chestDirection);
            b.setMetadata(SUBMISSION_ID, new FixedMetadataValue(redditBrowserPlugin, submission.getId()));
            b.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.COMMENT_CHEST));
            Chest chest = (Chest) b.getState();

            chest.setCustomName(UUID.randomUUID().toString());

            chest.setCustomName(UUID.randomUUID().toString());

            int in = 0;
            for (CommentNode<Comment> cn : comments.getReplies()) {
                Comment c = cn.getSubject();
                if (in < 26) {
                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta bookmeta = (BookMeta) book.getItemMeta();
                    bookmeta.setTitle("Comment");
                    bookmeta.setAuthor("u/" + c.getAuthor());
                    if (c.getBody().length() > 255) {
                        double f = ceil(((float) c.getBody().length()) / 255f);
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
                    redditBrowserPlugin.commentCache.put(c.getId(), cn);
                    chest.getInventory().addItem(book);
                } else {
                    break;
                }
                in++;
            }
        }
    }

    private void buildEmptyRoom(Location location, RoomDimensions roomDimensions) {
        cube(Material.WHITE_WOOL, location, location.clone().add(-roomDimensions.getRoomWidth(), -roomDimensions.getRoomHeight(), -roomDimensions.getRoomDepth()));
        cube(Material.AIR, location.clone().add(-1, -1, -1), location.clone().add(-roomDimensions.getRoomWidth() + 1, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1));
    }

    private void buildVoteButtons(Location location, Submission submission, RoomDimensions roomDimensions) {
        Block uv = location.getWorld().getBlockAt(location.clone().add(-roomDimensions.getRoomWidth() + 1, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1));
        uv.setType(Material.OAK_BUTTON);
        uv.setMetadata(SUBMISSION_ID, new FixedMetadataValue(redditBrowserPlugin, submission.getId()));
        uv.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.UPVOTE));
        Directional uvdir = (Directional) uv.getBlockData();
        uvdir.setFacing(BlockFace.SOUTH);
        uv.setBlockData(uvdir);


        Block dv = location.getWorld().getBlockAt(location.clone().add(-1, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1));
        dv.setType(Material.OAK_BUTTON);
        dv.setMetadata(SUBMISSION_ID, new FixedMetadataValue(redditBrowserPlugin, submission.getId()));
        dv.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.DOWNVOTE));

        Directional dvdir = (Directional) dv.getBlockData();
        dvdir.setFacing(BlockFace.SOUTH);
        dv.setBlockData(dvdir);

        spawnHologram(uv.getLocation().clone().add(.5, -2, .5), colorCode("a") + "+1");
        spawnHologram(dv.getLocation().clone().add(.5, -2, .5), colorCode("c") + "-1");
    }

    private void createSelfPost(Location location, Submission submission, RoomDimensions roomDimensions) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookmeta = (BookMeta) book.getItemMeta();
        bookmeta.setTitle(submission.getTitle());
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
        ItemFrame itf = (ItemFrame) location.getWorld().spawnEntity(location.clone().add(-(roomDimensions.getRoomWidth() / 2), -roomDimensions.getRoomHeight()+2, -roomDimensions.getRoomDepth() + 1), EntityType.ITEM_FRAME);
        itf.setFacingDirection(BlockFace.SOUTH);

        book.setItemMeta(bookmeta);
        itf.setItem(book);
    }

    private void buildDoors(Location location, Submission submission, boolean firstRoom, RoomDimensions roomDimensions) {
        int zPosition = (-roomDimensions.getRoomDepth() / 2) + 1;
        if(zPosition >= 0) {
            zPosition = -1;
        }
        if (!firstRoom) {
            putDoor(location.clone().add(-roomDimensions.getRoomWidth(), -roomDimensions.getRoomHeight() + 1, zPosition));
            Block pressurePlateLeft = location.clone().add(-roomDimensions.getRoomWidth() + 1, -roomDimensions.getRoomHeight() + 1, zPosition).getBlock();
            buildPressurePlateForSubmission(submission, pressurePlateLeft);
        }
        Block pressurePlateRight = location.clone().add(-1, -roomDimensions.getRoomHeight() + 1, zPosition).getBlock();
        buildPressurePlateForSubmission(submission, pressurePlateRight);
    }

    private void buildPressurePlateForSubmission(Submission submission, Block pressurePlateRight) {
        pressurePlateRight.setType(Material.STONE_PRESSURE_PLATE);
        pressurePlateRight.setMetadata(REDDIT_POST_TITLE, new FixedMetadataValue(redditBrowserPlugin, submission.getTitle()));
        pressurePlateRight.setMetadata(REDDIT_POST_SCORE, new FixedMetadataValue(redditBrowserPlugin, submission.getScore()));
        pressurePlateRight.setMetadata(REDDIT_POST_AUTHOR, new FixedMetadataValue(redditBrowserPlugin, submission.getAuthor()));
        pressurePlateRight.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.ROOM_ENTERED));
    }

    private void putDoor(Location location) {
        final Block bottom = location.getBlock();
        final Block top = bottom.getRelative(BlockFace.UP, 1);
        top.setType(Material.IRON_DOOR);
        Door topDoor = (Door) top.getBlockData();
        topDoor.setHinge(Door.Hinge.LEFT);
        topDoor.setHalf(Bisected.Half.TOP);
        topDoor.setFacing(BlockFace.WEST);
        top.setBlockData(topDoor);
        bottom.setType(Material.IRON_DOOR);
        Door bottomDoor = (Door) bottom.getBlockData();
        bottomDoor.setHinge(Door.Hinge.LEFT);
        bottomDoor.setHalf(Bisected.Half.BOTTOM);
        bottomDoor.setFacing(BlockFace.WEST);
        bottom.setBlockData(bottomDoor);
    }

    private void createTiledMapView(Location location, String url, RoomDimensions roomDimensions) {
        int screenWidth = roomDimensions.getScreenWidth();
        int screenHeight = roomDimensions.getScreenHeight();

        TiledRenderer tiledRenderer = new TiledRenderer(url, redditBrowserPlugin, screenWidth, screenHeight);
        int titleXStart = screenWidth > 2 ? screenWidth * -1 : (screenWidth +1) * -1;
        int titleYStart = roomDimensions.getScreenHeight() - roomDimensions.getRoomHeight() +1;
        for (int row = 0; row < screenHeight; row++) {
            for (int col = 0; col < screenWidth; col++) {
                location.clone().add(titleXStart + col, titleYStart - row, -roomDimensions.getRoomDepth()).getBlock().setType(Material.GLOWSTONE);
                ItemFrame itf = (ItemFrame) location.getWorld().spawnEntity(location.clone().add(titleXStart + col, titleYStart - row, -roomDimensions.getRoomDepth() + 1), EntityType.ITEM_FRAME);
                itf.setFacingDirection(BlockFace.SOUTH);
                ItemStack map = new ItemStack(Material.FILLED_MAP);
                MapMeta mapMeta = (MapMeta) map.getItemMeta();
                MapView mv = Bukkit.createMap(location.getWorld());
                mv.setTrackingPosition(false);
                mv.setUnlimitedTracking(false);
                mv.addRenderer(tiledRenderer.getRenderer(row, col));
                mapMeta.setMapView(mv);
                map.setItemMeta(mapMeta);
                itf.setItem(map);
            }
        }
    }

    private void cube(Material blockMaterial, Location from, Location to) {
        for (int x = from.getBlockX(); x >= to.getBlockX(); x--) {
            for (int y = from.getBlockY(); y >= to.getBlockY(); y--) {
                for (int z = from.getBlockZ(); z >= to.getBlockZ(); z--) {
                    from.getWorld().getBlockAt(x, y, z).setType(blockMaterial);
                }
            }
        }
    }

    private ArmorStand spawnHologram(Location l, String name) {
        ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        as.setCustomName(name);
        as.setCustomNameVisible(true);
        as.setGravity(false);
        as.setVisible(false);
        as.setInvulnerable(true);
        as.setCollidable(false);
        return as;
    }
}
