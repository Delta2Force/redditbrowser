package me.delta2force.redditbrowser.reddittext;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profiles.pegdown.Extensions;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import gui.ava.html.image.generator.HtmlImageGenerator;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class RedditMarkupToImageConverter {
    static final DataHolder OPTIONS = PegdownOptionsAdapter.flexmarkOptions(
            Extensions.ALL
    );

    static final MutableDataSet FORMAT_OPTIONS = new MutableDataSet();
    static {
        // copy extensions from Pegdown compatible to Formatting, but leave the rest default
        FORMAT_OPTIONS.set(Parser.EXTENSIONS, OPTIONS.get(Parser.EXTENSIONS));
    }

    public  static List<BufferedImage> render(String htmlHeader, String text, int width, int height, int blockHeight) {
        Parser parser = Parser.builder(OPTIONS).build();
        HtmlRenderer renderer = HtmlRenderer.builder(FORMAT_OPTIONS).build();

        // You can re-use parser and renderer instances
        Document document = parser.parse(text);
        String html = renderer.render(document);
        //Trying to force the width
        final double estimatedTextWidth = Math.floor(width * .7);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<div style=\"font-size: x-large; width: " )
                .append(estimatedTextWidth)
                .append( "px; word-break: break-all; overflow-wrap: break-word;\">")
                .append(htmlHeader)
                .append(html)
                .append("</div>");

        final HtmlImageGenerator htmlImageGenerator = new HtmlImageGenerator();
        htmlImageGenerator.setSize(new Dimension(width, height * 100));
        htmlImageGenerator.loadHtml(stringBuilder.toString());
        final BufferedImage bufferedImage = htmlImageGenerator.getBufferedImage();
        BufferedImage whiteBackgroundBufferedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics = whiteBackgroundBufferedImage.createGraphics();
        graphics.drawImage(bufferedImage, 0, 0, Color.WHITE, null);

        return splitImage(whiteBackgroundBufferedImage, width, height, blockHeight);
    }

    public  static List<BufferedImage> render(String text, int width, int height, int blockHeight) {
        return render(null, text, width, height, blockHeight);
    }

    private static List<BufferedImage> splitImage(BufferedImage image, int width, int height,int blockHeight) {
        final List<BufferedImage> pages = new ArrayList<>();
        for(int y = 0; y<image.getHeight(); y+=height-(blockHeight/2)) {
            final BufferedImage page = new BufferedImage(width, height, image.getType());
            pages.add(page);
            Graphics2D gr = page.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fillRect (0, 0, width, height);
            gr.drawImage(image,
                    0,
                    0,
                    image.getWidth(),
                    height,
                    0,
                    y,
                    image.getWidth(),
                    y + height,
                    Color.WHITE,
                    null);
            gr.dispose();
        }
        return pages;
    }
}
