package dao;

import Util.StringUtil;
import model.Bus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BusDao {
    public Bus addBus(Connection con, Bus bus) throws Exception {
        Bus resultBus = null;
        String sql = "insert into bus(staName,endName,date,time,price,seat) values(?,?,?,?,?,?)";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, bus.getStaName());
        pstmt.setString(2, bus.getEndName());
        pstmt.setDate(3, bus.getDate());
        pstmt.setTime(4, bus.getTime());
        pstmt.setFloat(5, bus.getPrice());
        pstmt.setInt(6, bus.getSeat());
        if (pstmt.executeUpdate() > 0) {
            resultBus = bus;
        }
        return resultBus;
    }

    public ResultSet list(Connection con, Bus bus, String power, boolean showEnded) throws Exception {
        StringBuilder sql;
        if (power.equals("管理员"))
            sql = new StringBuilder(
                    "SELECT b.bno, b.staName, b.endName, b.date, b.time,b.price,b.seat, COUNT(bt.btno) AS booked  FROM bus b LEFT JOIN book_ticket bt ON b.bno = bt.bno WHERE 1=1 ");
        else
            sql = new StringBuilder(
                    "SELECT b.bno, b.staName, b.endName, b.date, b.time,b.price,(b.seat - IFNULL(COUNT(bt.btno), 0)) AS res_seat FROM bus b LEFT JOIN book_ticket bt ON b.bno = bt.bno WHERE 1=1 ");

        if (showEnded) {
            sql.append(" AND (b.date < CURRENT_DATE() OR (b.date = CURRENT_DATE() AND b.time < CURRENT_TIME()))");
        } else {
            sql.append(" AND (b.date > CURRENT_DATE() OR (b.date = CURRENT_DATE() AND b.time > CURRENT_TIME()))");
        }

        if (bus.getBno() != 0) {
            sql.append(" AND b.bno = ?");
        }

        if (bus.getDate() != null) {
            sql.append(" AND b.date = ?");
        }

        if (StringUtil.isNotEmpty(bus.getStaName())) {
            sql.append(" AND b.staName LIKE ?");
        }

        if (StringUtil.isNotEmpty(bus.getEndName())) {
            sql.append(" AND b.endName LIKE ?");
        }
        sql.append(" GROUP BY b.bno, b.staName, b.endName, b.date, b.time, b.price ORDER BY b.bno;");

        PreparedStatement pstmt = con.prepareStatement(sql.toString());

        int index = 1;
        if (bus.getBno() != 0) {
            pstmt.setInt(index++, bus.getBno());
        }

        if (bus.getDate() != null) {
            pstmt.setDate(index++, bus.getDate());
        }

        if (StringUtil.isNotEmpty(bus.getStaName())) {
            pstmt.setString(index++, "%" + bus.getStaName() + "%");
        }

        if (StringUtil.isNotEmpty(bus.getEndName())) {
            pstmt.setString(index++, "%" + bus.getEndName() + "%");
        }

        return pstmt.executeQuery();
    }

    public ResultSet list(Connection con, Bus bus, String power) throws Exception {
        return list(con, bus, power, false);
    }

    public int delete(Connection con, Bus bus) throws Exception {
        String sql = "delete from bus where bno=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, bus.getBno());
        return pstmt.executeUpdate();
    }

    public int update(Connection con, Bus bus) throws Exception {
        String sql = "update bus set staName=?,endName=?,date=?,time=?,price=?,seat=? where bno=?;";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, bus.getStaName());
        pstmt.setString(2, bus.getEndName());
        pstmt.setDate(3, bus.getDate());
        pstmt.setTime(4, bus.getTime());
        pstmt.setFloat(5, bus.getPrice());
        pstmt.setInt(6, bus.getSeat());
        pstmt.setInt(7, bus.getBno());
        return pstmt.executeUpdate();
    }

    public Bus getBus(Connection con, int bno) {
        Bus bus = null;
        try {
            String sql = "select * from bus where bno=?";
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, bno);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                bus = new Bus();
                bus.setBno(rs.getInt("bno"));
                bus.setStaName(rs.getString("staName"));
                bus.setEndName(rs.getString("endName"));
                bus.setDate(rs.getDate("date"));
                bus.setTime(rs.getTime("time"));
                bus.setPrice(rs.getFloat("price"));
                bus.setSeat(rs.getInt("seat"));
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return bus;
    }
}
