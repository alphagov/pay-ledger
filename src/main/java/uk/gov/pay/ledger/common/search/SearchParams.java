package uk.gov.pay.ledger.common.search;

import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

public abstract class SearchParams {

    public abstract String buildQueryParamString(Long forPage);

    public abstract Long getPageNumber();

    public abstract Long getDisplaySize();

    public boolean limitTotal() {
        return false;
    }

    protected boolean isSet(CommaDelimitedSetParameter commaDelimitedSetParameter) {
        return commaDelimitedSetParameter != null && commaDelimitedSetParameter.isNotEmpty();
    }
}
