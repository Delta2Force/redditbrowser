package me.delta2force.redditbrowser;

import me.delta2force.redditbrowser.generator.RedditGenerator;
import me.delta2force.redditbrowser.interaction.InteractiveEnum;
import me.delta2force.redditbrowser.interaction.InteractiveLocation;
import me.delta2force.redditbrowser.inventory.RedditInventory;
import me.delta2force.redditbrowser.listeners.EventListener;
import me.delta2force.redditbrowser.renderer.TiledRenderer;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.KarmaBySubreddit;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.pagination.Stream;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Ladder;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RedditBrowserPlugin extends JavaPlugin {

    public static final int ROOM_DEPTH = 6;
    public static final int ROOM_HEIGHT = 6;
    public static final int ROOM_WIDTH = 4;
    private Map<UUID, Location> beforeTPLocation = new HashMap<>();
    private Map<UUID, RedditInventory> beforeTPInventory = new HashMap<>();
    private Map<UUID, Integer> beforeTPExperience = new HashMap<>();
    private List<UUID> redditBrowsers = new ArrayList<>();
    public Map<InteractiveLocation, InteractiveEnum> interactiveSubmissionID = new HashMap<>();
    public ArrayList<Runnable> runnableQueue = new ArrayList<>();
    public Map<String, CommentNode<Comment>> commentCache = new HashMap<>();
    
    private List<BukkitTask> task = new ArrayList<>();
    public RedditClient reddit;
    public EventListener listener;

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
    	
    	if(!this.getDescription().getVersion().contains(gar.tag_name)) {
    		this.getServer().getLogger().info("Your version of RedditBrowser is outdated! The newest version is: " + gar.tag_name + ". You can download it from: " + gar.html_url);
    	}
    }

    @Override
    public void onDisable() {
    	Iterator<UUID> redditBrowserIterator = redditBrowsers.iterator();
        while(redditBrowserIterator.hasNext()) {
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
        Client client = new Client(this);
        Credentials oauthCreds = Credentials.script(client.getUsername(), client.getPassword(), client.getClientId(), client.getClientSecret());
        UserAgent userAgent = new UserAgent("bot", "reddit.minecraft.browser", "1.0.0", client.getUsername());
        reddit = OAuthHelper.automatic(new OkHttpNetworkAdapter(userAgent), oauthCreds);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (command.getName().equals("reddit")) {
                if (reddit == null) {
                    attemptConnect();
                }
                if (redditBrowsers.contains(p.getUniqueId())) {
                    kickOut(p);
                } else {
                    beforeTPLocation.put(p.getUniqueId(), p.getLocation());
                    beforeTPInventory.put(p.getUniqueId(), new RedditInventory(p.getInventory()));
                    redditBrowsers.add(p.getUniqueId());
                    beforeTPExperience.put(p.getUniqueId(), p.getTotalExperience());
                    p.getInventory().clear();
                    setupReddit(p);
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
        try {
            p.getInventory().addItem(new ItemStack(Material.OAK_SIGN, 16));
        } catch (NoSuchFieldError e) {
            //Older version like 1.13.x
            p.getInventory().addItem(new ItemStack(Material.LEGACY_SIGN, 16));
        }
        p.sendMessage(ChatColor.GREEN + "There you go!");
    }

    public void spawnHologram(Location l, String name) {
        ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        as.setCustomName(name);
        as.setCustomNameVisible(true);
        as.setGravity(false);
        as.setVisible(false);
        as.setInvulnerable(true);
        as.setCollidable(false);
    }
    
    public String colorCode(String color) {
    	return (char) (0xfeff00a7) + color;
    }
    
    public void setRoom(Location l, String submissionId) {
        Submission s = reddit.submission(submissionId).inspect();
        RootCommentNode rcn = reddit.submission(submissionId).comments();
        cube(Material.POLISHED_ANDESITE, l, l.clone().add(-ROOM_WIDTH, -ROOM_HEIGHT, -ROOM_DEPTH));
        cube(Material.AIR, l.clone().add(-1, -1, -1), l.clone().add(-ROOM_WIDTH + 1, -ROOM_HEIGHT + 1, -ROOM_DEPTH + 1));

        for (int y = l.getBlockY(); y > l.getBlockY() - ROOM_HEIGHT; y--) {
            Block block = new Location(l.getWorld(), l.getBlockX() - 2, y, l.getBlockZ() - 1).getBlock();
            block.setType(Material.LADDER);
            Ladder ladder = (Ladder) block.getBlockData();
            ladder.setFacing(BlockFace.NORTH);
            block.setBlockData(ladder);
            block.getState().update();
        }

        Block b = l.clone().add(-2, -ROOM_HEIGHT + 1, -ROOM_DEPTH + 1).getBlock();
        b.setType(Material.CHEST);

        interactiveSubmissionID.put(new InteractiveLocation(b.getLocation(), s.getId()), InteractiveEnum.COMMENT_CHEST);

        Chest chest = (Chest) b.getState();
        Location bl = b.getLocation();
        String title = s.getTitle();
        int titleBaseYPlacement = ROOM_HEIGHT - 4;

        if (title.length() > 15) {
            spawnHologram(bl.clone().add(.5, titleBaseYPlacement + .5, .5), title.substring(0, 15));
            if (title.length() > 30) {
                spawnHologram(bl.clone().add(.5, titleBaseYPlacement +.25, .5), title.substring(15, 30));
                if (title.length() > 45) {
                    spawnHologram(bl.clone().add(.5, titleBaseYPlacement, .5), title.substring(30, 45));
                } else {
                    spawnHologram(bl.clone().add(.5, titleBaseYPlacement, .5), title.substring(30));
                }
            } else {
                spawnHologram(bl.clone().add(.5, titleBaseYPlacement +.25, .5), title.substring(15));
            }
        } else {
            spawnHologram(bl.clone().add(.5, titleBaseYPlacement +.5, .5), title);
        }

        spawnHologram(bl.clone().add(.5, titleBaseYPlacement +.25, .5), colorCode("6") + s.getScore());


        Block uv = l.getWorld().getBlockAt(l.clone().add(-ROOM_WIDTH+1, -ROOM_HEIGHT + 1, -ROOM_DEPTH + 1));
        uv.setType(Material.OAK_BUTTON);
        Directional uvdir = (Directional) uv.getBlockData();
        uvdir.setFacing(BlockFace.SOUTH);
        uv.setBlockData(uvdir);
        interactiveSubmissionID.put(new InteractiveLocation(uv.getLocation(), s.getId()), InteractiveEnum.UPVOTE);

        Block dv = l.getWorld().getBlockAt(l.clone().add(-1, -ROOM_HEIGHT + 1, -ROOM_DEPTH + 1));
        dv.setType(Material.OAK_BUTTON);
        Directional dvdir = (Directional) dv.getBlockData();
        dvdir.setFacing(BlockFace.SOUTH);
        dv.setBlockData(dvdir);
        interactiveSubmissionID.put(new InteractiveLocation(dv.getLocation(), s.getId()), InteractiveEnum.DOWNVOTE);
        
        spawnHologram(uv.getLocation().clone().add(.5, -2, .5), colorCode("a")+"+1");
        spawnHologram(dv.getLocation().clone().add(.5, -2, .5), colorCode("c")+"-1");
        
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
            ItemFrame itf = (ItemFrame) l.getWorld().spawnEntity(l.clone().add(-2, -2, -ROOM_DEPTH +1), EntityType.ITEM_FRAME);
            itf.setFacingDirection(BlockFace.SOUTH);

            book.setItemMeta(bookmeta);
            itf.setItem(book);
        } else {
            createTiledMapView(l, s.getUrl());
        }

        l.clone().add(-ROOM_WIDTH/2, -2, -ROOM_DEPTH + 2).getBlock().setType(Material.AIR);
        
        chest.setCustomName(UUID.randomUUID().toString());
        
        int in = 0;
        for (CommentNode<Comment> cn : rcn.getReplies()) {
            Comment c = cn.getSubject();
            if (in < 26) {
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                BookMeta bookmeta = (BookMeta) book.getItemMeta();
                bookmeta.setTitle("Comment");
                bookmeta.setAuthor("u/"+c.getAuthor());
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

    private void createTiledMapView (Location l, String url) {
        int tileWidth = 3;
        int tileHeight = 3;
        TiledRenderer tiledRenderer = new TiledRenderer(url, this, tileWidth, tileHeight);
        int titleXStart = tileWidth * -1;
        int titleYStart = -2;
        for(int y = 0; y < tileHeight; y++) {
            for(int x = 0; x < tileWidth ; x++) {
                ItemFrame itf = (ItemFrame) l.getWorld().spawnEntity(l.clone().add(titleXStart +x , titleYStart -y, -ROOM_DEPTH +1), EntityType.ITEM_FRAME);
                itf.setFacingDirection(BlockFace.SOUTH);
                ItemStack map = new ItemStack(Material.FILLED_MAP);
                MapMeta mapMeta = (MapMeta) map.getItemMeta();
                MapView mv = Bukkit.createMap(l.getWorld());
                mv.addRenderer(tiledRenderer.getRenderer(y, x));
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
    	for(KarmaBySubreddit kbs : reddit.me().karma()) {
    		karma += kbs.getLinkKarma();
    		karma += kbs.getCommentKarma();
    	}
    	p.setLevel(karma);
    }
    
    public void createTowerAndTP(Player p, String sub, World w) {
        Random r = new Random();
        Location l = new Location(w, r.nextInt(2000000) - 1000000, 255, r.nextInt(2000000) - 1000000);
        Stream<Submission> ll = reddit.subreddit(sub).posts().sorting(SubredditSort.HOT).build().stream();
        int i = 0;
        while (i < 27) {
            Submission s = ll.next();
            final int index = i;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                setRoom(l.clone().add(0, -ROOM_HEIGHT * index, 0), s.getId());
                p.sendMessage(""+ChatColor.DARK_GREEN + (index+1) + " / 27 posts built");
                if(index == 24) {
                	p.teleport(l.clone().add(0, 4, 0));
                    p.setGameMode(GameMode.SURVIVAL);
                    p.getInventory().clear();
                	p.getInventory().addItem(new ItemStack(Material.WRITABLE_BOOK));
                	setKarma(p);
                }
            }, 0);
            i++;
            p.sendMessage(""+ChatColor.GREEN + i + " / 27 posts loaded");
            if (i > 25) {
            	for(Runnable t : runnableQueue) {
                	Bukkit.getScheduler().runTaskAsynchronously(this, t);
            	}
            	runnableQueue.clear();
                if(!task.isEmpty()) {
                    BukkitTask bt = task.get(0);
                    task.remove(0);
                    bt.cancel();
                }
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
