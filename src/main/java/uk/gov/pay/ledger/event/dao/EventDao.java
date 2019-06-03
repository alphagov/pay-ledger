package uk.gov.pay.ledger.event.dao;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import uk.gov.pay.ledger.event.dao.mapper.EventMapper;
import uk.gov.pay.ledger.event.model.Event;

import java.util.Optional;

@RegisterRowMapper(EventMapper.class)
public interface EventDao {
    @SqlQuery("SELECT id FROM event WHERE id = :eventId")
    Optional<Event> getById(@Bind("eventId") String eventId);
}
