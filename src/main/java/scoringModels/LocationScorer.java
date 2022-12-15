package scoringModels;

import prospect.Prospect;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import java.util.*;
import java.util.stream.Collectors;

import static scoringModels.Preprocessor.normalizeLocation;

/**
 * Scores Twitter user's location field against prospect's locality location and region location fields
 *
 * Note: Needs some tweaking, there's room for improvement that I did not get a chance to complete
 */
public class LocationScorer {

    private final Map<String, List<String>> USCities;
    public final Map<String, List<String>> CanadianCities;
    private static final int SCORE_THRESHOLD = 85;

    public LocationScorer() {
        USCities = CityLoader.readUSCities();
        CanadianCities = CityLoader.readCanadianCities();
    }

    public HashMap<String, Integer> scoreLocation(String twitterFullLocation, Prospect prospect, HashMap<String, Integer> scores) {

        int score = -1;

        populateInitialScore(scores);

        if (twitterFullLocation == null || (prospect.getLocalityLocation() == null && prospect.getRegionLocation() == null))
            return scores;

        normalizeProspectLocation(prospect);
        twitterFullLocation = normalizeLocation(twitterFullLocation);

        if(hasComma(twitterFullLocation))
            score = scoreCommas(twitterFullLocation, prospect);

        if(score < SCORE_THRESHOLD){

            //potential normalization
            twitterFullLocation = getRegionName(twitterFullLocation);

            score = Math.max(score, score(twitterFullLocation, prospect));
        }
        if(score < SCORE_THRESHOLD && twitterFullLocation.contains(" "))
            score = Math.max(score, scoreSubstring(twitterFullLocation, prospect));

        System.out.println("\tLocation Score? " + score);
        scores.put("Location", score);

        return scores;
    }

    private void populateInitialScore(HashMap<String, Integer> scores) {
        scores.put("Location", -1);
    }

    /** Find substring based on spaces, need to loop since a locality/region can be two words **/
    private int scoreSubstring(String twitterLocation, Prospect prospect) {
        int score = -1;
        StringBuilder substring = new StringBuilder();

        while(!twitterLocation.isEmpty()){

            //adding space since a word is already present
            if(substring.length()> 0)
                substring.append(" ");

            int newWordIndex = twitterLocation.indexOf(" ");
            if (newWordIndex > 0)
                substring.append(twitterLocation, 0, newWordIndex);
            else
                substring.append(twitterLocation);

            String currSubstring = getRegionName(substring.toString());

            score = Math.max(score, score(currSubstring, prospect));

            if(newWordIndex > 0)
                twitterLocation = twitterLocation.substring(newWordIndex+1);
            else
                twitterLocation = "";
        }
        return score;
    }

    private void normalizeProspectLocation(Prospect prospect) {
        if(prospect.getLocalityLocation() == null)
            prospect.setLocalityLocation("");
        else
            prospect.setLocalityLocation(normalizeLocation(prospect.getLocalityLocation()));

        if(prospect.getRegionLocation() == null)
            prospect.setRegionLocation("");
        else {
            String prospectRegion = normalizeLocation(prospect.getRegionLocation());
            prospect.setRegionLocation(prospectRegion);;
        }
    }

    private int scoreCommas(String tLocation, Prospect prospect) {

        List<String> locationParts = splitByDelim(tLocation, ",");
        normalizeRegionName(locationParts);

        int score = -1;

        for(String location : locationParts){
            int currScore = score(location, prospect);

            score = Math.max(score, currScore);
        }
        return score;
    }

    /** Iterates through list and replaces element in region name with name returned from function call **/
    public void normalizeRegionName(List<String> regionNameList) {
        for(int i = 0; i < regionNameList.size(); i++) {
            regionNameList.set(i, getRegionName(regionNameList.get(i)));
        }
    }

    /** Returns region name from enum if found, else returns regionName parameter **/
    public String getRegionName(String regionName) {
        Region region = Region.getRegion(regionName);
        regionName = (region != Region.UNKNOWN) ? region.toString() : regionName;
        return regionName;
    }

    public boolean cityIsWithinRegion(String city, String region) {
        Region regionEnum = Region.getRegion(region);
        region = regionEnum != Region.UNKNOWN ? regionEnum.toString().toLowerCase() : region;

        if (USCities.containsKey(city))
            return USCities.get(city).contains(region);

        if (CanadianCities.containsKey(city))
            return CanadianCities.get(city).contains(region);

        return false;
    }

    public List<String> splitByDelim(String s, String delim) {
        if (s.contains(delim)) {
            String[] locations = s.split(delim);
            return Arrays.stream(locations).map(x -> x.trim()).collect(Collectors.toList());
        }
        return null;
    }

    public boolean hasComma(String s) {
        return s.contains(",");
    }

    public int score(String s1, String s2) {
        if (s1 == null || s2 == null) return -1;
        return FuzzySearch.ratio(s1, s2, s -> s.toLowerCase().replaceAll("[^A-Za-z ]", ""));
    }

    /** Region location, locality location, or combined locations score **/
    private int score(String twitterLocation, Prospect prospect) {
        if (cityIsWithinRegion(twitterLocation, prospect.getRegionLocation())){
            return 100;
        }
        Region region = Region.getRegion(prospect.getRegionLocation());
        int score;

        //score on region name and abbreviation
        if (region != Region.UNKNOWN) {
            score = score(twitterLocation, region.toString());
            score = Math.max(score, score(twitterLocation, region.getAbbreviation()));
        }
        else{
            score = score(twitterLocation, prospect.getRegionLocation());
        }
        //score on locality location
        score =  Math.max(score, score(twitterLocation, prospect.getLocalityLocation()));

        //score on combined location
        String prospectFullLocation = prospect.getLocalityLocation() + " " + prospect.getRegionLocation();
        return Math.max(score, score(twitterLocation, prospectFullLocation));
    }

    public int scoreRegion(String twitterLocation, Prospect prospect){

        Region region = Region.getRegion(prospect.getRegionLocation());
        int score;

        //score on region name and abbreviation
        if (region != Region.UNKNOWN) {
            score = score(twitterLocation, region.toString());
            score = Math.max(score, score(twitterLocation, region.getAbbreviation().toLowerCase()));
        }
        else{
            score = score(twitterLocation, prospect.getRegionLocation());
        }
        return score;
    }
}
