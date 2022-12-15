import prospect.ProspectDBController;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import twitterUser.FollowsAPIResponse;
import twitterUser.TwitterUser;
import twitterUser.TwitterUserController;
import twitterUser.TwitterUserDBController;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Background process to make permitted number of API calls every interval period
 * Retrieves followers and followings of prospects and writes them to the database
 *
 * @field prospects - list of prospects from the hatch research db that contain a twitter id
 * @field allowedCalls - number of API calls within timeframe
 * @field followerIndex - index of prospect list of where to begin saving followers
 * @field followingIndex - index of prospect list of where to begin saving followings
 * @field followerNextToken - token for next page of results of followers
 * @field followingNextToken - token for next page of results of followers
 */
public class WriteToDB extends TimerTask {

    static TwitterUserController twitterUserController = new TwitterUserController();
    static TwitterUserDBController dbController;
    static ProspectDBController prospectDBController;
    static List<String> prospects = prospectsFromJson();
    static Timer timer = new Timer();
    static final int allowedCalls = 15;
    static final String reachedLimit = "Too Many Requests";

    String followerNextToken;
    String followingNextToken;
    int followerIndex;
    int followingIndex;

    public WriteToDB(int followerIndex, int followingIndex, String followerNextToken, String followingNextToken) {
        this.followerIndex = followerIndex;
        this.followingIndex = followingIndex;
        this.followerNextToken = followerNextToken;
        this.followingNextToken = followingNextToken;
    }

    public static void main(String[] args) {

        int followerIndex = 132, followingIndex = 252;
        String followerNextToken = "LHJ07MQ1SB31CZZZ";
        String followingNextToken = "JC9QBHS6UMOH6ZZZ";
        TimerTask timerTask = new WriteToDB(followerIndex, followingIndex, followerNextToken, followingNextToken);

        timer.scheduleAtFixedRate(timerTask, 0, 16 * 60 * 1000);
    }

    public void runProcess() {
        dbController = new TwitterUserDBController();
        prospectDBController = new ProspectDBController();
        System.out.println("**Starting Process**");
        try {
            saveFollowers();
            saveFollowings();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbController.closeDBConnection(dbController.connection);
            prospectDBController.closeDBConnection(prospectDBController.connection, prospectDBController.session);
        }

    }
    private void saveFollowers() {
        int currCall = 0;

        while (currCall < allowedCalls) {
            if (followerIndex < prospects.size()) {
                System.out.println("Follower Index " + followerIndex + " - " + prospects.get(followerIndex));
                String token;

                do{
                    token = saveFollowersAndGetNextToken(prospects.get(followerIndex), followerNextToken);

                    if (token != null && token.equals(reachedLimit))
                        break;
                    else
                        followerNextToken = token;
                    currCall++;

                } while (followerNextToken != null & currCall < allowedCalls);


                System.out.println("Next Token " + followerNextToken);

                if (followerNextToken == null) {
                    System.out.println("---Followers Complete---");
                    followerIndex++;

                }
            } else
                break;
        }
    }

    public void saveFollowings(){
        int currCall = 0;

        while (currCall < allowedCalls) {
            if (followingIndex < prospects.size()) {
                System.out.println("Following Index " + followingIndex + " - " + prospects.get(followingIndex));
                String token;

                do{
                    token = saveFollowingsAndGetNextToken(prospects.get(followingIndex), followingNextToken);

                    if (token != null && token.equals(reachedLimit))
                        break;
                    else
                        followingNextToken = token;
                    currCall++;

                } while (followingNextToken != null & currCall < allowedCalls);


                System.out.println("Next Token " + followingNextToken);

                if (followingNextToken == null) {
                    System.out.println("---Followings Complete---");
                    followingIndex++;
                    if(followingIndex == 158)
                        followingIndex = 195;
                }
            } else
                break;
        }
    }

    public static List<String> removeDuplicates(List<String> list) {
        Set<String> set = new HashSet<>(list);
        list.clear();
        list.addAll(set);
        return list;
    }

    //write all prospects with twitter id to file
    public static void prospectsToJson() {
        Gson gson = new Gson();
        try {
            List<String> prospects = prospectDBController.getHatchUsersWithID();
            System.out.println(prospects.size());
            File file = new File("prospects.json");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file);
            gson.toJson(prospects, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            prospectDBController.closeDBConnection(prospectDBController.connection, prospectDBController.session);
        }
    }

    public static List<String> prospectsFromJson() {
        Gson gson = new Gson();
        try {
            FileReader reader = new FileReader("prospects.json");
            Type type = new TypeToken<ArrayList<String>>() {
            }.getType();
            List<String> prospects = gson.fromJson(reader, type);
            return removeDuplicates(prospects);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String saveFollowersAndGetNextToken(String userId, String nextToken) {
        FollowsAPIResponse data;
        if (nextToken == null)
            data = twitterUserController.getUserFollowers(userId);
        else
            data = twitterUserController.getUserFollowersPage(userId, nextToken);

        if (!data.isReachedLimit()) {
            List<TwitterUser> followers = data.getUserIds();
            if (followers != null) {
                System.out.print("\t" + followers.size());
            }
            dbController.saveFollowers(userId, followers);
            System.out.println(" saved");

            return data.getNextToken();
        }
        return "Too Many Requests";
    }

    public static String saveFollowingsAndGetNextToken(String userId, String nextToken) {
        FollowsAPIResponse data;
        if (nextToken == null)
            data = twitterUserController.getUserFollowings(userId);
        else
            data = twitterUserController.getUserFollowingsPage(userId, nextToken);

        List<TwitterUser> followings = data.getUserIds();
        if (followings != null) {
            System.out.print("\t" + followings.size());
        }
        dbController.saveFollowings(userId, followings);
        System.out.println(" saved");

        return data.getNextToken();
    }

    @Override
    public void run() {

        runProcess();
    }
}
