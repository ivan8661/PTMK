package ru.poltorakov.Services;

import ru.poltorakov.Database.DatabaseManager;
import ru.poltorakov.Database.Entities.User;
import ru.poltorakov.Database.Repositories.UserRepository;
import ru.poltorakov.TestTask;
import ru.poltorakov.Util.CustomLogger;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class UserService {

    private static final Logger LOG = CustomLogger.getFormatterLogger("UserService");

    public User createUser(String[] parameters) {
       User user = parseUser(parameters);
       if(new UserRepository().save(user) == -1)
           return null;
       else
            LOG.info("user: " + user + " was successfully added");
       return user;
    }

    public void printUniqueUsers() {
        ArrayList<User> users = new UserRepository().getAllUnique();
        System.out.println("full name \t\t\t\t birthday \t\t sex \t\t completed age");
        for(User user : users){
            System.out.println(user);
        }
        System.out.println("Total amount of unique users: " + users.size());
    }

    public void generateMillionUsers() {
        try {
            List<User> users = generateUsers(1000000, "");
            UserRepository userRepository = new UserRepository();
            LOG.info("users are generating...");
            File userFile = new File("users.txt");
            userFile.delete();
            BufferedWriter writer = new BufferedWriter(new FileWriter(userFile.getPath(), true));
            StringBuilder stringBuilder;
            for(User user : users){
                stringBuilder = new StringBuilder();
                stringBuilder.append(user.getFirstName()).append(";")
                                .append(user.getLastName()).append(";")
                                .append(user.getPatronymic()).append(";")
                                .append(user.getBirthday()).append(";")
                                .append(user.getSex());
                writer.write(stringBuilder.toString());
                writer.newLine();
            }
            writer.close();
            LOG.info("operation has finished");
            try {
                userRepository.importFromFile(userFile);
                LOG.info("the import of 1 million users is completed");
            } catch (SQLException e) {
                if(e.getMessage().contains("doesn't exist")) {
                    LOG.severe(e.getMessage());
                    return;
                }
                LOG.warning("problem with absolute path in ur OS or DBMS settings ...\nstart packet transfer, average time of waiting: 1-2 min..." + e.getMessage());
                userRepository.saveAll(users);
                LOG.info("the import of 1 million users is completed");
            }
        } catch (IOException e) {
            throw new RuntimeException("file with names, patronymics or surnames doesn't exist or is open, try again!!!" + e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("invalid path in sources!!!");
        }
    }

    public void printFUsers() {
        ArrayList<User> users = new UserRepository().getAllStartingWith("F");
        System.out.println("full name \t\t\t\t\t\t birthday \t\t sex \t\t completed age");
        for(User user : users){
            System.out.println(user);
        }
        System.out.println("Total amount of unique users: " + users.size());
    }

    public void checkIndexation() {
        Connection connection = DatabaseManager.getConnection();
        try {
            LOG.info("query without indexation:");
            printFUsers();
            LOG.info("creating index...");
            PreparedStatement preparedStatement = connection.prepareStatement("CREATE INDEX idx_users_last_name ON Users (last_name(1)) USING BTREE");
            preparedStatement.execute();
            LOG.info("query with indexation:");
            printFUsers();
            LOG.info("returning to previous state (removing index)...");
            preparedStatement = connection.prepareStatement("DROP INDEX idx_users_last_name ON Users");
            preparedStatement.execute();
        } catch (SQLException e) {
            LOG.info("error in index set up: " + e.getMessage());
        }
    }

    public List<User> generateUsers(Integer amount, String startWithSurname) throws URISyntaxException, IOException {
        ClassLoader cl = UserService.class.getClassLoader();
        List<String> firstNames = readFromStream(cl.getResourceAsStream("names.txt"));
        List<String> lastNames = readFromStream(cl.getResourceAsStream("surnames.txt"));
        List<String> patronymics = readFromStream(cl.getResourceAsStream("patronymics.txt"));
        String firstName, lastName, patronymic, birthday;
        int sex;
        Random rand = new Random(System.currentTimeMillis());
        ArrayList<User> users = new ArrayList<>();
        for(int i = 0; i < amount; ++i){
            firstName = firstNames.get(rand.nextInt(firstNames.size()));
            lastName = lastNames.get(rand.nextInt(lastNames.size()));
            while(lastName.equals("") || !lastName.startsWith(startWithSurname)){
                lastName = lastNames.get(rand.nextInt(lastNames.size()));
            }
            while(firstName.equals("")){
                firstName = firstNames.get(rand.nextInt(firstNames.size()));
            }
            patronymic = patronymics.get(rand.nextInt(patronymics.size()));
            birthday = LocalDate.now().minusYears(rand.nextInt(110)).minusMonths(rand.nextInt(12)).minusDays(rand.nextInt(30)).format(DateTimeFormatter.ISO_DATE);
            sex = rand.nextInt(2)+1;
            if(sex == 2) {
                if(patronymic.endsWith("vich"))
                    patronymic = (patronymic.substring(0, patronymic.length()-4) + "vna");
                else
                    patronymic = patronymic + "na";
            }
            users.add(new User(firstName, lastName, patronymic, birthday, sex));
        }
        return users;
    }

    public void generate100F() {
        try {
            List<User> users = generateUsers(100, "F");
            new UserRepository().saveAll(users);
        } catch (URISyntaxException | IOException e) {
            LOG.severe("error with 100 users generating...");
        }
    }

    private User parseUser(String[] parameters) {
        ArrayList<String> fullName = new ArrayList<>();
        String birthday;
        int sex, i;
        for(i = 1; i < parameters.length-2 && !parameters[i].contains("[0-9]+"); ++i){
            fullName.add(parameters[i]);
        }
        if(i <= 2){
            throw new RuntimeException("second or first name are missed, try again!!!");
        }
        if(i < parameters.length){
            if(isValidDate(parameters[i]))
                birthday = parameters[i];
            else
                throw new RuntimeException("invalid birthday... \nIt would be YYYY-MM-DD and can't be in the future, try again!!!");
            i++;
        } else {
            throw new RuntimeException("birthday and sex are missed, try again!!!");
        }
        if(i < parameters.length) {
            sex = parseSex(parameters[i]);
        } else {
            throw new RuntimeException("sex are missed, try again!!!");

        }
        return new User(
                fullName.get(1),
                fullName.get(0),
                fullName.size() > 2 ? fullName.get(2) : null,
                birthday,
                sex
        );
    }

    private int parseSex(String sex) {
        switch (sex.toLowerCase()){
            case "":
            case "0":
            case "not known": {
                return 0;
            }
            case "мужской":
            case "male":
            case "m":
            case "м":
            case "1": {
                return 1;
            }
            case "женский":
            case "female":
            case "f":
            case "ж":
            case "2": {
                return 2;
            }
            default:
                throw new RuntimeException("the sex is incorrect, try again!!!");
            }
    }

    private boolean isValidDate(String date) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE;
        try {
            LocalDate localDate = LocalDate.parse(date, dateTimeFormatter);
            if(localDate.isAfter(LocalDate.now())){
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private ArrayList<String> readFromStream(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        ArrayList<String> res = new ArrayList<>();
        String line;
        try {
            while((line = bufferedReader.readLine()) != null){
                res.add(line);
            }
            bufferedReader.close();
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("problem with inputStream...");
        }
        return res;
    }
}
