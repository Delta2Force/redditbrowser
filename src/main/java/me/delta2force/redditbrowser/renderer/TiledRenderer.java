package me.delta2force.redditbrowser.renderer;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import org.bukkit.map.MapRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TiledRenderer {
    private static final int IMAGE_SIZE = 128;
    private final RedditRenderer[][] tiles;
    private String url;
    private final RedditBrowserPlugin redditBrowserPlugin;
    private final int tileWidth;
    private final int tileHeight;

    public TiledRenderer(
            RedditBrowserPlugin redditBrowserPlugin,
            int tileWidth,
            int tileHeight) {
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;

        this.redditBrowserPlugin = redditBrowserPlugin;
        tiles = new RedditRenderer[tileHeight][tileWidth];
        for (int row = 0; row < tileHeight; row++) {
            for (int col = 0; col < tileWidth; col++) {
                tiles[row][col] = new RedditRenderer();
            }
        }
    }

    public void updateImage(String originalURL, BufferedImage originalImage) {
        url = originalURL;
        if (originalImage != null) {
            BufferedImage image = new BufferedImage(IMAGE_SIZE * tileWidth, IMAGE_SIZE * tileHeight, BufferedImage.TYPE_INT_ARGB);
            image.createGraphics().drawImage(originalImage, 0, 0, IMAGE_SIZE * tileWidth, IMAGE_SIZE * tileHeight, null);
            BufferedImage[][] bufferedImages = splitImage(image, tileHeight, tileWidth);
            for (int row = 0; row < tileHeight; row++) {
                for (int col = 0; col < tileWidth; col++) {
                    tiles[row][col].setImage(originalURL, bufferedImages[row][col]);
                }
            }
        } else {
            BufferedImage fallbackImage = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_BYTE_GRAY);
            for (int row = 0; row < tileHeight; row++) {
                for (int col = 0; col < tileWidth; col++) {
                    tiles[row][col].setImage(originalURL, fallbackImage);
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
                        IMAGE_SIZE * (col + 1),
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
}
