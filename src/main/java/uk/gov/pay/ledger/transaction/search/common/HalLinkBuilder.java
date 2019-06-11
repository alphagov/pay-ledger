package uk.gov.pay.ledger.transaction.search.common;

import uk.gov.pay.ledger.transaction.search.model.Link;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class HalLinkBuilder {
    private static String createLink(UriInfo uriInfo, String path, String... ids) {
        return UriBuilder.fromUri(uriInfo.getBaseUri())
                .path(path)
                .build(ids)
                .toString();
    }

    public static Link createSelfLink(UriInfo uriInfo, String path, String... ids) {
        String url = createLink(uriInfo, path, ids);
        return Link.ofValue(url, "GET", "self");
    }

    public static Link createRefundsLink(UriInfo uriInfo, String path, String... ids) {
        String url = createLink(uriInfo, path, ids);
        return Link.ofValue(url, "GET", "refunds");
    }
}
