package uk.gov.pay.ledger.util;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class DisputeReasonMapper {

    public static String mapToApi(String stripeReason) {
        if (isBlank(stripeReason)) {
            return "";
        }
        switch (stripeReason) {
            case "credit_not_processed":
            case "duplicate":
            case "fraudulent":
            case "general":
            case "product_not_received":
            case "product_unacceptable":
            case "subscription_canceled":
                return stripeReason;
            case "unrecognized":
                return "unrecognised";
            default:
                return "other";
        }
    }
}
