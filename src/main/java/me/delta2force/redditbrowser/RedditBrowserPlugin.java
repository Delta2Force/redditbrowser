package me.delta2force.redditbrowser;

import com.google.gson.Gson;
import me.delta2force.redditbrowser.generator.RedditGenerator;
import me.delta2force.redditbrowser.inventory.RedditInventory;
import me.delta2force.redditbrowser.listeners.EventListener;
import me.delta2force.redditbrowser.room.Room;
import me.delta2force.redditbrowser.room.RoomDimensions;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.KarmaBySubreddit;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.tree.CommentNode;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.delta2force.redditbrowser.room.Room.COMMENT_DISPLAY_NAME;

public class RedditBrowserPlugin extends JavaPlugin {
    public static final String SUBMISSION_ID = "submissionId";
    public static final String INTERACTIVE_ENUM = "interactiveEnum";
    public static final String BUTTON_ACTIVATED = "buttonActivated";
    public static final String ROOM_ID = "redditRoomId";

    private Map<UUID, Location> beforeTPLocation = new HashMap<>();
    private Map<UUID, Integer> beforeTPExperience = new HashMap<>();
    private List<UUID> redditBrowsers = new ArrayList<>();
    public Map<String, CommentNode<Comment>> commentCache = new HashMap<>();

    private List<BukkitTask> task = new ArrayList<>();
    public RedditClient redditClient;
    public EventListener listener;
    public final Map<UUID, Room> roomMap = new ConcurrentHashMap<>();

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
        redditBrowsers.clear();
        listener = null;
        roomMap.values().forEach(room -> {
            room.destroy();
            roomMap.remove(room.getRoomId());
        });
    }

    public void attemptConnect() {
        Client client = getClient();
        Credentials oauthCreds = Credentials.script(client.getUsername(), client.getPassword(), client.getClientId(), client.getClientSecret());
        UserAgent userAgent = new UserAgent("bot", "reddit.minecraft.browser", this.getDescription().getVersion(), client.getUsername());
        redditClient = OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), oauthCreds);
        redditClient.setAutoRenew(true);
    }

    private Client getClient() {
        return new Client(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equals("reddit")) {
                if (args != null && args.length > 0 && StringUtils.isNotBlank(args[0])) {
                    final Room playerRoom = roomMap.getOrDefault(player.getUniqueId(), null);
                    if (playerRoom == null) {
                        if (redditClient == null) {
                            attemptConnect();
                        }
                        storePreLocation(player);
                        setupReddit(player);
                        buildRoom(player, args[0]);
                    } else {
                        playerRoom.updateSubreddit(args[0]);
                    }
                } else {
                    player.sendMessage("Please provide a subreddit.");
                }
            } else if ("reddit-join".equalsIgnoreCase(command.getName())) {
                if (args != null && args.length > 0 && StringUtils.isNotBlank(args[0])) {
                    String possiblePlayerName = args[0];
                    Player targetPlayer = this.getServer().getPlayer(possiblePlayerName);
                    if (targetPlayer != null) {
                        if (!Objects.equals(targetPlayer.getUniqueId(), player.getUniqueId())) {
                            if (roomMap.containsKey(targetPlayer.getUniqueId())) {
                                storePreLocation(player);
                                setupReddit(player);
                                final Room room = roomMap.get(targetPlayer.getUniqueId());
                                room.addPlayer(player);
                            } else {
                                sender.sendMessage("Player " + args[0] + " is not owner of a reddit room!");
                            }
                        } else {
                            sender.sendMessage("You cannot join yourself");
                        }
                    } else {
                        sender.sendMessage("Player " + args[0] + " does not exist!");
                    }
                }
            }
        }
        return true;
    }

    private void storePreLocation(Player player) {
        beforeTPLocation.put(player.getUniqueId(), player.getLocation());
        beforeTPExperience.put(player.getUniqueId(), player.getTotalExperience());
    }

    public void setupReddit(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Please wait while I setup Reddit...");
        setKarma(player);

        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                setKarma(player);
            }
        });
    }

    public static String colorCode(String color) {
        return (char) (0xfeff00a7) + color;
    }

    public void setKarma(Player p) {
        p.setTotalExperience(0);
        int karma = 0;
        for (KarmaBySubreddit kbs : redditClient.me().karma()) {
            karma += kbs.getLinkKarma();
            karma += kbs.getCommentKarma();
        }
        p.setLevel(karma);
    }

    private World createWorld() {
        WorldCreator wc = new WorldCreator("reddit");
        wc.generator(new RedditGenerator());
        wc.generateStructures(false);
        World world = Bukkit.createWorld(wc);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setTime(1400);
        return world;
    }

    public void buildRoom(Player player, String subReddit) {
        Client client = getClient();
        World world = createWorld();
        RoomDimensions roomDimensions = createRoomDimensions(client);
        Random r = new Random();
        Location location = new Location(world, r.nextInt(2000000) - 1000000, 255, r.nextInt(2000000) - 1000000);

        Bukkit.getScheduler().runTaskAsynchronously(this, task -> {
            final Room room = new Room(
                    this,
                    location,
                    subReddit,
                    roomDimensions,
                    player);
            roomMap.put(room.getRoomId(), room);
            room.build(Collections.singletonList(player));
        });
    }


    @NotNull
    private RoomDimensions createRoomDimensions(Client client) {
        int screenWidth = client.getScreenWidth();
        if (screenWidth < 1) {
            screenWidth = 1;
        }
        int screenHeight = client.getScreenHeight();
        if (screenHeight < 1) {
            screenHeight = 1;
        }
        int roomDepth = client.getRoomDepth();
        if (roomDepth < 5) {
            roomDepth = 5;
        }
        int roomHeight = screenHeight > 1 ? screenHeight + 3 : screenHeight + 3;
        int roomWidth = screenWidth >= 3 ? screenWidth + 3 : screenWidth + 2;
        if (roomWidth < 5) {
            roomWidth = 5;
        }
        return new RoomDimensions(
                roomWidth,
                roomHeight,
                roomDepth,
                screenWidth,
                screenHeight
        );
    }

    public void kickOut(Player player) {
        player.sendMessage(ChatColor.GREEN + "Goodbye reddit!");
        final UUID uniqueId = player.getUniqueId();
        player.teleport(beforeTPLocation.get(uniqueId));
        removeCommentsFromPlayerInventory(player);
        player.setTotalExperience(beforeTPExperience.get(uniqueId));

        beforeTPLocation.remove(uniqueId);
        beforeTPExperience.remove(uniqueId);

        if (roomMap.containsKey(uniqueId)) {
            final Room room = roomMap.get(uniqueId);
            room.destroy();
            roomMap.remove(uniqueId);
        }
    }

    public static void removeCommentsFromPlayerInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents())
        {
            try {
                if (Material.WRITABLE_BOOK.equals(item.getType()) ||
                        Material.WRITTEN_BOOK.equals(item.getType())) {
                    if(Objects.equals(COMMENT_DISPLAY_NAME, item.getItemMeta().getDisplayName())) {
                        item.setAmount(0);
                    }
                }
            } catch (Exception e) {}
        }
    }
}
