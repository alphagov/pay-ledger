package uk.gov.pay.ledger.healthcheck;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import uk.gov.pay.commons.utils.startup.ApplicationStartupDependentResourceChecker;
import uk.gov.pay.commons.utils.startup.DatabaseStartupResource;
import uk.gov.pay.ledger.app.LedgerConfig;

public class DependentResourceWaitCommand extends ConfiguredCommand<LedgerConfig> {
    public DependentResourceWaitCommand() {
        super("waitOnDependencies", "Waits for dependent resources to become available");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
    }

    @Override
    protected void run(Bootstrap<LedgerConfig> bs, Namespace ns, LedgerConfig conf) {
        new ApplicationStartupDependentResourceChecker(new DatabaseStartupResource(conf.getDataSourceFactory()))
                .checkAndWaitForResource();
    }
}
