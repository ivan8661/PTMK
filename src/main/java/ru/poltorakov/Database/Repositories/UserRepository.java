package ru.poltorakov.Database.Repositories;

import ru.poltorakov.Database.DatabaseManager;
import ru.poltorakov.Database.Entities.User;
import ru.poltorakov.Util.CustomLogger;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class UserRepository {

    private static Logger LOG = CustomLogger.getFormatterLogger("UserRepository");


    public int save(User user) {
        Connection connection = DatabaseManager.getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Users(first_name, last_name, patronymic, birthday, sex) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setString(1, user.getFirstName());
            preparedStatement.setString(2, user.getLastName());
            preparedStatement.setString(3, user.getPatronymic());
            preparedStatement.setString(4, user.getBirthday());
            preparedStatement.setInt(5, user.getSex());
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOG.info("problem with save user: " + e.getMessage());
            return -1;
        }
    }

    public ArrayList<User> getAllUnique() {
        Connection connection = DatabaseManager.getConnection();
        long start = System.currentTimeMillis();
        LOG.info("query has started... (With a large number of records, the average waiting time may exceed several minutes...)");
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT first_name, last_name, patronymic, birthday, ANY_VALUE(sex) AS sex, TIMESTAMPDIFF(YEAR, birthday, NOW()) AS completed_years " +
                            "FROM Users " +
                            "GROUP BY last_name, first_name, patronymic, birthday " +
                            "HAVING COUNT(*) = 1 " +
                            "ORDER BY last_name, first_name, patronymic"
            );
            ResultSet resultSet = preparedStatement.executeQuery();
            long end = System.currentTimeMillis();
            LOG.info("query took over: " + (end - start) / 1000F + " seconds. Press some button to print users...");
            new Scanner(System.in).nextLine();
            ArrayList<User> userList = fillUserList(resultSet, true);
            connection.close();
            return userList;
        } catch (Exception e) {
            LOG.severe("database error: " + e.getMessage() + " user list will be empty");
            return new ArrayList<>();
        }
    }

    public boolean importFromFile(File file) throws SQLException {
        Connection connection = DatabaseManager.getConnection();
        if (!checkAndSetLoadingFile(connection)) {
            throw new SQLException();
        }
        connection.setAutoCommit(false);
        String path = file.getAbsolutePath();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            path = path.replace('\\', '/');
        }
        LOG.info("start import data from file with users...");
        PreparedStatement preparedStatement = connection.prepareStatement("LOAD DATA LOCAL INFILE '" + path +
                "' INTO TABLE Users FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n' (last_name, first_name, patronymic, birthday, sex)");
        preparedStatement.execute();
        connection.commit();
        connection.close();
        file.delete();
        return true;
    }

    public ArrayList<User> getAllStartingWith(String startWith) {
        Connection connection = DatabaseManager.getConnection();
        long start = System.currentTimeMillis();
        LOG.info("start query for users which start with: " + startWith);
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * " +
                            "FROM Users " +
                            "WHERE last_name LIKE '" + startWith + "%' AND sex=1"
            );
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();
            if(resultSet == null)
                return new ArrayList<>();

            long end = System.currentTimeMillis();
            LOG.info("query took over: " + (end - start) / 1000F + " seconds. Press some button to print users...");
            new Scanner(System.in).nextLine();
            ArrayList<User> userList = fillUserList(resultSet, false);
            connection.close();
            return userList;
        } catch (Exception e) {
            LOG.severe("database error: " + e.getMessage() + " user list will be empty");
            return new ArrayList<>();
        }
    }

    public boolean saveAll(List<User> users) {
        CopyOnWriteArrayList<User> concurrentUsers = new CopyOnWriteArrayList<>(users);
        Integer userAmount = concurrentUsers.size();
        DatabaseManager.getConnection();
        int countBatch = Math.min(100, userAmount / 100);
        int sizeBatch = userAmount / countBatch;
        for (int i = 0; i < countBatch; ++i) {
            final int curIndex = i;
            new Thread(() -> {
                Connection connection = DatabaseManager.getConnection();
                try {
                    User user;
                    connection.setAutoCommit(false);
                    PreparedStatement preparedStatement = connection.prepareStatement(
                            "INSERT INTO Users (first_name, last_name, patronymic, birthday, sex)" +
                                    " VALUES (?, ?, ?, ?, ?)");
                    for (int j = 0; j < sizeBatch; ++j) {
                        user = concurrentUsers.get(curIndex * sizeBatch + j);
                        preparedStatement.setString(1, user.getFirstName());
                        preparedStatement.setString(2, user.getLastName());
                        preparedStatement.setString(3, user.getPatronymic());
                        preparedStatement.setString(4, user.getBirthday());
                        preparedStatement.setInt(5, user.getSex());
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                    connection.commit();
                    connection.close();
                } catch (SQLException e) {
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        throw new RuntimeException("cannot cancel transaction!!!");
                    }
                    throw new RuntimeException("transaction is denied");
                }
            }).start();
        }
        return true;
    }

    private boolean checkAndSetLoadingFile(Connection connection) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SET GLOBAL local_infile = 1");
            preparedStatement.execute();
            LOG.info("successfully changing local infile parameter");
            return true;
        } catch (SQLException e) {
            LOG.severe("unfortunately, there is no enough permission to enable local infile load...");
            return false;
        }
    }

    private ArrayList<User> fillUserList(ResultSet resultSet, Boolean withAge) throws SQLException {
        User user;
        ArrayList<User> userList = new ArrayList<>();
        while (resultSet.next()) {
            user = new User();
            user.setFirstName(resultSet.getString("first_name"));
            user.setLastName(resultSet.getString("last_name"));
            user.setPatronymic(resultSet.getString("patronymic"));
            user.setBirthday(resultSet.getString("birthday"));
            user.setSex(resultSet.getInt("sex"));
            if(withAge)
                user.setAge(resultSet.getInt("completed_years"));
            userList.add(user);
        }
        return userList;
    }
}
