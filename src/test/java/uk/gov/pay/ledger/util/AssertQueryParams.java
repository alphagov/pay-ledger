package uk.gov.pay.ledger.util;

import org.apache.http.NameValuePair;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssertQueryParams {
    
    public static void assertQueryParams(List<NameValuePair> params, String page, String displaySize, String... gatewayAccountIds) {
        for (NameValuePair nameValuePair : params) {
            switch (nameValuePair.getName()) {
                case "gateway_account_id":
                    assertThat(Set.of(nameValuePair.getValue().split(",")), hasItems(gatewayAccountIds));
                    break;
                case "page":
                    assertEquals(page, nameValuePair.getValue());
                    break;
                case "display_size":
                    assertEquals(displaySize, nameValuePair.getValue());
                    break;
            }
        }
    }
}
