package twitterUser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TwitterUser {

    private String id;
    private String name;
    private String username;
    private String profile_image_url;
    private String location;
}
