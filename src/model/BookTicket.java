package model;

import java.sql.Date;
import java.sql.Time;

public class BookTicket {
    private int btno;
    private int bno;
    private String idno;
    private java.sql.Date bdate;
    private java.sql.Time btime;
    private String userName;
    private String passengerName;
    private String passengerPhone;

    public int getBtno() {
        return btno;
    }

    public void setBtno(int btno) {
        this.btno = btno;
    }

    public int getBno() {
        return bno;
    }

    public void setBno(int bno) {
        this.bno = bno;
    }

    public Date getBdate() {
        return bdate;
    }

    public void setBdate(Date bdate) {
        this.bdate = bdate;
    }

    public Time getBtime() {
        return btime;
    }

    public void setBtime(Time btime) {
        this.btime = btime;
    }

    public String getIdno() {
        return idno;
    }

    public void setIdno(String idno) {
        this.idno = idno;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerPhone() {
        return passengerPhone;
    }

    public void setPassengerPhone(String passengerPhone) {
        this.passengerPhone = passengerPhone;
    }

    public BookTicket() {
    }

    public BookTicket(int bno, String idno, Date bdate, Time btime, String userName) {
        this.bno = bno;
        this.idno = idno;
        this.bdate = bdate;
        this.btime = btime;
        this.userName = userName;
    }
}
