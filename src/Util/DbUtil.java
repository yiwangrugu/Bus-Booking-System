package Util;

import java.sql.Connection;

public class DbUtil {
    public Connection getCon() throws Exception {
        return DbPool.getConnection();
    }

    public void closeCon(Connection con) throws Exception {
        if (con != null) {
            DbPool.releaseConnection(con);
        }
    }
}
