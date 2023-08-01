package ru.poltorakov.Database.Entities;

public class User {

    private String firstName;
    private String lastName;
    private String patronymic;
    private String birthday;
    private Integer sex;
    private Integer age;

    public User(String firstName, String lastName, String birthday, Integer sex) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthday = birthday;
        this.sex = sex;
    }

    public User(String firstName, String lastName, String patronymic, String birthday, Integer sex) {
        this(firstName, lastName, birthday, sex);
        this.patronymic = patronymic;

    }

    public User(String firstName, String lastName, String patronymic, String birthday, Integer sex, Integer age) {
        this(firstName, lastName, patronymic, birthday, sex);
        this.age = age;
    }



    public User() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPatronymic() {
        return patronymic;
    }

    public void setPatronymic(String patronymic) {
        this.patronymic = patronymic;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return lastName + " " + firstName + " " + (patronymic == null ? "" : patronymic)
                + " " + birthday + " " + (sex == 1 ? "Male" : "Female") + " " + (age == null ? "" : age);
    }
}
