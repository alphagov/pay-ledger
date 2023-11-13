package uk.gov.pay.ledger.common.search;

import uk.gov.pay.ledger.util.CommaDelimitedSetParameter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    public String encodeParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("%3D", "=").replace("%26","&");
    }
}
