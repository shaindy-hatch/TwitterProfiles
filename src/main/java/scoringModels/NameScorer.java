package scoringModels;

import prospect.Prospect;
import twitterUser.TwitterUser;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static scoringModels.Preprocessor.isNullOrEmpty;

public class NameScorer {
    private Map<String, List<String>> nicknames;
    private static final int scoreThreshold = 65;
    private HashSet<String> names;

    public NameScorer() {
        this.nicknames = NicknameLoader.loadNicknames();
        names = readValidFirstNames();
    }

    public HashMap<String, Integer> scoreName(TwitterUser tUser, Prospect prospect, HashMap<String, Integer> scores) {

        populateInitialScores(scores);

        if (isNullOrEmpty(tUser.getName())) {
            return scores;
        }

        normalizeProspect(prospect);

        List<String> twitterNameParts = Preprocessor.normalizeTwitterUser(tUser.getName(), prospect);

        if (hasMiddleName(prospect.getFirstName()))
            prospect.setMiddleName();

        if (isReversedNames(twitterNameParts, prospect))
            reverseNames(twitterNameParts);

        if (twitterNameParts.size() >= 2)
            twitterMidNameIsProspectFirstName(twitterNameParts, prospect);

        scoreFirstName(twitterNameParts, prospect, scores);
        scoreMidName(twitterNameParts, prospect, scores);
        containsFirstAndMiddleNamesOnly(twitterNameParts, scores);

        scoreLastName(twitterNameParts, prospect, scores);

        if (!tUser.getName().contains(" ")) {

            HashMap<String, Integer> before_scores = new HashMap<>(scores);
            boolean removed = removeScoresOnMissingNamePart(scores);

            if(removed){
                System.out.println("Twitter : " + tUser.getName());
                System.out.println("Prospect: " + prospect.getFirstName() + " " + prospect.getLastName());
                System.out.println("Before scores:");
                printScores(before_scores);
                System.out.println("After scores:");
                printScores(scores);
            }
        }
        return scores;
    }

    private void populateInitialScores(HashMap<String, Integer> scores) {

        scores.put("FirstInitial", -1);
        scores.put("FirstName", -1);
        scores.put("MiddleInitial", -1);
        scores.put("MiddleName", -1);
        scores.put("LastInitial", -1);
        scores.put("LastName", -1);
    }

    /** Move last name to first position **/
    private void reverseNames(List<String> nameList) {
        String fNameTemp = nameList.remove(0);
        nameList.add(fNameTemp);
    }


    /** Iterate through name parts to find last name -- in case additional text is at the end of a user's name **/
    private void scoreLastName(List<String> twitterNameParts, Prospect prospect, HashMap<String, Integer> scores) {
        if(isNullOrEmpty(prospect.getLastName()) || isNullOrEmpty(twitterNameParts.get(twitterNameParts.size()-1)))
            return;

        int score = Math.max(scores.get("LastName"), scores.get("LastInitial"));
        int lNameIndex = twitterNameParts.size();

        //if first name scored well,  do not consider it for a last name
        int fNameIndex = firstNameScoredWell(scores) ? 0 : -1;

        while (score < scoreThreshold && lNameIndex - 1> fNameIndex) {
            lNameIndex--;
            if (twitterNameParts.get(lNameIndex) != null && !twitterNameParts.get(lNameIndex).isEmpty()) {
                if (hasCorrectInitial(twitterNameParts.get(lNameIndex), prospect.getLastName())) {
                    score = FuzzySearch.ratio(prospect.getLastName(), twitterNameParts.get(lNameIndex));
                }
            }
        }
        if (lNameIndex < twitterNameParts.size() && twitterNameParts.get(lNameIndex) != null && !twitterNameParts.get(lNameIndex).isEmpty()) {
            //check if contains last name or only last initial
            if(!isAnInitial(twitterNameParts.get(lNameIndex))) {
                score(twitterNameParts.get(lNameIndex), prospect.getLastName(), scores, "LastName");
            }
            score(twitterNameParts.get(lNameIndex).substring(0, 1), prospect.getLastName().substring(0, 1), scores, "LastInitial");
        }
    }

    private boolean hasCorrectInitial(String name1, String name2) {
        return name1.charAt(0) == name2.charAt(0);
    }

    private boolean firstNameScoredWell(HashMap<String, Integer> scores) {

        return Math.max(scores.get("FirstName"), scores.get("FirstInitial")) > scoreThreshold;
    }

    private void scoreFirstName(List<String> twitterNameParts, Prospect prospect, HashMap<String, Integer> scores) {
        if(isNullOrEmpty(prospect.getFirstName()))
            return;

        int fNameIndex = 0;
        if (twitterNameParts.size() > fNameIndex && !isNullOrEmpty(twitterNameParts.get(fNameIndex))) {

            //check if contains first name or only initial
            if (!isAnInitial(twitterNameParts.get(fNameIndex))) {
                score(twitterNameParts.get(fNameIndex), prospect.getFirstName(), scores, "FirstName");
            }
            score(twitterNameParts.get(fNameIndex).substring(0, 1), prospect.getFirstName().substring(0, 1), scores, "FirstInitial");
            scoreNickname(twitterNameParts.get(fNameIndex), prospect.getFirstName(), scores);
        }
    }

    private boolean isAnInitial(String name) {
        return name.replace(".", "").length() == 1;
    }

    /** Removes score if first name or last name was really an initial
     *  If name did not contain a space, and it was not able to be split into parts, remove the part that wasn't accurate?
     * **/
    private boolean removeScoresOnMissingNamePart(HashMap<String, Integer> scores) {
        if (scores.get("FirstName") < scoreThreshold && scores.get("FirstInitial") != 100 && scores.get("LastName") >= scoreThreshold && scores.get("LastInitial") == 100) {
            scores.put("FirstName", -1);
            scores.put("FirstInitial", -1);
            return true;
        } else if (scores.get("LastName") < scoreThreshold && scores.get("LastInitial") != 100 && scores.get("FirstName") >= scoreThreshold && scores.get("FirstInitial") == 100) {
            scores.put("LastName", -1);
            scores.put("LastInitial", -1);
            return true;
        }
        else{
            return false;
        }
    }

    /** check if prospect first name is equal to Twitter middle name **/
    private void twitterMidNameIsProspectFirstName(List<String> twitterNameParts, Prospect prospect) {
        if(isNullOrEmpty(prospect.getFirstName()) || isNullOrEmpty(twitterNameParts.get(0)) || isNullOrEmpty(twitterNameParts.get(1)))
            return;
        int fNameScore = FuzzySearch.ratio(twitterNameParts.get(0), prospect.getFirstName());
        int midNameScore = FuzzySearch.ratio(twitterNameParts.get(1), prospect.getFirstName());

        //remove first name is uses middle name instead
        if (midNameScore > fNameScore && hasCorrectInitial(prospect.getFirstName(), twitterNameParts.get(1)))
            twitterNameParts.remove(0);
    }


    /** final name component is the middle name and not the last name **/
    private void containsFirstAndMiddleNamesOnly(List<String> twitterNameParts, HashMap<String, Integer> scores) {
        if (scores.get("MiddleName") > 0 || scores.get("MiddleInitial") > 0)
            if (twitterNameParts.size() == 2)
                twitterNameParts.add(null);
    }

    /** last name precedes first name **/
    public boolean isReversedNames(List<String> twitterNameParts, Prospect prospect) {
        if (twitterNameParts.size() != 2 || twitterNameParts.contains(null))
            return false;
        if (isNickname(twitterNameParts.get(0), prospect.getFirstName()))
            return false;
        if (isNickname(twitterNameParts.get(twitterNameParts.size() - 1), prospect.getFirstName()))
            return true;

        int fNameScore = FuzzySearch.ratio(twitterNameParts.get(0), prospect.getFirstName());
        int lNameScore = FuzzySearch.ratio(twitterNameParts.get(twitterNameParts.size() - 1), prospect.getLastName());

        int revFNameScore = FuzzySearch.ratio(twitterNameParts.get(0), prospect.getLastName());
        int revLNameScore = FuzzySearch.ratio(twitterNameParts.get(twitterNameParts.size() - 1), prospect.getFirstName());

        if (revFNameScore > fNameScore && revLNameScore > lNameScore)
            return true;
        return !validName(twitterNameParts.get(0)) && validName(twitterNameParts.get(1));
    }

    /**
     * Search through name parts for the prospects middle name
     * It can be used as first name in Twitter, or it can be in a different position
     **/
    private void scoreMidName(List<String> twitterNameParts, Prospect prospect, HashMap<String, Integer> scores) {
        if (isNullOrEmpty(prospect.getMiddleName()) || twitterNameParts.size() < 2)
            return;

        String pMidName = prospect.getMiddleName();
        int midNameScore, midNameInd = 0 ;

        do {
            midNameScore = FuzzySearch.ratio(pMidName, twitterNameParts.get(midNameInd));
            if(isAnInitial(prospect.getMiddleName()))
                midNameScore = scoreMiddleInitial(twitterNameParts.get(midNameInd), pMidName, scores);
            midNameInd++;
        } while (midNameScore < scoreThreshold && midNameInd < twitterNameParts.size());

        if (midNameScore >= scoreThreshold) {
            scores.put("MiddleName", midNameScore);

            if(prospectMidNameIsTwitterFirstName(twitterNameParts.get(0), pMidName)) {
                scores.put("FirstName", -1);
                scores.put("FirstInitial", -1);
            }
        }
    }

    private boolean prospectMidNameIsTwitterFirstName(String twitterFirstName, String prospectMidName) {
        return FuzzySearch.ratio(twitterFirstName, prospectMidName) > scoreThreshold;
    }

    public int scoreMiddleInitial(String twitterMidName, String prospectMidName, HashMap<String, Integer> scores) {

        if(isAnInitial(twitterMidName)) {
            if (prospectMidName.equals(twitterMidName))
                scores.put("MiddleInitial", 100);
            else {
                scores.put("MiddleInitial", 0);
            }
        }
        return scores.get("MiddleInitial");
    }

    private void normalizeProspect(Prospect prospect) {
        prospect.setFirstName(prospect.getFirstName().toLowerCase());
        prospect.setLastName(prospect.getLastName().toLowerCase());
    }

    public boolean hasMiddleName(String prospectName) {
        return prospectName.contains(" ");
    }

    public void printScores(HashMap<String, Integer> scores) {
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            System.out.println("\t" + entry.getKey() + " Score: " + entry.getValue());
        }
    }

    private void scoreNickname(String twitterName, String prospectName, HashMap<String, Integer> scores) {
        if(isNickname(twitterName, prospectName))
            scores.put("FirstName", 100);
    }

    public boolean isNickname(String twitterName, String prospectName) {
        if (nicknames.containsKey(prospectName)) {
            if (nicknames.get(prospectName).contains(twitterName)) {
                return true;
            }
        }

        if (nicknames.containsKey(twitterName)) {
            if (nicknames.get(twitterName).contains(prospectName))
                return true;
        }
        return false;
    }

    public boolean validName(String name) {
        return names.contains(name);
    }

    public void score(String s1, String s2, HashMap<String, Integer> scores, String scoreType) {

        int score = FuzzySearch.ratio(s1, s2, s -> s.toUpperCase().replaceAll("[^A-Za-z]", ""));
        scores.put(scoreType, score);
    }

    public HashSet<String> readValidFirstNames() {
        HashSet<String> names = new HashSet<>();
        File directory = new File("validFirstNames");
        File[] files = directory.listFiles();

        try {
            for (File file : files) {
                Scanner input = new Scanner(file);

                while (input.hasNext()) {
                    String[] csvLine = input.nextLine().split(",");
                    names.add(csvLine[0]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return names;
    }
}
