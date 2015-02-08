package denominator.model.rdata;

import denominator.model.NumbersAreUnsignedIntsLinkedHashMap;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code NAPTR} (Naming Authority Pointer) RData
 *
 * <br> <br> <b>Example</b><br>
 *
 * <pre>
 * NAPTRData rdata = NAPTRData.builder()
 *                            .order(1)
 *                            .preference(1)
 *                            .flags("U")
 *                            .services("E2U+sip")
 *                            .regexp("!^.*$!sip:customer-service@example.com!")
 *                            .replacement(".").build()
 * </pre>
 *
 * See <a href="http://www.ietf.org/rfc/rfc3403.txt">RFC 3403</a>
 */
public final class NAPTRData extends NumbersAreUnsignedIntsLinkedHashMap {

  private static final long serialVersionUID = 1L;

  NAPTRData(int order, int preference, String flags, String services, String regexp,
            String replacement) {
    checkArgument(order <= 0xFFFF, "order must be 0-65535");
    checkArgument(preference <= 0xFFFF, "preference must be 0-65535");
    checkNotNull(flags, "flags");
    checkNotNull(services, "services");
    checkNotNull(regexp, "regexp");
    checkNotNull(replacement, "replacement");
    put("order", order);
    put("preference", preference);
    put("flags", flags);
    put("services", services);
    put("regexp", regexp);
    put("replacement", replacement);
  }

  public static NAPTRData.Builder builder() {
    return new Builder();
  }

  public int order() {
    return Integer.class.cast(get("order"));
  }

  public int preference() {
    return Integer.class.cast(get("preference"));
  }

  public String flags() {
    return get("flags").toString();
  }

  public String services() {
    return get("services").toString();
  }

  public String regexp() {
    return get("regexp").toString();
  }

  public String replacement() {
    return get("replacement").toString();
  }

  public NAPTRData.Builder toBuilder() {
    return builder().from(this);
  }

  public final static class Builder {

    private int order = -1;
    private int preference = -1;
    private String flags;
    private String services;
    private String regexp;
    private String replacement;

    public NAPTRData.Builder order(int order) {
      this.order = order;
      return this;
    }

    public NAPTRData.Builder preference(int preference) {
      this.preference = preference;
      return this;
    }

    public NAPTRData.Builder flags(String flags) {
      this.flags = flags;
      return this;
    }

    public NAPTRData.Builder services(String services) {
      this.services = services;
      return this;
    }

    public NAPTRData.Builder regexp(String regexp) {
      this.regexp = regexp;
      return this;
    }

    public NAPTRData.Builder replacement(String replacement) {
      this.replacement = replacement;
      return this;
    }

    public NAPTRData build() {
      return new NAPTRData(order, preference, flags, services, regexp, replacement);
    }

    public NAPTRData.Builder from(NAPTRData in) {
      return this.order(in.order())
          .preference(in.preference())
          .flags(in.flags())
          .services(in.services())
          .regexp(in.regexp())
          .replacement(in.replacement());
    }
  }
}
