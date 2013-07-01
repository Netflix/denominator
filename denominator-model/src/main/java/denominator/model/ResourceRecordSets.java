package denominator.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

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

    /**
     * evaluates to true if the input {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#name() name} corresponding to the {@code name}
     * parameter.
     * 
     * @param name
     *            the {@link ResourceRecordSet#name() name} of the desired
     *            record set
     */
    public static Predicate<ResourceRecordSet<?>> nameEqualTo(String name) {
        return new NameEqualToPredicate(name);
    }

    private static final class NameEqualToPredicate implements Predicate<ResourceRecordSet<?>> {
        private final String name;

        public NameEqualToPredicate(String name) {
            this.name = checkNotNull(name, "name");
        }

        @Override
        public boolean apply(ResourceRecordSet<?> input) {
            if (input == null)
                return false;
            return name.equals(input.name());
        }

        @Override
        public String toString() {
            return "NameEqualTo(" + name + ")";
        }
    }

    /**
     * evaluates to true if the input {@link ResourceRecordSet} exists with
     * {@link ResourceRecordSet#type() type} corresponding to the
     * {@code type} parameter.
     * 
     * @param type
     *            the {@link ResourceRecordSet#type() type} of the desired
     *            record set
     */
    public static Predicate<ResourceRecordSet<?>> typeEqualTo(String type) {
        return new TypeEqualToPredicate(type);
    }

    private static final class TypeEqualToPredicate implements Predicate<ResourceRecordSet<?>> {
        private final String type;

        public TypeEqualToPredicate(String type) {
            this.type = checkNotNull(type, "type");
        }

        @Override
        public boolean apply(ResourceRecordSet<?> input) {
            if (input == null)
                return false;
            return type.equals(input.type());
        }

        @Override
        public String toString() {
            return "TypeEqualTo(" + type + ")";
        }
    }

    /**
     * evaluates to true if the input {@link ResourceRecordSet} exists with a
     * {@link ResourceRecordSet#qualifier() qualifier} present and
     * corresponding to the {@code qualifier} parameter.
     * 
     * @param qualifier
     *            the {@link ResourceRecordSet#qualifier() qualifier} of the
     *            desired record set
     */
    public static Predicate<ResourceRecordSet<?>> qualifierEqualTo(String qualifier) {
        return new QualifierEqualToPredicate(qualifier);
    }

    private static final class QualifierEqualToPredicate implements Predicate<ResourceRecordSet<?>> {
        private final String qualifier;

        public QualifierEqualToPredicate(String qualifier) {
            this.qualifier = checkNotNull(qualifier, "qualifier");
        }

        @Override
        public boolean apply(ResourceRecordSet<?> input) {
            if (input == null)
                return false;
            return qualifier.equals(input.qualifier().orNull());
        }

        @Override
        public String toString() {
            return "QualifierEqualTo(" + qualifier + ")";
        }
    }

    /**
     * evaluates to true if the input {@link ResourceRecordSet} exists and
     * contains the {@code rdata} specified.
     * 
     * @param rdata
     *            the rdata in the desired record set
     */
    public static Predicate<ResourceRecordSet<?>> containsRData(Map<String, ?> rdata) {
        return new ContainsRData(rdata);
    }

    private static final class ContainsRData implements Predicate<ResourceRecordSet<?>> {
        private final Map<String, ?> rdata;

        public ContainsRData(Map<String, ?> rdata) {
            this.rdata = checkNotNull(rdata, "rdata");
        }

        @Override
        public boolean apply(ResourceRecordSet<?> input) {
            if (input == null)
                return false;
            return input.rdata().contains(rdata);
        }

        @Override
        public String toString() {
            return "containsRData(" + rdata + ")";
        }
    }

    /**
     * returns true if the input is not null and
     * {@link ResourceRecordSet#profiles() profile} is empty.
     */
    public static Predicate<ResourceRecordSet<?>> withoutProfile() {
        return WithoutProfile.INSTANCE;
    }

    private static enum WithoutProfile implements Predicate<ResourceRecordSet<?>> {

        INSTANCE;

        @Override
        public boolean apply(ResourceRecordSet<?> input) {
            return input != null && input.profiles().isEmpty();
        }

        @Override
        public String toString() {
            return "WithoutProfile()";
        }
    }

    /**
     * Returns true if {@link ResourceRecordSet#profiles() profile} contains a
     * value is where the value of key {@code type} corresponds to the parameter
     * {@code type}.
     * 
     * <br>
     * Ex.
     * 
     * <pre>
     * if (profileContainsType(&quot;weighted&quot;).apply(rrs)) {
     *     // delegate accordingly
     * }
     * </pre>
     * 
     * @param type
     *            expected type of the profile
     * @since 1.3.1
     */
    public static Predicate<ResourceRecordSet<?>> profileContainsType(String type) {
        return new ProfileContainsTypeToPredicate(type);
    }

    private static final class ProfileContainsTypeToPredicate implements Predicate<ResourceRecordSet<?>> {
        private final String type;

        private ProfileContainsTypeToPredicate(String type) {
            this.type = checkNotNull(type, "type");
        }

        @Override
        public boolean apply(ResourceRecordSet<?> input) {
            if (input == null)
                return false;
            if (input.profiles().isEmpty())
                return false;
            for (Map<String, Object> profile : input.profiles()) {
                if (type.equals(profile.get("type")))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "ProfileContainsTypeTo(" + type + ")";
        }
    }

    /**
     * Returns an {@link Optional} containing the first profile in {@code rrset}
     * that has an entry whose type corresponds to {@code profileType}, if such
     * a profile exists. If no such profile is found, an empty {@link Optional}
     * will be returned.
     * 
     * @since 1.3.1
     */
    public static Optional<Map<String, Object>> tryFindProfile(ResourceRecordSet<?> rrset, String profileType) {
        checkNotNull(rrset, "rrset");
        checkNotNull(profileType, "profileType");
        for (Map<String, Object> profile : rrset.profiles()) {
            if (profileType.equals(profile.get("type")))
                return Optional.of(profile);
        }
        return Optional.absent();
    }

    /**
     * Returns the set of profile types, if present, in the {@code rrset}.
     */
    public static Set<String> toProfileTypes(ResourceRecordSet<?> rrset) {
        return FluentIterable.from(rrset.profiles()).transform(ToTypeValue.INSTANCE).toSet();
    }

    private static enum ToTypeValue implements Function<Map<String, Object>, String> {
        INSTANCE;
        @Override
        public String apply(Map<String, Object> input) {
            return input.get("type").toString();
        }
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
     *            see {@link ResourceRecordSet#ttl()}
     * @param addresses
     *            address values ex. {@code [192.0.2.1, 192.0.2.2]}
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
     *            see {@link ResourceRecordSet#ttl()}
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
     *            see {@link ResourceRecordSet#ttl()}
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
    public static ResourceRecordSet<NSData> ns(String name, Iterable<String> nsdnames) {
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
    public static ResourceRecordSet<NSData> ns(String name, int ttl, Iterable<String> nsdnames) {
        return new NSBuilder().name(name).ttl(ttl).addAll(nsdnames).build();
    }

    private static class NSBuilder extends StringRDataBuilder<NSData> {
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
     * creates ptr set of {@link denominator.model.rdata.PTRData PTR} records for
     * the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ptrdnames
     *            ptrdname values ex.
     *            {@code [ptr1.denominator.io., ptr2.denominator.io.]}
     */
    public static ResourceRecordSet<PTRData> ptr(String name, Iterable<String> ptrdnames) {
        return new PTRBuilder().name(name).addAll(ptrdnames).build();
    }

    /**
     * creates ptr set of {@link denominator.model.rdata.PTRData PTR} records for
     * the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param ptrdnames
     *            ptrdname values ex.
     *            {@code [ptr1.denominator.io., ptr2.denominator.io.]}
     */
    public static ResourceRecordSet<PTRData> ptr(String name, int ttl, Iterable<String> ptrdnames) {
        return new PTRBuilder().name(name).ttl(ttl).addAll(ptrdnames).build();
    }

    private static class PTRBuilder extends StringRDataBuilder<PTRData> {
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
     * creates spf set of {@link denominator.model.rdata.SPFData SPF} records for
     * the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param spfdata
     *            spfdata values ex.
     *            {@code [v=spf1 a mx -all, v=spf1 ipv6 -all]}
     */
    public static ResourceRecordSet<SPFData> spf(String name, Iterable<String> spfdata) {
        return new SPFBuilder().name(name).addAll(spfdata).build();
    }

    /**
     * creates spf set of {@link denominator.model.rdata.SPFData SPF} records for
     * the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param spfdata
     *            spfdata values ex.
     *            {@code [v=spf1 a mx -all, v=spf1 ipv6 -all]}
     */
    public static ResourceRecordSet<SPFData> spf(String name, int ttl, Iterable<String> spfdata) {
        return new SPFBuilder().name(name).ttl(ttl).addAll(spfdata).build();
    }

    private static class SPFBuilder extends StringRDataBuilder<SPFData> {
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
     * creates txt set of {@link denominator.model.rdata.TXTData TXT} records for
     * the specified name.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param txtdata
     *            txtdata values ex.
     *            {@code ["made in sweden", "made in norway"]}
     */
    public static ResourceRecordSet<TXTData> txt(String name, Iterable<String> txtdata) {
        return new TXTBuilder().name(name).addAll(txtdata).build();
    }

    /**
     * creates txt set of {@link denominator.model.rdata.TXTData TXT} records for
     * the specified name and ttl.
     * 
     * @param name
     *            ex. {@code denominator.io.}
     * @param ttl
     *            see {@link ResourceRecordSet#ttl()}
     * @param txtdata
     *            txtdata values ex.
     *            {@code ["made in sweden", "made in norway"]}
     */
    public static ResourceRecordSet<TXTData> txt(String name, int ttl, Iterable<String> txtdata) {
        return new TXTBuilder().name(name).ttl(ttl).addAll(txtdata).build();
    }

    private static class TXTBuilder extends StringRDataBuilder<TXTData> {
        private TXTBuilder() {
            type("TXT");
        }

        public TXTData apply(String input) {
            return TXTData.create(input);
        }
    }
}
