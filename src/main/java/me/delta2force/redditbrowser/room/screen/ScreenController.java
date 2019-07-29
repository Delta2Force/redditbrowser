package me.delta2force.redditbrowser.room.screen;

import me.delta2force.redditbrowser.reddittext.RedditMarkupToImageConverter;
import me.delta2force.redditbrowser.repository.URLToImageRepository;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import org.bukkit.Bukkit;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

public class ScreenController {
    private Screen screen;
    private ScreenModel screenModel;
    private ControlStation controlStation;

    ScreenController() {
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public void setControlStation(ControlStation controlStation) {
        this.controlStation = controlStation;
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

    private void update() {
        screen.buildScreen(screenModel.getSelectedImage());
        controlStation.build();
    }

    public void showComment(Comment comment) {

    }

    private ScreenModel findBufferedImagesForPost(Submission submission) {
        if(!submission.isSelfPost()) {
            final BufferedImage image = URLToImageRepository.findImage(submission);
            return new ScreenModel(Collections.singletonList(image));
        } else {
            final int blockPixels = screen.getBlockPixels();
            final List<BufferedImage> bufferedImages = RedditMarkupToImageConverter.render(
                    submission.getSelfText(),
                    screen.getScreenWidth() * blockPixels / 2 ,
                    screen.getScreenHeight() * blockPixels /2 );
            return new ScreenModel(bufferedImages);
        }
    }


    public boolean canBack() {
        return screenModel.getSelectedIndex() > 0;
    }

    public boolean canForward() {
        return screenModel.getImages().size() > screenModel.getSelectedIndex() +1;
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

    public void clean() {
        screen.clean();
        controlStation.clean();
    }
}
