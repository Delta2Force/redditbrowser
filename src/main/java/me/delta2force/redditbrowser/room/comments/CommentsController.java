package me.delta2force.redditbrowser.room.comments;

import me.delta2force.redditbrowser.room.screen.ScreenController;
import me.delta2force.redditbrowser.room.screen.ScreenModelType;
import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.List;

public class CommentsController {
    private CommentNavigator commentNavigator;
    private CommentsControlStation commentsControlStation;
    private ScreenController screenController;

    public CommentsController() {
    }

    public void setScreenController(ScreenController screenController) {
        this.screenController = screenController;
    }

    public void setCommentsControlStation(CommentsControlStation commentsControlStation) {
        this.commentsControlStation = commentsControlStation;
    }

    public void update() {
        commentsControlStation.build();
        if(ScreenModelType.COMMENT.equals(screenController.getScreenModelType())) {
            screenController.showComment(commentNavigator.current());
        }
    }

    public CommentsControlStation getCommentsControlStation() {
        return commentsControlStation;
    }

    public void updateComments(List<CommentNode<Comment>> commentNodeList) {
        commentNavigator = new CommentNavigator(commentNodeList);
        screenController.showComment(commentNavigator.current());
    }

    public Comment getCurrent() {
        final Comment current = commentNavigator.current();
        commentsControlStation.build();
        return current;
    }

    public boolean canNext() {
        return commentNavigator != null && commentNavigator.canNext();
    }

    public boolean canPrevious() {
        return commentNavigator != null && commentNavigator.canPrevious();
    }

    public boolean canChild() {
        return commentNavigator != null && commentNavigator.canChild();
    }

    public boolean canParent() {
        return commentNavigator != null && commentNavigator.canParent();
    }

    public Comment next() {
        final Comment comment = commentNavigator.next();
        update();
        return comment;
    }

    public Comment previous() {
        final Comment previous = commentNavigator.previous();
        update();

        return previous;
    }

    public Comment child() {
        final Comment child = commentNavigator.child();
        update();
        return child;
    }

    public Comment parent() {
        final Comment parent = commentNavigator.parent();
        update();
        return parent;
    }
}
