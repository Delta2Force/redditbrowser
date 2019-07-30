package me.delta2force.redditbrowser.room.screen;

import java.awt.image.BufferedImage;
import java.util.List;

public class ScreenModel {
    private List<BufferedImage> images;
    private final ScreenModelType screenModelType;
    private int selectedIndex = 0;

    public ScreenModel(List<BufferedImage> images, ScreenModelType screenModelType) {
        this.images = images;
        this.screenModelType = screenModelType;
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

    public ScreenModelType getScreenModelType() {
        return screenModelType;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

}
