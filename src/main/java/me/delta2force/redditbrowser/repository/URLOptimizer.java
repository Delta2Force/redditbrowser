package me.delta2force.redditbrowser.repository;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convert the URL to an image
 *
 * Converts youtube videos to the thumbnail
 * Converts indirect imgur links to direct links to the image
 *
 */

class URLOptimizer {
    private static final Pattern IMGUR_PATTERN = Pattern.compile("https://imgur\\.com/(.*)");
    private static final Pattern VIMEO = Pattern.compile("https://vimeo.com/(\\d*).*");
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile("^.*((youtu.be\\/)|(v\\/)|(\\/u\\/\\w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*");

    static String optimizeURL(String url) {
        String optimizedURL = replaceImgUr(url);
        optimizedURL = replaceYoutubeVideoWithThumbNail(optimizedURL);
        optimizedURL = replaceVimeoVideoWithThumbnail(optimizedURL);

        return optimizedURL;
    }

    static String replaceImgUr(String url) {
        Matcher matcher = IMGUR_PATTERN.matcher(url);
        if(matcher.matches()) {
            String group = matcher.group(1);
            return "https://i.imgur.com/" + group+ ".jpg";
        }
        return url;
    }

    static String replaceYoutubeVideoWithThumbNail(String url) {
        Matcher matcher = YOUTUBE_PATTERN.matcher(url);
        if(matcher.matches()) {
            String group = matcher.group(7);
            return "https://img.youtube.com/vi/" + group + "/hqdefault.jpg";
        }
        return url;
    }

    static String replaceVimeoVideoWithThumbnail(String url) {
        Matcher matcher = VIMEO.matcher(url);
        if(matcher.matches()) {
            String videoId = matcher.group(1);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder documentBuilder = null;
            try {
                documentBuilder = documentBuilderFactory.newDocumentBuilder();
                URL xmlURL = new URL("https://vimeo.com/api/v2/video/" + videoId + ".xml");
                InputStream xml = xmlURL.openStream();

                Document document = documentBuilder.parse(xml);

                final NodeList thumbnail_large = document.getElementsByTagName("thumbnail_large");
                if(thumbnail_large.getLength() > 0 ) {
                    return thumbnail_large.item(0).getTextContent();
                }
            } catch (Exception e) {
                //Not intested in failures
            }
        }
        return url;
    }
}
