package dao;

import model.BookTicket;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BookDao {
    public BookTicket addBook(Connection con, BookTicket bt) throws Exception {
        BookTicket bt1 = new BookTicket();
        String sql = "insert into book_ticket(bno,idno,bdate,btime,userName,passengerName,passengerPhone) values(?,?,?,?,?,?,?)";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, bt.getBno());
        pstmt.setString(2, bt.getIdno());
        pstmt.setDate(3, new java.sql.Date(bt.getBdate().getTime()));
        pstmt.setTime(4, bt.getBtime());
        pstmt.setString(5, bt.getUserName());
        pstmt.setString(6, bt.getPassengerName());
        pstmt.setString(7, bt.getPassengerPhone());
        if (pstmt.executeUpdate() > 0) {
            bt1 = bt;
        }
        return bt1;
    }

    public BookTicket getBook(Connection con, int btno) throws Exception {
        BookTicket bt = null;
        String sql = "select * from book_ticket where btno=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, btno);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            bt = new BookTicket();
            bt.setBtno(rs.getInt("btno"));
            bt.setBno(rs.getInt("bno"));
            bt.setUserName(rs.getString("userName"));
            bt.setIdno(rs.getString("idno"));
            bt.setBdate(rs.getDate("bdate"));
            bt.setBtime(rs.getTime("btime"));
        }
        rs.close();
        pstmt.close();
        return bt;
    }

    public int refund(Connection con, BookTicket bt) throws Exception {
        String sql = "delete from book_ticket where btno=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, bt.getBtno());
        int result = pstmt.executeUpdate();
        pstmt.close();
        return result;
    }

    public ResultSet list(Connection con, User user) throws Exception {
        String sql = "select bt.btno,bt.bno,b.staName,b.endName,b.date,b.time,b.price,bt.bdate,bt.btime,bt.passengerName,bt.passengerPhone "
                +
                "from book_ticket bt left join user on bt.userName=user.userName left join bus b on bt.bno=b.bno "
                +
                "where user.userName=? order by bt.btno ASC ";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, user.getUserName());
        return pstmt.executeQuery();
    }

    public ResultSet getBookTicketsByUserName(Connection con, String userName) throws Exception {
        String sql = "select bt.btno,bt.bno,b.staName,b.endName,b.date,b.time,b.price,bt.bdate,bt.btime,bt.passengerName,bt.passengerPhone "
                +
                "from book_ticket bt left join bus b on bt.bno=b.bno "
                +
                "where bt.userName=? order by bt.btno ASC ";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setString(1, userName);
        return pstmt.executeQuery();
    }

    public boolean addBookTicket(Connection con, BookTicket bt) throws Exception {
        String sql = "insert into book_ticket(bno,idno,bdate,btime,userName,passengerName,passengerPhone) values(?,?,?,?,?,?,?)";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, bt.getBno());
        pstmt.setString(2, bt.getIdno());
        pstmt.setDate(3, new java.sql.Date(bt.getBdate().getTime()));
        pstmt.setTime(4, bt.getBtime());
        pstmt.setString(5, bt.getUserName());
        pstmt.setString(6, bt.getPassengerName());
        pstmt.setString(7, bt.getPassengerPhone());

        return pstmt.executeUpdate() > 0;
    }

    public int getno(Connection con, int bno) throws Exception {
        String sql = "select count(*) from book_ticket where bno = ?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, bno);
        ResultSet rs = pstmt.executeQuery();
        int result = 0;
        while (rs.next()) {
            result = rs.getInt(1) + 1;
        }
        rs.close();
        pstmt.close();
        return result;
    }

    public ResultSet list2(Connection con, int selbno) throws Exception {
        String sql = "select distinct b.bno,bt.btno,bt.bdate,bt.btime,bt.idno,bt.passengerName,bt.passengerPhone " +
                "from bus b left join book_ticket bt on b.bno=bt.bno " +
                "where b.bno=? order by bt.btno ASC ";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, selbno);
        return pstmt.executeQuery();
    }

    public ResultSet getAllBookTickets(Connection con) throws Exception {
        String sql = "select bt.btno,bt.bno,b.staName,b.endName,b.date,b.time,b.price,bt.bdate,bt.btime,bt.passengerName,bt.passengerPhone "
                +
                "from book_ticket bt left join bus b on bt.bno=b.bno "
                +
                "order by bt.btno ASC ";
        PreparedStatement pstmt = con.prepareStatement(sql);
        return pstmt.executeQuery();
    }
}
