package me.delta2force.redditbrowser.room.comments;

import net.dean.jraw.models.Comment;
import net.dean.jraw.tree.CommentNode;

import java.util.ArrayList;
import java.util.List;

public class CommentNavigator {
    private List<CommentNode<Comment>> commentNodes;
    private List<Integer> path;

    public CommentNavigator(List<CommentNode<Comment>> commentNodes) {
        this.commentNodes = commentNodes;
        this.path = new ArrayList<>();
        if(commentNodes != null && !commentNodes.isEmpty()) {
            this.path.add(0);
        }
    }

    public Comment current() {
        return findElement(path);
    }

    public boolean canPrevious() {
        final List<Integer> newPath = new ArrayList<>(path);
        newPath.set(newPath.size() - 1, newPath.get(newPath.size() - 1) - 1);
        final Comment element = findElement(newPath);
        return element != null;
    }

    public boolean canNext() {
        final List<Integer> newPath = new ArrayList<>(path);
        newPath.set(newPath.size() - 1, newPath.get(newPath.size() - 1) + 1);
        final Comment element = findElement(newPath);
        return element != null;
    }

    public boolean canChild() {
        final List<Integer> newPath = new ArrayList<>(path);
        newPath.add(0);
        final Comment element = findElement(newPath);
        return element != null;
    }

    public boolean canParent() {
        final List<Integer> newPath = new ArrayList<>(path);
        newPath.remove(path.size() - 1);
        final Comment element = findElement(newPath);
        return element != null;
    }


    public Comment previous() {
        if(canPrevious()) {
            this.path.set(path.size() - 1, path.get(path.size() - 1) - 1);
            return findElement(path);
        }
        return null;
    }

    public Comment next() {
        if(canNext()) {
            this.path.set(path.size() - 1, path.get(path.size() - 1) + 1);
            return findElement(path);
        }
        return null;
    }

    public Comment parent() {
        if(canParent()) {
            this.path.remove(path.size() - 1);
            return findElement(path);
        }
        return null;
    }

    public Comment child() {
        if(canChild()) {
            this.path.add(0);
            return findElement(path);
        }
        return null;
    }

    private Comment findElement(List<Integer> path) {
        List<CommentNode<Comment>> commentNodes = this.commentNodes;
        CommentNode<Comment> lastNode = null;
        boolean first = true;
        for (int index : path) {
            if (first) {
                if (commentNodes != null && index >= 0 && commentNodes.size() > index) {
                    lastNode = commentNodes.get(index);
                    first = false;
                } else {
                    return null;
                }

            } else if (lastNode != null && index >= 0 && lastNode.getReplies().size() > index) {
                lastNode = lastNode.getReplies().get(index);
            } else {
                return null;
            }
        }
        return lastNode != null ? lastNode.getSubject() : null;

    }
}

