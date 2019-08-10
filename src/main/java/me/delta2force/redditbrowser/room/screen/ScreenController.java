package me.delta2force.redditbrowser.room.screen;

import me.delta2force.redditbrowser.reddittext.CommentRenderer;
import me.delta2force.redditbrowser.reddittext.RedditMarkupToImageConverter;
import me.delta2force.redditbrowser.repository.URLToImageRepository;
import me.delta2force.redditbrowser.room.comments.CommentsController;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import org.bukkit.Bukkit;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

public class ScreenController {
    private Screen screen;
    private ScreenModel screenModel;
    private ScreenControlStation screenControlStation;
    private CommentsController commentsController;

    ScreenController() {
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public void setCommentsController(CommentsController commentsController) {
        this.commentsController = commentsController;
    }

    public void setScreenControlStation(ScreenControlStation screenControlStation) {
        this.screenControlStation = screenControlStation;
    }

    public void showPost(Submission submission) {
        Bukkit.getScheduler().runTaskAsynchronously(screen.getRoom().getRedditBrowserPlugin(), new Runnable() {
            @Override
            public void run() {
                screenModel = findBufferedImagesForPost(submission);
                Bukkit.getScheduler().runTask(screen.getRoom().getRedditBrowserPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        });
    }

    public void showComment(Comment comment) {
        Bukkit.getScheduler().runTaskAsynchronously(screen.getRoom().getRedditBrowserPlugin(), () -> {
            final List<BufferedImage> imageFrom = CommentRenderer.createImageFrom(
                    comment,
                    screen.getScreenWidth() * screen.getBlockPixels(),
                    screen.getScreenHeight() * screen.getBlockPixels(),
                    screen.getBlockPixels());
            screenModel = new ScreenModel(imageFrom, ScreenModelType.COMMENT);
            Bukkit.getScheduler().runTaskAsynchronously(screen.getRoom().getRedditBrowserPlugin(), () -> {
                Bukkit.getScheduler().runTask(screen.getRoom().getRedditBrowserPlugin(), this::update);
            });
        });
    }

    private void update() {
            screen.buildScreen(screenModel != null ? screenModel.getSelectedImage() : null);
            screenControlStation.build();
            commentsController.update();
    }

    private ScreenModel findBufferedImagesForPost(Submission submission) {
        if(!submission.isSelfPost()) {
            final BufferedImage image = URLToImageRepository.findImage(submission);
            return new ScreenModel(Collections.singletonList(image), ScreenModelType.POST);
        } else {
            final int blockPixels = screen.getBlockPixels();
            final List<BufferedImage> bufferedImages = RedditMarkupToImageConverter.render(
                    submission.getSelfText(),
                    screen.getScreenWidth() * blockPixels / 2 ,
                    screen.getScreenHeight() * blockPixels /2,
                    blockPixels);
            return new ScreenModel(bufferedImages, ScreenModelType.POST);
        }
    }


    public boolean canBack() {
        return screenModel != null && screenModel.getSelectedIndex() > 0;
    }

    public boolean canForward() {
        return screenModel != null && screenModel.getImages().size() > screenModel.getSelectedIndex() +1;
    }

    public void back() {
        if(canBack()) {
            screenModel.setSelectedIndex(screenModel.getSelectedIndex() - 1 );
            update();
        }
    }

    public void forward() {
        if(canForward()) {
            screenModel.setSelectedIndex(screenModel.getSelectedIndex() + 1 );
            update();
        }
    }

    public ScreenModelType getScreenModelType() {
        return screenModel.getScreenModelType();
    }

    public void clean() {
        screen.clean();
        screenModel = null;
        screenControlStation.clean();
    }

    public CommentsController getCommentsController() {
        return commentsController;
    }
}
