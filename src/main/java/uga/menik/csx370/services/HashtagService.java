package uga.menik.csx370.services;

import org.springframework.stereotype.Service;
import uga.menik.csx370.models.Post;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HashtagService {


   public List<Post> filterPostsByHashtags(List<Post> allPosts, List<String> hashtagList) {
        return allPosts.stream()
                .filter(post -> {
                    Set<String> postHashtags = getHashtags(post);
                    Set<String> hashTagSet = Set.copyOf(hashtagList);
                    return postHashtags.equals(hashTagSet);
                })
                .collect(Collectors.toList());
    }

    private Set<String> getHashtags(Post post) {
       String content = post.getContent();
       Set<String> hashtags = List.of(content.split(" ")).stream()
               .filter(s -> s.startsWith("#"))
               .collect(Collectors.toSet());
       return hashtags;
    }

}


