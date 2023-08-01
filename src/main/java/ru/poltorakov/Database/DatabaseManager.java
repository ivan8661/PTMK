package ru.poltorakov.Database;

import org.apache.commons.dbcp2.BasicDataSource;
import ru.poltorakov.Services.UserService;
import ru.poltorakov.TestTask;
import ru.poltorakov.Util.CustomLogger;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

public class DatabaseManager {

    private static String server_url;
    private static String user;
    private static String password;
    private static String dbName;
    private static BasicDataSource dataSource;

    private final static Logger LOG = CustomLogger.getFormatterLogger("DatabaseManager");

    public static void setSettings(String url, String user, String password, String dbName) {
        server_url = url;
        DatabaseManager.user = user;
        DatabaseManager.password = password;
        DatabaseManager.dbName = dbName;
        dataSource = refreshDataSource(url+dbName, user, password);
    }

    public static Connection getConnection() {
        if(dataSource != null) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                checkOrCreateDatabase(dbName);
                try {
                    return dataSource.getConnection();
                } catch (SQLException ex) {
                        return customDatabaseSettings();
                    }
                }
            } try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            return customDatabaseSettings();
        }
    }

    public static Connection customDatabaseSettings() {
        LOG.warning("cannot connect to default local MySQL-server: " + dataSource.getUrl() + " login: " + dataSource.getUsername() + " password: " + dataSource.getPassword() +
                " press any button to continue and set up ur own Database settings...");
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        String login, password;
        LOG.info("input user name of mySQL server: ");
        login = sc.nextLine();
        LOG.info("input password of mySQL server: ");
        password = sc.nextLine();
        updateDatabaseProperties(server_url, login, password, dbName);
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            checkOrCreateDatabase(dbName);
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void checkOrCreateDatabase(String dbName) {
        dataSource.setUrl(server_url);
        try {
            Connection connection = dataSource.getConnection();
            connection.prepareStatement("CREATE DATABASE IF NOT EXISTS " + dbName).execute();
            LOG.info("Database " + dbName + " (" + connection.getMetaData().getURL() + ") was successfully created");
            connection.close();
            dataSource = refreshDataSource(dataSource.getUrl()+dbName, user, password);
        } catch (SQLException e) {
            dataSource = refreshDataSource(dataSource.getUrl()+dbName, user, password);
        }
        }

    public static void updateDatabaseProperties(String url, String user, String password, String dbName) {
        Properties properties = new Properties();
        try {
            InputStream in = TestTask.class.getClassLoader().getResourceAsStream("database.properties");
            properties.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setSettings(url, user, password, dbName);
        properties.setProperty("jdbc.url", url);
        properties.setProperty("jdbc.username", user);
        properties.setProperty("jdbc.dbName", dbName);
        properties.setProperty("jdbc.password", password);
        try {

            File customSetting = new File("database.properties");
            if(!customSetting.createNewFile()){
                LOG.info("there is problem with writing database.properties in tmp file...");
                return;
            }
            OutputStream outputStream = Files.newOutputStream(Paths.get("database.properties"));
            properties.store(outputStream, "database.properties was modified in:");
            outputStream.close();
            LOG.info("successful record database.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createTable() {
        Connection connection = getConnection();
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Users " +
                            "(id INTEGER NOT NULL AUTO_INCREMENT," +
                            "last_name VARCHAR(255) NOT NULL," +
                            "first_name VARCHAR(255) NOT NULL," +
                            "patronymic VARCHAR(255)," +
                            "birthday DATE NOT NULL," +
                            "sex TINYINT NOT NULL," +
                            "PRIMARY KEY(id))"
            );
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Table \"user\" was successfully created");
    }

    public static void dropDatabases(String dbName) {
        Connection connection = getConnection();
        try {
            PreparedStatement statement = connection.prepareStatement("DROP DATABASE " + dbName);
            statement.execute();
            connection.close();
            LOG.info("database " + dbName + " was successfully dropped");
        } catch (SQLException e) {
            LOG.severe("Database " + dbName + " doesn't exist!!!");
        }
    }

    private static BasicDataSource refreshDataSource(String url, String user, String password) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(user);
        basicDataSource.setPassword(password);
        basicDataSource.setMaxIdle(150);
        basicDataSource.setInitialSize(75);
        basicDataSource.setMinIdle(20);
        basicDataSource.setConnectionProperties("allowLoadLocalInfile=TRUE");
        return basicDataSource;
    }



}
