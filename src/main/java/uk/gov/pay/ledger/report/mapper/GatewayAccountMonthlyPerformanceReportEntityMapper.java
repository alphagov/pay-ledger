package uk.gov.pay.ledger.report.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.report.entity.GatewayAccountMonthlyPerformanceReportEntity;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GatewayAccountMonthlyPerformanceReportEntityMapper implements RowMapper<GatewayAccountMonthlyPerformanceReportEntity> {

    @Override
    public GatewayAccountMonthlyPerformanceReportEntity map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new GatewayAccountMonthlyPerformanceReportEntity(
                rs.getLong("gateway_account_id"),
                rs.getLong("volume"),
                new BigDecimal(rs.getString("total_amount")),
                new BigDecimal(rs.getString("avg_amount")),
                rs.getLong("year"),
                rs.getLong("month")
        );
    }
}
