package me.delta2force.redditbrowser.room;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.pagination.DefaultPaginator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RedditQueue {
    public static final int PAGING_SIZE = 25;
    private RedditClient redditClient;
    private final String subReddit;
    private final List<String> history;
    private DefaultPaginator<Submission> defaultPaginator;
    private int index = -1;

    public RedditQueue(
            RedditClient redditClient,
            String subReddit) {
        this.redditClient = redditClient;
        this.subReddit = subReddit;
        this.history = new ArrayList<>();
        createRedditPaginator();
    }

    /**
     * Using a the paginator instead of the Stream because the stream does weird things if the subreddit doesn't exist
     */
    private void createRedditPaginator() {
        if("FRONTPAGE".equalsIgnoreCase(subReddit)) {
            defaultPaginator = redditClient
                    .frontPage()
                    .limit(PAGING_SIZE)
                    .build();
        } else {
            defaultPaginator = redditClient
                    .subreddit(subReddit)
                    .posts()
                    .sorting(SubredditSort.HOT)
                    .limit(PAGING_SIZE)
                    .build();
        }

    }


    Submission next() {
        if (index + 1 >= history.size()) {
            if(loadNextPage()) {
                return next();
            } else {
                return null;
            }
        } else {
            final String submissionId = history.get(index + 1);
            final Submission submission = redditClient.submission(submissionId).inspect();
            index++;
            return submission;
        }
    }

    private boolean loadNextPage() {
        final Listing<Submission> next = defaultPaginator.next();
        if (next != null) {
            final List<Submission> children = next.getChildren();
            if (children != null && !children.isEmpty()) {
                history.addAll(children.stream()
                        .map(Submission::getId)
                        .collect(Collectors.toList()));
                return true;
            }
        }
        return false;
    }

    Submission previous() {
        if (hasPrevious()) {
            final String submissionId = history.get(index - 1);
            final Submission submission = redditClient.submission(submissionId).inspect();
            index--;
            return submission;
        }
        return null;
    }

    boolean hasPrevious() {
        return index > 0;
    }

    void reset() {
        index = -1;
        history.clear();
        createRedditPaginator();
    }

}
