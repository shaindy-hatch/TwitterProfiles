package prospect;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Use closeDBConnection function to close connection established with instantiation of ProspectDBController */
public class ProspectDBController {

    private Properties properties;
    public Session session;
    public Connection connection;

    public ProspectDBController() {
        properties = loadProperties();
        session = sshConnect(properties, 3126, 3306);
        connection = connectToDB(session);
    }

    public List<Prospect> nonTwitterProspects(){

        List<Prospect> knownUsers = new ArrayList<>(1000);

        String sql = "SELECT id, name_first, name_last, location_locality, location_region "
                + "FROM prospects "
                + "WHERE twitter_id IS NULL";
        //+ "LIMIT 100";

        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(sql);

            String id, firstName, lastName, region, locality;
            while (result.next()) {
                id = result.getString("id");
                firstName = result.getString("name_first");
                lastName = result.getString("name_last");
                region = result.getString("location_region");
                locality = result.getString("location_locality");

                knownUsers.add(new Prospect(id,null, firstName, lastName, region, locality));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return knownUsers;
    }
    public List<Prospect> getHatchUsers(String whereClause) {

        List<Prospect> knownUsers = new ArrayList<>(100);
        String sql = "SELECT id, twitter_id, name_first, name_last, location_locality, location_region "
                + "FROM prospects "
                + whereClause;

        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(sql);

            String id, twitterId, firstName, lastName, region, locality;
            while (result.next()) {
                id = result.getString("id");
                twitterId = result.getString("twitter_id");
                firstName = result.getString("name_first");
                lastName = result.getString("name_last");
                region = result.getString("location_region");
                locality = result.getString("location_locality");

                knownUsers.add(new Prospect(id, twitterId, firstName, lastName, region, locality));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return knownUsers;
    }


    public List<String> getHatchUserTwitterIds(String whereClause) {

        List<String> knownUsers = new ArrayList<>(100);
        String sql = "SELECT id, twitter_id, name_first, name_last, location_locality, location_region "
                + "FROM prospects "
                + whereClause
                + "LIMIT 1100";

        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(sql);

            String id, twitterId, firstName, lastName, region, locality;
            while (result.next()) {
                twitterId = result.getString("twitter_id");
                firstName = result.getString("name_first");
                lastName = result.getString("name_last");
                region = result.getString("location_region");
                locality = result.getString("location_locality");

                knownUsers.add(twitterId);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return knownUsers;
    }

    public List<String> getHatchUsersWithID() {
        String whereClause = "WHERE twitter_id IS NOT NULL ";
        return getHatchUserTwitterIds(whereClause);
    }

    public List<String> getHatchUsersWithUsername() {
        String whereClause = "WHERE twitter_username IS NOT NULL AND twitter_id IS NULL";
        return getHatchUserTwitterIds(whereClause);
    }

    public Prospect getHatchUser(String twitterId) {

        Prospect user = new Prospect();
        String sql = "SELECT id, twitter_id, name_first, name_last, location_locality, location_region "
                + "FROM prospects "
                + "WHERE twitter_id LIKE ?;";

        try (PreparedStatement preparedStmt = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_UPDATABLE)) {
            preparedStmt.setString(1, twitterId);

            ResultSet rs = preparedStmt.executeQuery();

            if (!rs.first())
                return null; //get first record

            user.setId(rs.getString("id"));
            user.setTwitterId(rs.getString("twitter_id"));
            user.setFirstName(rs.getString("name_first"));
            user.setLastName(rs.getString("name_last"));
            user.setRegionLocation(rs.getString("location_region"));
            user.setLocalityLocation(rs.getString("location_locality"));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return user;
    }

    /** properties for connecting to DB from config.properties file **/
    public Properties loadProperties() {
        String propertyFileLocation = "src/main/config.properties";
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(propertyFileLocation)) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public Connection connectToDB(Session session) {

        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:" + 3126 + "/db_hatch",
                    properties.getProperty("dbUser"),
                    properties.getProperty("dbPassword"));

            connection.setAutoCommit(false);
        } catch (SQLException ex) {
            closeDBConnection(connection, session);
            ex.printStackTrace();
        }
        return connection;

    }

    public static void closeDBConnection(Connection connection, Session session) {
        try {
            connection.setAutoCommit(true);
            connection.close();
            session.disconnect();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Session sshConnect(Properties properties, int localPort, int remotePort) {

        Session session = null;

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(properties.getProperty("privateKeyPath"), properties.getProperty("keyPassphrase"));

            session = jsch.getSession(properties.getProperty("sshUser"), properties.getProperty("sshHost"), 22);
            session.setPassword(properties.getProperty("sshPassword"));

            final Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
            session.setPortForwardingL(localPort, properties.getProperty("dbHostName"), remotePort);
        } catch (JSchException ex) {
            ex.printStackTrace();
        }
        return session;
    }

    public List<Prospect> prospectsFromJson() {
        Gson gson = new Gson();
        try {
            FileReader reader = new FileReader("prospects.json");
            Type type = new TypeToken<ArrayList<Prospect>>() {
            }.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
