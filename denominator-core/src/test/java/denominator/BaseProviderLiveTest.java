package denominator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Iterators.any;
import static com.google.common.io.Closeables.close;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.qualifierEqualTo;
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
import denominator.model.Zone;
import denominator.model.Zones;
import denominator.model.rdata.MXData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.profile.WeightedResourceRecordSetApi;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseProviderLiveTest {

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        Zone zone = skipIfNoMutableZone();
        String recordSuffix =  recordPrefix + "." + zone.name();
        // TODO: metadata about whether zone names have trailing dots or not.
        String rdataSuffix = recordSuffix.endsWith(".") ? recordSuffix : recordSuffix + ".";
        Builder<String, ResourceRecordSet<?>> builder = ImmutableMap.<String, ResourceRecordSet<?>> builder();
        builder.put("AAAA", aaaa("ipv6-" + recordSuffix, ImmutableList.of("2001:0DB8:85A3:0000:0000:8A2E:0370:7334",
                "2001:0DB8:85A3:0000:0000:8A2E:0370:7335", "2001:0DB8:85A3:0000:0000:8A2E:0370:7336")));
        builder.put("A", a("ipv4-" + recordSuffix, ImmutableList.of("192.0.2.1", "198.51.100.1", "203.0.113.1")));
        builder.put(
                "CNAME",
                cname("www-" + recordSuffix,
                        ImmutableList.of("www-north-" + rdataSuffix, "www-east-" + rdataSuffix, "www-west-"
                                + rdataSuffix)));
        builder.put("MX", ResourceRecordSet.<MXData> builder()
                                           .name("mail-" + recordSuffix)
                                           .type("MX")
                                           .add(MXData.create(10, "mail1-" + rdataSuffix))
                                           .add(MXData.create(10, "mail2-" + rdataSuffix))
                                           .add(MXData.create(10, "mail3-" + rdataSuffix)).build());
        builder.put("NS", ns("ns-" + recordSuffix,
                ImmutableList.of("ns1-" + rdataSuffix, "ns2-" + rdataSuffix, "ns3-" + rdataSuffix)));
        builder.put("PTR", ptr("ptr-" + recordSuffix,
                ImmutableList.of("ptr1-" + rdataSuffix, "ptr2-" + rdataSuffix, "ptr3-" + rdataSuffix)));
        builder.put("SPF", spf("spf-" + recordSuffix,
                ImmutableList.of("v=spf1 a -all", "v=spf1 mx -all", "v=spf1 ipv6 -all")));
        builder.put("SRV", ResourceRecordSet.<SRVData> builder()
                                            .name("_http._tcp" + recordSuffix)
                                            .type("SRV")
                                            .add(SRVData.builder()
                                                        .priority(0)
                                                        .weight(1)
                                                        .port(80).target("ipv4-" + rdataSuffix).build())
                                            .add(SRVData.builder()
                                                        .priority(0)
                                                        .weight(1)
                                                        .port(8080).target("ipv4-" + rdataSuffix).build())
                                            .add(SRVData.builder()
                                                        .priority(0)
                                                        .weight(1)
                                                        .port(443).target("ipv4-" + rdataSuffix).build()).build());
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
    protected Zone mutableZone;

    protected void checkRRS(ResourceRecordSet<?> rrs) {
        checkNotNull(rrs.name(), "Name: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.type(), "Type: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.ttl(), "TTL: ResourceRecordSet %s", rrs);
        assertTrue(!rrs.records().isEmpty(), "Values absent on ResourceRecordSet: " + rrs);
    }

    protected void skipIfRRSetExists(Zone zone, String name, String type) {
        if (any(rrsApi(zone).iterator(), and(nameEqualTo(name), typeEqualTo(type))))
            throw new SkipException(format("recordset(%s, %s) already exists in %s", name, type, zone));
    }

    @SuppressWarnings("unchecked")
    protected void skipIfRRSetExists(Zone zone, String name, String type, String qualifier) {
        if (any(rrsApi(zone).iterator(), and(nameEqualTo(name), typeEqualTo(type), qualifierEqualTo(qualifier))))
            throw new SkipException(format("recordset(%s, %s, %s) already exists in %s", name, type, qualifier, zone));
    }

    protected void assertPresent(Optional<ResourceRecordSet<?>> rrs, Zone zone, String name, String type,
            String qualifier) {
        if (!rrs.isPresent()) {
            throw new AssertionError(format("recordset(%s, %s%s) not present in %s; rrsets: %s", name, type,
                    qualifier != null ? ", " + qualifier : "", zone, Iterators.toString(rrsApi(zone).iterator())));
        }
    }

    protected void assertPresent(Optional<ResourceRecordSet<?>> rrs, Zone zone, String name, String type) {
        assertPresent(rrs, zone, name, type, null);
    }

    @AfterClass
    protected void tearDown() throws IOException {
        close(manager, true);
    }

    protected void skipIfNoCredentials() {
        if (manager == null)
            throw new SkipException("manager not configured");
    }

    protected Zone skipIfNoMutableZone() {
        if (mutableZone == null)
            throw new SkipException("mutable zone not configured");
        return mutableZone;
    }

    protected void setMutableZoneIfPresent(String mutableZone) {
        if (mutableZone != null) {
            ImmutableList<Zone> zones = ImmutableList.copyOf(manager.api().zones());
            Optional<Zone> zone = tryFind(zones, Zones.nameEqualTo(mutableZone));
            if (!zone.isPresent())
                throw new SkipException(format("zone(%s) doesn't exist in %s", mutableZone, zones));
            this.mutableZone = zone.get();
        }
    }

    protected ZoneApi zones() {
        return manager.api().zones();
    }

    protected ResourceRecordSetApi rrsApi(Zone zone) {
        return manager.api().basicRecordSetsInZone(zone.idOrName());
    }

    protected AllProfileResourceRecordSetApi allApi(Zone zone) {
        return manager.api().recordSetsInZone(zone.idOrName());
    }

    protected GeoResourceRecordSetApi geoApi(Zone zone) {
        Optional<GeoResourceRecordSetApi> geoOption = manager.api().geoRecordSetsInZone(zone.idOrName());
        if (!geoOption.isPresent())
            throw new SkipException("geo not available or not available in zone " + zone);
        return geoOption.get();
    }

    protected WeightedResourceRecordSetApi weightedApi(Zone zone) {
        Optional<WeightedResourceRecordSetApi> weightedOption = manager.api().weightedRecordSetsInZone(zone.idOrName());
        if (!weightedOption.isPresent())
            throw new SkipException("weighted not available or not available in zone " + zone);
        return weightedOption.get();
    }
}
