package denominator.model;

import java.util.Collection;
import java.util.Map;

import denominator.common.Filter;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CERTData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NAPTRData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;
import denominator.model.rdata.TXTData;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Static utility methods that build {@code ResourceRecordSet} instances.
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
   * evaluates to true if the input {@link ResourceRecordSet} exists with {@link
   * ResourceRecordSet#name() name} corresponding to the {@code name} parameter.
   *
   * @param name the {@link ResourceRecordSet#name() name} of the desired record set
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
   * evaluates to true if the input {@link ResourceRecordSet} exists with {@link
   * ResourceRecordSet#name() name} corresponding to the {@code name} parameter and {@link
   * ResourceRecordSet#type() type} corresponding to the {@code type} parameter.
   *
   * @param name the {@link ResourceRecordSet#name() name} of the desired record set
   * @param type the {@link ResourceRecordSet#type() type} of the desired record set
   */
  public static Filter<ResourceRecordSet<?>> nameAndTypeEqualTo(final String name,
                                                                final String type) {
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
   * evaluates to true if the input {@link ResourceRecordSet} exists with {@link
   * ResourceRecordSet#name() name} corresponding to the {@code name} parameter, {@link
   * ResourceRecordSet#type() type} corresponding to the {@code type} parameter, and {@link
   * ResourceRecordSet#qualifier() qualifier} corresponding to the {@code qualifier} parameter.
   *
   * @param name      the {@link ResourceRecordSet#name() name} of the desired record set
   * @param type      the {@link ResourceRecordSet#type() type} of the desired record set
   * @param qualifier the {@link ResourceRecordSet#qualifier() qualifier} of the desired record set
   */
  public static Filter<ResourceRecordSet<?>> nameTypeAndQualifierEqualTo(final String name,
                                                                         final String type,
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
   * evaluates to true if the input {@link ResourceRecordSet} exists and contains the {@code record}
   * specified.
   *
   * @param record the record in the desired record set
   */
  public static Filter<ResourceRecordSet<?>> containsRecord(Map<String, ?> record) {
    return new ContainsRecord(record);
  }

  /**
   * Returns true if the input has no visibility qualifier. Typically indicates a basic record set.
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
   * Returns an updated SOA rrset, with an incremented serial number and the specified parameters.
   */
  public static ResourceRecordSet<SOAData> soa(ResourceRecordSet<?> soa, String email, int ttl) {
    SOAData soaData = (SOAData) soa.records().get(0);
    soaData = soaData.toBuilder().serial(soaData.serial() + 1).rname(email).build();
    return ResourceRecordSet.<SOAData>builder()
        .name(soa.name())
        .type("SOA")
        .ttl(ttl)
        .add(soaData)
        .build();
  }

  /**
   * creates a set of a single {@link denominator.model.rdata.AData A} record for the specified
   * name.
   *
   * @param name    ex. {@code www.denominator.io.}
   * @param address ex. {@code 192.0.2.1}
   */
  public static ResourceRecordSet<AData> a(String name, String address) {
    return new ABuilder().name(name).add(address).build();
  }

  /**
   * creates a set of a single {@link denominator.model.rdata.AData A} record for the specified name
   * and ttl.
   *
   * @param name    ex. {@code www.denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param address ex. {@code 192.0.2.1}
   */
  public static ResourceRecordSet<AData> a(String name, int ttl, String address) {
    return new ABuilder().name(name).ttl(ttl).add(address).build();
  }

  /**
   * creates a set of {@link denominator.model.rdata.AData A} records for the specified name.
   *
   * @param name      ex. {@code www.denominator.io.}
   * @param addresses address values ex. {@code [192.0.2.1, 192.0.2.2]}
   */
  public static ResourceRecordSet<AData> a(String name, Collection<String> addresses) {
    return new ABuilder().name(name).addAll(addresses).build();
  }

  /**
   * creates a set of {@link denominator.model.rdata.AData A} records for the specified name and
   * ttl.
   *
   * @param name      ex. {@code www.denominator.io.}
   * @param ttl       see {@link ResourceRecordSet#ttl()}
   * @param addresses address values ex. {@code [192.0.2.1, 192.0.2.2]}
   */
  public static ResourceRecordSet<AData> a(String name, int ttl, Collection<String> addresses) {
    return new ABuilder().name(name).ttl(ttl).addAll(addresses).build();
  }

  /**
   * creates aaaa set of aaaa single {@link denominator.model.rdata.AAAAData AAAA} record for the
   * specified name.
   *
   * @param name    ex. {@code www.denominator.io.}
   * @param address ex. {@code 1234:ab00:ff00::6b14:abcd}
   */
  public static ResourceRecordSet<AAAAData> aaaa(String name, String address) {
    return new AAAABuilder().name(name).add(address).build();
  }

  /**
   * creates aaaa set of aaaa single {@link denominator.model.rdata.AAAAData AAAA} record for the
   * specified name and ttl.
   *
   * @param name    ex. {@code www.denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param address ex. {@code 1234:ab00:ff00::6b14:abcd}
   */
  public static ResourceRecordSet<AAAAData> aaaa(String name, int ttl, String address) {
    return new AAAABuilder().name(name).ttl(ttl).add(address).build();
  }

  /**
   * creates aaaa set of {@link denominator.model.rdata.AAAAData AAAA} records for the specified
   * name.
   *
   * @param name      ex. {@code www.denominator.io.}
   * @param addresses address values ex. {@code [1234:ab00:ff00::6b14:abcd,
   *                  5678:ab00:ff00::6b14:abcd]}
   */
  public static ResourceRecordSet<AAAAData> aaaa(String name, Collection<String> addresses) {
    return new AAAABuilder().name(name).addAll(addresses).build();
  }

  /**
   * creates aaaa set of {@link denominator.model.rdata.AAAAData AAAA} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code www.denominator.io.}
   * @param ttl       see {@link ResourceRecordSet#ttl()}
   * @param addresses address values ex. {@code [1234:ab00:ff00::6b14:abcd,
   *                  5678:ab00:ff00::6b14:abcd]}
   */
  public static ResourceRecordSet<AAAAData> aaaa(String name, int ttl,
                                                 Collection<String> addresses) {
    return new AAAABuilder().name(name).ttl(ttl).addAll(addresses).build();
  }

  /**
   * creates cname set of cname single {@link denominator.model.rdata.CNAMEData CNAME} record for
   * the specified name.
   *
   * @param name  ex. {@code www.denominator.io.}
   * @param cname ex. {@code www1.denominator.io.}
   */
  public static ResourceRecordSet<CNAMEData> cname(String name, String cname) {
    return new CNAMEBuilder().name(name).add(cname).build();
  }

  /**
   * creates cname set of cname single {@link denominator.model.rdata.CNAMEData CNAME} record for
   * the specified name and ttl.
   *
   * @param name  ex. {@code www.denominator.io.}
   * @param ttl   see {@link ResourceRecordSet#ttl()}
   * @param cname ex. {@code www1.denominator.io.}
   */
  public static ResourceRecordSet<CNAMEData> cname(String name, int ttl, String cname) {
    return new CNAMEBuilder().name(name).ttl(ttl).add(cname).build();
  }

  /**
   * creates cname set of {@link denominator.model.rdata.CNAMEData CNAME} records for the specified
   * name.
   *
   * @param name   ex. {@code www.denominator.io.}
   * @param cnames cname values ex. {@code [www1.denominator.io., www2.denominator.io.]}
   */
  public static ResourceRecordSet<CNAMEData> cname(String name, Collection<String> cnames) {
    return new CNAMEBuilder().name(name).addAll(cnames).build();
  }

  /**
   * creates cname set of {@link denominator.model.rdata.CNAMEData CNAME} records for the specified
   * name and ttl.
   *
   * @param name   ex. {@code www.denominator.io.}
   * @param ttl    see {@link ResourceRecordSet#ttl()}
   * @param cnames cname values ex. {@code [www1.denominator.io., www2.denominator.io.]}
   */
  public static ResourceRecordSet<CNAMEData> cname(String name, int ttl,
                                                   Collection<String> cnames) {
    return new CNAMEBuilder().name(name).ttl(ttl).addAll(cnames).build();
  }

  /**
   * creates ns set of ns single {@link denominator.model.rdata.NSData NS} record for the specified
   * name.
   *
   * @param name    ex. {@code denominator.io.}
   * @param nsdname ex. {@code ns1.denominator.io.}
   */
  public static ResourceRecordSet<NSData> ns(String name, String nsdname) {
    return new NSBuilder().name(name).add(nsdname).build();
  }

  /**
   * creates ns set of ns single {@link denominator.model.rdata.NSData NS} record for the specified
   * name and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param nsdname ex. {@code ns1.denominator.io.}
   */
  public static ResourceRecordSet<NSData> ns(String name, int ttl, String nsdname) {
    return new NSBuilder().name(name).ttl(ttl).add(nsdname).build();
  }

  /**
   * creates ns set of {@link denominator.model.rdata.NSData NS} records for the specified name.
   *
   * @param name     ex. {@code denominator.io.}
   * @param nsdnames nsdname values ex. {@code [ns1.denominator.io., ns2.denominator.io.]}
   */
  public static ResourceRecordSet<NSData> ns(String name, Collection<String> nsdnames) {
    return new NSBuilder().name(name).addAll(nsdnames).build();
  }

  /**
   * creates ns set of {@link denominator.model.rdata.NSData NS} records for the specified name and
   * ttl.
   *
   * @param name     ex. {@code denominator.io.}
   * @param ttl      see {@link ResourceRecordSet#ttl()}
   * @param nsdnames nsdname values ex. {@code [ns1.denominator.io., ns2.denominator.io.]}
   */
  public static ResourceRecordSet<NSData> ns(String name, int ttl, Collection<String> nsdnames) {
    return new NSBuilder().name(name).ttl(ttl).addAll(nsdnames).build();
  }

  /**
   * creates ptr set of ptr single {@link denominator.model.rdata.PTRData PTR} record for the
   * specified name.
   *
   * @param name     ex. {@code denominator.io.}
   * @param ptrdname ex. {@code ptr1.denominator.io.}
   */
  public static ResourceRecordSet<PTRData> ptr(String name, String ptrdname) {
    return new PTRBuilder().name(name).add(ptrdname).build();
  }

  /**
   * creates ptr set of ptr single {@link denominator.model.rdata.PTRData PTR} record for the
   * specified name and ttl.
   *
   * @param name     ex. {@code denominator.io.}
   * @param ttl      see {@link ResourceRecordSet#ttl()}
   * @param ptrdname ex. {@code ptr1.denominator.io.}
   */
  public static ResourceRecordSet<PTRData> ptr(String name, int ttl, String ptrdname) {
    return new PTRBuilder().name(name).ttl(ttl).add(ptrdname).build();
  }

  /**
   * creates ptr set of {@link denominator.model.rdata.PTRData PTR} records for the specified name.
   *
   * @param name      ex. {@code denominator.io.}
   * @param ptrdnames ptrdname values ex. {@code [ptr1.denominator.io., ptr2.denominator.io.]}
   */
  public static ResourceRecordSet<PTRData> ptr(String name, Collection<String> ptrdnames) {
    return new PTRBuilder().name(name).addAll(ptrdnames).build();
  }

  /**
   * creates ptr set of {@link denominator.model.rdata.PTRData PTR} records for the specified name
   * and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param ttl       see {@link ResourceRecordSet#ttl()}
   * @param ptrdnames ptrdname values ex. {@code [ptr1.denominator.io., ptr2.denominator.io.]}
   */
  public static ResourceRecordSet<PTRData> ptr(String name, int ttl, Collection<String> ptrdnames) {
    return new PTRBuilder().name(name).ttl(ttl).addAll(ptrdnames).build();
  }

  /**
   * creates spf set of spf single {@link denominator.model.rdata.SPFData SPF} record for the
   * specified name.
   *
   * @param name    ex. {@code denominator.io.}
   * @param spfdata ex. {@code v=spf1 a mx -all}
   */
  public static ResourceRecordSet<SPFData> spf(String name, String spfdata) {
    return new SPFBuilder().name(name).add(spfdata).build();
  }

  /**
   * creates spf set of spf single {@link denominator.model.rdata.SPFData SPF} record for the
   * specified name and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param spfdata ex. {@code v=spf1 a mx -all}
   */
  public static ResourceRecordSet<SPFData> spf(String name, int ttl, String spfdata) {
    return new SPFBuilder().name(name).ttl(ttl).add(spfdata).build();
  }

  /**
   * creates spf set of {@link denominator.model.rdata.SPFData SPF} records for the specified name.
   *
   * @param name    ex. {@code denominator.io.}
   * @param spfdata spfdata values ex. {@code [v=spf1 a mx -all, v=spf1 ipv6 -all]}
   */
  public static ResourceRecordSet<SPFData> spf(String name, Collection<String> spfdata) {
    return new SPFBuilder().name(name).addAll(spfdata).build();
  }

  /**
   * creates spf set of {@link denominator.model.rdata.SPFData SPF} records for the specified name
   * and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param spfdata spfdata values ex. {@code [v=spf1 a mx -all, v=spf1 ipv6 -all]}
   */
  public static ResourceRecordSet<SPFData> spf(String name, int ttl, Collection<String> spfdata) {
    return new SPFBuilder().name(name).ttl(ttl).addAll(spfdata).build();
  }

  /**
   * creates txt set of txt single {@link denominator.model.rdata.TXTData TXT} record for the
   * specified name.
   *
   * @param name    ex. {@code denominator.io.}
   * @param txtdata ex. {@code "made in sweden"}
   */
  public static ResourceRecordSet<TXTData> txt(String name, String txtdata) {
    return new TXTBuilder().name(name).add(txtdata).build();
  }

  /**
   * creates txt set of txt single {@link denominator.model.rdata.TXTData TXT} record for the
   * specified name and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param txtdata ex. {@code "made in sweden"}
   */
  public static ResourceRecordSet<TXTData> txt(String name, int ttl, String txtdata) {
    return new TXTBuilder().name(name).ttl(ttl).add(txtdata).build();
  }

  /**
   * creates txt set of {@link denominator.model.rdata.TXTData TXT} records for the specified name.
   *
   * @param name    ex. {@code denominator.io.}
   * @param txtdata txtdata values ex. {@code ["made in sweden", "made in norway"]}
   */
  public static ResourceRecordSet<TXTData> txt(String name, Collection<String> txtdata) {
    return new TXTBuilder().name(name).addAll(txtdata).build();
  }

  /**
   * creates txt set of {@link denominator.model.rdata.TXTData TXT} records for the specified name
   * and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param txtdata txtdata values ex. {@code ["made in sweden", "made in norway"]}
   */
  public static ResourceRecordSet<TXTData> txt(String name, int ttl, Collection<String> txtdata) {
    return new TXTBuilder().name(name).ttl(ttl).addAll(txtdata).build();
  }

  /**
   * creates mx set of {@link denominator.model.rdata.MXData MX} records for the specified name and
   * ttl.
   *
   * @param name   ex. {@code denominator.io.}
   * @param mxdata {@code 1 mx1.denominator.io.}
   */
  public static ResourceRecordSet<MXData> mx(String name, String mxdata) {
    return new MXBuilder().name(name).add(mxdata).build();
  }

  /**
   * creates mx set of {@link denominator.model.rdata.MXData MX} records for the specified name and
   * ttl.
   *
   * @param name   ex. {@code denominator.io.}
   * @param ttl    see {@link ResourceRecordSet#ttl()}
   * @param mxdata {@code 1 mx1.denominator.io.}
   */
  public static ResourceRecordSet<MXData> mx(String name, int ttl, String mxdata) {
    return new MXBuilder().name(name).ttl(ttl).add(mxdata).build();
  }

  /**
   * creates mx set of {@link denominator.model.rdata.MXData MX} records for the specified name and
   * ttl.
   *
   * @param name   ex. {@code denominator.io.}
   * @param mxdata mxdata values ex. {@code [1 mx1.denominator.io., 2 mx2.denominator.io.]}
   */
  public static ResourceRecordSet<MXData> mx(String name, Collection<String> mxdata) {
    return new MXBuilder().name(name).addAll(mxdata).build();
  }

  /**
   * creates mx set of {@link denominator.model.rdata.MXData MX} records for the specified name and
   * ttl.
   *
   * @param name   ex. {@code denominator.io.}
   * @param ttl    see {@link ResourceRecordSet#ttl()}
   * @param mxdata mxdata values ex. {@code [1 mx1.denominator.io., 2 mx2.denominator.io.]}
   */
  public static ResourceRecordSet<MXData> mx(String name, int ttl, Collection<String> mxdata) {
    return new MXBuilder().name(name).ttl(ttl).addAll(mxdata).build();
  }

  /**
   * creates srv set of {@link denominator.model.rdata.SRVData SRV} records for the specified name
   * and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param srvdata {@code 0 1 80 www.foo.com.}
   */
  public static ResourceRecordSet<SRVData> srv(String name, String srvdata) {
    return new SRVBuilder().name(name).add(srvdata).build();
  }

  /**
   * creates srv set of {@link denominator.model.rdata.SRVData SRV} records for the specified name
   * and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param srvdata {@code 0 1 80 www.foo.com.}
   */
  public static ResourceRecordSet<SRVData> srv(String name, int ttl, String srvdata) {
    return new SRVBuilder().name(name).ttl(ttl).add(srvdata).build();
  }

  /**
   * creates srv set of {@link denominator.model.rdata.SRVData SRV} records for the specified name
   * and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param srvdata srvdata values ex. {@code [0 1 80 www.foo.com., 0 1 443 www.foo.com.]}
   */
  public static ResourceRecordSet<SRVData> srv(String name, Collection<String> srvdata) {
    return new SRVBuilder().name(name).addAll(srvdata).build();
  }

  /**
   * creates srv set of {@link denominator.model.rdata.SRVData SRV} records for the specified name
   * and ttl.
   *
   * @param name    ex. {@code denominator.io.}
   * @param ttl     see {@link ResourceRecordSet#ttl()}
   * @param srvdata srvdata values ex. {@code [0 1 80 www.foo.com., 0 1 443 www.foo.com.]}
   */
  public static ResourceRecordSet<SRVData> srv(String name, int ttl, Collection<String> srvdata) {
    return new SRVBuilder().name(name).ttl(ttl).addAll(srvdata).build();
  }

  /**
   * creates cert set of {@link denominator.model.rdata.CERTData CERT} records for the specified
   * name and ttl.
   *
   * @param name     ex. {@code denominator.io.}
   * @param certdata {@code 12345 1 1 B33F}
   */
  public static ResourceRecordSet<CERTData> cert(String name, String certdata) {
    return new CERTBuilder().name(name).add(certdata).build();
  }

  /**
   * creates cert set of {@link denominator.model.rdata.CERTData CERT} records for the specified
   * name and ttl.
   *
   * @param name     ex. {@code denominator.io.}
   * @param ttl      see {@link ResourceRecordSet#ttl()}
   * @param certdata {@code 12345 1 1 B33F}
   */
  public static ResourceRecordSet<CERTData> cert(String name, int ttl, String certdata) {
    return new CERTBuilder().name(name).ttl(ttl).add(certdata).build();
  }

  /**
   * creates cert set of {@link denominator.model.rdata.CERTData CERT} records for the specified
   * name and ttl.
   *
   * @param name     ex. {@code denominator.io.}
   * @param certdata certdata values ex. {@code [12345 1 1 B33F, 6789 1 1 F00M00]}
   */
  public static ResourceRecordSet<CERTData> cert(String name, Collection<String> certdata) {
    return new CERTBuilder().name(name).addAll(certdata).build();
  }

  /**
   * creates cert set of {@link denominator.model.rdata.CERTData CERT} records for the specified
   * name and ttl.
   *
   * @param name     ex. {@code denominator.io.}
   * @param ttl      see {@link ResourceRecordSet#ttl()}
   * @param certdata certdata values ex. {@code [12345 1 1 B33F, 6789 1 1 F00M00]}
   */
  public static ResourceRecordSet<CERTData> cert(String name, int ttl,
                                                 Collection<String> certdata) {
    return new CERTBuilder().name(name).ttl(ttl).addAll(certdata).build();
  }

  /**
   * creates naptr set of {@link denominator.model.rdata.NAPTRData NAPTR} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param naptrdata naptrdata values ex. {@code 1 1 U E2U+sip !^.*$!sip:customer-service@example.com!
   *                  .}
   */
  public static ResourceRecordSet<NAPTRData> naptr(String name, String naptrdata) {
    return new NAPTRBuilder().name(name).add(naptrdata).build();
  }

  /**
   * creates naptr set of {@link denominator.model.rdata.NAPTRData NAPTR} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param ttl       see {@link ResourceRecordSet#ttl()}
   * @param naptrdata naptrdata values ex. {@code 1 1 U E2U+sip !^.*$!sip:customer-service@example.com!
   *                  .}
   */
  public static ResourceRecordSet<NAPTRData> naptr(String name, int ttl, String naptrdata) {
    return new NAPTRBuilder().name(name).ttl(ttl).add(naptrdata).build();
  }

  /**
   * creates naptr set of {@link denominator.model.rdata.NAPTRData NAPTR} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param naptrdata naptrdata values ex. {@code [1 1 U E2U+sip !^.*$!sip:customer-service@example.com!
   *                  ., 1 1 U E2U+email !^.*$!mailto:information@example.com! .]}
   */
  public static ResourceRecordSet<NAPTRData> naptr(String name, Collection<String> naptrdata) {
    return new NAPTRBuilder().name(name).addAll(naptrdata).build();
  }

  /**
   * creates naptr set of {@link denominator.model.rdata.NAPTRData NAPTR} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param ttl       see {@link ResourceRecordSet#ttl()}
   * @param naptrdata naptrdata values ex. {@code [1 1 U E2U+sip !^.*$!sip:customer-service@example.com!
   *                  ., 1 1 U E2U+email !^.*$!mailto:information@example.com! .]}
   */
  public static ResourceRecordSet<NAPTRData> naptr(String name, int ttl,
                                                   Collection<String> naptrdata) {
    return new NAPTRBuilder().name(name).ttl(ttl).addAll(naptrdata).build();
  }

  /**
   * creates sshfp set of {@link denominator.model.rdata.SSHFPData SSHFP} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param sshfpdata sshfpdata values ex. {@code 1 1 B33F}
   */
  public static ResourceRecordSet<SSHFPData> sshfp(String name, String sshfpdata) {
    return new SSHFPBuilder().name(name).add(sshfpdata).build();
  }

  /**
   * creates sshfp set of {@link denominator.model.rdata.SSHFPData SSHFP} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param ttl       see {@link ResourceRecordSet#ttl()}
   * @param sshfpdata sshfpdata values ex. {@code 1 1 B33F}
   */
  public static ResourceRecordSet<SSHFPData> sshfp(String name, int ttl, String sshfpdata) {
    return new SSHFPBuilder().name(name).ttl(ttl).add(sshfpdata).build();
  }

  /**
   * creates sshfp set of {@link denominator.model.rdata.SSHFPData SSHFP} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param sshfpdata sshfpdata values ex. {@code [1 1 B33F, 2 1 F00M00]}
   */
  public static ResourceRecordSet<SSHFPData> sshfp(String name, Collection<String> sshfpdata) {
    return new SSHFPBuilder().name(name).addAll(sshfpdata).build();
  }

  /**
   * creates sshfp set of {@link denominator.model.rdata.SSHFPData SSHFP} records for the specified
   * name and ttl.
   *
   * @param name      ex. {@code denominator.io.}
   * @param ttl       see {@link ResourceRecordSet#ttl()}
   * @param sshfpdata sshfpdata values ex. {@code [1 1 B33F, 2 1 F00M00]}
   */
  public static ResourceRecordSet<SSHFPData> sshfp(String name, int ttl,
                                                   Collection<String> sshfpdata) {
    return new SSHFPBuilder().name(name).ttl(ttl).addAll(sshfpdata).build();
  }

  private static final class ContainsRecord implements Filter<ResourceRecordSet<?>> {

    private final Map<String, ?> record;

    public ContainsRecord(Map<String, ?> record) {
      this.record = checkNotNull(record, "record");
    }

    @Override
    public boolean apply(ResourceRecordSet<?> input) {
      if (input == null) {
        return false;
      }
      return input.records().contains(record);
    }

    @Override
    public String toString() {
      return "containsRecord(" + record + ")";
    }
  }

  private static class ABuilder extends StringRecordBuilder<AData> {

    private ABuilder() {
      type("A");
    }

    public AData apply(String input) {
      return AData.create(input);
    }
  }

  private static class AAAABuilder extends StringRecordBuilder<AAAAData> {

    private AAAABuilder() {
      type("AAAA");
    }

    public AAAAData apply(String input) {
      return AAAAData.create(input);
    }
  }

  private static class CNAMEBuilder extends StringRecordBuilder<CNAMEData> {

    private CNAMEBuilder() {
      type("CNAME");
    }

    public CNAMEData apply(String input) {
      return CNAMEData.create(input);
    }
  }

  private static class NSBuilder extends StringRecordBuilder<NSData> {

    private NSBuilder() {
      type("NS");
    }

    public NSData apply(String input) {
      return NSData.create(input);
    }
  }

  private static class PTRBuilder extends StringRecordBuilder<PTRData> {

    private PTRBuilder() {
      type("PTR");
    }

    public PTRData apply(String input) {
      return PTRData.create(input);
    }
  }

  private static class SPFBuilder extends StringRecordBuilder<SPFData> {

    private SPFBuilder() {
      type("SPF");
    }

    public SPFData apply(String input) {
      return SPFData.create(input);
    }
  }

  private static class TXTBuilder extends StringRecordBuilder<TXTData> {

    private TXTBuilder() {
      type("TXT");
    }

    public TXTData apply(String input) {
      return TXTData.create(input);
    }
  }

  private static class MXBuilder extends StringRecordBuilder<MXData> {

    private MXBuilder() {
      type("MX");
    }

    public MXData apply(String input) {
      String[] parts = input.split(" ");
      checkArgument(parts.length == 2, "record must have exactly two parts");
      return MXData.create(Integer.parseInt(parts[0]), parts[1]);
    }
  }

  private static class SRVBuilder extends StringRecordBuilder<SRVData> {

    private SRVBuilder() {
      type("SRV");
    }

    public SRVData apply(String input) {
      String[] parts = input.split(" ");
      checkArgument(parts.length == 4, "record must have exactly four parts");
      return SRVData.builder().priority(Integer.parseInt(parts[0]))
          .weight(Integer.parseInt(parts[1]))
          .port(Integer.parseInt(parts[2]))
          .target(parts[3])
          .build();
    }
  }

  private static class CERTBuilder extends StringRecordBuilder<CERTData> {

    private CERTBuilder() {
      type("CERT");
    }

    public CERTData apply(String input) {
      String[] parts = input.split(" ");
      checkArgument(parts.length == 4, "record must have exactly four parts");
      return CERTData.builder().format(Integer.parseInt(parts[0]))
          .tag(Integer.parseInt(parts[1]))
          .algorithm(Integer.parseInt(parts[2]))
          .certificate(parts[3])
          .build();
    }
  }

  private static class NAPTRBuilder extends StringRecordBuilder<NAPTRData> {

    private NAPTRBuilder() {
      type("NAPTR");
    }

    public NAPTRData apply(String input) {
      String[] parts = input.split(" ");
      checkArgument(parts.length == 6, "record must have exactly six parts");
      return NAPTRData.builder().order(Integer.parseInt(parts[0]))
          .preference(Integer.parseInt(parts[1]))
          .flags(parts[2])
          .services(parts[3])
          .regexp(parts[4])
          .replacement(parts[5])
          .build();
    }
  }

  private static class SSHFPBuilder extends StringRecordBuilder<SSHFPData> {

    private SSHFPBuilder() {
      type("SSHFP");
    }

    public SSHFPData apply(String input) {
      String[] parts = input.split(" ");
      checkArgument(parts.length == 3, "record must have exactly three parts");
      return SSHFPData.builder().algorithm(Integer.parseInt(parts[0]))
          .fptype(Integer.parseInt(parts[1]))
          .fingerprint(parts[2])
          .build();
    }
  }
}
