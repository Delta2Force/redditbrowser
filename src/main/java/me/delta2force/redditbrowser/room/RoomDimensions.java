package me.delta2force.redditbrowser.room;


public class RoomDimensions {
    private int roomWidth;
    private int roomHeight;
    private int roomDepth;
    private int screenWidth;
    private int screenHeight;

    public RoomDimensions(int roomWidth, int roomHeight, int roomDepth, int screenWidth, int screenHeight) {
        this.roomWidth = roomWidth;
        this.roomHeight = roomHeight;
        this.roomDepth = roomDepth;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public int getRoomWidth() {
        return roomWidth;
    }

    public int getRoomHeight() {
        return roomHeight;
    }

    public int getRoomDepth() {
        return roomDepth;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }
}
