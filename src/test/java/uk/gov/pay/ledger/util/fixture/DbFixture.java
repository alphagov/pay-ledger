package uk.gov.pay.ledger.util.fixture;

import org.jdbi.v3.core.Jdbi;

public interface DbFixture<F, E> {
    F insert(Jdbi jdbi);

    E toEntity();
}
