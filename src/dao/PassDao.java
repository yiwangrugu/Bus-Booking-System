package dao;

import model.Passenger;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PassDao {
    public Passenger addPass(Connection con, Passenger pass) throws Exception {
        Passenger pass1 = new Passenger();
        String sql1 = "select * from passenger where userName=? and idno=?";
        PreparedStatement pstmt1 = con.prepareStatement(sql1);
        pstmt1.setString(1, pass.getUserName());
        pstmt1.setString(2, pass.getIdno());
        ResultSet rs = pstmt1.executeQuery();
        boolean exists = rs.next();
        if (exists) {
            pass1.setUserName(rs.getString("userName"));
            pass1.setIdno(rs.getString("idno"));
            pass1.setName(rs.getString("name"));
            pass1.setPhone(rs.getString("tel"));
        }
        rs.close();
        pstmt1.close();

        if (exists) {
            String updateSql = "update passenger set name=?, tel=? where userName=? and idno=?";
            PreparedStatement updateStmt = con.prepareStatement(updateSql);
            updateStmt.setString(1, pass.getName());
            updateStmt.setString(2, pass.getPhone());
            updateStmt.setString(3, pass.getUserName());
            updateStmt.setString(4, pass.getIdno());
            updateStmt.executeUpdate();
            updateStmt.close();

            pass1.setName(pass.getName());
            pass1.setPhone(pass.getPhone());
        } else {
            String insertSql = "insert into passenger(userName,idno,name,tel) values(?,?,?,?)";
            PreparedStatement insertStmt = con.prepareStatement(insertSql);
            insertStmt.setString(1, pass.getUserName());
            insertStmt.setString(2, pass.getIdno());
            insertStmt.setString(3, pass.getName());
            insertStmt.setString(4, pass.getPhone());
            int insertResult = insertStmt.executeUpdate();
            insertStmt.close();

            if (insertResult > 0) {
                pass1 = pass;
            }
        }
        return pass1;
    }

    public List<Passenger> list(Connection con, User user) throws Exception {
        String sql = "select userName,idno,name,tel from passenger where userName=?";
        List<Passenger> passengers = new ArrayList<>();

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, user.getUserName());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Passenger passenger = new Passenger();
                    passenger.setUserName(rs.getString("userName"));
                    passenger.setIdno(rs.getString("idno"));
                    passenger.setName(rs.getString("name"));
                    passenger.setPhone(rs.getString("tel"));
                    passengers.add(passenger);
                }
            }
        }
        return passengers;
    }

    public Passenger getPass(Connection con, String idno) throws Exception {
        Passenger pass = new Passenger();
        String sql = "select * from passenger where idno=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, idno);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            pass.setIdno(rs.getString("idno"));
            pass.setName(rs.getString("name"));
            pass.setPhone(rs.getString("tel"));
        }
        rs.close();
        pstmt.close();
        return pass;
    }

    public int deletePass(Connection con, Passenger passenger) throws Exception {
        String sql = "delete from passenger where userName=? and idno=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, passenger.getUserName());
        pstmt.setString(2, passenger.getIdno());
        int result = pstmt.executeUpdate();
        pstmt.close();
        return result;
    }
}
