package denominator.ultradns;

import static denominator.common.Util.split;
import static java.util.Locale.US;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNS.NameAndType;
import denominator.ultradns.UltraDNS.Record;
import feign.codec.SAXDecoder.ContentHandlerWithResult;

/**
 * all decoders use {@code .endsWith} as a cheap way to strip out namespaces,
 * such as {@code ns2:}.
 */
class UltraDNSContentHandlers {

    static class RecordListHandler extends DefaultHandler implements ContentHandlerWithResult<List<Record>> {
        @Inject
        RecordListHandler(){
        }

        private Record rr = new Record();
        private final List<Record> rrs = new ArrayList<Record>();

        @Override
        public List<Record> result() {
            Collections.sort(rrs, byNameTypeAndCreateDate);
            return rrs;
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

    private static final Comparator<Record> byNameTypeAndCreateDate = new Comparator<Record>() {

        @Override
        public int compare(Record left, Record right) {
            int nameCompare = left.name.compareTo(right.name);
            if (nameCompare != 0)
                return nameCompare;
            int typeCompare = new Integer(left.typeCode).compareTo(right.typeCode);
            if (typeCompare != 0)
                return typeCompare;
            // insertion order attempt
            int createdCompare = left.created.compareTo(right.created);
            if (createdCompare != 0)
                return createdCompare;
            // UMP-5803 the order returned in getResourceRecordsOfZoneResponse
            // is different than getResourceRecordsOfDNameByTypeResponse.
            // We fallback to ordering by rdata to ensure consistent ordering.
            return left.rdata.toString().compareTo(right.rdata.toString());
        }
    };

    static class RRPoolListHandler extends DefaultHandler implements ContentHandlerWithResult<Map<NameAndType, String>> {
        @Inject
        RRPoolListHandler(){
        }

        private final Map<NameAndType, String> pools = new LinkedHashMap<NameAndType, String>();
        private NameAndType nameAndType = new NameAndType();
        private String id;

        @Override
        public Map<NameAndType, String> result() {
            return pools;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            if (qName.endsWith("PoolData")) {
                nameAndType.name = attrs.getValue("PoolDName");
                nameAndType.type = attrs.getValue("PoolRecordType");
                id = attrs.getValue("PoolId");
            }
        }

        @Override
        public void endElement(String uri, String name, String qName) {
            if (qName.endsWith("LBPoolData")) {
                pools.put(nameAndType, id);
                nameAndType = new NameAndType();
                id = null;
            }
        }
    }

    static class RegionTableHandler extends DefaultHandler implements
            ContentHandlerWithResult<Map<String, Collection<String>>> {
        @Inject
        RegionTableHandler(){
        }

        private final Map<String, Collection<String>> regions = new TreeMap<String, Collection<String>>();

        @Override
        public Map<String, Collection<String>> result() {
            return regions;
        }

        @Override
        public void startElement(String url, String name, String qName, Attributes attrs) {
            if (qName.endsWith("Region")) {
                List<String> territories = split(';', attrs.getValue("TerritoryName"));
                Collections.sort(territories);
                regions.put(attrs.getValue("RegionName"), territories);
            }
        }
    }

    static class DirectionalGroupHandler extends DefaultHandler implements ContentHandlerWithResult<DirectionalGroup> {
        @Inject
        DirectionalGroupHandler(){
        }

        private final DirectionalGroup group = new DirectionalGroup();

        @Override
        public DirectionalGroup result() {
            return group;
        }

        @Override
        public void startElement(String url, String name, String qName, Attributes attrs) {
            if (qName.endsWith("DirectionalDNSGroupDetail")) {
                group.name = attrs.getValue("GroupName");
            } else if (qName.endsWith("RegionForNewGroups")) {
                String regionName = attrs.getValue("RegionName");
                List<String> territories = split(';', attrs.getValue("TerritoryName"));
                Collections.sort(territories);
                group.regionToTerritories.put(regionName, territories);
            }
        }
    }

    static class DirectionalRecordListHandler extends DefaultHandler implements
            ContentHandlerWithResult<List<DirectionalRecord>> {
        @Inject
        DirectionalRecordListHandler(){
        }

        private DirectionalRecord rr = new DirectionalRecord();
        private final List<DirectionalRecord> rrs = new ArrayList<DirectionalRecord>();

        @Override
        public List<DirectionalRecord> result() {
            Collections.sort(rrs, byTypeAndGeoGroup);
            return rrs;
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
                // ensure this is a geo record, not a source ip one.
                if (rr.geoGroupName != null && rr.geoGroupId != null && rr.type != null)
                    rrs.add(rr);
                rr = new DirectionalRecord();
            }
        }
    }

    private static final Comparator<DirectionalRecord> byTypeAndGeoGroup = new Comparator<DirectionalRecord>() {
        @Override
        public int compare(DirectionalRecord left, DirectionalRecord right) {
            int typeCompare = left.type.compareTo(right.type);
            if (typeCompare != 0)
                return typeCompare;
            return left.geoGroupName.compareTo(right.geoGroupName);
        }
    };

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
}
