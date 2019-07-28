package me.delta2force.redditbrowser.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class URLOptimizerTest {

    @Test
    void youtube() {
        final String optimizedURL = URLOptimizer.optimizeURL("https://www.youtube.com/watch?v=kayFrIR-Qfw&feature=youtu.be");
        assertEquals("https://img.youtube.com/vi/kayFrIR-Qfw/hqdefault.jpg", optimizedURL);
    }


    @Test
    void vimeo() {
        final String optimizedURL = URLOptimizer.optimizeURL("https://vimeo.com/343241535");
        assertEquals("https://i.vimeocdn.com/video/794908709_640.jpg", optimizedURL);
    }
}