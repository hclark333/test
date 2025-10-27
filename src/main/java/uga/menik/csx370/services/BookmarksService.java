package uga.menik.csx370.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uga.menik.csx370.models.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookmarksService {

    private final DataSource dataSource;
    private final UserService userService;
    private final PostService postService;

    @Autowired
    public BookmarksService(DataSource dataSource, UserService userService,  PostService postService) {
        this.dataSource = dataSource;
        this.userService = userService;
        this.postService = postService; // could cause error
    }

    public List<Post> getBookPosts(String userId) throws SQLException {

        // retrieve all postIds book marked
        final String sql = "select postId from bookmark where userId = ?";
        List<String> postIds = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String postId = rs.getString("postId");
                    System.out.println("post id bookmarked: " + postId);
                    postIds.add(postId);
                }
            }
        }

        // return early if nothing found
        if (postIds.isEmpty()) {
            System.out.println("No bookmarked posts found.");
            return new ArrayList<>();
        }

        // List of postIds to fetch posts
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM post WHERE postId IN (");
        for (int i = 0; i < postIds.size(); i++) {
            sqlBuilder.append("?");
            if (i < postIds.size() - 1) {
                sqlBuilder.append(",");
            }
        }
        sqlBuilder.append(")");

        // retrieve bookmarked post information and populate post array
        List<Post> posts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < postIds.size(); i++) {
                pstmt.setString(i + 1, postIds.get(i));
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
                    boolean isHearted = postService.isHearted(postId, userId);
                    boolean isBookmarked = postService.isBookmarked(postId, userId);
                    List<Comment> comments = postService.getCommentsForPost(postId);
                    Post post = new ExpandedPost(postId, content, postDateTime, user,
                            Integer.parseInt(heartsCount), Integer.parseInt(commentsCount),
                            isHearted, isBookmarked, comments);
                    posts.add(post);
                }

                return posts;
            }
        }
    }

}
