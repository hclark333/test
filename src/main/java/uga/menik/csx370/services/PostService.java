package uga.menik.csx370.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uga.menik.csx370.models.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {

    private final DataSource dataSource;
    private final UserService userService;

    @Autowired
    public PostService(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }

    public boolean createPost(String postText, String loggedInUserId) throws SQLException {
        String insertPostSql = "INSERT INTO post (userId, content) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement insertPostStmt = null;
        PreparedStatement selectHashtagStmt = null;
        PreparedStatement insertHashtagStmt = null;
        PreparedStatement insertPostHashtagStmt = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            insertPostStmt = conn.prepareStatement(insertPostSql, Statement.RETURN_GENERATED_KEYS);
            insertPostStmt.setString(1, loggedInUserId);
            insertPostStmt.setString(2, postText);
            insertPostStmt.executeUpdate();

            rs = insertPostStmt.getGeneratedKeys();
            if (!rs.next()) throw new SQLException("Failed to get postId");
            String postId = rs.getString(1);

            selectHashtagStmt = conn.prepareStatement("SELECT hashtagId FROM hashtag WHERE tagName = ?");
            insertHashtagStmt = conn.prepareStatement("INSERT INTO hashtag (tagName) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            insertPostHashtagStmt = conn.prepareStatement("INSERT INTO post_hashtag (postId, hashtagId) VALUES (?, ?)");

            for (String word : postText.split(" ")) {
                if (!word.startsWith("#")) continue;
                String hashtag = word.trim();

                int hashtagId = -1;

                selectHashtagStmt.setString(1, hashtag);
                rs = selectHashtagStmt.executeQuery();
                if (rs.next()) {
                    hashtagId = rs.getInt("hashtagId");
                } else {
                    try {
                        insertHashtagStmt.setString(1, hashtag);
                        insertHashtagStmt.executeUpdate();
                        rs = insertHashtagStmt.getGeneratedKeys();
                        if (rs.next()) hashtagId = rs.getInt(1);
                    } catch (SQLException e) {
                        if (e.getErrorCode() == 1062) {
                            rs = selectHashtagStmt.executeQuery();
                            if (rs.next()) hashtagId = rs.getInt("hashtagId");
                        } else {
                            throw e;
                        }
                    }
                }

                if (hashtagId != -1) {
                    insertPostHashtagStmt.setString(1, postId);
                    insertPostHashtagStmt.setInt(2, hashtagId);
                    insertPostHashtagStmt.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (insertPostStmt != null) insertPostStmt.close();
            if (selectHashtagStmt != null) selectHashtagStmt.close();
            if (insertHashtagStmt != null) insertHashtagStmt.close();
            if (insertPostHashtagStmt != null) insertPostHashtagStmt.close();
            if (conn != null) conn.close();
        }
    }

    public boolean insertHashtag(String postId, String hashtag) throws SQLException {
        Connection conn = null;
        PreparedStatement selectHashtagStmt = null;
        PreparedStatement insertHashtagStmt = null;
        PreparedStatement insertPostHashtagStmt = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            int hashtagId = -1;

            selectHashtagStmt = conn.prepareStatement("SELECT hashtagId FROM hashtag WHERE tagName = ?");
            selectHashtagStmt.setString(1, hashtag);
            rs = selectHashtagStmt.executeQuery();
            if (rs.next()) {
                hashtagId = rs.getInt("hashtagId");
            } else {
                insertHashtagStmt = conn.prepareStatement(
                        "INSERT INTO hashtag (tagName) VALUES (?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                insertHashtagStmt.setString(1, hashtag);
                try {
                    insertHashtagStmt.executeUpdate();
                    rs = insertHashtagStmt.getGeneratedKeys();
                    if (rs.next()) hashtagId = rs.getInt(1);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1062) {
                        rs = selectHashtagStmt.executeQuery();
                        if (rs.next()) hashtagId = rs.getInt("hashtagId");
                        else throw e;
                    } else throw e;
                }
            }

            insertPostHashtagStmt = conn.prepareStatement(
                    "INSERT INTO post_hashtag (postId, hashtagId) VALUES (?, ?)"
            );
            insertPostHashtagStmt.setString(1, postId);
            insertPostHashtagStmt.setInt(2, hashtagId);
            insertPostHashtagStmt.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            e.printStackTrace();
            return false;
        } finally {
            if (rs != null) rs.close();
            if (selectHashtagStmt != null) selectHashtagStmt.close();
            if (insertHashtagStmt != null) insertHashtagStmt.close();
            if (insertPostHashtagStmt != null) insertPostHashtagStmt.close();
            if (conn != null) conn.close();
        }
    }

    public List<Post> searchPostsByHashtags(String hashtagsString, String userId) throws SQLException {
        String[] hashtagArray = hashtagsString.trim().split("\\s+");
        List<String> hashtags = new ArrayList<>();

        for (String tag : hashtagArray) {
            tag = tag.trim();
            if (!tag.isEmpty()) {
                if (!tag.startsWith("#")) tag = "#" + tag;
                hashtags.add(tag);
            }
        }

        if (hashtags.isEmpty()) return new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT p.* FROM post p ");
        sql.append("INNER JOIN post_hashtag ph ON p.postId = ph.postId ");
        sql.append("INNER JOIN hashtag h ON ph.hashtagId = h.hashtagId ");
        sql.append("WHERE h.tagName IN (");
        for (int i = 0; i < hashtags.size(); i++) {
            sql.append("?");
            if (i < hashtags.size() - 1) sql.append(", ");
        }
        sql.append(") ORDER BY p.postDate DESC");

        List<Post> posts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < hashtags.size(); i++) {
                pstmt.setString(i + 1, hashtags.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String postId = rs.getString("postId");
                    String foundUserId = rs.getString("userId");
                    String content = rs.getString("content");
                    User user = userService.getUser(foundUserId);
                    String postDateTime = rs.getString("postDate");
                    String heartsCount = rs.getString("heartsCount");
                    String commentsCount = rs.getString("commentsCount");
                    boolean isHearted = isHearted(postId, userId);
                    boolean isBookmarked = isBookmarked(postId, userId);

                    Post post = new Post(postId, content, postDateTime, user,
                            Integer.parseInt(heartsCount), Integer.parseInt(commentsCount),
                            isHearted, isBookmarked);
                    posts.add(post);
                }
            }
        }
        return posts;
    }

    // ----- Heart, Bookmark, Comment, and Post fetching methods -----

    public boolean heartPost(String postId, String loggedInUserId, boolean isAdd) throws SQLException {
        String sql = isAdd ? "INSERT INTO heart (postId, userId) VALUES (?, ?)"
                : "DELETE FROM heart WHERE postId = ? AND userId = ?";
        int heartRowsAffected = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, loggedInUserId);
            heartRowsAffected = pstmt.executeUpdate();
        }

        int heartsCount = getHeartsCount(postId);
        int newHeartsCount = isAdd ? heartsCount + 1 : Math.max(0, heartsCount - 1);

        final String updateSql = "UPDATE post SET heartsCount = ? WHERE postId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setInt(1, newHeartsCount);
            pstmt.setString(2, postId);
            pstmt.executeUpdate();
        }

        return heartRowsAffected > 0;
    }

    public boolean bookmarkPost(String postId, String loggedInUserId, boolean isAdd) throws SQLException {
        String sql = isAdd ? "INSERT INTO bookmark (postId, userId) VALUES (?, ?)"
                : "DELETE FROM bookmark WHERE postId = ? AND userId = ?";
        int bookmarkRowsAffected = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, loggedInUserId);
            bookmarkRowsAffected = pstmt.executeUpdate();
        }
        return bookmarkRowsAffected > 0;
    }

    public boolean isHearted(String postId, String userId) throws SQLException {
        final String sql = "SELECT * FROM heart WHERE postId = ? AND userId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean isBookmarked(String postId, String userId) throws SQLException {
        final String sql = "SELECT * FROM bookmark WHERE postId = ? AND userId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int getHeartsCount(String postId) throws SQLException {
        final String sql = "SELECT heartsCount FROM post WHERE postId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("heartsCount") : 0;
            }
        }
    }

    public int getCommentsCount(String postId) throws SQLException {
        final String sql = "SELECT commentsCount FROM post WHERE postId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("commentsCount") : 0;
            }
        }
    }

    public List<Comment> getCommentsForPost(String postId) throws SQLException {
        final String sql = "SELECT * FROM comment WHERE postId = ?";
        List<Comment> comments = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String commentId = rs.getString("commentId");
                    String userId = rs.getString("userId");
                    String content = rs.getString("content");
                    User user = userService.getUser(userId);
                    String commentDateTime = rs.getString("commentDate");

                    comments.add(new Comment(commentId, content, commentDateTime, user));
                }
            }
        }
        return comments;
    }

    public boolean createComment(String postId, String commentText, String loggedInUserId) throws SQLException {
        final String sql = "INSERT INTO comment (postId, userId, content) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.setString(2, loggedInUserId);
            pstmt.setString(3, commentText);
            pstmt.executeUpdate();
        }

        int newCommentsCount = getCommentsCount(postId) + 1;
        final String updateSql = "UPDATE post SET commentsCount = ? WHERE postId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setInt(1, newCommentsCount);
            pstmt.setString(2, postId);
            pstmt.executeUpdate();
        }

        return true;
    }

    public List<Post> getAllPosts(String userId) throws SQLException {
        final String sql = "SELECT * FROM post";
        List<Post> posts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String postId = rs.getString("postId");
                String foundUserId = rs.getString("userId");
                String content = rs.getString("content");
                User user = userService.getUser(foundUserId);
                String postDateTime = rs.getString("postDate");
                int heartsCount = rs.getInt("heartsCount");
                int commentsCount = rs.getInt("commentsCount");
                boolean isHearted = isHearted(postId, userId);
                boolean isBookmarked = isBookmarked(postId, userId);

                posts.add(new ExpandedPost(postId, content, postDateTime, user, heartsCount, commentsCount, isHearted, isBookmarked, getCommentsForPost(postId)));
            }
        }
        return posts;
    }

    public List<Post> getAllPostsByUser(String loggedInUser, String userID) throws SQLException {
        System.out.println("Fetching all posts where userId = " + userID);
        final String sql = "select * from post where userId = ?";
        List<Post> posts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userID);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String postId = rs.getString("postId");
                    String foundUserId = rs.getString("userId");
                    String content = rs.getString("content");
                    User user = userService.getUser(foundUserId);
                    String postDateTime = rs.getString("postDate");
                    String heartsCount = rs.getString("heartsCount");
                    String commentsCount = rs.getString("commentsCount");
                    boolean isHearted = isHearted(postId, loggedInUser);
                    boolean isBookmarked = isBookmarked(postId, loggedInUser);
                    List<Comment> comments = getCommentsForPost(postId);
                    Post post = new ExpandedPost(postId, content, postDateTime, user,
                            Integer.parseInt(heartsCount), Integer.parseInt(commentsCount),
                            isHearted, isBookmarked, comments);
                    posts.add(post);
                }
                //System.out.println("Fetched Posts: " + posts);
                return posts;
            }
        }
    }

    public ExpandedPost getPostById(String postId, String userId) throws SQLException {
        System.out.println("Fetching post with id: " + postId);
        final String sql = "select * from post where postId = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String foundUserId = rs.getString("userId");
                    String content = rs.getString("content");
                    User user = userService.getUser(foundUserId);
                    String postDateTime = rs.getString("postDate");
                    String heartsCount = rs.getString("heartsCount");
                    String commentsCount = rs.getString("commentsCount");
                    boolean isHearted = isHearted(postId, userId);
                    boolean isBookmarked = isBookmarked(postId, userId);
                    List<Comment> comments = getCommentsForPost(postId);
                    ExpandedPost post = new ExpandedPost(postId, content, postDateTime, user,
                            Integer.parseInt(heartsCount), Integer.parseInt(commentsCount),
                            isHearted, isBookmarked, comments);
                    return post;
                } else {
                    System.out.println("No post found with id: " + postId);
                    return null;
                }
            }
        }
    }

    public String getPostIdByUsernameAndContent(String userId, String content) throws SQLException {
        // could run into a problem when we have duplicates.
        final String sql = "select postId from post where userId = ? and content = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, content);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("postId");
                } else {
                    return ""; // or throw an exception if preferred
                }
            }
        }
    }
}
