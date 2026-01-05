package model;

public class User {
    private int id;
    private String userName;
    private String password;
    private String power;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public User() {
    }

    public User(String userName, String password, String power) {
        this.userName = userName;
        this.password = password;
        this.power = power;
    }
}
