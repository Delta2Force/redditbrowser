package me.delta2force.redditbrowser.listeners;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {

    private RedditBrowserPlugin reddit;

    public EventListener(RedditBrowserPlugin reddit) {
        this.reddit = reddit;
    }


    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();

        // Check if the list contains the players UUID
        if (!reddit.getRedditBrowsers().contains(player.getUniqueId())) {
            return;
        }

        // Set the line to a variable
        String line = event.getLine(0);

        // Make sure the line exist
        if (line != null) {
            // Check if the sign's first line is what we want
            if (line.startsWith("r/")) {
                return;
            }

            // Get the sub they want
            String sub = line.replace("r/", "");

            // Set the player to spectator mode
            player.setGameMode(GameMode.SPECTATOR);
            // Send them a message
            player.sendMessage(ChatColor.YELLOW + "Please wait...");
            // Teleport them
            player.teleport(player.getLocation().add(0, 400, 0));
            // Add new task
            reddit.getTask().add(reddit.getServer().getScheduler().runTaskAsynchronously(reddit, () -> reddit.createTowerAndTP(player, sub, player.getWorld())));
        }

    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        reddit.kickOut(player);
    }

}
