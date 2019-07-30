package me.delta2force.redditbrowser.room.screen;

import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.List;

public class CommentsController {
    private CommentNavigator commentNavigator;

    public CommentsController() {
    }

    public void updateComments(List<CommentNode<Comment>> commentNodeList) {
        commentNavigator = new CommentNavigator(commentNodeList);
    }

    public Comment getCurrent() {
        return commentNavigator.current();
    }

    public boolean canNext() {
        return commentNavigator.canNext();
    }

    public boolean canPrevious() {
        return commentNavigator.canPrevious();
    }

    public boolean canChild() {
        return commentNavigator.canChild();
    }

    public boolean canParent() {
        return commentNavigator.canParent();
    }

    public Comment next() {
        return commentNavigator.next();
    }

    public Comment previous() {
        return commentNavigator.previous();
    }

    public Comment child() {
        return commentNavigator.child();
    }

    public Comment parent() {
        return commentNavigator.parent();
    }
}
