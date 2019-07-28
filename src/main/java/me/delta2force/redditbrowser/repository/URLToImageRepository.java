package me.delta2force.redditbrowser.repository;

import net.dean.jraw.models.Submission;
import org.bukkit.Bukkit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;

import static me.delta2force.redditbrowser.repository.URLOptimizer.optimizeURL;

public class URLToImageRepository {

    public static BufferedImage findImage(Submission submission) {
        final String url = findImageOrThumbnailUrl(submission);
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new URL(url));
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.INFO, "Could not retrieve '" + url + "' as an image. This could be a gif which is not supported or the url isn't a direct link to the image");
        }
        return bufferedImage;
    }

    private static String findImageOrThumbnailUrl(Submission submission) {
        if ("image".equalsIgnoreCase(submission.getPostHint())
                && !submission.getUrl().endsWith(".gif")) {
            return submission.getUrl();
        } else if (submission.getPreview() != null
                && submission.getPreview().getImages() != null
                && !submission.getPreview().getImages().isEmpty()
                && submission.getPreview().getImages().get(0).getSource() != null
                && submission.getPreview().getImages().get(0).getSource().getUrl() != null) {
            return submission.getPreview().getImages().get(0).getSource().getUrl();
        } else if (submission.getThumbnail() != null) {
            return submission.getThumbnail();
        } else {
            return optimizeURL(submission.getUrl());
        }
    }
}