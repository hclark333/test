/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.services.PostService;
import uga.menik.csx370.services.UserService;
import uga.menik.csx370.utility.Utility;

/**
 * This controller handles the home page and some of it's sub URLs.
 */
@Controller
@RequestMapping
public class HomeController {

    private final UserService userService;
    private final PostService postService;
    public HomeController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    /**
     * This is the specific function that handles the root URL itself.
     * 
     * Note that this accepts a URL parameter called error.
     * The value to this parameter can be shown to the user as an error message.
     * See notes in HashtagSearchController.java regarding URL parameters.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("home_page");


        String loggedInUserId = userService.getLoggedInUser().getUserId();
        try {
            List<String> followedUserIds = userService.getFollowedUserIds(loggedInUserId);
            //System.out.println("followedUserIds: " + followedUserIds);
            List<Post> followedPosts = new ArrayList<Post>();
            for (String id : followedUserIds) {
                //System.out.println("followed user id: " + id);
                followedPosts.addAll(postService.getAllPostsByUser(loggedInUserId, id));
                // won't have to worry about duplicates because a post can't be made by multiple users
            }
            //sort posts by createdAt descending
            //already sorted by the postId in the table
            followedPosts.sort((p1, p2) -> Integer.parseInt(p2.getPostId())
                    - Integer.parseInt(p1.getPostId()));
            System.out.println("followedPosts: " + followedPosts);
            mv.addObject("posts", followedPosts);
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // An error message can be optionally specified with a url query parameter too.
        String errorMessage = error;
        mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);

        return mv;
    }

    /**
     * This function handles the /createpost URL.
     * This handles a post request that is going to be a form submission.
     * The form for this can be found in the home page. The form has a
     * input field with name = posttext. Note that the @RequestParam
     * annotation has the same name. This makes it possible to access the value
     * from the input from the form after it is submitted.
     */
    @PostMapping("/createpost")
    public String createPost(@RequestParam(name = "posttext") String postText) {
        System.out.println("User is creating post: " + postText);
        String loggedInUserId = userService.getLoggedInUser().getUserId();
        //System.out.println("loggedInUser: " + loggedInUserId);
        try {
            boolean postSuccess = postService.createPost(postText, loggedInUserId);
            if (postSuccess) {
                // Redirect the user if the post creation is a success.
                return "redirect:/";
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        // Redirect the user if the post creation is a success.
        // return "redirect:/";

        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to create the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/?error=" + message;
    }

}
