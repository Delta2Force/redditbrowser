package me.delta2force.redditbrowser.room.screen.renderer;

import java.awt.image.BufferedImage;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class RedditRenderer extends MapRenderer{
	public BufferedImage image;
	private boolean drawn;

	public void setImage( BufferedImage image) {
		this.drawn = false;
		this.image = image;
	}

	@Override
	public void render(MapView mv, MapCanvas mc, Player p) {
		if(image != null) {
			if(!drawn) { //Don't redraw every time because the server calls this too much
				//this will need to track for which player it was drawn I guess
				mc.drawImage(0, 0, image);
				drawn = true;
			}
		}
	}
}
