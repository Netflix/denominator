package denominator.route53;

import static denominator.model.ResourceRecordSets.a;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;

public class SerializeRRSTest {

    @Test
    public void defaultsTTLTo300() {
        ResourceRecordSet<AData> rrs = a("www.denominator.io.", ImmutableSet.of("192.0.2.1", "192.0.2.2"));
        assertEquals(
                SerializeRRS.INSTANCE.apply(rrs),
                "<ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>300</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>192.0.2.2</Value></ResourceRecord></ResourceRecords></ResourceRecordSet>");
    }

    @Test
    public void identifierAndWeightedElements() {
        ResourceRecordSet<CNAMEData> rrs = ResourceRecordSet.<CNAMEData> builder()//
                .name("www.denominator.io.")//
                .type("CNAME")//
                .qualifier("foobar")
                .add(CNAMEData.create("dom1.foo.com."))//
                .weighted(Weighted.create(1)).build();
        assertEquals(
                SerializeRRS.INSTANCE.apply(rrs),
                "<ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>foobar</SetIdentifier><Weight>1</Weight><TTL>300</TTL><ResourceRecords><ResourceRecord><Value>dom1.foo.com.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet>");
    }
}
