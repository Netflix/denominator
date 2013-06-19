package denominator.profile;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Maps.filterKeys;
import static com.google.common.collect.Multimaps.filterValues;
import static denominator.model.profile.Geo.asGeo;
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
import com.google.common.collect.ImmutableMap;
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
        return filterKeys(super.stockRRSets(), in(manager.provider().profileToRecordTypes().get("geo")));
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
    
    @Test(dataProvider = "geoRecords")
    private void createNewRRSWithAllProfileApi(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();
        
        Multimap<String, String> regions = geoApi(skipIfNoMutableZone()).supportedRegions();

        Multimap<String, String> allButOne = filterValues(regions, not(equalTo(getLast(regions.values()))));
        Multimap<String, String> onlyOne = filterValues(regions, equalTo(getLast(regions.values())));

        int i = 0;
        for (String qualifier : new String[] { qualifier1, qualifier2 }) {
            skipIfRRSetExists(zone, recordSet.name(), recordSet.type(), qualifier);

            // prove map can be used as a profile
            Map<String, Object> territories = ImmutableMap.<String, Object> builder()
                                                          .put("type", "geo")
                                                          .put("regions", i == 0 ? allButOne.asMap() : onlyOne.asMap())
                                                          .build();

            allApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
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
            assertEquals(asGeo(rrs.get()).regions(), asGeo(territories).regions());
            assertEquals(rrs.get().rdata().size(), 1);
            assertEquals(rrs.get().rdata().get(0), recordSet.rdata().get(i++));
        }
    }

    @Test(dependsOnMethods = "createNewRRSWithAllProfileApi", dataProvider = "geoRecords")
    private void yankTerritoryIntoAnotherRRSet(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        ResourceRecordSet<?> rrs1 = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1).get();

        Multimap<String, String> regionsFrom1 = asGeo(rrs1).regions();
        Multimap<String, String> toYank = filterValues(regionsFrom1, equalTo(getLast(regionsFrom1.values())));

        ResourceRecordSet<?> rrs2 = geoApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2).get();

        Multimap<String, String> plus1 = ImmutableMultimap.<String, String> builder()//
                .putAll(asGeo(rrs2).regions())//
                .putAll(toYank).build();

        geoApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                          .name(recordSet.name())
                                          .type(recordSet.type())
                                          .qualifier(qualifier2)
                                          .ttl(rrs2.ttl().orNull())
                                          .addProfile(Geo.create(plus1))
                                          .addAll(rrs2.rdata()).build());

        rrs1 = geoApi(zone).getByNameTypeAndQualifier(
                recordSet.name(), recordSet.type(), qualifier1).get();

        Multimap<String, String> minus1 = ImmutableMultimap.copyOf(
                filterValues(regionsFrom1, not(equalTo(getLast(regionsFrom1.values())))));

        assertEquals(asGeo(rrs1).regions(), minus1);

        rrs2 = geoApi(zone).getByNameTypeAndQualifier(
                recordSet.name(), recordSet.type(), qualifier2).get();

        assertEquals(asGeo(rrs2).regions(), plus1);
    }

    @Test(dependsOnMethods = "yankTerritoryIntoAnotherRRSet", dataProvider = "geoRecords")
    private void changeTTLWithAllProfileApi(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        int i = 0;
        for (String qualifier : new String[] { qualifier1, qualifier2 }) {
            ResourceRecordSet<?> rrs = geoApi(zone).getByNameTypeAndQualifier(recordSet.name(), recordSet.type(),
                    qualifier).get();

            int ttl = rrs.ttl().or(60000) + 60000;
            Geo oldGeo = asGeo(rrs);

            allApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
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
            assertEquals(asGeo(rrs).regions(), oldGeo.regions());
            assertEquals(rrs.rdata().size(), 1);
            assertEquals(rrs.rdata().get(0), recordSet.rdata().get(i++));
        }
    }

    @Test(dependsOnMethods = "changeTTLWithAllProfileApi", dataProvider = "geoRecords")
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
        allApi(zone).deleteByNameAndType(recordSet.name(), recordSet.type());

        rrs = allApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        assertFalse(rrs.isPresent(), format("recordset(%s, %s, %s) still present in %s",
                recordSet.name(), recordSet.type(), qualifier2, zone));
    }
}
