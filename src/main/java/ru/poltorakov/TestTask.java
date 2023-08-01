package ru.poltorakov;

import ru.poltorakov.Database.DatabaseManager;
import ru.poltorakov.Services.UserService;
import ru.poltorakov.Util.CustomLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class TestTask {

        static final String server_url;
        static final String login;
        static final String password;

        private static Logger LOG = CustomLogger.getFormatterLogger("TestTask");

        static {
            Properties properties = new Properties();
            try {
                InputStream in;
                if(Files.exists(Paths.get("database.properties"))){
                    in = new FileInputStream("database.properties");
                } else {
                    in = TestTask.class.getClassLoader().getResourceAsStream("database.properties");
                }
                properties.load(in);
            } catch (IOException e) {
                LOG.severe("error with opening database.properties...");
            }
            server_url = properties.getProperty("jdbc.url");
            login = properties.getProperty("jdbc.username");
            password = properties.getProperty("jdbc.password");
            DatabaseManager.setSettings(server_url, login, password, "PTMK");
        }

    public static void main(String[] args) {
        switch (args[0]) {
            case "1": {
                DatabaseManager.createTable();
                break;
            }
            case "2": {
                new UserService().createUser(args);
                break;
            }
            case "3": {
                new UserService().printUniqueUsers();
                break;
            }
            case "4": {
                UserService userService = new UserService();
                userService.generateMillionUsers();
                userService.generate100F();
                break;
            }
            case "5": {
                new UserService().printFUsers();
                break;
            }
            case "6": {
                new UserService().checkIndexation();
                break;
            }
            case "7": {
                DatabaseManager.dropDatabases("PTMK");
                break;
            }
        }
    }
}
