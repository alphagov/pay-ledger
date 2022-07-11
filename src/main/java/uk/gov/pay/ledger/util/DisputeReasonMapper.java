package uk.gov.pay.ledger.util;

public class DisputeReasonMapper {

    public static String mapToApi(String stripeReason) {
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
