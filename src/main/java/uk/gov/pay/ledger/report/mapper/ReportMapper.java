package uk.gov.pay.ledger.report.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import uk.gov.pay.ledger.report.entity.TimeseriesReportSlice;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ReportMapper implements RowMapper<TimeseriesReportSlice>  {

    @Override
    public TimeseriesReportSlice map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new TimeseriesReportSlice(
                ZonedDateTime.ofInstant(rs.getTimestamp("timestamp").toInstant(), ZoneOffset.UTC),
                rs.getInt("all_payments"),
                rs.getInt("errored_payments"),
                rs.getInt("completed_payments"),
                rs.getInt("amount"),
                rs.getInt("net_amount"),
                rs.getInt("total_amount"),
                rs.getInt("fee")
        );
    }
}
