package denominator;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Some apis use numerical type value of a resource record rather than their names. This class helps
 * convert the numerical values to what people more commonly use. Note that this does not complain a
 * complete mapping and may need updates over time.
 */
public class ResourceTypeToValue {

  private static Map<String, Integer> lookup = new LinkedHashMap<String, Integer>();
  private static Map<Integer, String> inverse = new LinkedHashMap<Integer, String>();

  static {
    for (ResourceTypes r : EnumSet.allOf(ResourceTypes.class)) {
      lookup.put(r.name(), r.value);
      inverse.put(r.value, r.name());
    }
  }

  /**
   * look up the value (ex. {@code 28}) for the mnemonic name (ex. {@code AAAA} ).
   *
   * @param type type to look up. ex {@code AAAA}
   * @throws IllegalArgumentException if the type was not configured.
   */
  public static Integer lookup(String type) throws IllegalArgumentException {
    checkNotNull(type, "resource type was null");
    checkArgument(lookup.containsKey(type), "%s do not include %s; types: %s",
                  ResourceTypes.class.getSimpleName(),
                  type, EnumSet.allOf(ResourceTypes.class));
    return lookup.get(type);
  }

  /**
   * look up a mnemonic name (ex. {@code AAAA} ) by its value (ex. {@code 28} ).
   *
   * @param type type to look up. ex {@code 28}
   * @throws IllegalArgumentException if the type was not configured.
   */
  public static String lookup(Integer type) throws IllegalArgumentException {
    checkNotNull(type, "resource type was null");
    checkArgument(inverse.containsKey(type), "%s do not include %s; types: %s",
                  ResourceTypes.class.getSimpleName(), type, EnumSet.allOf(ResourceTypes.class));
    return inverse.get(type);
  }

  /**
   * Taken from <a href= "http://www.iana.org/assignments/dns-parameters/dns-parameters.xml#dns-parameters-3"
   * >iana types</a>.
   */
  // enum only to look and format prettier than fluent bimap builder calls
  enum ResourceTypes {
    /**
     * a host address
     */
    A(1),

    /**
     * an authoritative name server
     */
    NS(2),

    /**
     * the canonical name for an alias
     */
    CNAME(5),

    /**
     * marks the start of a zone of authority
     */
    SOA(6),

    /**
     * a domain name pointer
     */
    PTR(12),

    /**
     * mail exchange
     */
    MX(15),

    /**
     * text strings
     */
    TXT(16),

    /**
     * IP6 Address
     */
    AAAA(28),

    /**
     * Location record
     */
    LOC(29),

    /**
     * Naming Authority Pointer
     */
    NAPTR(35),
    
    /**
     * Certificate record
     */
    CERT(37),

    /**
     * Delegation signer
     */
    DS(43),

    /**
     * SSH Public Key Fingerprint
     */
    SSHFP(44),

    /**
     * TLSA certificate association
     */
    TLSA(52),

    /**
     * Sender Policy Framework
     */
    SPF(99),

    /**
     * Server Selection
     */
    SRV(33);

    private final int value;

    ResourceTypes(int value) {
      this.value = value;
    }
  }
}
