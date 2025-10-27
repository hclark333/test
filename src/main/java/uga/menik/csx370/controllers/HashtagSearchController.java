package uga.menik.csx370.controllers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.services.PostService;
import uga.menik.csx370.services.UserService;

/**
 * Handles /hashtagsearch URL and possibly others.
 * At this point no other URLs.
 */
@Controller
@RequestMapping("/hashtagsearch")
public class HashtagSearchController {

    private final PostService postService;
    private final UserService userService;

    @Autowired
    public HashtagSearchController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    /**
     * This function handles the /hashtagsearch URL itself.
     * This URL can process a request parameter with name hashtags.
     * In the browser the URL will look something like below:
     * http://localhost:8081/hashtagsearch?hashtags=%23amazing+%23fireworks
     * Note: the value of the hashtags is URL encoded.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "hashtags") String hashtags) {
        System.out.println("User is searching: " + hashtags);

        ModelAndView mv = new ModelAndView("posts_page");

        String loggedInUserId = userService.getLoggedInUser().getUserId();
        List<Post> posts = new ArrayList<>();

        try {
            posts = postService.searchPostsByHashtags(hashtags, loggedInUserId);

            if (posts.isEmpty()) {
                mv.addObject("isNoContent", true);
            }

        } catch (SQLException e) {
            System.err.println("Error searching posts by hashtags: " + e.getMessage());
            String errorMessage = "Some error occured! Failed to search posts. Please try again.";
            mv.addObject("errorMessage", errorMessage);
        }

        mv.addObject("posts", posts);
        return mv;
    }

}
