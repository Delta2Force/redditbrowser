package me.delta2force.redditbrowser.renderer;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import org.bukkit.map.MapRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class TiledRenderer {
    public static final int IMAGE_SIZE = 128;
    private final RedditRenderer[][] tiles;
    private final String url;
    @org.jetbrains.annotations.NotNull
    private final RedditBrowserPlugin reddit;

    public TiledRenderer(
            String url,
            RedditBrowserPlugin reddit,
            int tileWidth,
            int tileHeight) {
        this.url = url;
        this.reddit = reddit;
        tiles = new RedditRenderer[tileWidth][tileHeight];
        for(int y = 0; y < tileHeight; y++) {
            for(int x = 0; x < tileWidth; x++) {
                tiles[x][y] = new RedditRenderer();
            }
        }
        reddit.runnableQueue.add(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedImage bi = ImageIO.read(new URL(url));
                    BufferedImage image = new BufferedImage(IMAGE_SIZE*tileWidth, IMAGE_SIZE*tileHeight, BufferedImage.TYPE_INT_ARGB);
                    image.createGraphics().drawImage(bi, 0, 0, IMAGE_SIZE*tileWidth, IMAGE_SIZE*tileHeight, null);
                    BufferedImage[][] bufferedImages = splitImage(image, tileHeight, tileWidth);
                    for(int y = 0; y < tileHeight; y++) {
                        for(int x = 0; x < tileWidth; x++) {
                            tiles[y][x].setImage(bufferedImages[y][x]);
                        }
                    }
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


    private BufferedImage[][] splitImage(BufferedImage image, int rows, int cols) {
        BufferedImage[][] imgs = new BufferedImage[rows][cols]; //Image array to hold image chunks
        for (int y = 0; y < cols; y++) {
            for (int x = 0; x < rows; x++) {
                imgs[y][x] = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, image.getType());

                Graphics2D gr = imgs[y][x].createGraphics();
                gr.drawImage(image, 0, 0, IMAGE_SIZE, IMAGE_SIZE, IMAGE_SIZE * x, IMAGE_SIZE * y, IMAGE_SIZE * ( x +1), IMAGE_SIZE * (y + 1), null);
                gr.dispose();
            }
        }
        return imgs;
    }

    public MapRenderer getRenderer(int y, int x) {
        return tiles[y][x];
    }
}
