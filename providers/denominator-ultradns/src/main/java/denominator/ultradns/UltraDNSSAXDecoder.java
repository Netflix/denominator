package denominator.ultradns;

import static java.util.Locale.US;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.reflect.TypeToken;

import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNS.Record;
import feign.codec.SAXDecoder;

/**
 * all decoders use {@code .endsWith} as a cheap way to strip out namespaces,
 * such as {@code ns2:}.
 */
class UltraDNSSAXDecoder extends SAXDecoder {

    @Override
    protected ContentHandlerWithResult typeToNewHandler(TypeToken<?> type) {
        if (RR_LIST.equals(type))
            return new RecordListHandler();
        else if (RRPOOL_LIST.equals(type))
            return new RRPoolListHandler();
        else if (DR_LIST.equals(type))
            return new DirectionalRecordListHandler();
        else if (DGROUP.equals(type))
            return new DirectionalGroupHandler();
        else if (REGION_TABLE.equals(type))
            return new RegionTableHandler();
        throw new UnsupportedOperationException(type + "");
    }

    static class RecordListHandler extends DefaultHandler implements feign.codec.SAXDecoder.ContentHandlerWithResult {

        private Record rr = new Record();
        private final Builder<Record> rrs = ImmutableList.<Record> builder();

        @Override
        public List<Record> getResult() {
            return rrs.build();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.endsWith("ResourceRecord")) {
                rr.id = attrs.getValue("Guid");
                rr.created = tryParseDate(attrs.getValue("Created"));
                rr.typeCode = Integer.parseInt(attrs.getValue("Type"));
                rr.name = attrs.getValue("DName");
                rr.ttl = Integer.parseInt(attrs.getValue("TTL"));
            } else if (qName.endsWith("InfoValues")) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    rr.rdata.add(attrs.getValue(i));
                }
            }
        }

        @Override
        public void endElement(String uri, String name, String qName) {
            if (qName.endsWith("ResourceRecord")) {
                rrs.add(rr);
                rr = new Record();
            }
        }

    }

    static class RRPoolListHandler extends DefaultHandler implements feign.codec.SAXDecoder.ContentHandlerWithResult {

        private final ImmutableTable.Builder<String, String, String> pools = ImmutableTable.builder();
        private String name, type, id;

        @Override
        public Table<String, String, String> getResult() {
            return pools.build();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.endsWith("PoolData")) {
                name = attrs.getValue("PoolDName");
                type = attrs.getValue("PoolRecordType");
                id = attrs.getValue("PoolId");
            }
        }

        @Override
        public void endElement(String uri, String name, String qName) {
            if (qName.endsWith("LBPoolData")) {
                pools.put(this.name, type, id);
                name = type = id = null;
            }
        }
    }

    static class RegionTableHandler extends DefaultHandler implements feign.codec.SAXDecoder.ContentHandlerWithResult {

        private final ImmutableTable.Builder<String, Integer, SortedSet<String>> regions = ImmutableTable.builder();

        @Override
        public Table<String, Integer, SortedSet<String>> getResult() {
            return regions.build();
        }

        @Override
        public void startElement(String url, String name, String qName, Attributes attrs) {
            if (qName.endsWith("Region")) {
                Iterable<String> territories = Splitter.on(';').split(attrs.getValue("TerritoryName"));
                regions.put(attrs.getValue("RegionName"), Integer.parseInt(attrs.getValue("RegionID")),
                        ImmutableSortedSet.copyOf(territories));
            }
        }
    }

    static class DirectionalGroupHandler extends DefaultHandler implements
            feign.codec.SAXDecoder.ContentHandlerWithResult {

        private final DirectionalGroup group = new DirectionalGroup();

        @Override
        public DirectionalGroup getResult() {
            return group;
        }

        @Override
        public void startElement(String url, String name, String qName, Attributes attrs) {
            if (qName.endsWith("DirectionalDNSGroupDetail")) {
                group.name = attrs.getValue("GroupName");
            } else if (qName.endsWith("RegionForNewGroups")) {
                String regionName = attrs.getValue("RegionName");
                Iterable<String> territories = Splitter.on(';').split(attrs.getValue("TerritoryName"));
                // until api has consistent order
                group.regionToTerritories.putAll(regionName, ImmutableSortedSet.copyOf(territories));
            }
        }
    }

    static class DirectionalRecordListHandler extends DefaultHandler implements
            feign.codec.SAXDecoder.ContentHandlerWithResult {

        private DirectionalRecord rr = new DirectionalRecord();
        private final Builder<DirectionalRecord> rrs = ImmutableList.<DirectionalRecord> builder();

        @Override
        public List<DirectionalRecord> getResult() {
            return rrs.build();
        }

        private String currentName;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.endsWith("DirectionalDNSRecordDetailList")) {
                currentName = attrs.getValue("DName");
            } else if (qName.endsWith("DirectionalDNSRecordDetail")) {
                rr.id = attrs.getValue("DirPoolRecordId");
                rr.geoGroupId = attrs.getValue("GeolocationGroupId");
                rr.geoGroupName = attrs.getValue("GeolocationGroupName");
            } else if (qName.endsWith("DirectionalDNSRecord")) {
                rr.type = attrs.getValue("recordType");
                rr.noResponseRecord = Boolean.parseBoolean(attrs.getValue("noResponseRecord"));
                rr.ttl = Integer.parseInt(attrs.getValue("TTL"));
            } else if (qName.endsWith("InfoValues")) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    rr.rdata.add(attrs.getValue(i));
                }
            }
        }

        @Override
        public void endElement(String uri, String name, String qName) {
            if (qName.endsWith("DirectionalDNSRecordDetail")) {
                rr.name = currentName;
                rrs.add(rr);
                rr = new DirectionalRecord();
            }
        }
    }

    static Date tryParseDate(String dateString) {
        synchronized (iso8601SimpleDateFormat) {
            try {
                return iso8601SimpleDateFormat.parse(dateString);
            } catch (ParseException ignored) {
                // only used for sorting, so don't break terribly
                return null;
            }
        }
    }

    static final SimpleDateFormat iso8601SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", US);
    @SuppressWarnings("serial")
    static final TypeToken<List<Record>> RR_LIST = new TypeToken<List<Record>>() {
    };
    @SuppressWarnings("serial")
    static final TypeToken<Table<String, String, String>> RRPOOL_LIST = new TypeToken<Table<String, String, String>>() {
    };
    @SuppressWarnings("serial")
    static final TypeToken<List<DirectionalRecord>> DR_LIST = new TypeToken<List<DirectionalRecord>>() {
    };
    static final TypeToken<DirectionalGroup> DGROUP = TypeToken.of(DirectionalGroup.class);
    @SuppressWarnings("serial")
    static final TypeToken<Table<String, Integer, SortedSet<String>>> REGION_TABLE = new TypeToken<Table<String, Integer, SortedSet<String>>>() {
    };
}
