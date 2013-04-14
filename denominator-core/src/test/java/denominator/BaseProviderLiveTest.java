package denominator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterators.any;
import static com.google.common.io.Closeables.close;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.spf;
import static denominator.model.ResourceRecordSets.txt;
import static denominator.model.ResourceRecordSets.typeEqualTo;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterators;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.MXData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseProviderLiveTest {

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        String zoneName = skipIfNoMutableZone();
        String recordSuffix =  recordPrefix + "." + zoneName;
        Builder<String, ResourceRecordSet<?>> builder = ImmutableMap.<String, ResourceRecordSet<?>> builder();
        builder.put("AAAA", aaaa("ipv6-" + recordSuffix, ImmutableList.of("2001:0DB8:85A3:0000:0000:8A2E:0370:7334",
                "2001:0DB8:85A3:0000:0000:8A2E:0370:7335", "2001:0DB8:85A3:0000:0000:8A2E:0370:7336")));
        builder.put("A", a("ipv4-" + recordSuffix, ImmutableList.of("192.0.2.1", "198.51.100.1", "203.0.113.1")));
        builder.put(
                "CNAME",
                cname("www-" + recordSuffix,
                        ImmutableList.of("www-north-" + recordSuffix, "www-east-" + recordSuffix, "www-west-"
                                + recordSuffix)));
        builder.put("MX", ResourceRecordSet.<MXData> builder()
                                           .name("mail-" + recordSuffix)
                                           .type("MX")
                                           .add(MXData.create(10, "mail1-" + recordSuffix))
                                           .add(MXData.create(10, "mail2-" + recordSuffix))
                                           .add(MXData.create(10, "mail3-" + recordSuffix)).build());
        builder.put("NS", ns("ns-" + recordSuffix,
                ImmutableList.of("ns1-" + recordSuffix, "ns2-" + recordSuffix, "ns3-" + recordSuffix)));
        builder.put("PTR", ptr("ptr-" + recordSuffix,
                ImmutableList.of("ptr1-" + recordSuffix, "ptr2-" + recordSuffix, "ptr3-" + recordSuffix)));
        builder.put("SPF", spf("spf-" + recordSuffix,
                ImmutableList.of("v=spf1 a -all", "v=spf1 mx -all", "v=spf1 ipv6 -all")));
        builder.put("SRV", ResourceRecordSet.<SRVData> builder()
                                            .name("_http._tcp" + recordSuffix)
                                            .type("SRV")
                                            .add(SRVData.builder()
                                                        .priority(0)
                                                        .weight(1)
                                                        .port(80).target("ipv4-" + recordSuffix).build())
                                            .add(SRVData.builder()
                                                        .priority(0)
                                                        .weight(1)
                                                        .port(8080).target("ipv4-" + recordSuffix).build())
                                            .add(SRVData.builder()
                                                        .priority(0)
                                                        .weight(1)
                                                        .port(443).target("ipv4-" + recordSuffix).build()).build());
        builder.put("SSHFP", ResourceRecordSet.<SSHFPData> builder()
                                              .name("ipv4-" + recordSuffix)
                                              .type("SSHFP")
                                              .add(SSHFPData.createDSA("190E37C5B5DB9A1C455E648A41AF3CC83F99F102"))
                                              .add(SSHFPData.createDSA("290E37C5B5DB9A1C455E648A41AF3CC83F99F102"))
                                              .add(SSHFPData.createDSA("390E37C5B5DB9A1C455E648A41AF3CC83F99F102")).build());
        builder.put("TXT", txt("txt-" + recordSuffix,
                ImmutableList.of("made in norway", "made in sweden", "made in finland")));
        return builder.build();
    }

    protected String recordPrefix = getClass().getSimpleName().toLowerCase() + "."
            + getProperty("user.name").replace('.', '-');

    protected DNSApiManager manager;
    protected String mutableZone;

    protected void checkRRS(ResourceRecordSet<?> rrs) {
        checkNotNull(rrs.getName(), "Name: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.getType(), "Type: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.getTTL(), "TTL: ResourceRecordSet %s", rrs);
        assertTrue(!rrs.isEmpty(), "Values absent on ResourceRecordSet: " + rrs);
    }

    protected void skipIfRRSetExists(String zoneName, String name, String type) {
        if (any(rrsApi(zoneName).list(), and(nameEqualTo(name), typeEqualTo(type))))
            throw new SkipException(format("recordset with name %s and type %s already exists", name, type));
    }

    protected void assertPresent(Optional<ResourceRecordSet<?>> rrs, String zoneName, String recordName,
            String recordType) {
        if (!rrs.isPresent()) {
            throw new AssertionError(format("recordset(%s, %s) not present in zone(%s); rrsets: %s", recordName,
                    recordType, zoneName, Iterators.toString(rrsApi(zoneName).list())));
        }
    }

    @AfterClass
    protected void tearDown() throws IOException {
        close(manager, true);
    }

    protected void skipIfNoCredentials() {
        if (manager == null)
            throw new SkipException("manager not configured");
    }

    protected String skipIfNoMutableZone() {
        if (mutableZone == null)
            throw new SkipException("mutable zone not configured");
        return mutableZone;
    }

    protected ZoneApi zoneApi() {
        return manager.getApi().getZoneApi();
    }

    protected ResourceRecordSetApi rrsApi(String zoneName) {
        return manager.getApi().getResourceRecordSetApiForZone(zoneName);
    }
}
