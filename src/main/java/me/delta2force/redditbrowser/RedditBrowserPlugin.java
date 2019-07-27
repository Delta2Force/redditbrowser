package me.delta2force.redditbrowser;

import com.google.gson.Gson;
import me.delta2force.redditbrowser.generator.RedditGenerator;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.inventory.RedditInventory;
import me.delta2force.redditbrowser.listeners.EventListener;
import me.delta2force.redditbrowser.renderer.TiledRenderer;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.*;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.pagination.Stream;
import net.dean.jraw.references.CommentsRequest;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class RedditBrowserPlugin extends JavaPlugin {
    private static final int SCREEN_WIDTH_HEIGHT = 8;
    private static final int ROOM_DEPTH = 8;
    private static final int ROOM_HEIGHT = SCREEN_WIDTH_HEIGHT + 2;
    private static final int ROOM_WIDTH = SCREEN_WIDTH_HEIGHT + 1;
    public static final String SUBMISSION_ID = "submissionId";
    public static final String INTERACTIVE_ENUM = "interactiveEnum";
    public static final String REDDIT_POST_TITLE = "redditPostTitle";
    public static final String REDDIT_POST_SCORE = "redditPostScore";
    public static final String REDDIT_POST_AUTHOR= "redditPostAuthor";
    public static final int TITLE_LENGHT_LIMIT = 64;

    private Map<UUID, Location> beforeTPLocation = new HashMap<>();
    private Map<UUID, RedditInventory> beforeTPInventory = new HashMap<>();
    private Map<UUID, Integer> beforeTPExperience = new HashMap<>();
    private List<UUID> redditBrowsers = new ArrayList<>();
    public ArrayList<Runnable> runnableQueue = new ArrayList<>();
    public Map<String, CommentNode<Comment>> commentCache = new HashMap<>();

    private List<BukkitTask> task = new ArrayList<>();
    public RedditClient reddit;
    public EventListener listener;
    private Client client;

    @Override
    public void onEnable() {
        // Save the default config
        saveDefaultConfig();
        listener = new EventListener(this);
        Bukkit.getServer().getPluginManager().registerEvents(listener, this);
        try {
            checkLatestVersion();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkLatestVersion() throws MalformedURLException, IOException {
        Gson g = new Gson();
        InputStream stream = new URL("https://api.github.com/repos/Delta2Force/redditbrowser/releases/latest").openStream();
        InputStreamReader reader = new InputStreamReader(stream);
        GithubAPIResponse gar = g.fromJson(reader, GithubAPIResponse.class);
        reader.close();
        stream.close();

        if (!this.getDescription().getVersion().contains(gar.tag_name)) {
            this.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "Your version of RedditBrowser is outdated! The newest version is: " + gar.tag_name + ". You can download it from: " + gar.html_url);
        }
    }

    @Override
    public void onDisable() {
        Iterator<UUID> redditBrowserIterator = redditBrowsers.iterator();
        while (redditBrowserIterator.hasNext()) {
            UUID u = redditBrowserIterator.next();
            Player p = Bukkit.getPlayer(u);
            kickOut(p);
        }
        beforeTPLocation.clear();
        beforeTPInventory.clear();
        redditBrowsers.clear();
        listener = null;
    }

    public void attemptConnect() {
        Client client = getClient();
        Credentials oauthCreds = Credentials.script(client.getUsername(), client.getPassword(), client.getClientId(), client.getClientSecret());
        UserAgent userAgent = new UserAgent("bot", "reddit.minecraft.browser", this.getDescription().getVersion(), client.getUsername());
        reddit = OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), oauthCreds);
    }


    private Client getClient() {
        if (client == null) {
            client = new Client(this);
        }
        return client;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (command.getName().equals("reddit")) {

                if (reddit == null) {
                    attemptConnect();
                }
                beforeTPLocation.put(p.getUniqueId(), p.getLocation());
                beforeTPInventory.put(p.getUniqueId(), new RedditInventory(p.getInventory()));
                redditBrowsers.add(p.getUniqueId());
                beforeTPExperience.put(p.getUniqueId(), p.getTotalExperience());
                p.getInventory().clear();
                setupReddit(p);
                if (args != null && args.length > 0 && StringUtils.isNotBlank(args[0])) {
                    createTowerAndTP(p, args[0], p.getWorld());
                }

            } else if(command.getName().equals("leave")) {
                if (redditBrowsers.contains(p.getUniqueId())) {
                    kickOut(p);
                }
            }
        }
        return true;
    }

    public void setupReddit(Player p) {
        p.sendMessage(ChatColor.YELLOW + "Please wait while I setup Reddit...");
        WorldCreator wc = new WorldCreator("reddit");
        wc.generator(new RedditGenerator());
        wc.generateStructures(false);
        World w = Bukkit.createWorld(wc);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        p.setInvulnerable(true);
        w.setTime(1400);
        Random r = new Random();
        Location l = new Location(w, r.nextInt(2000000) - 1000000, 30, r.nextInt(2000000) - 1000000);
        int fx = l.getBlockX() - 11;
        int fz = l.getBlockZ() - 1;
        int tx = l.getBlockX() + 11;
        int tz = l.getBlockZ() + 10;
        for (int x = fx; x < tx; x++) {
            for (int z = fz; z < tz; z++) {
                new Location(w, x, 30, z).getBlock().setType(Material.POLISHED_ANDESITE);
            }
        }
        p.teleport(l.clone().add(0, 1, 0));
        giveSign(p);
        p.sendMessage(ChatColor.GREEN + "There you go!");
        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                setKarma(p);
            }
        });
    }

    private void giveSign(Player player) {
        try {
            player.getInventory().addItem(new ItemStack(Material.OAK_SIGN, 16));
        } catch (NoSuchFieldError e) {
            //Older version like 1.13.x
            player.getInventory().addItem(new ItemStack(Material.LEGACY_SIGN, 16));
        }
    }

    public ArmorStand spawnHologram(Location l, String name) {
        ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        as.setCustomName(name);
        as.setCustomNameVisible(true);
        as.setGravity(false);
        as.setVisible(false);
        as.setInvulnerable(true);
        as.setCollidable(false);
        return as;
    }

    public String colorCode(String color) {
        return (char) (0xfeff00a7) + color;
    }

    public void setRoom(Location l, Submission s, RootCommentNode comments, boolean firstRoom, Player player) {
        cube(Material.POLISHED_ANDESITE, l, l.clone().add(-ROOM_WIDTH, -ROOM_HEIGHT, -ROOM_DEPTH));
        cube(Material.AIR, l.clone().add(-1, -1, -1), l.clone().add(-ROOM_WIDTH + 1, -ROOM_HEIGHT + 1, -ROOM_DEPTH + 1));

        if (!firstRoom) {
            putDoor(l.clone().add(-ROOM_WIDTH, -ROOM_HEIGHT + 1, (-ROOM_DEPTH / 2) + 1));
            Block pressurePlateLeft = l.clone().add(-ROOM_WIDTH + 1, -ROOM_HEIGHT + 1, (-ROOM_DEPTH / 2) + 1).getBlock();
            pressurePlateLeft.setType(Material.STONE_PRESSURE_PLATE);
            pressurePlateLeft.setMetadata(REDDIT_POST_TITLE, new FixedMetadataValue(this, s.getTitle()));
            pressurePlateLeft.setMetadata(REDDIT_POST_SCORE, new FixedMetadataValue(this,s.getScore()));
            pressurePlateLeft.setMetadata(REDDIT_POST_AUTHOR, new FixedMetadataValue(this,s.getAuthor()));
            pressurePlateLeft.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(this, InteractiveEnum.ROOM_ENTERED));
        }
        Block pressurePlateRight = l.clone().add(-1, -ROOM_HEIGHT + 1, (-ROOM_DEPTH / 2) + 1).getBlock();
        pressurePlateRight.setType(Material.STONE_PRESSURE_PLATE);
        pressurePlateRight.setMetadata(REDDIT_POST_TITLE, new FixedMetadataValue(this, s.getTitle()));
        pressurePlateRight.setMetadata(REDDIT_POST_SCORE, new FixedMetadataValue(this,s.getScore()));
        pressurePlateRight.setMetadata(REDDIT_POST_AUTHOR, new FixedMetadataValue(this,s.getAuthor()));
        pressurePlateRight.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(this, InteractiveEnum.ROOM_ENTERED));

        Block uv = l.getWorld().getBlockAt(l.clone().add(-ROOM_WIDTH + 1, -ROOM_HEIGHT + 1, -ROOM_DEPTH + 1));
        uv.setType(Material.OAK_BUTTON);
        uv.setMetadata(SUBMISSION_ID, new FixedMetadataValue(this, s.getId()));
        uv.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(this, InteractiveEnum.UPVOTE));
        Directional uvdir = (Directional) uv.getBlockData();
        uvdir.setFacing(BlockFace.SOUTH);
        uv.setBlockData(uvdir);


        Block dv = l.getWorld().getBlockAt(l.clone().add(-1, -ROOM_HEIGHT + 1, -ROOM_DEPTH + 1));
        dv.setType(Material.OAK_BUTTON);
        dv.setMetadata(SUBMISSION_ID, new FixedMetadataValue(this, s.getId()));
        dv.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(this, InteractiveEnum.DOWNVOTE));

        Directional dvdir = (Directional) dv.getBlockData();
        dvdir.setFacing(BlockFace.SOUTH);
        dv.setBlockData(dvdir);

        spawnHologram(uv.getLocation().clone().add(.5, -2, .5), colorCode("a") + "+1");
        spawnHologram(dv.getLocation().clone().add(.5, -2, .5), colorCode("c") + "-1");

        if (s.isSelfPost()) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta bookmeta = (BookMeta) book.getItemMeta();
            bookmeta.setTitle(s.getTitle());
            bookmeta.setAuthor(s.getAuthor());
            if (s.getSelfText().length() > 255) {
                double f = Math.ceil(((float) s.getSelfText().length()) / 255f);
                for (int i = 0; i < f; i++) {
                    if (s.getSelfText().length() < (i + 1) * 255) {
                        bookmeta.addPage(s.getSelfText().substring(i * 255, s.getSelfText().length()));
                    } else {
                        bookmeta.addPage(s.getSelfText().substring(i * 255, (i + 1) * 255));
                    }
                }
            } else {
                bookmeta.addPage(s.getSelfText());
            }
            ItemFrame itf = (ItemFrame) l.getWorld().spawnEntity(l.clone().add(-(ROOM_WIDTH / 2), -ROOM_HEIGHT+2, -ROOM_DEPTH + 1), EntityType.ITEM_FRAME);
            itf.setFacingDirection(BlockFace.SOUTH);

            book.setItemMeta(bookmeta);
            itf.setItem(book);
        } else {
            try {
                createTiledMapView(l, s.getUrl());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //l.clone().add(-2, -2, -2).getBlock().setType(Material.AIR);

        if (comments != null) {
            Location chestLocation = l.clone().add(-ROOM_WIDTH / 2, -ROOM_HEIGHT + 1, -ROOM_DEPTH + 1);
            Block b = chestLocation.getBlock();

            b.setType(Material.CHEST);
            Directional chestDirection = (Directional) b.getBlockData();
            chestDirection.setFacing(BlockFace.SOUTH);
            b.setBlockData(chestDirection);
            b.setMetadata(SUBMISSION_ID, new FixedMetadataValue(this, s.getId()));
            b.setMetadata(INTERACTIVE_ENUM, new FixedMetadataValue(this, InteractiveEnum.COMMENT_CHEST));
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
                    commentCache.put(c.getId(), cn);
                    chest.getInventory().addItem(book);
                } else {
                    break;
                }
                in++;
            }
        }
    }

    public void updateTitle(String title, String author, int score, Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard newScoreboard = manager.getNewScoreboard();
        Objective objective = newScoreboard.registerNewObjective("reddit", "Score", chopOffTitle(title));
//        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Score scoreField = objective.getScore(ChatColor.GREEN + "Upvotes:");
        scoreField.setScore(score);
        player.setScoreboard(newScoreboard);
        player.sendMessage(ChatColor.GREEN + author + ": " + ChatColor.WHITE + title);
//        player.sendTitle(chopOffTitle(title), ChatColor.GREEN + "Score: " + score , 10, 100, 10);
    }

    public static String chopOffTitle(String title) {
        if(title.length() > TITLE_LENGHT_LIMIT) {
            return title.substring(0, TITLE_LENGHT_LIMIT) + "...";
        }
        return title;
    }

    private void putDoor(Location location) {
        final Block bottom = location.getBlock();
        final Block top = bottom.getRelative(BlockFace.UP, 1);
        top.setType(Material.OAK_DOOR);
        Door topDoor = (Door) top.getBlockData();
        topDoor.setHinge(Door.Hinge.LEFT);
        topDoor.setHalf(Bisected.Half.TOP);
        topDoor.setFacing(BlockFace.WEST);
        top.setBlockData(topDoor);
        bottom.setType(Material.OAK_DOOR);
        Door bottomDoor = (Door) bottom.getBlockData();
        bottomDoor.setHinge(Door.Hinge.LEFT);
        bottomDoor.setHalf(Bisected.Half.BOTTOM);
        bottomDoor.setFacing(BlockFace.WEST);
        bottom.setBlockData(bottomDoor);
    }

    private void createTiledMapView(Location l, String url) {
        int tileWidth = SCREEN_WIDTH_HEIGHT;
        int tileHeight = SCREEN_WIDTH_HEIGHT;
        TiledRenderer tiledRenderer = new TiledRenderer(url, this, tileWidth, tileHeight);
        int titleXStart = tileWidth * -1;
        int titleYStart = -1;
        for (int row = 0; row < tileHeight; row++) {
            for (int col = 0; col < tileWidth; col++) {
                l.clone().add(titleXStart + col, titleYStart - row, -ROOM_DEPTH).getBlock().setType(Material.GLOWSTONE);
                ItemFrame itf = (ItemFrame) l.getWorld().spawnEntity(l.clone().add(titleXStart + col, titleYStart - row, -ROOM_DEPTH + 1), EntityType.ITEM_FRAME);
                itf.setFacingDirection(BlockFace.SOUTH);
                ItemStack map = new ItemStack(Material.FILLED_MAP);
                MapMeta mapMeta = (MapMeta) map.getItemMeta();
                MapView mv = Bukkit.createMap(l.getWorld());
                mv.setTrackingPosition(false);
                mv.setUnlimitedTracking(false);
                mv.addRenderer(tiledRenderer.getRenderer(row, col));
                mapMeta.setMapView(mv);
                map.setItemMeta(mapMeta);
                itf.setItem(map);
            }
        }
    }

    public void cube(Material blockMaterial, Location from, Location to) {
        for (int x = from.getBlockX(); x >= to.getBlockX(); x--) {
            for (int y = from.getBlockY(); y >= to.getBlockY(); y--) {
                for (int z = from.getBlockZ(); z >= to.getBlockZ(); z--) {
                    from.getWorld().getBlockAt(x, y, z).setType(blockMaterial);
                }
            }
        }
    }

    public void setKarma(Player p) {
        p.setTotalExperience(0);
        int karma = 0;
        for (KarmaBySubreddit kbs : reddit.me().karma()) {
            karma += kbs.getLinkKarma();
            karma += kbs.getCommentKarma();
        }
        p.setLevel(karma);
    }

    public void createTowerAndTP(Player player, String sub, World w) {
        Random r = new Random();
        Location l = new Location(w, r.nextInt(2000000) - 1000000, 255, r.nextInt(2000000) - 1000000);
        Bukkit.getScheduler().runTaskAsynchronously(this , task -> {
            Stream<Submission> submissionStream = reddit
                    .subreddit(sub)
                    .posts()
                    .sorting(SubredditSort.HOT)
                    .limit(getClient().getMaxPosts())
                    .build()
                    .stream();
            int i = 0;
            Submission firstSubmission = null;
            if(submissionStream.hasNext()) {
                while (i < getClient().getMaxPosts() && submissionStream.hasNext()) {
                    Submission submission = submissionStream.next();
                    if (i == 0) {
                        firstSubmission = submission;
                    }
                    RootCommentNode comments = getClient().isCommentsEnabled() ?
                            reddit.submission(submission.getId()).comments(new CommentsRequest(null, null, 1, 10, CommentSort.TOP)) :
                            null;

                    final int index = i;
                    i++;

                    build(player, l, firstSubmission, submission, comments, index, i == getClient().getMaxPosts() || !submissionStream.hasNext());
                    player.sendMessage("" + ChatColor.GREEN + i + " / " + getClient().getMaxPosts() + " posts loaded");
                }
            } else {
                player.sendMessage(ChatColor.RED + "No posts found.");
            }
        });

    }

    private void build(Player p, Location l, Submission firstSubmission, Submission s, RootCommentNode comments, int index, boolean isLast) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            setRoom(l.clone().add(ROOM_WIDTH * index, 0, 0), s, comments, index == 0, p);
            p.sendMessage("" + ChatColor.DARK_GREEN + (index + 1) + " / " + getClient().getMaxPosts() + " posts built");
            if (isLast) {
                for (Runnable t : runnableQueue) {
                    Bukkit.getScheduler().runTaskAsynchronously(this, t);
                }
                runnableQueue.clear();
                if (!task.isEmpty()) {
                    BukkitTask bt = task.get(0);
                    task.remove(0);
                    bt.cancel();
                }
                Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                            @Override
                            public void run() {
                                Location loc = l.clone().add(-ROOM_WIDTH / 2, -ROOM_HEIGHT + 1, -ROOM_DEPTH / 2);
                                loc.getChunk().load();
                                updateTitle(firstSubmission.getTitle(), firstSubmission.getAuthor(), firstSubmission.getScore(), p);
                                loc.setPitch(0);
                                loc.setYaw(180);
                                p.teleport(loc);
                                p.setGameMode(GameMode.SURVIVAL);
                                p.getInventory().clear();
                                p.getInventory().addItem(new ItemStack(Material.WRITABLE_BOOK));
                                giveSign(p);

                            }
                        },
                        10);
            }
        }, 0);
    }

    public Location roundedLocation(Location loc) {
        return new Location(loc.getWorld(), (int) loc.getX(), (int) loc.getY(), (int) loc.getZ());
    }

    public void kickOut(Player p) {
        p.sendMessage(ChatColor.GREEN + "Goodbye reddit!");
        p.teleport(beforeTPLocation.get(p.getUniqueId()));
        p.getInventory().clear();
        p.setTotalExperience(beforeTPExperience.get(p.getUniqueId()));
        RedditInventory beforeTP = beforeTPInventory.get(p.getUniqueId());
        beforeTP.apply(p);
        p.setInvulnerable(false);

        redditBrowsers.remove(p.getUniqueId());
        beforeTPLocation.remove(p.getUniqueId());
        beforeTPInventory.remove(p.getUniqueId());
        beforeTPExperience.remove(p.getUniqueId());
    }

    public Map<UUID, Location> getBeforeTPLocation() {
        return beforeTPLocation;
    }

    public Map<UUID, RedditInventory> getBeforeTPInventory() {
        return beforeTPInventory;
    }

    public List<UUID> getRedditBrowsers() {
        return redditBrowsers;
    }

    public List<BukkitTask> getTask() {
        return task;
    }
}
