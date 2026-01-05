package dao;

import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDao {
    public User login(Connection con, User user) throws Exception {
        User resultUser = null;
        String sql = "select * from user where userName=? and password=? and power=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, user.getUserName());
        pstmt.setString(2, user.getPassword());
        pstmt.setString(3, user.getPower());

        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            resultUser = new User();
            resultUser.setUserName(rs.getString("userName"));
            resultUser.setPassword(rs.getString("password"));
            resultUser.setPower(rs.getString("power"));
        }
        rs.close();
        pstmt.close();
        return resultUser;
    }

    public boolean isExist(Connection con, User user) throws Exception {
        String sql = "select * from user where userName=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, user.getUserName());
        ResultSet rs = pstmt.executeQuery();
        boolean exists = rs.next() && user.getPower().equals("用户");
        rs.close();
        pstmt.close();
        return exists;
    }

    public int addUser(Connection con, User user) throws Exception {
        if (isExist(con, user)) {
            return -1;
        }
        String sql = "insert into user values(null,?,?,?)";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, user.getUserName());
        pstmt.setString(2, user.getPassword());
        pstmt.setString(3, "用户");
        int result = pstmt.executeUpdate();
        pstmt.close();
        return result;
    }

    public int updatePassword(Connection con, User user, String newPassword) throws Exception {
        String sql = "update user set password=? where userName=? and password=? and power=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, newPassword);
        pstmt.setString(2, user.getUserName());
        pstmt.setString(3, user.getPassword());
        pstmt.setString(4, user.getPower());
        int result = pstmt.executeUpdate();
        pstmt.close();
        return result;
    }
}
