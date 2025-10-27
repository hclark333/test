/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.services.PostService;
import uga.menik.csx370.services.UserService;
import uga.menik.csx370.utility.Utility;

/**
 * Handles /profile URL and its sub URLs.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    // UserService has user login and registration related functions.
    private final UserService userService;
    private final PostService postService;

    /**
     * See notes in AuthInterceptor.java regarding how this works 
     * through dependency injection and inversion of control.
     */
    @Autowired
    public ProfileController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    /**
     * This function handles /profile URL itself.
     * This serves the webpage that shows posts of the logged in user.
     */
    @GetMapping
    public ModelAndView profileOfLoggedInUser() {
        System.out.println("User is attempting to view profile of the logged in user.");
        return profileOfSpecificUser(userService.getLoggedInUser().getUserId());
    }

    /**
     * This function handles /profile/{userId} URL.
     * This serves the webpage that shows posts of a speific user given by userId.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * how path variables work.
     */
    @GetMapping("/{userId}")
    public ModelAndView profileOfSpecificUser(@PathVariable("userId") String userId) {

        ModelAndView mv = new ModelAndView("posts_page");
        
        try {
            // Grab the user ID of the profile viewer
            String viewerId = userService.getLoggedInUser().getUserId();

            // Fetch all posts
            List<Post> all = postService.getAllPosts(viewerId);

            // Keep only the specified user's posts
            List<Post> posts = all.stream()
                .filter(p -> String.valueOf(p.getUser().getUserId()).equals(userId))
                .sorted((a, b) -> {
                    String da = a.getPostDate();
                    String db = b.getPostDate();
                    // Reverse order (newest first)
                    return db.compareTo(da);
                })
                .collect(Collectors.toList());

            // Add the posts to the ModelAndView object
            // If there are no posts, declare isNoContent true
            mv.addObject("posts", posts);
            if (posts.isEmpty()) {
                mv.addObject("isNoContent", true);
            }
            return mv;

        } catch (SQLException e) {
            mv.addObject("errorMessage", "Failed to load profile posts: " + e.getMessage());
            mv.addObject("isNoContent", true);
            return mv;
        }
    }
    
}
