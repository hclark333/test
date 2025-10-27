/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import uga.menik.csx370.models.FollowableUser;
import uga.menik.csx370.utility.Utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import uga.menik.csx370.models.User;

/**
 * This service contains people related functions.
 */
@Service
public class PeopleService {

    // dataSource enables talking to the database.
    private final DataSource dataSource;
    // passwordEncoder is used for password security.
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public PeopleService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * This function should query and return all users that 
     * are followable. The list should not contain the user 
     * with id userIdToExclude.
     */
    public List<FollowableUser> getFollowableUsers (String userIdToExclude)  throws SQLException {

        // Write an SQL query to find the users that are not the current user.
        final String sql = "select * from user where userID != ?";

        List<FollowableUser> followableUsers = new ArrayList<>();

        // Run the query with a datasource.
        // See UserService.java to see how to inject DataSource instance and
        // use it to run a query.
        try (Connection conn = dataSource.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Replace ? with the user's ID
            pstmt.setString(1, userIdToExclude);

            // Get the results of the query and add to list
            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");

                    followableUsers.add(new FollowableUser(userId, firstName, lastName,
                            false, "Mar 07, 2024, 10:54 PM"));
                }
            }

        }

        List<FollowableUser> followableUsersExpanded = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {

            final String lastActiveSql =
                "SELECT DATE_FORMAT(MAX(postDate), '%b %d, %Y, %h:%i %p') AS lastActive " +
                "FROM post WHERE userId = ?";
            final String followCheckSql =
                "SELECT 1 FROM follows WHERE followerId = ? AND followeeId = ? LIMIT 1";

            try (PreparedStatement psLast = conn.prepareStatement(lastActiveSql);
                PreparedStatement psFollow = conn.prepareStatement(followCheckSql)) {

                // Grab the last active time and follow data
                for (FollowableUser u : followableUsers) {
                    String uid = u.getUserId();

                    String lastActive = "N/A";
                    psLast.setString(1, uid);

                    try (ResultSet r1 = psLast.executeQuery()) {
                        if (r1.next()) {
                            String v = r1.getString("lastActive");
                            if (v != null && !v.isEmpty()) lastActive = v;
                        }
                    }

                    boolean isFollowed = false;
                    psFollow.setString(1, userIdToExclude);
                    psFollow.setString(2, uid);

                    try (ResultSet r2 = psFollow.executeQuery()) {
                        isFollowed = r2.next();
                    }

                    followableUsersExpanded.add(new FollowableUser(
                        uid, u.getFirstName(), u.getLastName(), isFollowed, lastActive
                    ));
                }
            }
        }

        followableUsers = followableUsersExpanded;

        return followableUsers;

    }

    public void followUser(String followerId, String followeeId) throws SQLException {

        if (followerId.equals(followeeId)) {
            throw new SQLException("Cannot follow self");
        }

        final String sql = "INSERT INTO follows (followerId, followeeId) VALUES (?, ?)";
        
        // fulfill the follow request
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, followerId);
            ps.setString(2, followeeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (!(e instanceof java.sql.SQLIntegrityConstraintViolationException)) {
                throw e;
            }
        }
    }

    public void unfollowUser(String followerId, String followeeId) throws SQLException {

        final String sql = "DELETE FROM follows WHERE followerId = ? AND followeeId = ?";

        // fulfill the unfollow request
        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, followerId);
            ps.setString(2, followeeId);
            ps.executeUpdate();
        }
    }

}
