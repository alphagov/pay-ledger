package uk.gov.pay.ledger.performance.dao.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.performance.entity.PerformanceReportEntity;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PerformanceReportEntityMapper implements RowMapper<PerformanceReportEntity>  {

    @Override
    public PerformanceReportEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new PerformanceReportEntity(
                rs.getLong("volume"),
                new BigDecimal(rs.getString("total_amount")),
                new BigDecimal(rs.getString("avg_amount"))
        );
    }
}
