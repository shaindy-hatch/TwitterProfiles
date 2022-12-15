package twitterUser;

import java.util.HashMap;
import java.util.HashSet;

public class Connections {

    private HashMap<String, HashSet<String>> followers;
    private HashMap<String, HashSet<String>> followings;

    public Connections(){
        followers = new HashMap<>();
        followings = new HashMap<>();
    }

    public boolean addUser(String userId){

        if(!followers.containsKey(userId) && !followings.containsKey(userId)) {

            followers.put(userId, new HashSet<>());
            followings.put(userId, new HashSet<>());
            return true;
        }
        return false;
    }

    public boolean addFollower(String userId, String followerId){
        HashSet<String> userFollowers = followers.get(userId);
        HashSet<String> userFollowings = followings.get(followerId);

        if(userFollowers != null && userFollowings != null){
            userFollowers.add(followerId);
            userFollowings.add(userId);
            return true;
        }
        return false;
    }
    public HashSet<String> followers(String userId){
        return followers.get(userId);
    }

    public HashSet<String> followings(String userId){
        return followings.get(userId);
    }
}
