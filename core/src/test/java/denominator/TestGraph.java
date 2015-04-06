package denominator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.mock.MockProvider;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.rdata.CERTData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NAPTRData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.spf;
import static denominator.model.ResourceRecordSets.txt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static org.junit.Assume.assumeFalse;
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

  Zone createZoneIfAbsent() {
    assumeTrue("manager not initialized", manager() != null);
    assumeTrue("zone not specified", zoneName != null);

    Iterator<Zone> zonesWithName = manager().api().zones().iterateByName(zoneName);
    if (zonesWithName.hasNext()) {
      return zonesWithName.next();
    }
    String id = manager().api().zones().put(Zone.create(null, zoneName, 86400, "test@" + zoneName));
    return Zone.create(id, zoneName, 86400, "test@" + zoneName);
  }

  String deleteTestZone() {
    assumeTrue("manager not initialized", manager() != null);
    assumeTrue("zone not specified", zoneName != null);
    String zoneToCreate = "zonetest." + zoneName;

    Iterator<Zone> zonesWithName = manager().api().zones().iterateByName(zoneToCreate);
    while (zonesWithName.hasNext()) {
      manager().api().zones().delete(zonesWithName.next().id());
    }
    return zoneToCreate;
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
    String rPrefix = testClass.getSimpleName().toLowerCase() + "."
                     + getProperty("user.name").replace('.', '-');
    String rSuffix = rPrefix + "." + zoneName;
    String dSuffix = rSuffix;

    if (addTrailingDotToZone && !rSuffix.endsWith(".")) {
      dSuffix = rSuffix + ".";
    }

    Map<String, ResourceRecordSet<?>> result = new LinkedHashMap<String, ResourceRecordSet<?>>();
    result.put("A", a("ipv4-" + rSuffix, asList("192.0.2.1", "198.51.100.1", "203.0.113.1")));
    result.put("AAAA",
               aaaa("ipv6-" + rSuffix, asList("2001:1DB8:85A3:1001:1001:8A2E:1371:7334",
                                              "2001:1DB8:85A3:1001:1001:8A2E:1371:7335",
                                              "2001:1DB8:85A3:1001:1001:8A2E:1371:7336")));
    result.put("CNAME",
               cname("www-" + rSuffix,
                     asList("www-north-" + dSuffix, "www-east-" + dSuffix, "www-west-"
                                                                           + dSuffix)));
    result.put("CERT",
               ResourceRecordSet.<CERTData>builder().name("cert-" + rSuffix).type("CERT")
                   .add(CERTData.builder().format(1).tag(2).algorithm(3).certificate("ABCD")
                            .build())
                   .add(CERTData.builder().format(1).tag(2).algorithm(3).certificate("EFGH")
                            .build())
                   .build());
    result.put("MX",
               ResourceRecordSet.<MXData>builder().name("mail-" + rSuffix).type("MX")
                   .add(MXData.create(10, "mail1-" + dSuffix))
                   .add(MXData.create(10, "mail2-" + dSuffix))
                   .add(MXData.create(10, "mail3-" + dSuffix)).build());
    result.put("NS",
               ns("ns-" + rSuffix,
                  asList("ns1-" + dSuffix, "ns2-" + dSuffix, "ns3-" + dSuffix)));
    result.put("NAPTR",
               ResourceRecordSet.<NAPTRData>builder().name("naptr-" + rSuffix).type("NAPTR")
                   .add(NAPTRData.builder().order(1).preference(1).flags("U").services("E2U+sip")
                            .regexp("!^.*$!sip:customer-service@example.com!").replacement(".")
                            .build())
                   .add(NAPTRData.builder().order(2).preference(1).flags("U").services("E2U+sip")
                            .regexp("!^.*$!sip:admin-service@example.com!").replacement(".")
                            .build())
                   .build());
    result.put("PTR",
               ptr("ptr-" + rSuffix,
                   asList("ptr1-" + dSuffix, "ptr2-" + dSuffix,
                          "ptr3-" + dSuffix)));
    result.put("SPF",
               spf("spf-" + rSuffix,
                   asList("v=spf1 a -all", "v=spf1 mx -all", "v=spf1 ipv6 -all")));
    result.put("SRV", // designate does not support priority zero!
               ResourceRecordSet.<SRVData>builder().name("_http._tcp" + rSuffix).type("SRV")
                   .add(SRVData.builder().priority(1).weight(1).port(80)
                            .target("ipv4-" + dSuffix)
                            .build())
                   .add(SRVData.builder().priority(1).weight(1).port(8080)
                            .target("ipv4-" + dSuffix)
                            .build())
                   .add(SRVData.builder().priority(1).weight(1).port(443)
                            .target("ipv4-" + dSuffix)
                            .build())
                   .build());
    result.put("SSHFP",
               ResourceRecordSet.<SSHFPData>builder().name("ipv4-" + rSuffix).type("SSHFP")
                   .add(SSHFPData.createDSA("190E37C5B5DB9A1C455E648A41AF3CC83F99F102"))
                   .add(SSHFPData.createDSA("290E37C5B5DB9A1C455E648A41AF3CC83F99F102"))
                   .add(SSHFPData.createDSA("390E37C5B5DB9A1C455E648A41AF3CC83F99F102")).build());
    result.put("TXT",
               txt("txt-" + rSuffix,
                   asList("made in norway", "made in sweden", "made in finland")));
    return result;
  }
}
