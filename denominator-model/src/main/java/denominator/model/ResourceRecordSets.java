package denominator.model;

import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.NSData;

/**
 * Static utility methods that build {@code ResourceRecordSet} instances.
 * 
 */
public class ResourceRecordSets {

    private ResourceRecordSets() {
    }

    /**
     * creates a set of a single {@link denominator.model.rdata.AData A} record
     * for the specified name.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param address
     *            ex. {@code 1.1.1.1}
     */
    public static ResourceRecordSet<AData> a(String name, String address) {
        return new ABuilder().name(name).add(address).build();
    }

    /**
     * creates a set of a single {@link denominator.model.rdata.AData A} record
     * for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#getTTL()}
     * @param address
     *            ex. {@code 1.1.1.1}
     */
    public static ResourceRecordSet<AData> a(String name, int ttl, String address) {
        return new ABuilder().name(name).ttl(ttl).add(address).build();
    }

    /**
     * creates a set of {@link denominator.model.rdata.AData A} records for the
     * specified name.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param addresses
     *            address values ex. {@code [1.1.1.1, 1.1.1.2]}
     */
    public static ResourceRecordSet<AData> a(String name, Iterable<String> addresses) {
        return new ABuilder().name(name).addAll(addresses).build();
    }

    /**
     * creates a set of {@link denominator.model.rdata.AData A} records for the
     * specified name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#getTTL()}
     * @param addresses
     *            address values ex. {@code [1.1.1.1, 1.1.1.2]}
     */
    public static ResourceRecordSet<AData> a(String name, int ttl, Iterable<String> addresses) {
        return new ABuilder().name(name).ttl(ttl).addAll(addresses).build();
    }

    private static class ABuilder extends StringRDataBuilder<AData> {
        private ABuilder() {
            type("A");
        }

        public AData apply(String input) {
            return AData.create(input);
        }
    }

    /**
     * creates aaaa set of aaaa single {@link denominator.model.rdata.AAAAData
     * AAAA} record for the specified name.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param address
     *            ex. {@code 1234:ab00:ff00::6b14:abcd}
     */
    public static ResourceRecordSet<AAAAData> aaaa(String name, String address) {
        return new AAAABuilder().name(name).add(address).build();
    }

    /**
     * creates aaaa set of aaaa single {@link denominator.model.rdata.AAAAData
     * AAAA} record for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#getTTL()}
     * @param address
     *            ex. {@code 1234:ab00:ff00::6b14:abcd}
     */
    public static ResourceRecordSet<AAAAData> aaaa(String name, int ttl, String address) {
        return new AAAABuilder().name(name).ttl(ttl).add(address).build();
    }

    /**
     * creates aaaa set of {@link denominator.model.rdata.AAAAData AAAA} records
     * for the specified name.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param addresses
     *            address values ex.
     *            {@code [1234:ab00:ff00::6b14:abcd, 5678:ab00:ff00::6b14:abcd]}
     */
    public static ResourceRecordSet<AAAAData> aaaa(String name, Iterable<String> addresses) {
        return new AAAABuilder().name(name).addAll(addresses).build();
    }

    /**
     * creates aaaa set of {@link denominator.model.rdata.AAAAData AAAA} records
     * for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#getTTL()}
     * @param addresses
     *            address values ex.
     *            {@code [1234:ab00:ff00::6b14:abcd, 5678:ab00:ff00::6b14:abcd]}
     */
    public static ResourceRecordSet<AAAAData> aaaa(String name, int ttl, Iterable<String> addresses) {
        return new AAAABuilder().name(name).ttl(ttl).addAll(addresses).build();
    }

    private static class AAAABuilder extends StringRDataBuilder<AAAAData> {
        private AAAABuilder() {
            type("AAAA");
        }

        public AAAAData apply(String input) {
            return AAAAData.create(input);
        }
    }

    /**
     * creates cname set of cname single
     * {@link denominator.model.rdata.CNAMEData CNAME} record for the specified
     * name.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param cname
     *            ex. {@code www1.denominator.io.}
     */
    public static ResourceRecordSet<CNAMEData> cname(String name, String cname) {
        return new CNAMEBuilder().name(name).add(cname).build();
    }

    /**
     * creates cname set of cname single
     * {@link denominator.model.rdata.CNAMEData CNAME} record for the specified
     * name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#getTTL()}
     * @param cname
     *            ex. {@code www1.denominator.io.}
     */
    public static ResourceRecordSet<CNAMEData> cname(String name, int ttl, String cname) {
        return new CNAMEBuilder().name(name).ttl(ttl).add(cname).build();
    }

    /**
     * creates cname set of {@link denominator.model.rdata.CNAMEData CNAME}
     * records for the specified name.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param cnames
     *            cname values ex.
     *            {@code [www1.denominator.io., www2.denominator.io.]}
     */
    public static ResourceRecordSet<CNAMEData> cname(String name, Iterable<String> cnames) {
        return new CNAMEBuilder().name(name).addAll(cnames).build();
    }

    /**
     * creates cname set of {@link denominator.model.rdata.CNAMEData CNAME}
     * records for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#getTTL()}
     * @param cnames
     *            cname values ex.
     *            {@code [www1.denominator.io., www2.denominator.io.]}
     */
    public static ResourceRecordSet<CNAMEData> cname(String name, int ttl, Iterable<String> cnames) {
        return new CNAMEBuilder().name(name).ttl(ttl).addAll(cnames).build();
    }

    private static class CNAMEBuilder extends StringRDataBuilder<CNAMEData> {
        private CNAMEBuilder() {
            type("CNAME");
        }

        public CNAMEData apply(String input) {
            return CNAMEData.create(input);
        }
    }

    /**
     * creates ns set of ns single {@link denominator.model.rdata.NSData NS}
     * record for the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param address
     *            ex. {@code ns1.denominator.io.}
     */
    public static ResourceRecordSet<NSData> ns(String name, String address) {
        return new NSBuilder().name(name).add(address).build();
    }

    /**
     * creates ns set of ns single {@link denominator.model.rdata.NSData NS}
     * record for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#getTTL()}
     * @param address
     *            ex. {@code ns1.denominator.io.}
     */
    public static ResourceRecordSet<NSData> ns(String name, int ttl, String address) {
        return new NSBuilder().name(name).ttl(ttl).add(address).build();
    }

    /**
     * creates ns set of {@link denominator.model.rdata.NSData NS} records for
     * the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param addresses
     *            address values ex.
     *            {@code [ns1.denominator.io., ns2.denominator.io.]}
     */
    public static ResourceRecordSet<NSData> ns(String name, Iterable<String> addresses) {
        return new NSBuilder().name(name).addAll(addresses).build();
    }

    /**
     * creates ns set of {@link denominator.model.rdata.NSData NS} records for
     * the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#getTTL()}
     * @param addresses
     *            address values ex.
     *            {@code [ns1.denominator.io., ns2.denominator.io.]}
     */
    public static ResourceRecordSet<NSData> ns(String name, int ttl, Iterable<String> addresses) {
        return new NSBuilder().name(name).ttl(ttl).addAll(addresses).build();
    }

    private static class NSBuilder extends StringRDataBuilder<NSData> {
        private NSBuilder() {
            type("NS");
        }

        public NSData apply(String input) {
            return NSData.create(input);
        }
    }
}