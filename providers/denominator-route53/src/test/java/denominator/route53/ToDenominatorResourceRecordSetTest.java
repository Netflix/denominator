package denominator.route53;
import static denominator.model.ResourceRecordSets.ns;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.SOAData;

@Test
public class ToDenominatorResourceRecordSetTest {

    public void transformsSOARecordSet() {
        org.jclouds.route53.domain.ResourceRecordSet input = org.jclouds.route53.domain.ResourceRecordSet.builder()
                .name("denominator.io")
                .type("SOA")
                .ttl(3600)
                .add("ns1.p28.dynect.net. admin.denominator.io. 1 3600 600 604800 60")
                .build();

        assertEquals(ToDenominatorResourceRecordSet.INSTANCE.apply(input), ResourceRecordSet.<SOAData> builder()
                .name("denominator.io")
                .type("SOA")
                .ttl(3600)
                .add(SOAData.builder()
                            .mname("ns1.p28.dynect.net.")
                            .rname("admin.denominator.io.")
                            .serial(1)
                            .refresh(3600)
                            .retry(600)
                            .expire(604800)
                            .minimum(60).build()).build());
    }

    public void transformsNSRecordSet() {
        List<String> nameServers = ImmutableList.<String> builder()
                                                .add("ns-2048.awsdns-64.com.")
                                                .add("ns-2049.awsdns-65.net.")
                                                .add("ns-2050.awsdns-66.org.")
                                                .add("ns-2051.awsdns-67.co.uk.").build();

        org.jclouds.route53.domain.ResourceRecordSet input = org.jclouds.route53.domain.ResourceRecordSet.builder()
                .name("denominator.io")
                .type("NS")
                .ttl(3600)
                .addAll(nameServers)                
                .build();

        assertEquals(ToDenominatorResourceRecordSet.INSTANCE.apply(input), ns("denominator.io", 3600, nameServers));
    }
}