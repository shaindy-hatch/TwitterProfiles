package scoringModels;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/** Reads file of cities mapped to regions **/
public class CityLoader {

    public static Map<String, List<String>> readCanadianCities(){
        return readCities("canadian_cities.csv");
    }
    public static Map<String, List<String>> readUSCities(){
        return readCities("us_cities.csv");
    }

    private static Map<String, List<String>> readCities(String filename){
        Map<String, List<String>> cities = new HashMap<>();

        BufferedReader input = null;

        try {
            input = new BufferedReader(new FileReader(filename));
            String line =  input.readLine(); //skip header row
            while ((line = input.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                String city = st.nextToken().toLowerCase();
                String state = st.nextToken().toLowerCase();

                if(cities.containsKey(city)){
                    cities.get(city).add(state);
                }
                else{
                    List<String> states = new ArrayList<>();
                    states.add(state);
                    cities.put(city, states);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return cities;
    }
}