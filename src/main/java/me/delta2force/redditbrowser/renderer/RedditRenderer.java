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
	
	public RedditRenderer(final String url, RedditBrowserPlugin reddit) {
		this.url=url;
		reddit.runnableQueue.add(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedImage bi = ImageIO.read(new URL(url));
					image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
					image.createGraphics().drawImage(bi, 0, 0, 128, 128, null);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public String toString() {
				return url;
			}
		});
	}
	
	@Override
	public void render(MapView mv, MapCanvas mc, Player p) {
		if(image != null) {
			mc.drawImage(0, 0, image);
		}
	}
}
