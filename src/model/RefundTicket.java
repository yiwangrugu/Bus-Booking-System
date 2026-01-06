package model;

import java.sql.Date;
import java.sql.Time;

public class RefundTicket {
    private int btno;
    private int bno;
    private String idno;
    private Date rdate;
    private Time rtime;
    private String staName;
    private String endName;
    private Date date;
    private Time time;

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

    public String getIdno() {
        return idno;
    }

    public void setIdno(String idno) {
        this.idno = idno;
    }

    public Date getRdate() {
        return rdate;
    }

    public void setRdate(Date rdate) {
        this.rdate = rdate;
    }

    public Time getRtime() {
        return rtime;
    }

    public void setRtime(Time rtime) {
        this.rtime = rtime;
    }

    public String getStaName() {
        return staName;
    }

    public void setStaName(String staName) {
        this.staName = staName;
    }

    public String getEndName() {
        return endName;
    }

    public void setEndName(String endName) {
        this.endName = endName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public RefundTicket() {
    }

    public RefundTicket(int bno, String idno, Date rdate, Time rtime) {
        this.bno = bno;
        this.idno = idno;
        this.rdate = rdate;
        this.rtime = rtime;
    }

    public RefundTicket(int bno, int btno, String idno, Date rdate, Time rtime) {
        this.bno = bno;
        this.btno = btno;
        this.idno = idno;
        this.rdate = rdate;
        this.rtime = rtime;
    }
}
