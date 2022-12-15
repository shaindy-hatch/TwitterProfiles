package prospect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data

/** Prospect from hatch db **/
public class Prospect {
    public Prospect(String id, String twitterId, String firstName, String lastName, String regionLocation, String localityLocation) {
        this.id = id;
        this.twitterId = twitterId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.regionLocation = regionLocation;
        this.localityLocation = localityLocation;
    }

    private String id;
    private String twitterId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String regionLocation;
    private String localityLocation;

    public void setMiddleName() {
        int midNameIndex = firstName.indexOf(" ");
        if(midNameIndex > 0){
            String firstNameTemp = firstName.substring(0, midNameIndex);
            middleName = firstName.substring(midNameIndex+1).replaceAll("[^A-Za-z]", "");
            firstName = firstNameTemp;
        }
    }
}
