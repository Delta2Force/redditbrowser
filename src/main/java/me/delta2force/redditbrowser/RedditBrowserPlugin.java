package me.delta2force.redditbrowser;

import com.google.gson.Gson;
import me.delta2force.redditbrowser.generator.RedditGenerator;
import me.delta2force.redditbrowser.generator.RoomDimensions;
import me.delta2force.redditbrowser.generator.RoomGenerator;
import me.delta2force.redditbrowser.inventory.RedditInventory;
import me.delta2force.redditbrowser.listeners.EventListener;
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class RedditBrowserPlugin extends JavaPlugin {
    public static final String SUBMISSION_ID = "submissionId";
    public static final String INTERACTIVE_ENUM = "interactiveEnum";
    public static final String REDDIT_POST_TITLE = "redditPostTitle";
    public static final String REDDIT_POST_SCORE = "redditPostScore";
    public static final String REDDIT_POST_AUTHOR= "redditPostAuthor";
    private static final int TITLE_LENGHT_LIMIT = 64;

    private Map<UUID, Location> beforeTPLocation = new HashMap<>();
    private Map<UUID, RedditInventory> beforeTPInventory = new HashMap<>();
    private Map<UUID, Integer> beforeTPExperience = new HashMap<>();
    private List<UUID> redditBrowsers = new ArrayList<>();
    public ArrayList<Runnable> runnableQueue = new ArrayList<>();
    public Map<String, CommentNode<Comment>> commentCache = new HashMap<>();

    private List<BukkitTask> task = new ArrayList<>();
    public RedditClient reddit;
    public EventListener listener;
    private RoomGenerator roomGenerator = new RoomGenerator(this);

    @Override
    public void onEnable() {
        // Save the default config
        saveDefaultConfig();
        listener = new EventListener(this);
        Bukkit.getServer().getPluginManager().registerEvents(listener, this);
        try {
            checkLatestVersion();
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
        return new Client(this);
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
            //Older versions like 1.13.x
            player.getInventory().addItem(new ItemStack(Material.LEGACY_SIGN, 16));
        }
    }


    public static String colorCode(String color) {
        return (char) (0xfeff00a7) + color;
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
        Client client = getClient();
        RoomDimensions roomDimensions = createRoomDimensions(client);
        Random r = new Random();
        Location location = new Location(w, r.nextInt(2000000) - 1000000, 255, r.nextInt(2000000) - 1000000);
        Bukkit.getScheduler().runTaskAsynchronously(this , task -> {
            int maxPosts = client.getMaxPosts();
            Stream<Submission> submissionStream = reddit
                    .subreddit(sub)
                    .posts()
                    .sorting(SubredditSort.HOT)
                    .limit(maxPosts)
                    .build()
                    .stream();
            int i = 0;
            Submission firstSubmission = null;
            if(submissionStream.hasNext()) {
                while (i < maxPosts && submissionStream.hasNext()) {
                    Submission submission = submissionStream.next();
                    if (i == 0) {
                        firstSubmission = submission;
                    }
                    RootCommentNode comments = client.isCommentsEnabled() ?
                            reddit.submission(submission.getId()).comments(new CommentsRequest(null, null, 1, 10, CommentSort.TOP)) :
                            null;

                    final int index = i;
                    i++;

                    boolean isLast = i == maxPosts || !submissionStream.hasNext();
                    runBuildTask(
                            player,
                            location,
                            firstSubmission,
                            submission,
                            comments,
                            index,
                            isLast,
                            roomDimensions,
                            client.getMaxPosts());
                    player.sendMessage("" + ChatColor.GREEN + i + " / " + maxPosts + " posts loaded");
                }
            } else {
                player.sendMessage(ChatColor.RED + "No posts found.");
            }
        });

    }

    private void buildRoom(
            Location location,
            Submission submission,
            RootCommentNode comments,
            boolean isFirst,
            RoomDimensions roomDimensions) {
        roomGenerator.createRoom(
                location,
                submission,
                comments,
                isFirst,
                roomDimensions
        );

    }

    @NotNull
    private RoomDimensions createRoomDimensions(Client client) {
        int screenWidth = client.getScreenWidth();
        if(screenWidth < 1) {
            screenWidth = 1;
        }
        int screenHeight = client.getScreenHeight();
        if(screenHeight < 1) {
            screenHeight = 1;
        }
        int roomDepth = client.getRoomDepth();
        if(roomDepth < 5) {
            roomDepth = 5;
        }
        int roomHeight = screenHeight > 1 ? screenHeight + 2 : screenHeight + 3;
        int roomWidth = screenWidth >= 3 ? screenWidth + 1 : screenWidth + 2;
        if(roomWidth < 4) {
            roomWidth = 4;
        }
        return new RoomDimensions(
                roomWidth,
                roomHeight,
                roomDepth,
                screenWidth,
                screenHeight
        );
    }

    private void runBuildTask(Player player,
                              Location location,
                              Submission firstSubmission,
                              Submission submission,
                              RootCommentNode comments,
                              int index,
                              boolean isLast,
                              RoomDimensions roomDimensions,
                              int maxPosts) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            int nextX = roomDimensions.getRoomWidth() * index;
            buildRoom(location.clone().add(nextX, 0, 0), submission, comments, index == 0, roomDimensions);
            player.sendMessage("" + ChatColor.DARK_GREEN + (index + 1) + " / " + maxPosts + " posts built");
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
                                Location loc = location.clone().add(-roomDimensions.getRoomWidth() / 2, -roomDimensions.getRoomHeight() + 1, -roomDimensions.getRoomDepth() / 2);
                                loc.getChunk().load();
                                loc.setPitch(0);
                                loc.setYaw(180);

                                player.teleport(loc);
                                player.setGameMode(GameMode.SURVIVAL);
                                player.getInventory().clear();
                                player.getInventory().addItem(new ItemStack(Material.WRITABLE_BOOK));
                                giveSign(player);
                                updateTitle(firstSubmission.getTitle(), firstSubmission.getAuthor(), firstSubmission.getScore(), player);
                            }
                        },
                        100);
            }
        }, 0);
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

    public List<UUID> getRedditBrowsers() {
        return redditBrowsers;
    }

    public List<BukkitTask> getTask() {
        return task;
    }
}
