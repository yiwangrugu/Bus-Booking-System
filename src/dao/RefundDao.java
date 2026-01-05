package dao;

import model.RefundTicket;

import java.sql.Connection;
import java.sql.ResultSet;

public class RefundDao {
    public RefundTicket addRefund(Connection con, RefundTicket rt) throws Exception {
        RefundTicket refundTicket = null;
        String sql = "insert into refund_ticket(bno,btno,idno,sno,rdate,rtime,staName,endName,date,time) values(?,?,?,?,?,?,?,?,?,?)";
        java.sql.PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, rt.getBno());
        ps.setInt(2, rt.getBtno());
        ps.setString(3, rt.getIdno());
        ps.setInt(4, rt.getSno());
        ps.setDate(5, new java.sql.Date(rt.getRdate().getTime()));
        ps.setTime(6, rt.getRtime());
        ps.setString(7, rt.getStaName());
        ps.setString(8, rt.getEndName());
        ps.setDate(9, rt.getDate());
        ps.setTime(10, rt.getTime());
        int result = ps.executeUpdate();
        ps.close();
        if (result > 0) {
            refundTicket = rt;
        }
        return refundTicket;
    }

    public ResultSet list(Connection con) throws Exception {
        ResultSet rs = null;
        String sql = "select rt.btno,rt.bno,COALESCE(rt.date,b.date) as date,rt.rdate,rt.rtime,MAX(p.idno) as idno,MAX(p.name) as name,MAX(p.tel) as tel,COALESCE(rt.staName,b.staName) as staName,COALESCE(rt.endName,b.endName) as endName,MAX(b.price) as price,COALESCE(rt.time,b.time) as time from refund_ticket rt left join bus b on rt.bno=b.bno left join passenger p on rt.idno=p.idno group by rt.btno";
        java.sql.PreparedStatement ps = con.prepareStatement(sql);
        rs = ps.executeQuery();
        return rs;
    }

    public ResultSet search(Connection con, String keyword) throws Exception {
        ResultSet rs = null;
        String sql = "select rt.btno,rt.bno,COALESCE(rt.date,b.date) as date,rt.rdate,rt.rtime,MAX(p.idno) as idno,MAX(p.name) as name,MAX(p.tel) as tel,COALESCE(rt.staName,b.staName) as staName,COALESCE(rt.endName,b.endName) as endName,MAX(b.price) as price,COALESCE(rt.time,b.time) as time from refund_ticket rt left join bus b on rt.bno=b.bno left join passenger p on rt.idno=p.idno where rt.btno like ? or b.bno like ? group by rt.btno";
        java.sql.PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ps.setString(2, "%" + keyword + "%");
        rs = ps.executeQuery();
        return rs;
    }
}
