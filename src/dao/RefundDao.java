package dao;

import model.RefundTicket;

import java.sql.Connection;
import java.sql.ResultSet;

public class RefundDao {
    public RefundTicket addRefund(Connection con, RefundTicket rt) throws Exception {
        RefundTicket refundTicket = null;

        String sql = "insert into refund_ticket(btno,userName,bno,idno,rdate,rtime,staName,endName,date,time,passengerName,passengerPhone,price,refundAmount) values(?,?,?,?,CURDATE(),CURTIME(),?,?,?,?,?,?,?,?)";
        java.sql.PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, rt.getBtno());
        ps.setString(2, rt.getUserName());
        ps.setInt(3, rt.getBno());
        ps.setString(4, rt.getIdno());
        ps.setString(5, rt.getStaName());
        ps.setString(6, rt.getEndName());
        ps.setDate(7, rt.getDate());
        ps.setTime(8, rt.getTime());
        ps.setString(9, rt.getPassengerName());
        ps.setString(10, rt.getPassengerPhone());
        ps.setFloat(11, rt.getPrice());
        ps.setFloat(12, rt.getRefundAmount());
        int result = ps.executeUpdate();
        ps.close();
        if (result > 0) {
            refundTicket = rt;
        }
        return refundTicket;
    }

    public ResultSet list(Connection con) throws Exception {
        ResultSet rs = null;
        String sql = "select rt.btno,rt.bno,COALESCE(rt.date,b.date) as date,rt.rdate,rt.rtime,rt.idno,rt.passengerName,rt.passengerPhone,COALESCE(rt.staName,b.staName) as staName,COALESCE(rt.endName,b.endName) as endName,b.price,COALESCE(rt.time,b.time) as time,rt.refundAmount from refund_ticket rt left join bus b on rt.bno=b.bno";
        java.sql.PreparedStatement ps = con.prepareStatement(sql);
        rs = ps.executeQuery();
        return rs;
    }

    public ResultSet search(Connection con, String keyword) throws Exception {
        ResultSet rs = null;
        String sql = "select rt.btno,rt.bno,COALESCE(rt.date,b.date) as date,rt.rdate,rt.rtime,rt.idno,rt.passengerName,rt.passengerPhone,COALESCE(rt.staName,b.staName) as staName,COALESCE(rt.endName,b.endName) as endName,b.price,COALESCE(rt.time,b.time) as time,rt.refundAmount from refund_ticket rt left join bus b on rt.bno=b.bno where rt.btno like ? or b.bno like ?";
        java.sql.PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ps.setString(2, "%" + keyword + "%");
        rs = ps.executeQuery();
        return rs;
    }
}
