package me.delta2force.redditbrowser.reddittext;

import net.dean.jraw.models.Comment;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class CommentRenderer {

    public static List<BufferedImage> createImageFrom(
            Comment comment,
            int width,
            int height,
            int blockHeight
    ) {
        final StringBuilder headerBuilder = new StringBuilder();
        final Date created = comment.getCreated();
        final LocalDateTime createdLocalDateTime = LocalDateTime.ofInstant(created.toInstant(), ZoneId.systemDefault());
        headerBuilder.append("<div><a>")
                .append(comment.getAuthor())
                .append("</a>")
                .append(" - ")
                .append(comment.getScore())
                .append(" points - ")
                .append(createdLocalDateTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .append("</div>");
        return RedditMarkupToImageConverter.render(
                headerBuilder.toString(),
                comment.getBody(),
                width,
                height,
                blockHeight);
    }
}
