package twitterUser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data

/** Follower and Following API call response object **/
public class FollowsAPIResponse {

    //follower or following user ids
    private List<TwitterUser> userIds;
    //token for next page
    private String nextToken;
    //reached api call rate limit within time period
    private boolean reachedLimit;

    public FollowsAPIResponse(List<TwitterUser> userIds, String nextToken ){
        this.userIds = userIds;
        this.nextToken = nextToken;
    }
}
