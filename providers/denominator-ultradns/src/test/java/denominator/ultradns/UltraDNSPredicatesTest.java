package denominator.ultradns;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.sql.Date;

import org.jclouds.ultradns.ws.domain.ResourceRecord;
import org.jclouds.ultradns.ws.domain.ResourceRecordDetail;
import org.jclouds.ultradns.ws.domain.RoundRobinPool;
import org.testng.annotations.Test;



@Test
public class UltraDNSPredicatesTest {

    ResourceRecord a = ResourceRecord.rrBuilder().name("www.foo.com.")
                                                 .type(1)
                                                 .ttl(3600)
                                                 .rdata("1.1.1.1").build();

    ResourceRecordDetail aRecord = ResourceRecordDetail.builder()
                                                           .guid("AAAAAAAAAAAA")
                                                           .zoneId("0000000000000001")
                                                           .zoneName("foo.com.")
                                                           .created(new Date(1l))
                                                           .modified(new Date(1l))
                                                           .record(a).build();

    RoundRobinPool pool = RoundRobinPool.builder()
                                        .dname(a.getName())
                                        .id("POOLA")
                                        .name("A")
                                        .zoneId(aRecord.getGuid()).build();

    public void resourceTypeEqualToFalseOnDifferentType() {
        assertFalse(UltraDNSPredicates.resourceTypeEqualTo(28).apply(a));
    }

    public void resourceTypeEqualToTrueOnSameType() {
        assertTrue(UltraDNSPredicates.resourceTypeEqualTo(a.getType()).apply(a));
    }

    public void recordGuidEqualToFalseOnDifferentGuid() {
        assertFalse(UltraDNSPredicates.recordGuidEqualTo("BBBBBBBBBBBB").apply(aRecord));
    }

    public void recordGuidEqualToTrueOnSameGuid() {
        assertTrue(UltraDNSPredicates.recordGuidEqualTo(aRecord.getGuid()).apply(aRecord));
    }

    public void poolNameEqualToFalseOnDifferentName() {
        assertFalse(UltraDNSPredicates.poolNameEqualTo("AAAA").apply(pool));
    }

    public void poolNameEqualToTrueOnSameName() {
        assertTrue(UltraDNSPredicates.poolNameEqualTo(pool.getName()).apply(pool));
    }

    public void poolDNameEqualToFalseOnDifferentDName() {
        assertFalse(UltraDNSPredicates.poolDNameEqualTo("www.bar.com.").apply(pool));
    }

    public void poolDNameEqualToTrueOnSameDName() {
        assertTrue(UltraDNSPredicates.poolDNameEqualTo(pool.getDName()).apply(pool));
    }
}
