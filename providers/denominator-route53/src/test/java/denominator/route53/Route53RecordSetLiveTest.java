package denominator.route53;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import denominator.BaseRecordSetLiveTest;

@Test
public class Route53RecordSetLiveTest extends BaseRecordSetLiveTest {
    @BeforeClass
    private void setUp() {
        Route53Connection connection = new Route53Connection();
        manager = connection.manager;
        setMutableZoneIfPresent(connection.mutableZone);
        supportedRecordTypes = ImmutableList.copyOf(filter(supportedRecordTypes, not(equalTo("SSHFP"))));
    }
}
