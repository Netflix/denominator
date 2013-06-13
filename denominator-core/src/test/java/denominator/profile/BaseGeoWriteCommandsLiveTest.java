package denominator.profile;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Maps.filterKeys;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.ResourceRecordSets.toProfile;
import static denominator.profile.BaseGeoReadOnlyLiveTest.checkGeoRRS;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import denominator.BaseProviderLiveTest;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseGeoWriteCommandsLiveTest extends BaseProviderLiveTest {

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        return filterKeys(super.stockRRSets(), in(geoApi(skipIfNoMutableZone()).supportedTypes()));
    }

    String qualifier1 = "US-East";
    String qualifier2 = "US-West";

    /**
     * value at index 1 in the rrs is used for qualifier2
     */
    @DataProvider(name = "geoRecords")
    public Object[][] geoRecords() {
        ImmutableList<ResourceRecordSet<?>> rrsets = ImmutableList.copyOf(stockRRSets().values());
        Object[][] data = new Object[rrsets.size()][1];
        for (int i = 0; i < rrsets.size(); i++)
            data[i][0] = rrsets.get(i);
        return data;
    }
    
    @SuppressWarnings("deprecation")
    @Test(dataProvider = "geoRecords")
    private void createNewRRS(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();
        
        Multimap<String, String> regions = geoApi(skipIfNoMutableZone()).supportedRegions();

        Multimap<String, String> allButOne = filterValues(regions, not(equalTo(getLast(regions.values()))));
        Multimap<String, String> onlyOne = filterValues(regions, equalTo(getLast(regions.values())));

        int i = 0;
        for (String qualifier : new String[] { qualifier1, qualifier2 }) {
            skipIfRRSetExists(zone, recordSet.name(), recordSet.type(), qualifier);

            // TODO: remove qualifier from geo in 2.0
            Geo territories = Geo.create(qualifier, i == 0 ? allButOne : onlyOne);

            geoApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                              .name(recordSet.name())
                                              .type(recordSet.type())
                                              .ttl(1800)
                                              .qualifier(qualifier)
                                              .addProfile(territories)
                                              .add(recordSet.rdata().get(i)).build());
    
            Optional<ResourceRecordSet<?>> rrs = geoApi(zone)
                    .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier);
    
            assertPresent(rrs, zone, recordSet.name(), recordSet.type(), qualifier);
    
            checkGeoRRS(rrs.get());
            assertEquals(rrs.get().name(), recordSet.name());
            assertEquals(rrs.get().ttl().get(), Integer.valueOf(1800));
            assertEquals(rrs.get().type(), recordSet.type());
            assertEquals(rrs.get().qualifier().get(), qualifier);
            assertEquals(toProfile(Geo.class).apply(rrs.get()).regions(), territories.regions());
            assertEquals(rrs.get().rdata().size(), 1);
            assertEquals(rrs.get().rdata().get(0), recordSet.rdata().get(i++));
        }
    }

    @Test(dependsOnMethods = "createNewRRS", dataProvider = "geoRecords")
    private void yankTerritoryIntoAnotherRRSet(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        ResourceRecordSet<?> rrs1 = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1).get();

        Multimap<String, String> regionsFrom1 = toProfile(Geo.class).apply(rrs1).regions();
        Multimap<String, String> toYank = filterValues(regionsFrom1, equalTo(getLast(regionsFrom1.values())));

        ResourceRecordSet<?> rrs2 = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2).get();

        Multimap<String, String> plus1 = ImmutableMultimap.<String, String> builder()//
                .putAll(toProfile(Geo.class).apply(rrs2).regions())//
                .putAll(toYank).build();

        geoApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                          .name(recordSet.name())
                                          .type(recordSet.type())
                                          .qualifier(qualifier2)
                                          .ttl(rrs2.ttl().orNull())
                                          .addProfile(Geo.create(qualifier2, plus1))
                                          .addAll(rrs2.rdata()).build());

        rrs1 = geoApi(zone).getByNameTypeAndQualifier(
                recordSet.name(), recordSet.type(), qualifier1).get();

        Multimap<String, String> minus1 = ImmutableMultimap.copyOf(
                filterValues(regionsFrom1, not(equalTo(getLast(regionsFrom1.values())))));

        assertEquals(toProfile(Geo.class).apply(rrs1).regions(), minus1);

        rrs2 = geoApi(zone).getByNameTypeAndQualifier(
                recordSet.name(), recordSet.type(), qualifier2).get();

        assertEquals(toProfile(Geo.class).apply(rrs2).regions(), plus1);
    }

    @Test(dependsOnMethods = "yankTerritoryIntoAnotherRRSet", dataProvider = "geoRecords")
    private void changeTTLIndependently(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        int i = 0;
        for (String qualifier : new String[] { qualifier1, qualifier2 }) {
            ResourceRecordSet<?> rrs = geoApi(zone).getByNameTypeAndQualifier(recordSet.name(), recordSet.type(),
                    qualifier).get();

            int ttl = rrs.ttl().or(60000) + 60000;
            Geo oldGeo = toProfile(Geo.class).apply(rrs);

            geoApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                    .name(recordSet.name())
                    .type(recordSet.type())
                    .ttl(ttl)
                    .qualifier(qualifier)
                    .addProfile(oldGeo)
                    .add(recordSet.rdata().get(i)).build());

            rrs = geoApi(zone).getByNameTypeAndQualifier(recordSet.name(), recordSet.type(),
                    qualifier).get();

            checkGeoRRS(rrs);
            assertEquals(rrs.name(), recordSet.name());
            assertEquals(rrs.ttl().get(), Integer.valueOf(ttl));
            assertEquals(rrs.type(), recordSet.type());
            assertEquals(rrs.qualifier().get(), qualifier);
            assertEquals(toProfile(Geo.class).apply(rrs).regions(), oldGeo.regions());
            assertEquals(rrs.rdata().size(), 1);
            assertEquals(rrs.rdata().get(0), recordSet.rdata().get(i++));
        }
    }

    @Test(dependsOnMethods = "changeTTLIndependently", dataProvider = "geoRecords")
    @Deprecated
    private void applyRegionsToNameTypeAndGroup(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        ResourceRecordSet<?> rrs2 = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2).get();

        Geo rrs2Geo = toProfile(Geo.class).apply(rrs2);
        
        String last = getLast(rrs2Geo.regions().values());
        
        Multimap<String, String> regions = filterValues(rrs2Geo.regions(), not(equalTo(last)));

        geoApi(zone).applyRegionsToNameTypeAndGroup(regions, rrs2.name(), rrs2.type(), qualifier2);

        ResourceRecordSet<?> rrs = 
                geoApi(zone).getByNameTypeAndQualifier(rrs2.name(), rrs2.type(), qualifier2).get();

        checkRRS(rrs);
        assertEquals(rrs.name(), rrs2.name());
        assertEquals(rrs.type(), rrs2.type());
        assertEquals(rrs.ttl(), rrs2.ttl());
        assertEquals(ImmutableList.copyOf(rrs), ImmutableList.copyOf(rrs2));
        assertEquals(toProfile(Geo.class).apply(rrs).regions(), ImmutableMultimap.copyOf(regions));
    }

    @Test(dependsOnMethods = "applyRegionsToNameTypeAndGroup", dataProvider = "geoRecords")
    @Deprecated
    private void applyTTLToNameTypeAndGroup(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        int i = 0;
        for (String qualifier : new String[] { qualifier1, qualifier2 }) {
            int ttl = 60000;

            geoApi(zone).applyTTLToNameTypeAndGroup(ttl, recordSet.name(), recordSet.type(), qualifier);

            ResourceRecordSet<?> rrs = geoApi(zone).getByNameTypeAndQualifier(recordSet.name(), recordSet.type(),
                    qualifier).get();

            checkGeoRRS(rrs);
            assertEquals(rrs.name(), recordSet.name());
            assertEquals(rrs.ttl().get(), Integer.valueOf(ttl));
            assertEquals(rrs.type(), recordSet.type());
            assertEquals(rrs.qualifier().get(), qualifier);
            assertEquals(rrs.rdata().size(), 1);
            assertEquals(rrs.rdata().get(0), recordSet.rdata().get(i++));
        }
    }

    @Test(dependsOnMethods = "applyTTLToNameTypeAndGroup", dataProvider = "geoRecords")
    private void deleteOneQualifierDoesntAffectOther(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        geoApi(zone).deleteByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        Optional<ResourceRecordSet<?>> rrs = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        assertFalse(rrs.isPresent(), format("recordset(%s, %s, %s) still present in %s",
                recordSet.name(), recordSet.type(), qualifier1, zone));

        rrs = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        assertPresent(rrs, zone, recordSet.name(), recordSet.type(), qualifier2);
        
        // safe to call twice
        geoApi(zone).deleteByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        // clean up the other one
        geoApi(zone).deleteByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        rrs = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        assertFalse(rrs.isPresent(), format("recordset(%s, %s, %s) still present in %s",
                recordSet.name(), recordSet.type(), qualifier2, zone));
    }
}
