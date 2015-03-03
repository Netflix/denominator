package denominator.route53;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;

import static denominator.model.ResourceRecordSets.a;
import static org.assertj.core.api.Assertions.assertThat;

public class EncodeChangesTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void defaultsTTLTo300() {
    ResourceRecordSet<AData>
        rrs =
        a("www.denominator.io.", Arrays.asList("192.0.2.1", "192.0.2.2"));

    assertThat(EncodeChanges.apply(rrs))
        .isXmlEqualTo("<ResourceRecordSet>\n"
                      + "  <Name>www.denominator.io.</Name>\n"
                      + "  <Type>A</Type>\n"
                      + "  <TTL>300</TTL>\n"
                      + "  <ResourceRecords>\n"
                      + "    <ResourceRecord>\n"
                      + "      <Value>192.0.2.1</Value>\n"
                      + "    </ResourceRecord>\n"
                      + "    <ResourceRecord>\n"
                      + "      <Value>192.0.2.2</Value>\n"
                      + "    </ResourceRecord>\n"
                      + "  </ResourceRecords>\n"
                      + "</ResourceRecordSet>");
  }

  @Test
  public void encodeAliasRRSet() {
    ResourceRecordSet<AliasTarget>
        rrs =
        ResourceRecordSet.<AliasTarget>builder().name("fooo.myzone.com.")
            .type("A")
            .add(AliasTarget.create("Z3I0BTR7N27QRM",
                                    "ipv4-route53recordsetlivetest.adrianc.myzone.com.")).build();

    assertThat(EncodeChanges.apply(rrs))
        .isXmlEqualTo("<ResourceRecordSet>\n"
                      + "  <Name>fooo.myzone.com.</Name>\n"
                      + "  <Type>A</Type>\n"
                      + "  <AliasTarget>\n"
                      + "    <HostedZoneId>Z3I0BTR7N27QRM</HostedZoneId>\n"
                      + "    <DNSName>ipv4-route53recordsetlivetest.adrianc.myzone.com.</DNSName>\n"
                      + "    <EvaluateTargetHealth>false</EvaluateTargetHealth>\n"
                      + "  </AliasTarget>\n"
                      + "</ResourceRecordSet>");
  }

  @Test
  public void ignoreTTLOnAliasRRSet() {
    ResourceRecordSet<AliasTarget>
        rrs =
        ResourceRecordSet.<AliasTarget>builder().name("fooo.myzone.com.")
            .type("A").ttl(600)
            .add(AliasTarget.create("Z3I0BTR7N27QRM",
                                    "ipv4-route53recordsetlivetest.adrianc.myzone.com.")).build();

    assertThat(EncodeChanges.apply(rrs))
        .isXmlEqualTo("<ResourceRecordSet>\n"
                      + "  <Name>fooo.myzone.com.</Name>\n"
                      + "  <Type>A</Type>\n"
                      + "  <AliasTarget>\n"
                      + "    <HostedZoneId>Z3I0BTR7N27QRM</HostedZoneId>\n"
                      + "    <DNSName>ipv4-route53recordsetlivetest.adrianc.myzone.com.</DNSName>\n"
                      + "    <EvaluateTargetHealth>false</EvaluateTargetHealth>\n"
                      + "  </AliasTarget>\n"
                      + "</ResourceRecordSet>");
  }

  @Test
  public void aliasRRSetMissingDNSName() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("missing DNSName in alias target ResourceRecordSet");
    
    Map<String, Object> missingDNSName = new LinkedHashMap<String, Object>();
    missingDNSName.put("HostedZoneId", "Z3I0BTR7N27QRM");

    ResourceRecordSet<Map<String, Object>> rrs = ResourceRecordSet.<Map<String, Object>>builder()
        .name("fooo.myzone.com.").type("A")
        .add(missingDNSName).build();

    EncodeChanges.apply(rrs);
  }

  @Test
  public void identifierAndWeightedElements() {
    ResourceRecordSet<CNAMEData> rrs = ResourceRecordSet.<CNAMEData>builder()
        .name("www.denominator.io.")
        .type("CNAME")
        .qualifier("foobar")
        .add(CNAMEData.create("dom1.foo.com."))
        .weighted(Weighted.create(1)).build();

    assertThat(EncodeChanges.apply(rrs))
        .isXmlEqualTo("<ResourceRecordSet>\n"
                      + "  <Name>www.denominator.io.</Name>\n"
                      + "  <Type>CNAME</Type>\n"
                      + "  <SetIdentifier>foobar</SetIdentifier>\n"
                      + "  <Weight>1</Weight>\n"
                      + "  <TTL>300</TTL>\n"
                      + "  <ResourceRecords>\n"
                      + "    <ResourceRecord>\n"
                      + "      <Value>dom1.foo.com.</Value>\n"
                      + "    </ResourceRecord>\n"
                      + "  </ResourceRecords>\n"
                      + "</ResourceRecordSet>");
  }
}
