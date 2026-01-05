package model;

import java.sql.Date;
import java.sql.Time;

public class Bus {
    private int bno;
    private String staName;
    private String endName;
    private Date date;
    private Time time;
    private float price;
    private int seat;

    public int getBno() {
        return bno;
    }

    public void setBno(int bno) {
        this.bno = bno;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getEndName() {
        return endName;
    }

    public void setEndName(String endName) {
        this.endName = endName;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public int getSeat() {
        return seat;
    }

    public void setSeat(int seat) {
        this.seat = seat;
    }

    public String getStaName() {
        return staName;
    }

    public void setStaName(String staName) {
        this.staName = staName;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public Bus() {
    }

    public Bus(String staName, String endName, Date date, Time time, float price, int seat) {
        this.staName = staName;
        this.endName = endName;
        this.date = date;
        this.time = time;
        this.price = price;
        this.seat = seat;
    }
}
