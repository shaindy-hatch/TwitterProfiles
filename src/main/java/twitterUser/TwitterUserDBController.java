package twitterUser;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Use closeDBConnection function to close connection established with instantiation of TwitterUserDBController */
public class TwitterUserDBController {

    public Connection connection;

    public TwitterUserDBController(){
        connection = connectToDB();
    }

    public Connection connectToDB() {

        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/twitterprofiles",
                    "root",
                    "sqlPass1!");

            connection.setAutoCommit(false);
        } catch (SQLException ex) {
            closeDBConnection(connection);
            ex.printStackTrace();
        }
        return connection;

    }

    public void closeDBConnection(Connection connection) {
        try {
            connection.setAutoCommit(true);
            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public TwitterUser getDBUser(String userId) {
        TwitterUser user  = new TwitterUser();

        String sql = "SELECT * " +
                "FROM users " +
                "WHERE userId = ? ";

        try (PreparedStatement preparedStmt = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE)) {
            preparedStmt.setString(1, userId);

            ResultSet rs = preparedStmt.executeQuery();

            if(!rs.first())
                return null;//get first record

                user.setId(rs.getString("userId"));
                user.setUsername(rs.getString("username"));
                user.setName(rs.getString("name"));
                user.setLocation(rs.getString("location"));
                user.setProfile_image_url(rs.getString("profile_image_url"));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return user;
    }

    public List<String> getDBUserIds() {
        List<String> users = new ArrayList<>();
        try{
            String sql = "SELECT userId "
                    + "FROM users";

            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery(sql);

                String userId;
                while (result.next()) {
                    userId = result.getString("userId");
                    users.add(userId);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
    public List<TwitterUser> getDBUsers() {
        List<TwitterUser> users = new ArrayList<>(100);
        try{
            String sql = "SELECT userId, username, name, profile_image_url, location "
                    + "FROM users";

            try (Statement statement = connection.createStatement()) {
                ResultSet result = statement.executeQuery(sql);

                String userId, username, name, profile_image_url, location;
                while (result.next()) {
                    userId = result.getString("userId");
                    username = result.getString("username");
                    name = result.getString("name");
                    profile_image_url = result.getString("profile_image_url");
                    location = result.getString("location");

                    users.add(new TwitterUser(userId, username, name, profile_image_url, location));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
    public void saveUserToDB(TwitterUser user) {

        try{
            connection.setAutoCommit(false);

            String sql = "INSERT INTO USERS (userId, name, username, profile_image_url, location)"
                    + " VALUES (?, ?, ?, ?, ?)"
                    + "ON DUPLICATE KEY UPDATE"
                    + " userId = VALUES(userId), name = VALUES(name), username = VALUES(username),"
                    + "profile_image_url = VALUES(profile_image_url), location = VALUES(location);";

            try (PreparedStatement preparedStmt = connection.prepareStatement(sql)) {
                preparedStmt.setString(1, user.getId());
                preparedStmt.setString(2, user.getName());
                preparedStmt.setString(3, user.getUsername());
                preparedStmt.setString(4, user.getProfile_image_url());
                preparedStmt.setString(5, user.getLocation());

                preparedStmt.execute();
                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                System.out.printf("\n----not saved %s\n", user.getId());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void saveFollowerToDB(String userId, String followerId) {
        try{
            connection.setAutoCommit(false);

            String sql = "INSERT IGNORE INTO connections (userId, followerId)"
                    + " VALUES (?, ?)";

            try (PreparedStatement preparedStmt = connection.prepareStatement(sql)) {

                preparedStmt.setString(1, userId);
                preparedStmt.setString(2, followerId);
                preparedStmt.execute();
                connection.commit();

            } catch (SQLException ex) {
                connection.rollback();
                ex.printStackTrace();
                System.out.printf("\n----not saved %s and %s\n", userId, followerId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveFollowers(String userId, List<TwitterUser> followers) {
        if (followers == null)
            return;

        for (TwitterUser user : followers) {
            saveUserToDB(user);
            saveFollowerToDB(userId, user.getId());
        }
    }

    public void saveFollowings(String userId, List<TwitterUser> followings) {
        if (followings == null)
            return;

        for (TwitterUser user : followings) {
            saveUserToDB(user);
            saveFollowerToDB(user.getId(), userId);
        }
    }
}
