package denominator.ultradns;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.ns;
import static org.jclouds.ultradns.ws.domain.ResourceRecord.rrBuilder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Iterator;

import org.jclouds.date.internal.SimpleDateFormatDateService;
import org.jclouds.ultradns.ws.domain.ResourceRecordDetail;
import org.jclouds.ultradns.ws.domain.ResourceRecordDetail.Builder;
import org.testng.annotations.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.SOAData;

public class GroupByRecordNameAndTypeIteratorTest {
    SimpleDateFormatDateService dateService = new SimpleDateFormatDateService();
    
    @Test
    public void testResourceRecordSetCreation() throws Exception {
        ImmutableList<ResourceRecordDetail> metadataList = createMetadataList();
        GroupByRecordNameAndTypeIterator  iter = new GroupByRecordNameAndTypeIterator(metadataList.iterator());
        assertTrue(iter.hasNext());
    
        Iterator<ResourceRecordSet<?>> expectedIter = expected();
        while (iter.hasNext()) {
            assertTrue(expectedIter.hasNext());
            ResourceRecordSet<?> rrset1 = iter.next();
            ResourceRecordSet<?> rrset2 = expectedIter.next();
            assertEquals(rrset1, rrset2);
        }
        assertFalse(expectedIter.hasNext());
    }

    public Iterator<ResourceRecordSet<?>> expected() {
        ImmutableList<ResourceRecordSet<?>> rrsetList = ImmutableList.<ResourceRecordSet<?>>builder()
                .add(a("www.jclouds.org.", 3600, "1.2.3.4"))
                .add(ns("jclouds.org.", 86400,
                        ImmutableList.of("pdns1.ultradns.net.", "pdns2.ultradns.net.", "pdns3.ultradns.org.", 
                                "pdns4.ultradns.org.", "pdns5.ultradns.info.", "pdns6.ultradns.co.uk.")))
                .add(a("jclouds.org.", 3000, "1.2.3.4"))
                .add(ResourceRecordSet.<SOAData>builder()
                        .name("jclouds.org.")
                        .ttl(3600)
                        .type("SOA")
                        .add(SOAData.builder()
                                    .mname("pdns2.ultradns.net.")
                                    .rname("admin.jclouds.org.")
                                    .serial(2011092701)
                                    .refresh(10800)
                                    .retry(3600)
                                    .expire(604800)
                                    .minimum(86400).build())
                         .build())
                .build();
        return rrsetList.iterator();
    }

    public ImmutableList<ResourceRecordDetail> createMetadataList() throws Exception {
          Builder builder = ResourceRecordDetail.builder().zoneId("0000000000000001").zoneName("jclouds.org.");
          
          return Ordering.usingToString().immutableSortedCopy(ImmutableList.<ResourceRecordDetail> builder()
                  .add(builder.guid("04023A2507B6468F")
                          .created(dateService.iso8601DateParse("2010-10-02T16:57:16.000Z"))
                          .modified(dateService.iso8601DateParse("2011-09-27T23:49:21.000Z"))
                          .record(rrBuilder().type(1).name("www.jclouds.org.").ttl(3600).rdata("1.2.3.4")).build())
                  .add(builder.guid("0B0338C2023F7969")
                          .created(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .modified(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .record(rrBuilder().type(2).name("jclouds.org.").ttl(86400).rdata("pdns2.ultradns.net.")).build())
                  .add(builder.guid("0B0338C2023F7968")
                          .created(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .modified(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .record(rrBuilder().type(2).name("jclouds.org.").ttl(86400).rdata("pdns1.ultradns.net.")).build())
                  .add(builder.guid("0B0338C2023F796B")
                          .created(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .modified(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .record(rrBuilder().type(2).name("jclouds.org.").ttl(86400).rdata("pdns4.ultradns.org.")).build())
                  .add(builder.guid("0B0338C2023F7983")
                          .created(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .modified(dateService.iso8601DateParse("2011-09-27T23:49:22.000Z"))
                          .record(rrBuilder().type(6).name("jclouds.org.").ttl(3600).rdata(Splitter.on(' ').split(
                                   "pdns2.ultradns.net. admin.jclouds.org. 2011092701 10800 3600 604800 86400"))).build())
                  .add(builder.guid("0B0338C2023F796E")
                          .created(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .modified(dateService.iso8601DateParse("2011-09-27T23:49:22.000Z"))
                          .record(rrBuilder().type(1).name("jclouds.org.").ttl(3000).rdata("1.2.3.4")).build())
                  .add(builder.guid("0B0338C2023F796C")
                          .created(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .modified(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .record(rrBuilder().type(2).name("jclouds.org.").ttl(86400).rdata("pdns5.ultradns.info.")).build())
                  .add(builder.guid("0B0338C2023F796D")
                          .created(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .modified(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .record(rrBuilder().type(2).name("jclouds.org.").ttl(86400).rdata("pdns6.ultradns.co.uk.")).build())
                  .add(builder.guid("0B0338C2023F796A")
                          .created(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .modified(dateService.iso8601DateParse("2009-10-12T12:02:23.000Z"))
                          .record(rrBuilder().type(2).name("jclouds.org.").ttl(86400).rdata("pdns3.ultradns.org.")).build())
                  .build());
    }
}
