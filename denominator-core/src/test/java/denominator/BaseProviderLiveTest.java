package denominator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.any;
import static com.google.common.collect.Iterators.size;
import static com.google.common.io.Closeables.close;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.nameAndType;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.logging.Logger.getAnonymousLogger;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.UnsignedInteger;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseProviderLiveTest {

    protected DNSApiManager manager;
    protected String mutableZone;

    @Test
    public void testListZones() {
        skipIfNoCredentials();
        int zoneCount = size(zoneApi().list());
        getAnonymousLogger().info(format("%s ::: zones: %s", manager, zoneCount));
    }

    @Test
    private void testListRRSs() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            for (Iterator<ResourceRecordSet<?>> rrsIterator = rrsApi(zoneName).list(); rrsIterator.hasNext();) {
                ResourceRecordSet<?> rrs = rrsIterator.next();
                recordTypeCounts.getUnchecked(rrs.getType()).addAndGet(rrs.size());
                checkRRS(rrs);
                Optional<ResourceRecordSet<?>> byNameAndType = rrsApi(zoneName).getByNameAndType(rrs.getName(), rrs.getType());
                assertTrue(byNameAndType.isPresent(), "could not lookup by name and type: " + rrs);
                assertEquals(rrsApi(zoneName).getByNameAndType(rrs.getName(), rrs.getType()).get(), rrs);
            }
        }
        logRecordSummary();
    }

    @Test
    private void testGetByNameAndTypeWhenAbsent() {
        skipIfNoCredentials();
        for (Iterator<String> zone = zoneApi().list(); zone.hasNext();) {
            String zoneName = zone.next();
            assertEquals(rrsApi(zoneName).getByNameAndType("ARGHH." + zoneName, "TXT"), Optional.absent());
        }
    }

    private void logRecordSummary() {
        for (Entry<String, AtomicLong> entry : recordTypeCounts.asMap().entrySet())
            getAnonymousLogger().info(
                    format("%s ::: %s records: count: %s", manager, entry.getKey(), entry.getValue()));
    }

    private void checkRRS(ResourceRecordSet<?> rrs) {
        checkNotNull(rrs.getName(), "Name: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.getType(), "Type: ResourceRecordSet %s", rrs);
        checkNotNull(rrs.getTTL(), "TTL: ResourceRecordSet %s", rrs);
        assertTrue(!rrs.isEmpty(), "Values absent on ResourceRecordSet: " + rrs);
    }

    String recordPrefix = "www." + getProperty("user.name").replace('.', '-');
    String recordType = "A";
    AData rdata = AData.create("1.1.1.1");

    @Test
    private void createNewRRS() {
        skipIfNoCredentials();
        String zoneName = skipIfNoMutableZone();
        String recordName = recordPrefix + "." + zoneName;

        skipIfRRSetExists(zoneName, recordName, recordType);

        UnsignedInteger ttl = UnsignedInteger.fromIntBits(1800);

        rrsApi(zoneName).add(a(recordName, ttl.intValue(), rdata.getAddress()));

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zoneName).getByNameAndType(recordName, recordType);

        assertTrue(rrs.isPresent(), format("recordset(%s, %s) not present in zone(%s)", recordName, recordType, zoneName));
        checkRRS(rrs.get());
        assertEquals(rrs.get().getName(), recordName);
        assertEquals(rrs.get().getTTL().get(), ttl);
        assertEquals(rrs.get().getType(), recordType);
        assertEquals(rrs.get().size(), 1);
        assertEquals(rrs.get().get(0), rdata);
    }

    AData rdata2 = AData.create("1.1.1.2");

    @Test(dependsOnMethods = "createNewRRS")
    private void addRecordToExistingRRS() {
        skipIfNoCredentials();
        String zoneName = skipIfNoMutableZone();
        String recordName = recordPrefix + "." + zoneName;

        rrsApi(zoneName).add(a(recordName, rdata2.getAddress()));

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zoneName).getByNameAndType(recordName, recordType);

        assertTrue(rrs.isPresent(), format("recordset(%s, %s) not present in zone(%s)", recordName, recordType, zoneName));
        checkRRS(rrs.get());
        assertEquals(rrs.get().getName(), recordName);
        assertEquals(rrs.get().getType(), recordType);
        assertEquals(rrs.get().size(), 2);
        assertEquals(rrs.get().get(0), rdata);
        assertEquals(rrs.get().get(1), rdata2);
    }

    @Test(dependsOnMethods = "addRecordToExistingRRS")
    private void applyTTL() {
        skipIfNoCredentials();
        String zoneName = skipIfNoMutableZone();
        String recordName = recordPrefix + "." + zoneName;

        rrsApi(zoneName).applyTTLToNameAndType(recordName, recordType, UnsignedInteger.fromIntBits(200000));

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zoneName).getByNameAndType(recordName, recordType);

        assertTrue(rrs.isPresent(), format("recordset(%s, %s) not present in zone(%s)", recordName, recordType, zoneName));
        checkRRS(rrs.get());
        assertEquals(rrs.get().getName(), recordName);
        assertEquals(rrs.get().getType(), recordType);
        assertEquals(rrs.get().getTTL().get().intValue(), 200000);
        assertEquals(rrs.get().size(), 2);
        assertEquals(rrs.get().get(0), rdata);
        assertEquals(rrs.get().get(1), rdata2);
    }

    AData rdata3 = AData.create("1.1.1.3");

    @Test(dependsOnMethods = "applyTTL")
    private void replaceExistingRRSUpdatingTTL() {
        skipIfNoCredentials();
        String zoneName = skipIfNoMutableZone();
        String recordName = recordPrefix + "." + zoneName;

        rrsApi(zoneName).replace(a(recordName, 10000, ImmutableSet.of(rdata.getAddress(), rdata3.getAddress())));

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zoneName).getByNameAndType(recordName, recordType);

        assertTrue(rrs.isPresent(), format("recordset(%s, %s) not present in zone(%s)", recordName, recordType, zoneName));
        checkRRS(rrs.get());
        assertEquals(rrs.get().getName(), recordName);
        assertEquals(rrs.get().getType(), recordType);
        assertEquals(rrs.get().getTTL().get().intValue(), 10000);
        assertEquals(rrs.get().size(), 2);
        assertEquals(rrs.get().get(0), rdata);
        assertEquals(rrs.get().get(1), rdata3);
    }

    @Test(dependsOnMethods = "replaceExistingRRSUpdatingTTL")
    private void removeRecordFromExistingRRS() {
        skipIfNoCredentials();
        String zoneName = skipIfNoMutableZone();
        String recordName = recordPrefix + "." + zoneName;

        rrsApi(zoneName).remove(a(recordName, rdata3.getAddress()));

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zoneName).getByNameAndType(recordName, recordType);

        assertTrue(rrs.isPresent(), format("recordset(%s, %s) not present in zone(%s)", recordName, recordType, zoneName));
        checkRRS(rrs.get());
        assertEquals(rrs.get().getName(), recordName);
        assertEquals(rrs.get().getType(), recordType);
        assertEquals(rrs.get().size(), 1);
        assertEquals(rrs.get().get(0), rdata);
    }

    @Test(dependsOnMethods = "removeRecordFromExistingRRS")
    private void removeLastRecordFromExistingRRSRemovesRRS() {
        skipIfNoCredentials();
        String zoneName = skipIfNoMutableZone();
        String recordName = recordPrefix + "." + zoneName;

        rrsApi(zoneName).remove(a(recordName, rdata.getAddress()));

        Optional<ResourceRecordSet<?>> rrs = rrsApi(zoneName).getByNameAndType(recordName, recordType);

        assertFalse(rrs.isPresent(), format("recordset(%s, %s) still present in zone(%s)", recordName, recordType, zoneName));
    }

    protected void skipIfRRSetExists(String zoneName, String name, String type) {
        if (any(rrsApi(zoneName).list(), nameAndType(name, type)))
            throw new SkipException(format("recordset with name %s and type %s already exists", name, type));
    }

    @AfterClass
    private void tearDown() throws IOException {
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

    private ZoneApi zoneApi() {
        return manager.getApi().getZoneApi();
    }

    private ResourceRecordSetApi rrsApi(String zoneName) {
        return manager.getApi().getResourceRecordSetApiForZone(zoneName);
    }

    LoadingCache<String, AtomicLong> recordTypeCounts = CacheBuilder.newBuilder().build(
            new CacheLoader<String, AtomicLong>() {
                public AtomicLong load(String key) throws Exception {
                    return new AtomicLong();
                }
            });
}
