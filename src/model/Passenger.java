package model;

public class Passenger {
    private String userName;
    private String idno;
    private String name;
    private String phone;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIdno() {
        return idno;
    }

    public void setIdno(String idno) {
        this.idno = idno;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Passenger() {
    }

    public Passenger(String userName, String idno, String name, String phone) {
        this.userName = userName;
        this.idno = idno;
        this.name = name;
        this.phone = phone;
    }
}
