package denominator.route53;

import static denominator.model.ResourceRecordSets.a;
import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;

public class EncodeChangesTest {

    @Test
    public void defaultsTTLTo300() {
        ResourceRecordSet<AData> rrs = a("www.denominator.io.", ImmutableSet.of("192.0.2.1", "192.0.2.2"));
        assertEquals(
                EncodeChanges.apply(rrs),
                "<ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>300</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>192.0.2.2</Value></ResourceRecord></ResourceRecords></ResourceRecordSet>");
    }

    @Test
    public void encodeAliasRRSet() {
        ResourceRecordSet<AliasTarget> rrs = ResourceRecordSet.<AliasTarget> builder().name("fooo.myzone.com.")
                .type("A")
                .add(AliasTarget.create("Z3I0BTR7N27QRM", "ipv4-route53recordsetlivetest.adrianc.myzone.com.")).build();

        assertEquals(
                EncodeChanges.apply(rrs),
                "<ResourceRecordSet><Name>fooo.myzone.com.</Name><Type>A</Type><AliasTarget><HostedZoneId>Z3I0BTR7N27QRM</HostedZoneId><DNSName>ipv4-route53recordsetlivetest.adrianc.myzone.com.</DNSName><EvaluateTargetHealth>false</EvaluateTargetHealth></AliasTarget></ResourceRecordSet>");
    }

    @Test
    public void ignoreTTLOnAliasRRSet() {
        ResourceRecordSet<AliasTarget> rrs = ResourceRecordSet.<AliasTarget> builder().name("fooo.myzone.com.")
                .type("A").ttl(600)
                .add(AliasTarget.create("Z3I0BTR7N27QRM", "ipv4-route53recordsetlivetest.adrianc.myzone.com.")).build();

        assertEquals(
                EncodeChanges.apply(rrs),
                "<ResourceRecordSet><Name>fooo.myzone.com.</Name><Type>A</Type><AliasTarget><HostedZoneId>Z3I0BTR7N27QRM</HostedZoneId><DNSName>ipv4-route53recordsetlivetest.adrianc.myzone.com.</DNSName><EvaluateTargetHealth>false</EvaluateTargetHealth></AliasTarget></ResourceRecordSet>");
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "missing DNSName in alias target ResourceRecordSet .*")
    public void aliasRRSetMissingDNSName() {
        ResourceRecordSet<Map<String, Object>> rrs = ResourceRecordSet.<Map<String, Object>> builder()
                .name("fooo.myzone.com.").type("A")
                .add(ImmutableMap.<String, Object> of("HostedZoneId", "Z3I0BTR7N27QRM")).build();

        EncodeChanges.apply(rrs);
    }

    @Test
    public void identifierAndWeightedElements() {
        ResourceRecordSet<CNAMEData> rrs = ResourceRecordSet.<CNAMEData> builder()//
                .name("www.denominator.io.")//
                .type("CNAME")//
                .qualifier("foobar")//
                .add(CNAMEData.create("dom1.foo.com."))//
                .weighted(Weighted.create(1)).build();
        assertEquals(
                EncodeChanges.apply(rrs),
                "<ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>foobar</SetIdentifier><Weight>1</Weight><TTL>300</TTL><ResourceRecords><ResourceRecord><Value>dom1.foo.com.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet>");
    }
}
