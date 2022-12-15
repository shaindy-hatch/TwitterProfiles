package scoringModels;

import prospect.Prospect;
import java.io.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/** Functions to clean and normalize data for the NameScorer and LocationScorer **/

public class Preprocessor {
    private static List<String> prefixes = readNamePrefix();
    private static List<String> suffixes = readNameSuffix();


    public static String normalizeName(String s) {
        if (s == null)
            return s;

        s = Normalizer.normalize(s, Normalizer.Form.NFKC);

        s = s.replaceAll("[^A-Za-z ]", "");

        if (!s.contains(" "))
            s = splitOnCamelCase(s);

        s = s.toLowerCase();
        s = removePrefix(s);
        s = removeSuffix(s);

        return s;
    }

    /** Normalize name and split into name parts **/
    public static List<String> normalizeTwitterUser(String name, Prospect prospect){
        name = normalizeName(name);

        //split on first or last name when no space in full name
        if (!name.contains(" "))
            name = splitOnSubstring(name, prospect.getFirstName(), prospect.getLastName());

        String[] nameParts = splitOnSpace(name);
        return Arrays.stream(nameParts).collect(Collectors.toList());
    }

    private static String removeSuffix(String s) {
        int suffixIndex = s.indexOf(",");
        if (suffixIndex > 0) {
            s = s.substring(0, suffixIndex);
        }

        for (String suf : suffixes) {
            if(s.endsWith(suf))
                s = s.substring(0, s.length() - suf.length());
        }
        return s.trim();
    }

    public static String[] splitOnSpace(String s) {
        String[] nameParts = null;
        if (s.contains(" ")) {
            nameParts = s.split(" ");
            for(int i = 0; i < nameParts.length; i++){
                if(nameParts[i].isBlank() || nameParts[i].isEmpty())
                    nameParts[i] = null;
            }
            return nameParts;
        }

        return new String[]{s};
    }

    public static String removePrefix(String s) {

        for (String p : prefixes) {
            if(s.startsWith(p))
                s = s.substring(p.length());
        }
        return s.trim();
    }
    public static List<String> readNameSuffix() {
        return readCSVList("namesuffix.csv");
    }

    public static List<String> readNamePrefix() {
        return readCSVList("nameprefix.csv");
    }

    public static List<String> readCSVList(String filename){
        List<String> prefixes = new ArrayList<>();
        try {
            File file = new File(filename);
            Scanner input = new Scanner(file);

            while (input.hasNext()) {
                prefixes.add(input.nextLine().toLowerCase().replaceAll("[^A-Za-z]", ""));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return prefixes;
    }

    public static String splitOnCamelCase(String name) {
        StringBuffer str = new StringBuffer();

        int lowerCaseCount = 0;
        for (char c : name.toCharArray()) {
            if (Character.isLowerCase(c)) {
                str.append(c);
                lowerCaseCount++;
            } else
                str.append(" " + c);
        }

        //if substantial amount of lower case then split on camelcase
        //prevents one lower case letter to be counted
        return lowerCaseCount > 3 ?
                str.toString()
                : name;
    }

    public static String splitOnSubstring(String name, String prospectFName, String prospectLName) {
        int lNameIndex = name.indexOf(prospectLName);

        //if last name is present
        if(isPresent(lNameIndex)) {
            return name.substring(0, lNameIndex) + " " + name.substring(lNameIndex);
        }
        //if not split by the end of first name
        else {
            int fNameIndex = name.indexOf(prospectFName);

            if (isPresent(fNameIndex))
                return name.substring(0, prospectFName.length()) + " " + name.substring(prospectFName.length());
            else
                return name;
        }
    }

    private static boolean isPresent(int index) {
        return index >= 0;
    }

    public static boolean isNullOrEmpty(String s){
        return s == null || s.isEmpty();
    }
    public static String normalizeLocation(String s){
        return s.toLowerCase().replaceAll("[^A-Za-z, ]", "");
    }
}
