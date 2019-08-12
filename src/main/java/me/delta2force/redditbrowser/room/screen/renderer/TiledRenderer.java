package me.delta2force.redditbrowser.room.screen.renderer;

import me.delta2force.redditbrowser.RedditBrowserPlugin;
import org.bukkit.map.MapRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;

import static me.delta2force.redditbrowser.room.screen.Screen.BLOCK_PIXELS;

public class TiledRenderer {
    private final RedditRenderer[][] tiles;
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

    public void updateImage( BufferedImage originalImage) {
        if (originalImage != null) {
            BufferedImage image = new BufferedImage(BLOCK_PIXELS * tileWidth, BLOCK_PIXELS * tileHeight, BufferedImage.TYPE_INT_ARGB);
            image.createGraphics().drawImage(originalImage, 0, 0, BLOCK_PIXELS * tileWidth, BLOCK_PIXELS * tileHeight, null);
            BufferedImage[][] bufferedImages = splitImage(image, tileHeight, tileWidth);
            for (int row = 0; row < tileHeight; row++) {
                for (int col = 0; col < tileWidth; col++) {
                    tiles[row][col].setImage(bufferedImages[row][col]);
                }
            }
        } else {
            BufferedImage fallbackImage = new BufferedImage(BLOCK_PIXELS, BLOCK_PIXELS, BufferedImage.TYPE_BYTE_GRAY);
            for (int row = 0; row < tileHeight; row++) {
                for (int col = 0; col < tileWidth; col++) {
                    tiles[row][col].setImage(fallbackImage);
                }
            }
        }
    }

    private BufferedImage[][] splitImage(BufferedImage image, int rows, int cols) {
        BufferedImage[][] imgs = new BufferedImage[rows][cols]; //Image array to hold image chunks
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                imgs[row][col] = new BufferedImage(BLOCK_PIXELS, BLOCK_PIXELS, image.getType());

                Graphics2D gr = imgs[row][col].createGraphics();
                gr.drawImage(image, 0, 0,
                        BLOCK_PIXELS,
                        BLOCK_PIXELS,
                        BLOCK_PIXELS * col,
                        BLOCK_PIXELS * row,
                        BLOCK_PIXELS * (col + 1),
                        BLOCK_PIXELS * (row + 1),
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
