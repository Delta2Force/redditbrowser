package me.delta2force.redditbrowser.renderer;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TiledRenderer {
    private static final int IMAGE_SIZE = 128;
    private static final Pattern PATTERN = Pattern.compile("https://imgur\\.com/(.*)");
    private final RedditRenderer[][] tiles;
    private String url;
    private final RedditBrowserPlugin redditBrowserPlugin;
    private final int tileWidth;
    private final int tileHeight;

    public TiledRenderer(
            String url,
            RedditBrowserPlugin redditBrowserPlugin,
            int tileWidth,
            int tileHeight) {
        this.url = url;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;

        this.redditBrowserPlugin = redditBrowserPlugin;
        tiles = new RedditRenderer[tileHeight][tileWidth];
        for(int row = 0; row < tileHeight; row++) {
            for(int col = 0; col < tileWidth; col++) {
                tiles[row][col] = new RedditRenderer(url);
            }
        }
        scheduleFindImage();
    }

    public void update(String url) {
        this.url = url;
        scheduleFindImage();
    }

    private void scheduleFindImage() {
        Bukkit.getScheduler().runTaskAsynchronously(redditBrowserPlugin, this::findImage);
    }

    private void findImage() {
        try {
            BufferedImage bi = ImageIO.read(new URL(replaceImgUr(url)));
            BufferedImage image = new BufferedImage(IMAGE_SIZE*tileWidth, IMAGE_SIZE*tileHeight, BufferedImage.TYPE_INT_ARGB);
            image.createGraphics().drawImage(bi, 0, 0, IMAGE_SIZE*tileWidth, IMAGE_SIZE*tileHeight, null);
            BufferedImage[][] bufferedImages = splitImage(image, tileHeight, tileWidth);
            for(int row = 0; row < tileHeight; row++) {
                for(int col = 0; col < tileWidth; col++) {
                    tiles[row][col].setImage(bufferedImages[row][col]);
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.INFO, "Could not retrieve '" + url + "' as an image. This could be a gif which is not supported or the url isn't a direct link to the image");
            BufferedImage fallbackImage= new BufferedImage(IMAGE_SIZE,IMAGE_SIZE,BufferedImage.TYPE_BYTE_GRAY);
            for(int row = 0; row < tileHeight; row++) {
                for(int col = 0; col < tileWidth; col++) {
                    tiles[row][col].setImage(fallbackImage);
                }
            }
        }
    }

    private BufferedImage[][] splitImage(BufferedImage image, int rows, int cols) {
        BufferedImage[][] imgs = new BufferedImage[rows][cols]; //Image array to hold image chunks
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                imgs[row][col] = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, image.getType());

                Graphics2D gr = imgs[row][col].createGraphics();
                gr.drawImage(image, 0, 0,
                        IMAGE_SIZE,
                        IMAGE_SIZE,
                        IMAGE_SIZE * col,
                        IMAGE_SIZE * row,
                        IMAGE_SIZE * ( col +1),
                        IMAGE_SIZE * (row + 1),
                        null);
                gr.dispose();
            }
        }
        return imgs;
    }

    public MapRenderer getRenderer(int row, int col) {
        return tiles[row][col];
    }

    private String replaceImgUr(String url) {
        Matcher matcher = PATTERN.matcher(url);
        if(matcher.matches()) {
            String group = matcher.group(1);
            return "https://i.imgur.com/" + group+ ".jpg";
        }
        return url;
    }
}
