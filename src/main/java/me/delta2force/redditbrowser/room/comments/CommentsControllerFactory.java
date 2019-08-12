package me.delta2force.redditbrowser.room.comments;

import me.delta2force.redditbrowser.room.Room;
import me.delta2force.redditbrowser.room.screen.ScreenController;
import org.bukkit.Location;

public class CommentsControllerFactory {

    public static CommentsController create(
            Location location,
            ScreenController screenController,
            Room room
    ) {
        final CommentsController commentsController = new CommentsController();
        final CommentsControlStation commentsControlStation = new CommentsControlStation(
                location,
                commentsController,
                screenController,
                room
        );
        commentsController.setCommentsControlStation(commentsControlStation);
        commentsController.setScreenController(screenController);
        return commentsController;
    }
}
