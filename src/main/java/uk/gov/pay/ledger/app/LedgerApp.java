package uk.gov.pay.ledger.app;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class LedgerApp extends Application<LedgerConfiguration> {

    public static void main(String[] args) throws Exception {
        new LedgerApp().run(args);
    }


    @Override
    public void run(LedgerConfiguration ledgerConfiguration, Environment environment) throws Exception {

    }
}
