package me.delta2force.redditbrowser.room.screen;

import me.delta2force.redditbrowser.room.Room;
import org.bukkit.Location;

public class ScreenControllerFactory {

    public static ScreenController create(Room room,
                                          Location screenLocation,
                                          Location controlStationLocation,
                                          int screenWidth,
                                          int screenHeight) {

        final ScreenController screenController = new ScreenController();
        final Screen screen = new Screen(room, screenLocation, screenWidth, screenHeight, screenController);
        final ControlStation controlStation = new ControlStation(screenController, room, controlStationLocation);
        screenController.setScreen(screen);
        screenController.setControlStation(controlStation);
        return screenController;
    }
}
