package denominator.model;

import static denominator.common.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;

import denominator.common.Filter;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.TXTData;

/**
 * Static utility methods that build {@code ResourceRecordSet} instances.
 * 
 */
public class ResourceRecordSets {

    private ResourceRecordSets() {
    }

    public static Filter<ResourceRecordSet<?>> notNull() {
        return new Filter<ResourceRecordSet<?>>() {
            @Override
            public boolean apply(ResourceRecordSet<?> in) {
                return in != null;
            }
        };
    }

    /**
     * evaluates to true if the input {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#name() name} corresponding to the {@code name}
     * parameter.
     * 
     * @param name
     *            the {@link ResourceRecordSet#name() name} of the desired
     *            record set
     */
    public static Filter<ResourceRecordSet<?>> nameEqualTo(final String name) {
        checkNotNull(name, "name");
        return new Filter<ResourceRecordSet<?>>() {

            @Override
            public boolean apply(ResourceRecordSet<?> in) {
                return in != null && name.equals(in.name());
            }

            @Override
            public String toString() {
                return "nameEqualTo(" + name + ")";
            }
        };
    }

    /**
     * evaluates to true if the input {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#name() name} corresponding to the {@code name}
     * parameter and {@link ResourceRecordSet#type() type} corresponding to the
     * {@code type} parameter.
     * 
     * @param name
     *            the {@link ResourceRecordSet#name() name} of the desired
     *            record set
     * @param type
     *            the {@link ResourceRecordSet#type() type} of the desired
     *            record set
     */
    public static Filter<ResourceRecordSet<?>> nameAndTypeEqualTo(final String name, final String type) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        return new Filter<ResourceRecordSet<?>>() {

            @Override
            public boolean apply(ResourceRecordSet<?> in) {
                return in != null && name.equals(in.name()) && type.equals(in.type());
            }

            @Override
            public String toString() {
                return "nameAndTypeEqualTo(" + name + "," + type + ")";
            }
        };
    }

    /**
     * evaluates to true if the input {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#name() name} corresponding to the {@code name}
     * parameter, {@link ResourceRecordSet#type() type} corresponding to the
     * {@code type} parameter, and {@link ResourceRecordSet#qualifier()
     * qualifier} corresponding to the {@code qualifier} parameter.
     * 
     * @param name
     *            the {@link ResourceRecordSet#name() name} of the desired
     *            record set
     * @param type
     *            the {@link ResourceRecordSet#type() type} of the desired
     *            record set
     * @param qualifier
     *            the {@link ResourceRecordSet#qualifier() qualifier} of the
     *            desired record set
     */
    public static Filter<ResourceRecordSet<?>> nameTypeAndQualifierEqualTo(final String name, final String type,
            final String qualifier) {
        checkNotNull(name, "name");
        checkNotNull(type, "type");
        checkNotNull(qualifier, "qualifier");
        return new Filter<ResourceRecordSet<?>>() {

            @Override
            public boolean apply(ResourceRecordSet<?> in) {
                return in != null && name.equals(in.name()) && type.equals(in.type())
                        && qualifier.equals(in.qualifier());
            }

            @Override
            public String toString() {
                return "nameTypeAndQualifierEqualTo(" + name + "," + type + "," + qualifier + ")";
            }
        };
    }

    /**
     * evaluates to true if the input {@link ResourceRecordSet} exists and
     * contains the {@code record} specified.
     * 
     * @param record
     *            the record in the desired record set
     */
    public static Filter<ResourceRecordSet<?>> containsRecord(Map<String, ?> record) {
        return new ContainsRecord(record);
    }

    private static final class ContainsRecord implements Filter<ResourceRecordSet<?>> {
        private final Map<String, ?> record;

        public ContainsRecord(Map<String, ?> record) {
            this.record = checkNotNull(record, "record");
        }

        @Override
        public boolean apply(ResourceRecordSet<?> input) {
            if (input == null)
                return false;
            return input.records().contains(record);
        }

        @Override
        public String toString() {
            return "containsRecord(" + record + ")";
        }
    }

    /**
     * Returns true if the input has no visibility qualifier. Typically
     * indicates a basic record set.
     */
    public static Filter<ResourceRecordSet<?>> alwaysVisible() {
        return new Filter<ResourceRecordSet<?>>() {

            @Override
            public boolean apply(ResourceRecordSet<?> in) {
                return in != null && in.qualifier() == null;
            }

            @Override
            public String toString() {
                return "alwaysVisible()";
            }
        };
    }

    /**
     * creates a set of a single {@link denominator.model.rdata.AData A} record
     * for the specified name.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param address
     *            ex. {@code 192.0.2.1}
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
     *            see {@link ResourceRecordSet#ttl()}
     * @param address
     *            ex. {@code 192.0.2.1}
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
     *            address values ex. {@code [192.0.2.1, 192.0.2.2]}
     */
    public static ResourceRecordSet<AData> a(String name, Collection<String> addresses) {
        return new ABuilder().name(name).addAll(addresses).build();
    }

    /**
     * creates a set of {@link denominator.model.rdata.AData A} records for the
     * specified name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param addresses
     *            address values ex. {@code [192.0.2.1, 192.0.2.2]}
     */
    public static ResourceRecordSet<AData> a(String name, int ttl, Collection<String> addresses) {
        return new ABuilder().name(name).ttl(ttl).addAll(addresses).build();
    }

    private static class ABuilder extends StringRecordBuilder<AData> {
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
     *            see {@link ResourceRecordSet#ttl()}
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
    public static ResourceRecordSet<AAAAData> aaaa(String name, Collection<String> addresses) {
        return new AAAABuilder().name(name).addAll(addresses).build();
    }

    /**
     * creates aaaa set of {@link denominator.model.rdata.AAAAData AAAA} records
     * for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param addresses
     *            address values ex.
     *            {@code [1234:ab00:ff00::6b14:abcd, 5678:ab00:ff00::6b14:abcd]}
     */
    public static ResourceRecordSet<AAAAData> aaaa(String name, int ttl, Collection<String> addresses) {
        return new AAAABuilder().name(name).ttl(ttl).addAll(addresses).build();
    }

    private static class AAAABuilder extends StringRecordBuilder<AAAAData> {
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
     *            see {@link ResourceRecordSet#ttl()}
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
    public static ResourceRecordSet<CNAMEData> cname(String name, Collection<String> cnames) {
        return new CNAMEBuilder().name(name).addAll(cnames).build();
    }

    /**
     * creates cname set of {@link denominator.model.rdata.CNAMEData CNAME}
     * records for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code www.denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param cnames
     *            cname values ex.
     *            {@code [www1.denominator.io., www2.denominator.io.]}
     */
    public static ResourceRecordSet<CNAMEData> cname(String name, int ttl, Collection<String> cnames) {
        return new CNAMEBuilder().name(name).ttl(ttl).addAll(cnames).build();
    }

    private static class CNAMEBuilder extends StringRecordBuilder<CNAMEData> {
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
     * @param nsdname
     *            ex. {@code ns1.denominator.io.}
     */
    public static ResourceRecordSet<NSData> ns(String name, String nsdname) {
        return new NSBuilder().name(name).add(nsdname).build();
    }

    /**
     * creates ns set of ns single {@link denominator.model.rdata.NSData NS}
     * record for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param nsdname
     *            ex. {@code ns1.denominator.io.}
     */
    public static ResourceRecordSet<NSData> ns(String name, int ttl, String nsdname) {
        return new NSBuilder().name(name).ttl(ttl).add(nsdname).build();
    }

    /**
     * creates ns set of {@link denominator.model.rdata.NSData NS} records for
     * the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param nsdnames
     *            nsdname values ex.
     *            {@code [ns1.denominator.io., ns2.denominator.io.]}
     */
    public static ResourceRecordSet<NSData> ns(String name, Collection<String> nsdnames) {
        return new NSBuilder().name(name).addAll(nsdnames).build();
    }

    /**
     * creates ns set of {@link denominator.model.rdata.NSData NS} records for
     * the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param nsdnames
     *            nsdname values ex.
     *            {@code [ns1.denominator.io., ns2.denominator.io.]}
     */
    public static ResourceRecordSet<NSData> ns(String name, int ttl, Collection<String> nsdnames) {
        return new NSBuilder().name(name).ttl(ttl).addAll(nsdnames).build();
    }

    private static class NSBuilder extends StringRecordBuilder<NSData> {
        private NSBuilder() {
            type("NS");
        }

        public NSData apply(String input) {
            return NSData.create(input);
        }
    }

    /**
     * creates ptr set of ptr single {@link denominator.model.rdata.PTRData PTR}
     * record for the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ptrdname
     *            ex. {@code ptr1.denominator.io.}
     */
    public static ResourceRecordSet<PTRData> ptr(String name, String ptrdname) {
        return new PTRBuilder().name(name).add(ptrdname).build();
    }

    /**
     * creates ptr set of ptr single {@link denominator.model.rdata.PTRData PTR}
     * record for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param ptrdname
     *            ex. {@code ptr1.denominator.io.}
     */
    public static ResourceRecordSet<PTRData> ptr(String name, int ttl, String ptrdname) {
        return new PTRBuilder().name(name).ttl(ttl).add(ptrdname).build();
    }

    /**
     * creates ptr set of {@link denominator.model.rdata.PTRData PTR} records
     * for the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ptrdnames
     *            ptrdname values ex.
     *            {@code [ptr1.denominator.io., ptr2.denominator.io.]}
     */
    public static ResourceRecordSet<PTRData> ptr(String name, Collection<String> ptrdnames) {
        return new PTRBuilder().name(name).addAll(ptrdnames).build();
    }

    /**
     * creates ptr set of {@link denominator.model.rdata.PTRData PTR} records
     * for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param ptrdnames
     *            ptrdname values ex.
     *            {@code [ptr1.denominator.io., ptr2.denominator.io.]}
     */
    public static ResourceRecordSet<PTRData> ptr(String name, int ttl, Collection<String> ptrdnames) {
        return new PTRBuilder().name(name).ttl(ttl).addAll(ptrdnames).build();
    }

    private static class PTRBuilder extends StringRecordBuilder<PTRData> {
        private PTRBuilder() {
            type("PTR");
        }

        public PTRData apply(String input) {
            return PTRData.create(input);
        }
    }

    /**
     * creates spf set of spf single {@link denominator.model.rdata.SPFData SPF}
     * record for the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param spfdata
     *            ex. {@code v=spf1 a mx -all}
     */
    public static ResourceRecordSet<SPFData> spf(String name, String spfdata) {
        return new SPFBuilder().name(name).add(spfdata).build();
    }

    /**
     * creates spf set of spf single {@link denominator.model.rdata.SPFData SPF}
     * record for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param spfdata
     *            ex. {@code v=spf1 a mx -all}
     */
    public static ResourceRecordSet<SPFData> spf(String name, int ttl, String spfdata) {
        return new SPFBuilder().name(name).ttl(ttl).add(spfdata).build();
    }

    /**
     * creates spf set of {@link denominator.model.rdata.SPFData SPF} records
     * for the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param spfdata
     *            spfdata values ex.
     *            {@code [v=spf1 a mx -all, v=spf1 ipv6 -all]}
     */
    public static ResourceRecordSet<SPFData> spf(String name, Collection<String> spfdata) {
        return new SPFBuilder().name(name).addAll(spfdata).build();
    }

    /**
     * creates spf set of {@link denominator.model.rdata.SPFData SPF} records
     * for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param spfdata
     *            spfdata values ex.
     *            {@code [v=spf1 a mx -all, v=spf1 ipv6 -all]}
     */
    public static ResourceRecordSet<SPFData> spf(String name, int ttl, Collection<String> spfdata) {
        return new SPFBuilder().name(name).ttl(ttl).addAll(spfdata).build();
    }

    private static class SPFBuilder extends StringRecordBuilder<SPFData> {
        private SPFBuilder() {
            type("SPF");
        }

        public SPFData apply(String input) {
            return SPFData.create(input);
        }
    }

    /**
     * creates txt set of txt single {@link denominator.model.rdata.TXTData TXT}
     * record for the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param txtdata
     *            ex. {@code "made in sweden"}
     */
    public static ResourceRecordSet<TXTData> txt(String name, String txtdata) {
        return new TXTBuilder().name(name).add(txtdata).build();
    }

    /**
     * creates txt set of txt single {@link denominator.model.rdata.TXTData TXT}
     * record for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param txtdata
     *            ex. {@code "made in sweden"}
     */
    public static ResourceRecordSet<TXTData> txt(String name, int ttl, String txtdata) {
        return new TXTBuilder().name(name).ttl(ttl).add(txtdata).build();
    }

    /**
     * creates txt set of {@link denominator.model.rdata.TXTData TXT} records
     * for the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param txtdata
     *            txtdata values ex.
     *            {@code ["made in sweden", "made in norway"]}
     */
    public static ResourceRecordSet<TXTData> txt(String name, Collection<String> txtdata) {
        return new TXTBuilder().name(name).addAll(txtdata).build();
    }

    /**
     * creates txt set of {@link denominator.model.rdata.TXTData TXT} records
     * for the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param txtdata
     *            txtdata values ex.
     *            {@code ["made in sweden", "made in norway"]}
     */
    public static ResourceRecordSet<TXTData> txt(String name, int ttl, Collection<String> txtdata) {
        return new TXTBuilder().name(name).ttl(ttl).addAll(txtdata).build();
    }

    private static class TXTBuilder extends StringRecordBuilder<TXTData> {
        private TXTBuilder() {
            type("TXT");
        }

        public TXTData apply(String input) {
            return TXTData.create(input);
        }
    }
}
