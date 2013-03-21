package denominator;

import org.testng.annotations.BeforeClass;

import com.google.common.collect.ImmutableList;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseLoadBalancedCNAMELiveTest extends BaseRoundRobinLiveTest {
    protected BaseLoadBalancedCNAMELiveTest() {
        supportedRecordTypes = ImmutableList.of("CNAME");
    }
}
