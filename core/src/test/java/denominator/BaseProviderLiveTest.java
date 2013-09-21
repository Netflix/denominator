package denominator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.any;
import static com.google.common.io.Closeables.close;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameTypeAndQualifierEqualTo;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.spf;
import static denominator.model.ResourceRecordSets.txt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.bind.MapTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
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
        String recordSuffix = recordPrefix + "." + zone.name();
        String rdataSuffix = recordSuffix;

        if (addTrailingDotToZone && !recordSuffix.endsWith(".")) {
            rdataSuffix = recordSuffix + ".";
        }

        Builder<String, ResourceRecordSet<?>> builder = ImmutableMap.<String, ResourceRecordSet<?>> builder();
        builder.put(
                "AAAA",
                aaaa("ipv6-" + recordSuffix, ImmutableList.of("2001:1DB8:85A3:1001:1001:8A2E:1371:7334",
                        "2001:1DB8:85A3:1001:1001:8A2E:1371:7335", "2001:1DB8:85A3:1001:1001:8A2E:1371:7336")));
        builder.put("A", a("ipv4-" + recordSuffix, ImmutableList.of("192.0.2.1", "198.51.100.1", "203.0.113.1")));
        builder.put(
                "CNAME",
                cname("www-" + recordSuffix,
                        ImmutableList.of("www-north-" + rdataSuffix, "www-east-" + rdataSuffix, "www-west-"
                                + rdataSuffix)));
        builder.put(
                "MX",
                ResourceRecordSet.<MXData> builder().name("mail-" + recordSuffix).type("MX")
                        .add(MXData.create(10, "mail1-" + rdataSuffix)).add(MXData.create(10, "mail2-" + rdataSuffix))
                        .add(MXData.create(10, "mail3-" + rdataSuffix)).build());
        builder.put(
                "NS",
                ns("ns-" + recordSuffix,
                        ImmutableList.of("ns1-" + rdataSuffix, "ns2-" + rdataSuffix, "ns3-" + rdataSuffix)));
        builder.put(
                "PTR",
                ptr("ptr-" + recordSuffix,
                        ImmutableList.of("ptr1-" + rdataSuffix, "ptr2-" + rdataSuffix, "ptr3-" + rdataSuffix)));
        builder.put("SPF",
                spf("spf-" + recordSuffix, ImmutableList.of("v=spf1 a -all", "v=spf1 mx -all", "v=spf1 ipv6 -all")));
        builder.put(
                "SRV",
                ResourceRecordSet.<SRVData> builder().name("_http._tcp" + recordSuffix).type("SRV")
                        .add(SRVData.builder().priority(0).weight(1).port(80).target("ipv4-" + rdataSuffix).build())
                        .add(SRVData.builder().priority(0).weight(1).port(8080).target("ipv4-" + rdataSuffix).build())
                        .add(SRVData.builder().priority(0).weight(1).port(443).target("ipv4-" + rdataSuffix).build())
                        .build());
        builder.put(
                "SSHFP",
                ResourceRecordSet.<SSHFPData> builder().name("ipv4-" + recordSuffix).type("SSHFP")
                        .add(SSHFPData.createDSA("190E37C5B5DB9A1C455E648A41AF3CC83F99F102"))
                        .add(SSHFPData.createDSA("290E37C5B5DB9A1C455E648A41AF3CC83F99F102"))
                        .add(SSHFPData.createDSA("390E37C5B5DB9A1C455E648A41AF3CC83F99F102")).build());
        builder.put("TXT",
                txt("txt-" + recordSuffix, ImmutableList.of("made in norway", "made in sweden", "made in finland")));
        return builder.build();
    }

    protected String recordPrefix = getClass().getSimpleName().toLowerCase() + "."
            + getProperty("user.name").replace('.', '-');

    protected DNSApiManager manager;
    protected Zone mutableZone;
    // TODO: metadata about whether zone names have trailing dots or not.
    protected boolean addTrailingDotToZone = true;

    protected void checkRRS(ResourceRecordSet<?> rrs) {
        checkNotNull(rrs.name(), "Name: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.type(), "Type: ResourceRecordSet %s", rrs);
        assertTrue(!rrs.records().isEmpty(), "Values absent on ResourceRecordSet: " + rrs);
    }

    protected void skipIfRRSetExists(Zone zone, String name, String type) {
        if (any(rrsApi(zone).iterator(), predicate(nameAndTypeEqualTo(name, type))))
            throw new SkipException(format("recordset(%s, %s) already exists in %s", name, type, zone));
    }

    protected void skipIfRRSetExists(Zone zone, String name, String type, String qualifier) {
        if (any(rrsApi(zone).iterator(), predicate(nameTypeAndQualifierEqualTo(name, type, qualifier))))
            throw new SkipException(format("recordset(%s, %s, %s) already exists in %s", name, type, qualifier, zone));
    }

    protected void assertPresent(ResourceRecordSet<?> rrs, Zone zone, String name, String type, String qualifier) {
        if (rrs == null) {
            throw new AssertionError(format("recordset(%s, %s%s) not present in %s; rrsets: %s", name, type,
                    qualifier != null ? ", " + qualifier : "", zone, Iterators.toString(rrsApi(zone).iterator())));
        }
    }

    protected void assertPresent(ResourceRecordSet<?> rrs, Zone zone, String name, String type) {
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
            List<Zone> currentZones = new ArrayList<Zone>();
            for (Zone zone : manager.api().zones()) {
                if (mutableZone.equals(zone.name())) {
                    this.mutableZone = zone;
                    return;
                }
                currentZones.add(zone);
            }
            checkArgument(false, "zone %s not found in %s", mutableZone, currentZones);
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
        GeoResourceRecordSetApi geoOption = manager.api().geoRecordSetsInZone(zone.idOrName());
        if (geoOption == null)
            throw new SkipException("geo not available or not available in zone " + zone);
        return geoOption;
    }

    protected WeightedResourceRecordSetApi weightedApi(Zone zone) {
        WeightedResourceRecordSetApi weightedOption = manager.api().weightedRecordSetsInZone(zone.idOrName());
        if (weightedOption == null)
            throw new SkipException("weighted not available or not available in zone " + zone);
        return weightedOption;
    }

    public static <T> Predicate<T> predicate(final Filter<T> in) {
        return new Predicate<T>() {

            @Override
            public boolean apply(T input) {
                return in.apply(input);
            }

            @Override
            public String toString() {
                return in.toString();
            }
        };
    }

    public static <K, V> Multimap<K, V> multimap(Map<K, Collection<V>> in) {
        ImmutableMultimap.Builder<K, V> builder = ImmutableMultimap.<K, V> builder();
        for (Entry<K, Collection<V>> entry : in.entrySet())
            builder.putAll(entry.getKey(), entry.getValue());
        return builder.build();
    }

    private final TypeToken<Map<String, Object>> token = new TypeToken<Map<String, Object>>() {
    };

    private final TypeAdapter<Map<String, Object>> doubleToInt = new TypeAdapter<Map<String, Object>>() {
        TypeAdapter<Map<String, Object>> delegate = new MapTypeAdapterFactory(new ConstructorConstructor(
                Collections.<Type, InstanceCreator<?>> emptyMap()), false).create(new Gson(), token);

        @Override
        public void write(JsonWriter out, Map<String, Object> value) throws IOException {
            delegate.write(out, value);
        }

        @Override
        public Map<String, Object> read(JsonReader in) throws IOException {
            Map<String, Object> map = delegate.read(in);
            for (Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Double) {
                    entry.setValue(Double.class.cast(entry.getValue()).intValue());
                }
            }
            return map;
        }
    }.nullSafe();

    // deals with scenario where gson Object type treats all numbers as doubles.
    protected final Gson json = new GsonBuilder().registerTypeAdapter(token.getType(), doubleToInt).create();
}
