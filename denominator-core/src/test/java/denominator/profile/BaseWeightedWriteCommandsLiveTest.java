package denominator.profile;

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Maps.filterKeys;
import static denominator.model.profile.Weighted.asWeighted;
import static denominator.profile.BaseWeightedReadOnlyLiveTest.checkWeightedRRS;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import denominator.BaseProviderLiveTest;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Weighted;

/**
 * extend this and initialize manager {@link BeforeClass}
 */
public abstract class BaseWeightedWriteCommandsLiveTest extends BaseProviderLiveTest {

    protected Map<String, ResourceRecordSet<?>> stockRRSets() {
        return filterKeys(super.stockRRSets(), in(manager.provider().profileToRecordTypes().get("weighted")));
    }

    String qualifier1 = "US-East";
    String qualifier2 = "US-West";

    /**
     * value at index 1 in the rrs is used for qualifier2
     */
    @DataProvider(name = "weightedRecords")
    public Object[][] weightedRecords() {
        ImmutableList<ResourceRecordSet<?>> rrsets = ImmutableList.copyOf(stockRRSets().values());
        Object[][] data = new Object[rrsets.size()][1];
        for (int i = 0; i < rrsets.size(); i++)
            data[i][0] = rrsets.get(i);
        return data;
    }
    
    @Test(dataProvider = "weightedRecords")
    private void createNewRRSWithAllProfileApi(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        int i = 0;
        for (String qualifier : new String[] { qualifier1, qualifier2 }) {
            skipIfRRSetExists(zone, recordSet.name(), recordSet.type(), qualifier);
    
            allApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                              .name(recordSet.name())
                                              .type(recordSet.type())
                                              .ttl(1800)
                                              .qualifier(qualifier)
                                              // prove maps can be used as profiles
                                              .addProfile(ImmutableMap.<String, Object> builder()//
                                                      .put("type", "weighted")//
                                                      .put("weight", 0).build())
                                              .add(recordSet.rdata().get(i)).build());
    
            ResourceRecordSet<?> rrs = weightedApi(zone)
                    .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier);
    
            assertPresent(rrs, zone, recordSet.name(), recordSet.type(), qualifier);
    
            checkWeightedRRS(rrs);
            assertEquals(rrs.name(), recordSet.name());
            assertEquals(rrs.ttl(), Integer.valueOf(1800));
            assertEquals(rrs.type(), recordSet.type());
            assertEquals(rrs.qualifier(), qualifier);
            assertEquals(asWeighted(rrs).weight(), 0);
            assertEquals(rrs.rdata().size(), 1);
            assertEquals(rrs.rdata().get(0), recordSet.rdata().get(i++));
        }
    }

    @Test(dependsOnMethods = "createNewRRSWithAllProfileApi", dataProvider = "weightedRecords")
    private void replaceWeight(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();
        
        int heaviest =  weightedApi(zone).supportedWeights().last();

        weightedApi(zone).put(ResourceRecordSet.<Map<String, Object>> builder()
                                               .name(recordSet.name())
                                               .type(recordSet.type())
                                               .ttl(1800)
                                               .qualifier(qualifier1)
                                               .addProfile(Weighted.create(heaviest))
                                               .add(recordSet.rdata().get(0)).build());

        ResourceRecordSet<?> rrs1 = weightedApi(zone).getByNameTypeAndQualifier(
                recordSet.name(), recordSet.type(), qualifier1);

        assertEquals(asWeighted(rrs1).weight(), heaviest);

        ResourceRecordSet<?> rrs2 = weightedApi(zone).getByNameTypeAndQualifier(
                recordSet.name(), recordSet.type(), qualifier2);

        assertEquals(asWeighted(rrs2).weight(), 0);
    }

    @Test(dependsOnMethods = "replaceWeight", dataProvider = "weightedRecords")
    private void deleteOneQualifierDoesntAffectOther(ResourceRecordSet<?> recordSet) {
        skipIfNoCredentials();
        Zone zone = skipIfNoMutableZone();

        weightedApi(zone).deleteByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        ResourceRecordSet<?> rrs = weightedApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        assertNull(rrs, format("recordset(%s, %s, %s) still present in %s",
                recordSet.name(), recordSet.type(), qualifier1, zone));

        rrs = weightedApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        assertPresent(rrs, zone, recordSet.name(), recordSet.type(), qualifier2);
        
        // safe to call twice
        weightedApi(zone).deleteByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier1);

        // clean up the other one
        allApi(zone).deleteByNameAndType(recordSet.name(), recordSet.type());

        rrs = allApi(zone)
                .getByNameTypeAndQualifier(recordSet.name(), recordSet.type(), qualifier2);

        assertNull(rrs, format("recordset(%s, %s, %s) still present in %s",
                recordSet.name(), recordSet.type(), qualifier2, zone));
    }
}
