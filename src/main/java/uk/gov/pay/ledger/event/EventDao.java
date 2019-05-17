package uk.gov.pay.ledger.event;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.Optional;

@RegisterRowMapper(EventMapper.class)
public interface EventDao {
    @SqlQuery("SELECT id FROM event WHERE id = :eventId")
    Optional<Event> getById(@Bind("eventId") String eventId);
}
