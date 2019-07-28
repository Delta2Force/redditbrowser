package me.delta2force.redditbrowser.room;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.renderer.TiledRenderer;
import me.delta2force.redditbrowser.repository.URLToImageRepository;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.metadata.FixedMetadataValue;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.lang.Math.ceil;
import static me.delta2force.redditbrowser.RedditBrowserPlugin.*;

public class Room {
    private static final String PREVIOUS_HOLOGRAM_NAME = colorCode("9") + "Previous";
    private static final Material ROOM_MATERIAL = Material.WHITE_WOOL;
    private static final String COMMENTS_HOLOGRAM = colorCode("9") + "Comments";
    private static final String WRITE_COMMENT_HOLOGRAM = colorCode("9") + "Write comment";
    public static final String COMMENT_DISPLAY_NAME = "Comment";
    public static final String NEWLINE = "\n";
    private final RedditBrowserPlugin redditBrowserPlugin;
    private final Player owner;
    private final Location location;
    private final RoomDimensions roomDimensions;
    private RedditQueue redditQueue;
    private String subreddit;
    private Submission currentSubmission;
    private TiledRenderer tiledRenderer;

    public Room(
            RedditBrowserPlugin redditBrowserPlugin,
            Location location,
            String subreddit,
            RoomDimensions roomDimensions,
            Player owner) {
        this.redditBrowserPlugin = redditBrowserPlugin;
        this.owner = owner;
        this.location = location;
        this.roomDimensions = roomDimensions;
        setSubReddit(subreddit);
    }

    public void build(Collection<Player> startingPlayers) {
        tiledRenderer = null;
        Bukkit.getScheduler().runTaskAsynchronously(redditBrowserPlugin, () -> {
            final Submission submission = redditQueue.next();
            if (submission != null) {
                currentSubmission = submission;
                Bukkit.getScheduler().runTask(redditBrowserPlugin, () -> {
                    createRoom(submission);

                    Bukkit.getScheduler().runTaskLater(redditBrowserPlugin, () -> {
                        setupPlayers(submission, startingPlayers);
                    }, 10);
                });
            } else {
                Bukkit.getScheduler().runTask(redditBrowserPlugin, () -> {
                    createRoom(null);
                    Bukkit.getScheduler().runTaskLater(redditBrowserPlugin, () -> {
                        setupPlayers(submission, startingPlayers);
                        startingPlayers.forEach(player -> player.sendMessage(ChatColor.RED + "No posts found."));

                    }, 10);
                });
            }
        });
    }

    public void addPlayer(Player player) {
        setupPlayers(currentSubmission, Arrays.asList(player));
    }

    public void refresh() {
        redditQueue.reset();
        build(getPlayers());
    }

    public void updateSubreddit(String subreddit) {
        setSubReddit(subreddit);
        build(getPlayers());
    }

    private void setSubReddit(String subreddit) {
        this.subreddit = subreddit;
        redditQueue = new RedditQueue(redditBrowserPlugin.redditClient, subreddit);
    }

    private void setupPlayers(Submission submission, Collection<Player> startingPlayers) {
        Location loc = location.clone().add(-roomDimensions.getRoomWidth() / 2, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() / 2);
        loc.setPitch(0);
        loc.setYaw(180);
        loc.getChunk().load();
        location.clone().add(-roomDimensions.getRoomWidth(), -roomDimensions.getRoomHeight(), -roomDimensions.getRoomDepth()).getChunk().load();
        startingPlayers.forEach(player -> {
            // If it's pewds' subreddit, display LWIAY title
            if(subreddit.equalsIgnoreCase("pewdiepiesubmissions")) {
                player.sendTitle(colorCode("4")+"L"+colorCode("a")+"W"+colorCode("1")+"I"+colorCode("d")+"A"+colorCode("e")+"Y", "", 10, 70, 20);
            }
            player.teleport(loc);
            player.setGameMode(GameMode.SURVIVAL);
            cleanupInventory();
            if (submission != null) {
                updateTitleForPlayer(submission, player);
            }
        });
    }

    public static ItemStack createWritableBookStack() {
        final ItemStack itemStack = new ItemStack(Material.WRITABLE_BOOK);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(COMMENT_DISPLAY_NAME);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void updateTitleForRoom(Submission submission) {
        getPlayers().forEach(player -> updateTitleForPlayer(submission, player));
    }

    private void updateTitleForPlayer(Submission submission, Player player) {
        player.sendMessage(
                ChatColor.RESET + NEWLINE + NEWLINE +
                        ChatColor.DARK_BLUE +
                        "[" + getPostType(submission) + "] " +
                        ChatColor.WHITE + submission.getTitle()
                        + NEWLINE + ChatColor.GREEN +
                        ChatColor.ITALIC + " by /u/" + submission.getAuthor() + ChatColor.RESET
                        + NEWLINE + ChatColor.GOLD + "Karma : " + ChatColor.BOLD + submission.getScore() + NEWLINE + ChatColor.RESET);
    }

    private static String getPostType(Submission submission) {
        if (submission.isSelfPost()) {
            return "self";
        }
        return submission.getPostHint();
    }

    public void nextPost() {
        Bukkit.getScheduler().runTaskAsynchronously(redditBrowserPlugin, new Runnable() {
            @Override
            public void run() {
                final Submission submission = redditQueue.next();
                if (submission != null) {
                    currentSubmission = submission;

                    Bukkit.getScheduler().runTask(redditBrowserPlugin, new Runnable() {
                        @Override
                        public void run() {
                            updateRoom(submission);
                            updateTitleForRoom(submission);
                            cleanupInventory();
                        }
                    });
                } else {
                    getPlayers().forEach(player -> player.sendMessage(ChatColor.RED + "No more posts found."));
                }
            }
        });
    }

    private void cleanupInventory() {
        getPlayers().forEach(RedditBrowserPlugin::removeCommentsFromPlayerInventory);
    }

    public void previousPost() {
        Bukkit.getScheduler().runTaskAsynchronously(redditBrowserPlugin, () -> {
            final Submission submission = redditQueue.previous();
            currentSubmission = submission;
            if (submission != null) {
                Bukkit.getScheduler().runTask(redditBrowserPlugin, () -> {
                    updateRoom(submission);
                    updateTitleForRoom(submission);
                    cleanupInventory();
                });
            } else {
                getPlayers().forEach(player -> player.sendMessage(ChatColor.RED + "No previous posts found."));
            }
        });
    }

    private void updateRoom(Submission submission) {
        removeHologramByType(location, EntityType.ARMOR_STAND);
        emptyCommentsChest();
        buildLeaveButton();
        removeNewCommentsButton();
        if (submission != null) {
            buildNavigationButton();
            buildVoteButtons(submission);
            buildCommentsButton();
            buildRefreshButton();
            buildLeaveButton();
            buildSubredditHologram(submission);
            if (submission.isSelfPost()) {
                buildSelfPost(submission);
            } else {
                try {
                    buildTiledMapView(submission);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            cleanBackWall();
        }
        realignItemFrames();
    }

    private void realignItemFrames() {
        location.getWorld()
                .getNearbyEntities(location,
                        -roomDimensions.getRoomWidth(),
                        -roomDimensions.getRoomHeight(),
                        -roomDimensions.getRoomDepth(),
                        o -> Objects.equals(EntityType.ITEM_FRAME, o.getType()))
                .forEach(o -> ((ItemFrame) o).setRotation(Rotation.NONE));
    }

    private void buildSubredditHologram(Submission submission) {
        final int height = roomDimensions.getRoomHeight() > 4 ? -(roomDimensions.getRoomHeight() / 2) : -1;
        spawnHologram(location.clone()
                .add(-(roomDimensions.getRoomWidth() / 2),
                        height,
                        -(roomDimensions.getRoomDepth() / 2))
                .clone().add(.5, -2, .5), colorCode("a") + "r/" + submission.getSubreddit());
    }

    private void buildRefreshButton() {
        Block refreshButton = location.getWorld().getBlockAt(location.clone().add(-(roomDimensions.getRoomWidth() / 2), -roomDimensions.getRoomHeight() + 2, -1));
        refreshButton.setType(Material.OAK_BUTTON);
        refreshButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.REFRESH));
        refreshButton.setMetadata(ROOM_ID, new FixedMetadataValue(redditBrowserPlugin, owner.getUniqueId()));
        refreshButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(redditBrowserPlugin, false));
        Directional refreshButtonDirectional = (Directional) refreshButton.getBlockData();
        refreshButtonDirectional.setFacing(BlockFace.NORTH);
        refreshButton.setBlockData(refreshButtonDirectional);
        spawnHologram(refreshButton.getLocation().clone().add(.5, -2, .5), colorCode("a") + "Refresh");
    }

    private void buildLeaveButton() {
        Block leaveButton = location.getWorld().getBlockAt(location.clone().add(-(roomDimensions.getRoomWidth() / 2) - 1, -roomDimensions.getRoomHeight() + 2, -1));
        leaveButton.setType(Material.OAK_BUTTON);
        leaveButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.LEAVE));
        leaveButton.setMetadata(ROOM_ID, new FixedMetadataValue(redditBrowserPlugin, owner.getUniqueId()));
        leaveButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(redditBrowserPlugin, false));
        Directional leaveButtonDirectional = (Directional) leaveButton.getBlockData();
        leaveButtonDirectional.setFacing(BlockFace.NORTH);
        leaveButton.setBlockData(leaveButtonDirectional);
        spawnHologram(leaveButton.getLocation().clone().add(.5, -2, .5), colorCode("c") + "Leave");
    }

    private void emptyCommentsChest() {
        Location chestLocation = location.clone().add(-roomDimensions.getRoomWidth() / 2, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1);
        final Block block = chestLocation.getBlock();
        if (Material.CHEST.equals(block.getType())) {
            Chest chest = (Chest) block.getState();
            chest.getBlockInventory().clear();
        }
    }

    private void buildCommentsButton() {
        Location chestLocation = location.clone().add(-roomDimensions.getRoomWidth() / 2, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1);
        final Block block = chestLocation.getBlock();
        block.setType(ROOM_MATERIAL);
        Block commentButton = location.getWorld().getBlockAt(chestLocation.clone().add(0, 0, 1));
        commentButton.setType(Material.OAK_BUTTON);
        commentButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.LOAD_COMMENTS));
        commentButton.setMetadata(ROOM_ID, new FixedMetadataValue(redditBrowserPlugin, owner.getUniqueId()));
        commentButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(redditBrowserPlugin, false));

        Directional commentsButtonDirection = (Directional) commentButton.getBlockData();
        commentsButtonDirection.setFacing(BlockFace.SOUTH);
        commentButton.setBlockData(commentsButtonDirection);

        spawnHologram(commentButton.getLocation().clone().add(.5, -2, .5), COMMENTS_HOLOGRAM);
    }

    private void buildEmptyRoom() {
        emptyCommentsChest();
        cube(ROOM_MATERIAL, location, location.clone().add(-roomDimensions.getRoomWidth(), -roomDimensions.getRoomHeight(), -roomDimensions.getRoomDepth()));
        cube(Material.AIR, location.clone().add(-1, -1, -1), location.clone().add(-roomDimensions.getRoomWidth() + 1, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1));
    }

    private void createRoom(
            Submission submission) {
        buildEmptyRoom();
        updateRoom(submission);
    }

    public void displayComments() {
        Bukkit.getScheduler().runTaskAsynchronously(redditBrowserPlugin, () -> {
            final RootCommentNode comments = redditBrowserPlugin.redditClient
                    .submission(currentSubmission.getId())
                    .comments();
            Bukkit.getScheduler().runTask(
                    redditBrowserPlugin,
                    () -> buildCommentsChest(location, currentSubmission.getId(), comments, roomDimensions));
        });
    }

    private void buildCommentsChest(
            Location location,
            String submissionId,
            RootCommentNode comments,
            RoomDimensions roomDimensions) {
        if (comments != null) {
            buildNewCommentButton();
            Location chestLocation = location.clone().add(-roomDimensions.getRoomWidth() / 2, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1);
            final Location buttonLocation = chestLocation.clone().add(0, 0, 1);
            buttonLocation.getBlock().setType(Material.AIR);
            Block b = chestLocation.getBlock();

            b.setType(Material.CHEST);
            Directional chestDirection = (Directional) b.getBlockData();
            chestDirection.setFacing(BlockFace.SOUTH);
            b.setBlockData(chestDirection);
            b.setMetadata(SUBMISSION_ID, new FixedMetadataValue(redditBrowserPlugin, submissionId));
            b.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.COMMENT_CHEST));
            Chest chest = (Chest) b.getState();

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
                    bookmeta.setDisplayName(COMMENT_DISPLAY_NAME);
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

    private void buildNewCommentButton() {
        Location buttonLocation = location.clone().add(-roomDimensions.getRoomWidth() / 2 - 1, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1);
        final Block writeCommentsButton = buttonLocation.getBlock();
        writeCommentsButton.setType(Material.OAK_BUTTON);
        writeCommentsButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.WRITE_COMMENT));
        writeCommentsButton.setMetadata(ROOM_ID, new FixedMetadataValue(redditBrowserPlugin, owner.getUniqueId()));
        writeCommentsButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(redditBrowserPlugin, false));

        Directional commentsButtonDirection = (Directional) writeCommentsButton.getBlockData();
        commentsButtonDirection.setFacing(BlockFace.SOUTH);
        writeCommentsButton.setBlockData(commentsButtonDirection);

        spawnHologram(writeCommentsButton.getLocation().clone().add(.5, -2, .5), WRITE_COMMENT_HOLOGRAM);
    }

    private void removeNewCommentsButton() {
        Location buttonLocation = location.clone().add(-roomDimensions.getRoomWidth() / 2 - 1, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1);
        final Block writeCommentsButton = buttonLocation.getBlock();
        writeCommentsButton.setType(Material.AIR);
    }

    private void buildVoteButtons(Submission submission) {
        Block uv = location.getWorld().getBlockAt(location.clone().add(-roomDimensions.getRoomWidth() + 1, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1));
        uv.setType(Material.OAK_BUTTON);
        uv.setMetadata(SUBMISSION_ID, new FixedMetadataValue(redditBrowserPlugin, submission.getId()));
        uv.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.UPVOTE));
        Directional uvdir = (Directional) uv.getBlockData();
        uvdir.setFacing(BlockFace.SOUTH);
        uv.setBlockData(uvdir);
        spawnHologram(uv.getLocation().clone().add(.5, -2, .5), colorCode("a") + "+1");


        Block dv = location.getWorld().getBlockAt(location.clone().add(-1, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() + 1));
        dv.setType(Material.OAK_BUTTON);
        dv.setMetadata(SUBMISSION_ID, new FixedMetadataValue(redditBrowserPlugin, submission.getId()));
        dv.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.DOWNVOTE));

        Directional dvdir = (Directional) dv.getBlockData();
        dvdir.setFacing(BlockFace.SOUTH);
        dv.setBlockData(dvdir);

        spawnHologram(dv.getLocation().clone().add(.5, -2, .5), colorCode("c") + "-1");
    }

    private void buildSelfPost(Submission submission) {
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

        cleanBackWall();
        tiledRenderer = null;
        Bukkit.getScheduler().runTaskLater(redditBrowserPlugin, new Runnable() {

            @Override
            public void run() {
                location.clone().add(-(roomDimensions.getRoomWidth() / 2), -roomDimensions.getRoomHeight() + 2, -roomDimensions.getRoomDepth())
                        .getBlock().setType(Material.GLOWSTONE);
                ItemFrame itf = (ItemFrame) location.getWorld().spawnEntity(location.clone().add(-(roomDimensions.getRoomWidth() / 2), -roomDimensions.getRoomHeight() + 2, -roomDimensions.getRoomDepth() + 1), EntityType.ITEM_FRAME);
                itf.setFacingDirection(BlockFace.SOUTH);

                book.setItemMeta(bookmeta);
                itf.setItem(book);
            }
        }, 5);

    }

    private void buildNavigationButton() {
        int zPosition = (-roomDimensions.getRoomDepth() / 2) + 1;
        if (zPosition > -2) {
            zPosition = -2;
        }

        Block nextButton = location.getWorld().getBlockAt(location.clone().add(-1, -roomDimensions.getRoomHeight() + 2, zPosition));
        nextButton.setType(Material.OAK_BUTTON);
        nextButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.NEXT_ROOM));
        nextButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(redditBrowserPlugin, false));
        nextButton.setMetadata(ROOM_ID, new FixedMetadataValue(redditBrowserPlugin, owner.getUniqueId()));
        Directional nextButtonDirection = (Directional) nextButton.getBlockData();
        nextButtonDirection.setFacing(BlockFace.WEST);
        nextButton.setBlockData(nextButtonDirection);
        spawnHologram(nextButton.getLocation().clone().add(.5, -2, .5), colorCode("9") + "Next");

        if (redditQueue.hasPrevious()) {
            Block previousButton = location.getWorld().getBlockAt(location.clone().add(-roomDimensions.getRoomWidth() + 1, -roomDimensions.getRoomHeight() + 2, zPosition));
            previousButton.setType(Material.OAK_BUTTON);
            previousButton.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.PREVIOUS_ROOM));
            previousButton.setMetadata(BUTTON_ACTIVATED, new FixedMetadataValue(redditBrowserPlugin, false));
            previousButton.setMetadata(ROOM_ID, new FixedMetadataValue(redditBrowserPlugin, owner.getUniqueId()));
            Directional previousButtonDirection = (Directional) nextButton.getBlockData();
            previousButtonDirection.setFacing(BlockFace.EAST);
            previousButton.setBlockData(previousButtonDirection);
            spawnHologram(previousButton.getLocation().clone().add(.5, -2, .5), PREVIOUS_HOLOGRAM_NAME);
        } else {
            Block previousButton = location.getWorld().getBlockAt(location.clone().add(-roomDimensions.getRoomWidth() + 1, -roomDimensions.getRoomHeight() + 2, zPosition));
            previousButton.setType(Material.AIR);
            removeHologramByName(previousButton.getLocation().clone().add(.5, -2, .5), PREVIOUS_HOLOGRAM_NAME);
        }
    }

    private void removeHologramByName(Location location, String name) {
        location.getWorld()
                .getNearbyEntities(location,
                        -roomDimensions.getRoomWidth(),
                        -roomDimensions.getRoomHeight(),
                        -roomDimensions.getRoomDepth(),
                        o -> Objects.equals(name, o.getName()))
                .forEach(Entity::remove);
    }

    private void removeHologramByType(Location location, EntityType entityType) {
        location.getWorld()
                .getNearbyEntities(location,
                        -roomDimensions.getRoomWidth(),
                        -roomDimensions.getRoomHeight(),
                        -roomDimensions.getRoomDepth(),
                        o -> Objects.equals(entityType, o.getType()))
                .forEach(Entity::remove);
    }

    private void cleanBackWall() {
        clearItemFrames();

        int screenWidth = roomDimensions.getScreenWidth();
        int screenHeight = roomDimensions.getScreenHeight();

        int titleXStart = (screenWidth + 1) * -1;
        int titleYStart = roomDimensions.getScreenHeight() - roomDimensions.getRoomHeight() + 1;
        for (int row = 0; row < screenHeight; row++) {
            for (int col = 0; col < screenWidth; col++) {
                location.clone().add(titleXStart + col, titleYStart - row, -roomDimensions.getRoomDepth()).getBlock().setType(ROOM_MATERIAL);
            }
        }
    }

    private void buildTiledMapView(Submission submission) {
        final World world = location.getWorld();

        int screenWidth = roomDimensions.getScreenWidth();
        int screenHeight = roomDimensions.getScreenHeight();

        int titleXStart = (screenWidth + 1) * -1;
        int titleYStart = roomDimensions.getScreenHeight() - roomDimensions.getRoomHeight() + 1;
        if (tiledRenderer == null) {
            cleanBackWall();
            tiledRenderer = new TiledRenderer(redditBrowserPlugin, screenWidth, screenHeight);
            Bukkit.getScheduler().runTaskLater(redditBrowserPlugin, () -> {
                for (int row = 0; row < screenHeight; row++) {
                    for (int col = 0; col < screenWidth; col++) {
                        location.clone().add(titleXStart + col, titleYStart - row, -roomDimensions.getRoomDepth()).getBlock().setType(Material.GLOWSTONE);
                        ItemFrame itf = (ItemFrame) world.spawnEntity(location.clone().add(titleXStart + col, titleYStart - row, -roomDimensions.getRoomDepth() + 1), EntityType.ITEM_FRAME);
                        itf.setFacingDirection(BlockFace.SOUTH);
                        itf.setInvulnerable(true);
                        itf.setMetadata(ROOM_ID, new FixedMetadataValue(redditBrowserPlugin, getRoomId()));
                        itf.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(redditBrowserPlugin, InteractiveEnum.SHOW_URL));
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

            }, 5);
        }
        Bukkit.getScheduler().runTaskAsynchronously(redditBrowserPlugin, () -> {
            final BufferedImage image = URLToImageRepository.findImage(submission);
            tiledRenderer.updateImage(submission.getUrl(), image);
        });
    }

    private void clearItemFrames() {
        removeHologramByType(location, EntityType.ITEM_FRAME);
    }

    private void cube(Material blockMaterial, Location from, Location to) {
        for (int x = from.getBlockX(); x >= to.getBlockX(); x--) {
            for (int y = from.getBlockY(); y >= to.getBlockY(); y--) {
                for (int z = from.getBlockZ(); z >= to.getBlockZ(); z--) {
                    final Block block = from.getWorld().getBlockAt(x, y, z);
                    block.setType(blockMaterial);
                }
            }
        }
    }

    private void spawnHologram(Location l, String name) {
        ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        as.setCustomName(name);
        as.setCustomNameVisible(true);
        as.setGravity(false);
        as.setVisible(false);
        as.setInvulnerable(true);
        as.setCollidable(false);
    }

    public boolean hasPlayers() {
        return location.getWorld().getNearbyEntities(location,
                -roomDimensions.getRoomWidth(),
                -roomDimensions.getRoomHeight(),
                -roomDimensions.getRoomDepth(),
                entity -> entity instanceof Player).size() > 0;
    }

    public boolean hasPlayer(UUID playerId) {
        return location.getWorld().getNearbyEntities(location,
                -roomDimensions.getRoomWidth(),
                -roomDimensions.getRoomHeight(),
                -roomDimensions.getRoomDepth(),
                entity -> entity instanceof Player)
                .stream()
                .anyMatch(o -> Objects.equals(playerId, o.getUniqueId()));
    }

    public Set<Player> getPlayers() {
        return location.getWorld().getNearbyEntities(location,
                -roomDimensions.getRoomWidth(),
                -roomDimensions.getRoomHeight(),
                -roomDimensions.getRoomDepth(),
                entity -> entity instanceof Player)
                .stream()
                .map(o -> (Player) o)
                .collect(Collectors.toSet());
    }

    public void destroy() {
        try {
            Bukkit.getScheduler().runTask(redditBrowserPlugin, new Runnable() {
                @Override
                public void run() {
                    location.getWorld().getNearbyEntities(location,
                            -roomDimensions.getRoomWidth(),
                            -roomDimensions.getRoomHeight(),
                            -roomDimensions.getRoomDepth(),
                            entity -> !(entity instanceof Player))
                            .forEach(Entity::remove);
                    getPlayers().forEach(redditBrowserPlugin::kickOut);
                    cube(Material.AIR, location, location.clone().add(-roomDimensions.getRoomWidth(), -roomDimensions.getRoomHeight(), -roomDimensions.getRoomDepth()));
                    Bukkit.getLogger().log(Level.INFO, "Destroyed reddit room of player " + owner.getDisplayName());
                }
            });
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.INFO, "Could not remove reddit room of player " + owner.getDisplayName());
        }
    }

    public UUID getRoomId() {
        return owner.getUniqueId();
    }

    public boolean isInside(Location locationToTest) {
        Location roomMin = this.location.clone().add(-roomDimensions.getRoomWidth(), -roomDimensions.getRoomHeight(), -roomDimensions.getRoomDepth());
        Location roomMax = this.location;
        if (!Objects.equals(roomMax.getWorld().getName(), locationToTest.getWorld().getName())) {
            return false;
        }
        boolean x = locationToTest.getX() >= Math.min(roomMax.getX(), roomMin.getX()) && locationToTest.getX() <= Math.max(roomMax.getX(), roomMin.getX());
        boolean y = locationToTest.getY() >= Math.min(roomMax.getY(), roomMin.getY()) && locationToTest.getY() <= Math.max(roomMax.getY(), roomMin.getY());
        boolean z = locationToTest.getZ() >= Math.min(roomMax.getZ(), roomMin.getZ()) && locationToTest.getZ() <= Math.max(roomMax.getZ(), roomMin.getZ());
        return x && y && z;
    }

    public void showURLtoPlayers(Player player) {
        if(currentSubmission != null) {
            player.sendMessage(ChatColor.YELLOW + "Image/Video link: " + ChatColor.BLUE + ChatColor.UNDERLINE + currentSubmission.getUrl());
        }
    }
}
