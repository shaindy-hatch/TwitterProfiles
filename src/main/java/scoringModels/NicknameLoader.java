package scoringModels;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/** Reads file of names mapped to nicknames **/
public class NicknameLoader {

    public static Map<String, List<String>> loadNicknames() {

        Map<String, List<String>> nicknames = new HashMap<>();

        BufferedReader input = null;

        try {
            input = new BufferedReader(new FileReader("nicknames.csv"));
            String line = null;
            while ((line = input.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, ",");
                String key = st.nextToken().toLowerCase();
                List<String> values = new ArrayList<String>();
                while (st.hasMoreElements()) {
                    values.add(st.nextToken().toLowerCase());
                }
                nicknames.put(key, values);
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
        return nicknames;
    }
}
