package me.delta2force.redditbrowser.reddittext;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedditMarkupToImageConverterTest {
    @BeforeAll
    public static void setup() {
        new File("target/images").mkdir();
    }

    public static final String TEXT = "Hello Reddit!!! This is my first TIFU, and it's because I chose to fly EasyJet, even after a bunch of people told me to avoid it! I hope you all enjoy this story so here it begins.\n" +
            "\n" +
            "**UPDATES POSTED BELOW** \n" +
            "\n" +
            "My wife and I decided to take a trip to Amsterdam from the states this summer to visit family. During said trip we booked a seperate flight to London for 3 days. The 22nd-24th.  We arrived in Amsterdam on the 21st and left the next day, all went smooth. We enjoyed the city of London and visited pretty much all of it in the time we had. Then the chaos began.\n" +
            "\n" +
            "On the 24th while at Gatwick Airport our flight to Amsterdam was canceled due to a fueling issue in Amsterdam, this was understandable as our flight home from Amsterdam was on the 28th, so no big deal; we'd catch another flight. My wife had purchased alcohol after security and was told by a manager we could get a free checked bag. After being online at customer service for 3 hours the airline said the next flight was on the 26th and they had booked us for 2 nights at a hotel. When we arrived at the hotel they said we were booked for only 1 night and dinner was no longer being served.\n" +
            "\n" +
            "On the 25th we went back to the airport to request another hotel since they had mistakingly booked just a night, and we had to wait another 5 hours on line. They eventually booked us a night at a different hotel. When we arrived at the hotel they told us nothing was booked, and to go back to EasyJet. We went back and had to wait another 3 hours, totalling 8. They apologized and said the confirmation was never sent. So a whole day of opportunity wasted. We got back to the hotel and missed our food vouchers, again.\n" +
            "\n" +
            "On the 26th we got to the airport and our flights were canceled again, due to weather issues. However employees told us it was due to a crew issue and other airlines were flying to Amsterdam without a problem. We had to stay on line again for 4 hours, only for them to tell us there were no available flights for a few days. We needed to be back in Amsterdam by the 28th. They hired a taxi, and drove us to Luton airport 2 hours away on the 27th. Another whole day wasted. \n" +
            "\n" +
            "On the 27th when we arrived at Luton Airport we asked about our free checked bag we were promised on the 24th, and an employee told us \"I don't care what a random person or employee told you, it's not happening\". I tried to explain how we have had multiple canceled flights and the employee said \"I'm going to walk away now.\"I started to get upset.. my wife made me walk away. We decided it wasn't worth our fight. Another hour at customer service. When we got to our hotel we were told we had no food accommodations and had to argue there for an hour, which then they only allowed us pizza for dinner. Whatever, it was food! Yet again, another day wasted. \n" +
            "\n" +
            "On the 28th our 7 am flight was canceled again due to a crew issue.. It was unbelievable. We had only gotten 4 hours of sleep. In response they put us on the 8 am flight. We ran to security, got to the gate and got loaded into a bus. After 20 minutes waiting to be driven to the plane our flight got canceled.. For the fourth time; and they announced it was a crew issue. We would now miss our flight home. When we got back into the airport the attendant told us to wait in an area to recieve further information. They had then come to us and told us they don't know where our checked bags went and may be lost and could take a few hours to have them returned. I was livid! This whole trip had become a comic book! We got our bags an hour later. We waited on customer service for another 3 hours and they told us we had to go 2 hours back to Gatwick Airport to catch a flight home and they would pay for our flight to the states. We called Norwegian, who we missed, and to reschedule our flights cost us 900 pounds, which we are hoping EasyJet will refund (they said they would, and wrote a notice in our account.. But who knows, my trust is gone). They drove us back to Gatwick and we stayed the night. \n" +
            "\n" +
            "So now today, on the 29th, we were told we had no check in bags on our account. We explained our insane situation and they still were hesitant to check a bag for us at no cost. Eventually after a long period of time they did. Now we await in the airport for our 5th flight to Amsterdam.. And hopefully then home. I won't ever choose this airline again. \n" +
            "\n" +
            "I understand the first cancelation, the second, third and fourth not at all. The people we dealt with were mostly aggressive, stressful, and non-caring. We totalled 19 hours dealing with customer service, we were told different things by different employees, failures in bookings, missed accommodations, etc.\n" +
            "\n" +
            "**UPDATE 1: Loaded onto a bus for the 5th flight and have turned around due to a technical failure on the plane! Currently in a permanent delay!! 5 flights!!!**\n" +
            "\n" +
            "**UPDATE 2: They are trying to divert a new plane for us to use.. May be another canceled**\n" +
            "\n" +
            "**UPDATE 3: FINALLY ARRIVED IN AMSTERDAM! It took EasyJet an additional 2 hours to get us a new plane and allow us to fly to Amsterdam, they weren't allowed any discretion to give us free water or snacks and were demanding it be charged even after everything. At any rate, now I need to wait a few days to fly home.. Hopefully that doesn't get fucked!!**\n" +
            "\n" +
            "TL;DR - Stuck in England from the 24th-29th, 4 canceled flights at 2 different airports with EasyJet. Driven to another airport and then back to the original airport. Three cancelations likely from crew issues. Miss managed accommodations, rude and aggressive employees, rescheduling our flights home, lost baggage... What a trip!";

    @Test
    public void test() throws IOException {
        final int width = 128 * 5;
        final int height = 128 * 5;
        final List<BufferedImage> bufferedImages = new RedditMarkupToImageConverter().render(TEXT, width, height, 128);
        assertNotNull(bufferedImages);
        assertFalse(bufferedImages.isEmpty());
        for(int i = 0; i<bufferedImages.size(); i++) {
            final BufferedImage image = bufferedImages.get(i);

            ImageIO.write(image, "jpeg", new File("target/images/1-RedditMarkupToImageConverterTest" + i + ".jpg"));
            assertEquals(width, image.getWidth(), "Image " + i + " doesn't have the correct width");
            assertEquals(height, image.getHeight(), "Image " + i + " doesn't have the correct height");

        }
    }

    @Test
    public void testComment() throws IOException {
        final int width = 128 * 5;
        final int height = 128 * 5;

        final List<BufferedImage> bufferedImages = new RedditMarkupToImageConverter().render("<div><a>nosir_nomaam</a> - 10 points - 2019-07-29T21:23:18</div>", "Their faces are priceless!", width, height, 128);
        assertNotNull(bufferedImages);
        assertFalse(bufferedImages.isEmpty());
        for(int i = 0; i<bufferedImages.size(); i++) {
            final BufferedImage image = bufferedImages.get(i);

            ImageIO.write(image, "jpeg", new File("target/images/2-RedditMarkupToImageConverterTest" + i + ".jpg"));
            assertEquals(width, image.getWidth(), "Image " + i + " doesn't have the correct width");
            assertEquals(height, image.getHeight(), "Image " + i + " doesn't have the correct height");
        }
    }
}