package me.delta2force.redditbrowser.room.screen;

import java.awt.image.BufferedImage;
import java.util.List;

public class ScreenModel {
    private List<BufferedImage> images;
    private int selectedIndex = 0;

    public ScreenModel(List<BufferedImage> images) {
        this.images = images;
    }

    public List<BufferedImage> getImages() {
        return images;
    }

    public BufferedImage getSelectedImage() {
        if(selectedIndex < images.size()) {
            return images.get(selectedIndex);
        }
        return null;
    }


    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }
}
