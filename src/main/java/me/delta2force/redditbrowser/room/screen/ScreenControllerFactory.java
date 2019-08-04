package me.delta2force.redditbrowser.room.screen;

import me.delta2force.redditbrowser.room.Room;
import me.delta2force.redditbrowser.room.comments.CommentsControlStation;
import me.delta2force.redditbrowser.room.comments.CommentsController;
import me.delta2force.redditbrowser.room.comments.CommentsControllerFactory;
import org.bukkit.Location;

public class ScreenControllerFactory {

    public static ScreenController create(Room room,
                                          Location screenLocation,
                                          Location controlStationLocation,
                                          Location commentsControlStationLocation,
                                          int screenWidth,
                                          int screenHeight) {

        final ScreenController screenController = new ScreenController();
        final Screen screen = new Screen(room, screenLocation, screenWidth, screenHeight, screenController);
        final ScreenControlStation screenControlStation = new ScreenControlStation(screenController, room, controlStationLocation);
        screenController.setScreen(screen);
        screenController.setScreenControlStation(screenControlStation);
        final CommentsController commentsController = CommentsControllerFactory.create(commentsControlStationLocation, screenController, room);
        screenController.setCommentsController(commentsController);
        return screenController;
    }
}
