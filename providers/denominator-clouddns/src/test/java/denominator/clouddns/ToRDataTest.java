package denominator.clouddns;

import static org.testng.Assert.assertEquals;

import org.jclouds.rackspace.clouddns.v1.domain.Record;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

@Test
public class ToRDataTest {

    public void transformsNSRecordSet() {
        Record input = Record.builder()
                .name("denominator.io")
                .type("NS")
                .ttl(3600)
                .data("dns1.stabletransit.com")                
                .build();

        assertEquals(GroupByRecordNameAndTypeIterator.toRData(input), ImmutableMap.<String, String> of(
        		"nsdname", "dns1.stabletransit.com"));
    }

    public void transformsTXTRecordSet() {
        Record input = Record.builder()
                .name("denominator.io")
                .type("TXT")
                .ttl(3600)
                .data("Hello DNS")                
                .build();

        assertEquals(GroupByRecordNameAndTypeIterator.toRData(input), ImmutableMap.<String, String> of(
        		"txtdata", "Hello DNS"));
    }
}
