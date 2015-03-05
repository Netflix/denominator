package denominator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.mock.MockProvider;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.rdata.CERTData;
import denominator.model.rdata.DSData;
import denominator.model.rdata.LOCData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NAPTRData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;
import denominator.model.rdata.TLSAData;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.spf;
import static denominator.model.ResourceRecordSets.txt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.junit.Assume.assumeTrue;

public class TestGraph {

  private final DNSApiManager manager;
  private final String zoneName;
  private final boolean addTrailingDotToZone;

  public TestGraph() {
    this(Denominator.create(new MockProvider()), "denominator.io.");
  }

  /**
   * To lazy initialize {@code manager}, pass null and override {@link #manager()}.
   */
  protected TestGraph(DNSApiManager manager, String zoneName) {
    this.manager = manager;
    this.zoneName = zoneName;
    this.addTrailingDotToZone = manager == null || !manager.provider().name().equals("clouddns");
  }

  /**
   * Returns null if the manager could not be initialized.
   *
   * <p/> Override to lazy initialize, for example if you are using TLS certificate auth.
   */
  protected DNSApiManager manager() {
    return manager;
  }

  Zone zoneIfPresent() {
    assumeTrue("manager not initialized", manager() != null);
    assumeTrue("zone not specified", zoneName != null);

    Zone result = null;
    List<Zone> currentZones = new ArrayList<Zone>();
    for (Zone zone : manager().api().zones()) {
      if (zoneName.equals(zone.name())) {
        result = zone;
        break;
      }
      currentZones.add(zone);
    }
    assumeTrue(format("zone %s not found in %s", zoneName, currentZones), result != null);
    return result;
  }

  List<ResourceRecordSet<?>> basicRecordSets(Class<?> testClass) {
    return filterRecordSets(testClass, manager().provider().basicRecordTypes());
  }

  List<ResourceRecordSet<?>> recordSetsForProfile(Class<?> testClass, String profile) {
    return filterRecordSets(testClass, manager().provider().profileToRecordTypes().get(profile));
  }

  private List<ResourceRecordSet<?>> filterRecordSets(Class<?> testClass,
                                                      Collection<String> types) {
    List<ResourceRecordSet<?>> filtered = new ArrayList<ResourceRecordSet<?>>();
    for (Map.Entry<String, ResourceRecordSet<?>> entry : stockRRSets(testClass).entrySet()) {
      if (types.contains(entry.getKey())) {
        filtered.add(entry.getValue());
      }
    }
    return filtered;
  }

  /**
   * Creates sample record sets named base on the {@code testClass}.
   */
  private Map<String, ResourceRecordSet<?>> stockRRSets(Class<?> testClass) {
    String recordPrefix = testClass.getSimpleName().toLowerCase() + "."
                          + getProperty("user.name").replace('.', '-');
    String recordSuffix = recordPrefix + "." + zoneName;
    String rdataSuffix = recordSuffix;

    if (addTrailingDotToZone && !recordSuffix.endsWith(".")) {
      rdataSuffix = recordSuffix + ".";
    }

    Map<String, ResourceRecordSet<?>> result = new LinkedHashMap<String, ResourceRecordSet<?>>();
    result.put(
        "AAAA",
        aaaa("ipv6-" + recordSuffix, Arrays.asList("2001:1DB8:85A3:1001:1001:8A2E:1371:7334",
                                                   "2001:1DB8:85A3:1001:1001:8A2E:1371:7335",
                                                   "2001:1DB8:85A3:1001:1001:8A2E:1371:7336")));
    result.put("A", a("ipv4-" + recordSuffix,
                      Arrays.asList("192.0.2.1", "198.51.100.1", "203.0.113.1")));
    result.put(
        "CNAME",
        cname("www-" + recordSuffix,
              Arrays.asList("www-north-" + rdataSuffix, "www-east-" + rdataSuffix, "www-west-"
                                                                                   + rdataSuffix)));
    result.put(
        "MX",
        ResourceRecordSet.<MXData>builder().name("mail-" + recordSuffix).type("MX")
            .add(MXData.create(10, "mail1-" + rdataSuffix))
            .add(MXData.create(10, "mail2-" + rdataSuffix))
            .add(MXData.create(10, "mail3-" + rdataSuffix)).build());
    result.put(
        "NS",
        ns("ns-" + recordSuffix,
           Arrays.asList("ns1-" + rdataSuffix, "ns2-" + rdataSuffix, "ns3-" + rdataSuffix)));
    result.put(
        "PTR",
        ptr("ptr-" + recordSuffix,
            Arrays.asList("ptr1-" + rdataSuffix, "ptr2-" + rdataSuffix, "ptr3-" + rdataSuffix)));
    result.put("SPF",
               spf("spf-" + recordSuffix,
                   Arrays.asList("v=spf1 a -all", "v=spf1 mx -all", "v=spf1 ipv6 -all")));
    result.put(
        "SRV", // designate does not support priority zero!
        ResourceRecordSet.<SRVData>builder().name("_http._tcp" + recordSuffix).type("SRV")
            .add(SRVData.builder().priority(1).weight(1).port(80).target("ipv4-" + rdataSuffix)
                     .build())
            .add(SRVData.builder().priority(1).weight(1).port(8080).target("ipv4-" + rdataSuffix)
                     .build())
            .add(SRVData.builder().priority(1).weight(1).port(443).target("ipv4-" + rdataSuffix)
                     .build())
            .build());
    result.put(
        "SSHFP",
        ResourceRecordSet.<SSHFPData>builder().name("ipv4-" + recordSuffix).type("SSHFP")
            .add(SSHFPData.createDSA("190E37C5B5DB9A1C455E648A41AF3CC83F99F102"))
            .add(SSHFPData.createDSA("290E37C5B5DB9A1C455E648A41AF3CC83F99F102"))
            .add(SSHFPData.createDSA("390E37C5B5DB9A1C455E648A41AF3CC83F99F102")).build());
    result.put("TXT",
               txt("txt-" + recordSuffix,
                   Arrays.asList("made in norway", "made in sweden", "made in finland")));
    result.put("DS",
               ResourceRecordSet.<DSData>builder().name("dnssec-" + recordSuffix).type("DS")
                   .add(DSData.builder().keyTag(12345).algorithmId(1).digestId(1).digest("B33F")
                            .build())
                   .add(DSData.builder().keyTag(65535).algorithmId(1).digestId(1).digest("B33F")
                            .build())
                   .build());
    result.put("CERT",
               ResourceRecordSet.<CERTData>builder().name("cert-" + recordSuffix).type("CERT")
                   .add(CERTData.builder().certType(12345).keyTag(1).algorithm(1).cert("B33F")
                            .build())
                   .add(CERTData.builder().certType(65535).keyTag(1).algorithm(1).cert("B33F")
                            .build())
                   .build());
    result.put("NAPTR",
               ResourceRecordSet.<NAPTRData>builder().name("naptr-" + recordSuffix).type("NAPTR")
                   .add(NAPTRData.builder().order(1).preference(1).flags("U").services("E2U+sip")
                            .regexp("!^.*$!sip:customer-service@example.com!").replacement(".")
                            .build())
                   .add(NAPTRData.builder().order(2).preference(1).flags("U").services("E2U+sip")
                            .regexp("!^.*$!sip:admin-service@example.com!").replacement(".")
                            .build())
                   .build());
    result.put("LOC",
               ResourceRecordSet.<LOCData>builder().name("loc-" + recordSuffix).type("LOC")
                   .add(LOCData.builder().latitude("37 48 48.892 S").longitude("144 57 57.502 E")
                            .altitude("26m").diameter("10m").hprecision("100m").vprecision("10m")
                            .build())
                   .add(LOCData.builder().latitude("37 48 48.893 S").longitude("144 57 57.503 E")
                            .altitude("26m").diameter("10m").hprecision("100m").vprecision("10m")
                            .build())
                   .build());
    result.put("LOC",
               ResourceRecordSet.<LOCData>builder().name("loc-simple-" + recordSuffix).type("LOC")
                   .add(LOCData.builder().latitude("37 48 48.892 S").longitude("144 57 57.502 E")
                            .altitude("26m").build())
                   .add(LOCData.builder().latitude("37 48 48.893 S").longitude("144 57 57.503 E")
                            .altitude("26m").build())
                   .build());
    result.put("TLSA",
               ResourceRecordSet.<TLSAData>builder().name("tlsa-" + recordSuffix).type("TLSA")
                   .add(TLSAData.builder().usage(1).selector(1).matchingType(1)
                            .certificateAssociationData("B33F").build())
                   .add(TLSAData.builder().usage(2).selector(1).matchingType(1)
                            .certificateAssociationData("B33F").build())
                   .build());
    return result;
  }
}
