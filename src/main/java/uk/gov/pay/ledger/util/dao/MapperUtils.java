package uk.gov.pay.ledger.util.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MapperUtils {
    public static Boolean getBooleanWithNullCheck(ResultSet rs, String columnName) throws SQLException {
        var value = rs.getBoolean(columnName);
        return rs.wasNull() ? null : value;
    }
}
