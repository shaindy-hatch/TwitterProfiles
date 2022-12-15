package twitterUser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import java.net.URISyntaxException;
import java.util.ArrayList;

/** Retrieves API response from calls to Twitter API **/
public class TwitterUserRepository {

    private HttpClient httpClient;
    private String bearerToken;
    private ArrayList<NameValuePair> userParameters;

    public TwitterUserRepository(String bearerToken) {
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD).build())
                .build();

        this.bearerToken = bearerToken;

        userParameters = new ArrayList<>();
        userParameters.add(new BasicNameValuePair("user.fields", "name,profile_image_url,location"));
    }

    public HttpGet httpGetter(URIBuilder uriBuilder) throws URISyntaxException {
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.setHeader("Authorization", String.format("Bearer %s", bearerToken));
        httpGet.setHeader("Content-Type", "application/json");

        return httpGet;
    }

    public String getUser(String userId) {
        String userResponse = null;

        try {
            URIBuilder uriBuilder = new URIBuilder(String.format("https://api.twitter.com/2/users/%s", userId));
            uriBuilder.addParameters(userParameters);

            HttpGet httpGet = httpGetter(uriBuilder);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (null != entity) {
                userResponse = EntityUtils.toString(entity, "UTF-8");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return userResponse;
    }

    public String getUserByUsername(String username) {
        String userResponse = null;

        try {
            URIBuilder uriBuilder = new URIBuilder("https://api.twitter.com/2/users/by");
            userParameters.add(new BasicNameValuePair("usernames", username));
            uriBuilder.addParameters(userParameters);

            HttpGet httpGet = httpGetter(uriBuilder);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (null != entity) {
                userResponse = EntityUtils.toString(entity, "UTF-8");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return userResponse;
    }

    private String getFollowersOrFollowings(String uri) {
        String response = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(uri);
            uriBuilder.addParameters(userParameters);

            HttpGet httpGet = httpGetter(uriBuilder);

            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();

            if (null != entity) {
                response = EntityUtils.toString(entity, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }
    /** retrieves subsequent page of followers **/
    public String getFollowingsPage(String userId, String nextToken) {
        String uri = String.format("https://api.twitter.com/2/users/%s/following?&pagination_token=%s", userId, nextToken);
        return getFollowersOrFollowings(uri);
    }

    /** retrieves first page of followings **/
    public String getFollowings(String userId) {
        String uri = String.format("https://api.twitter.com/2/users/%s/following", userId);
        return getFollowersOrFollowings(uri);
    }

    /** retrieves subsequent page of followers **/
    public String getFollowersPage(String userId, String nextToken) {
        String uri = String.format("https://api.twitter.com/2/users/%s/followers?&pagination_token=%s", userId, nextToken);
        return getFollowersOrFollowings(uri);
    }

    /** retrieves first page of followers **/
    public String getFollowers(String userId) {
        String uri = String.format("https://api.twitter.com/2/users/%s/followers", userId);
        return getFollowersOrFollowings(uri);
    }
}
