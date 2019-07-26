package me.delta2force.redditbrowser.renderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import me.delta2force.redditbrowser.RedditBrowserPlugin;

public class RedditRenderer extends MapRenderer{
	public BufferedImage image;
	public String url;

	public RedditRenderer(String url) {
		this.url = url;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	@Override
	public void render(MapView mv, MapCanvas mc, Player p) {
		if(image != null) {
			mc.drawImage(0, 0, image);
		}
	}
}
