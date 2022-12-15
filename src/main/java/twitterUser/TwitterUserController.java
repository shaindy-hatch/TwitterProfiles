package twitterUser;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Returns parsed Twitter user data from API responses made by TwitterUserRepository **/
public class TwitterUserController {

    private TwitterUserRepository repository;
    private Gson gson;

    public TwitterUserController() {
        String bearerToken = "AAAAAAAAAAAAAAAAAAAAANUshQEAAAAAdGd7WHOWd3zcwshnJ8PDH7aMoJY%3D5aL4Tgx6VtmCq1y74M9mIdsai0qA0VDIKwPvEp5Avu7KVQYQgm";
        repository = new TwitterUserRepository(bearerToken);
        gson = new Gson();
    }

    public TwitterUser getUser(String userId) {
        String response = repository.getUser(userId);
        return parseUser(response);
    }
    /** retrieve first page of followers **/
    public FollowsAPIResponse getUserFollowers(String userId) {
        String response = repository.getFollowers(userId);
        TwitterUser[] users = parseUsers(response);
        String nextToken = getNextToken(response);

        List<TwitterUser> tUsers = users != null ? Arrays.stream(users).collect(Collectors.toList()) : null;
        FollowsAPIResponse followsAPIResponse = new FollowsAPIResponse(tUsers, nextToken);

        if(reachedTooManyRequests(response))
            followsAPIResponse.setReachedLimit(true);

        return followsAPIResponse;
    }
    /** retrieve subsequent page of followers **/
    public FollowsAPIResponse getUserFollowersPage(String userId, String nextToken) {
        String response = repository.getFollowersPage(userId, nextToken);
        TwitterUser[] users = parseUsers(response);
        nextToken = getNextToken(response);

        List<TwitterUser> tUsers = users != null ? Arrays.stream(users).collect(Collectors.toList()) : null;
        FollowsAPIResponse followsAPIResponse = new FollowsAPIResponse(tUsers, nextToken);

        if(reachedTooManyRequests(response))
            followsAPIResponse.setReachedLimit(true);

        return followsAPIResponse;
    }
    /** retrieve first page of followings **/
    public FollowsAPIResponse getUserFollowings(String userId) {
        String response = repository.getFollowings(userId);
        TwitterUser[] users = parseUsers(response);
        String nextToken = getNextToken(response);

        List<TwitterUser> tUsers = users != null ? Arrays.stream(users).collect(Collectors.toList()) : null;
        FollowsAPIResponse followsAPIResponse = new FollowsAPIResponse(tUsers, nextToken);

        if(reachedTooManyRequests(response))
            followsAPIResponse.setReachedLimit(true);

        return followsAPIResponse;
    }

    /** retrieve subsequent page of followings **/
    public FollowsAPIResponse getUserFollowingsPage(String userId, String nextToken) {
        String response = repository.getFollowingsPage(userId, nextToken);
        TwitterUser[] users = parseUsers(response);
        nextToken = getNextToken(response);

        List<TwitterUser> tUsers = users != null ? Arrays.stream(users).collect(Collectors.toList()) : null;
        FollowsAPIResponse followsAPIResponse = new FollowsAPIResponse(tUsers, nextToken);

        if(reachedTooManyRequests(response))
            followsAPIResponse.setReachedLimit(true);

        return followsAPIResponse;
    }

    /** reached api calls limit in time period **/
    private boolean reachedTooManyRequests(String response) {
        try {
            String errorTitle = JsonParser.parseString(response).getAsJsonObject().get("title").getAsString();
            if(errorTitle.equalsIgnoreCase("Too Many Requests"))
                return true;
        } catch (Exception e){
            return false;
        }
        return false;
    }

    public TwitterUser parseUser(String response) {
        TwitterUser user = null;
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonElement element = json.get("data");

            if (element != null) {
                String jsonString = element.toString();
                user = gson.fromJson(jsonString, TwitterUser.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    public TwitterUser[] parseUsers(String response) {
        TwitterUser[] users = null;
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonElement element = json.get("data");

            if (element != null) {
                String jsonString = element.toString();
                users = gson.fromJson(jsonString, TwitterUser[].class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }

    /** retrieve token for next page of results **/
    public String getNextToken(String response) {
        String nextToken = null;
        try {

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();

            JsonObject meta = json.get("meta").getAsJsonObject();
            JsonElement element = meta.get("next_token");

            if (element != null && !element.isJsonNull())
                nextToken = element.getAsString();
        } catch (Exception e) {
            nextToken = null;
        }
        return nextToken;
    }
}
