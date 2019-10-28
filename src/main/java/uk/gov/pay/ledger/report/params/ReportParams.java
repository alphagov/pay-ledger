package uk.gov.pay.ledger.report.params;

import java.util.List;
import java.util.Map;

public interface ReportParams {

    String GATEWAY_ACCOUNT_EXTERNAL_FIELD = "account_id";
    String FROM_DATE_FIELD = "from_date";

    List<String> getFilterTemplates();
    Map<String, Object> getQueryMap();
}
