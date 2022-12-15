import scoringModels.LocationScorer;
import scoringModels.NameScorer;
import prospect.*;
import twitterUser.*;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;

/** Runs NameScorer and LocationScorer **/

public class RunScoringModel {

    private static List<String> prospects = prospectsFromJson(); //prospect from hatch db with a twitter id that can test algorithm on
    private static TwitterUserDBController dbController = new TwitterUserDBController();
    private static ProspectDBController prospectDBController = new ProspectDBController();

    private static NameScorer nameScorer = new NameScorer();
    private static LocationScorer locationScorer = new LocationScorer();

    public static void main(String[] args) {
        try {
            runAllNameAndLocationScores(prospects);

            //other functions you can try:

//            testOneLocationCase("15772885");
//            testOneLocationCase("16225863");
//            runAllLocationScores(prospects);
//            runAllNameScores(prospects);
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            dbController.closeDBConnection(dbController.connection);
            prospectDBController.closeDBConnection(prospectDBController.connection, prospectDBController.session);
        }
    }
    public static void testOneLocationCase(String userId){

        Prospect p = prospectDBController.getHatchUser(userId);
        TwitterUser t =dbController.getDBUser(userId);

        System.out.println("Twitter : " + t.getName());
        System.out.println("Prospect: " + p.getFirstName() + " " + p.getLastName());
        locationScorer.scoreLocation(t.getLocation(), p, new HashMap<>());
    }

    public static void testOneNameCase(String userId){

        Prospect p = prospectDBController.getHatchUser(userId);
        TwitterUser t =dbController.getDBUser(userId);

        System.out.println("Twitter : " + t.getName());
        System.out.println("Prospect: " + p.getFirstName() + " " + p.getLastName());
        nameScorer.scoreName(t,p,new HashMap<>());
    }

    public static void runAllNameAndLocationScores(List<String> userIds){

        for (String id : userIds) {
            TwitterUser t = dbController.getDBUser(id);
            Prospect p = prospectDBController.getHatchUser(id);
            if (p != null && t != null) {
                System.out.println("Twitter Name: " + t.getName());
                System.out.println("Location: " + t.getLocation());
                System.out.println("Prospect Name: " + p.getFirstName() + " " + p.getLastName());
                System.out.println("Location: " + p.getLocalityLocation() + " " + p.getRegionLocation());
                HashMap<String, Integer> scores = new HashMap<>();
                scores = nameScorer.scoreName(t, p, scores);
                scores = locationScorer.scoreLocation(t.getLocation(), p, scores);
                printScores(scores);
            }
        }
    }

    public static void runAllLocationScores(List<String> userIds) {

        for (String id : userIds) {
            TwitterUser user = dbController.getDBUser(id);
            Prospect p = prospectDBController.getHatchUser(id);

            if (p != null && user != null){
                    System.out.println("Twitter : " + user.getLocation());
                    System.out.println("Prospect: " + p.getLocalityLocation() + " " + p.getRegionLocation());
                  HashMap<String, Integer> scores = locationScorer.scoreLocation(user.getLocation(), p, new HashMap<>());
                  printScores(scores);
            }
        }
    }

    public static void runAllNameScores(List<String> userIds) {

        for (String id : userIds) {
            TwitterUser tUser = dbController.getDBUser(id);
            Prospect p = prospectDBController.getHatchUser(id);
            if (tUser != null) {
                System.out.println("Twitter : " + tUser.getName());
                System.out.println("Prospect: " + p.getFirstName() + " " + p.getLastName());
                HashMap<String, Integer> scores = new HashMap<>();
                nameScorer.scoreName(tUser, p, scores);
                printScores(scores);
            }
        }
    }

    public static void printScores(HashMap<String, Integer> scores) {
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            System.out.println("\t" + entry.getKey() + " Score: " + entry.getValue());
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
    public static List<String> removeDuplicates(List<String> list) {
        Set<String> set = new HashSet<>(list);
        list.clear();
        list.addAll(set);
        return list;
    }
}
