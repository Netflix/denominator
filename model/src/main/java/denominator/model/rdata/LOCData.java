package denominator.model.rdata;

import denominator.model.NumbersAreUnsignedIntsLinkedHashMap;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

/**
 * Corresponds to the binary representation of the {@code LOC} (Location) RData
 *
 * <br> <br> <b>Example</b><br>
 *
 * <pre>
 * LOCData rdata = LOCData.builder()
 *                        .latitude("37 48 48.892 S")
 *                        .longitude("144 57 57.502 E")
 *                        .altitude("26m")
 *                        .diameter("10m")
 *                        .hprecision("100m")
 *                        .vprecision("10m").build()
 * </pre>
 *
 * See <a href="http://www.ietf.org/rfc/rfc1876.txt">RFC 1876</a>
 */
public final class LOCData extends NumbersAreUnsignedIntsLinkedHashMap {

  private static final long serialVersionUID = 1L;

  LOCData(String latitude, String longitude, String altitude, String diameter, String hprecision,
          String vprecision) {
    checkNotNull(latitude, "latitude");
    checkNotNull(longitude, "longitude");
    checkNotNull(altitude, "altitude");
    put("latitude", latitude);
    put("longitude", longitude);
    put("altitude", altitude);
    if (diameter != null) {
      put("diameter", diameter);
    }
    if (hprecision != null) {
      put("hprecision", hprecision);
    }
    if (vprecision != null) {
      put("vprecision", vprecision);
    }
  }

  public static LOCData create(String rdata) {
    Builder builder = LOCData.builder();
    int pos = 0;

    int latOffset = rdata.indexOf('N', 0);
    if (latOffset == -1) {
      latOffset = rdata.indexOf('S', 0);
    }
    checkArgument(latOffset != -1, "could not find latitude");
    String latitude = rdata.substring(pos, pos = (latOffset + 1));
    builder = builder.latitude(latitude);
    pos++;

    int lonOffset = rdata.indexOf('W', pos);
    if (lonOffset == -1) {
      lonOffset = rdata.indexOf('E', pos);
    }
    checkArgument(lonOffset != -1, "could not find longitude");
    String longitude = rdata.substring(pos, pos = (lonOffset + 1));
    builder = builder.longitude(longitude);
    pos++;

    String rest = rdata.substring(pos);
    String[] parts = rest.split(" ");
    checkArgument(parts.length >= 1, "missing altitude");
    builder = builder.altitude(parts[0]);
    switch (parts.length) {
      case 4:
        builder = builder.vprecision(parts[3]);
      case 3:
        builder = builder.hprecision(parts[2]);
      case 2:
        builder = builder.diameter(parts[1]);
    }
    return builder.build();
  }

  public static LOCData.Builder builder() {
    return new Builder();
  }

  public String latitude() {
    return get("latitude").toString();
  }

  public String longitude() {
    return get("longitude").toString();
  }

  public String altitude() {
    return get("altitude").toString();
  }

  public String diameter() {
    Object diameter = get("diameter");
    if (diameter != null) {
      return diameter.toString();
    }
    return null;
  }

  public String hprecision() {
    Object hprecision = get("hprecision");
    if (hprecision != null) {
      return hprecision.toString();
    }
    return null;
  }

  public String vprecision() {
    Object vprecision = get("vprecision");
    if (vprecision != null) {
      return vprecision.toString();
    }
    return null;
  }

  public LOCData.Builder toBuilder() {
    return builder().from(this);
  }

  public final static class Builder {

    private String latitude;
    private String longitude;
    private String altitude;
    private String diameter;
    private String hprecision;
    private String vprecision;

    public LOCData.Builder latitude(String latitude) {
      this.latitude = latitude;
      return this;
    }

    public LOCData.Builder longitude(String longitude) {
      this.longitude = longitude;
      return this;
    }

    public LOCData.Builder altitude(String altitude) {
      this.altitude = altitude;
      return this;
    }

    public LOCData.Builder diameter(String diameter) {
      this.diameter = diameter;
      return this;
    }

    public LOCData.Builder hprecision(String hprecision) {
      this.hprecision = hprecision;
      return this;
    }

    public LOCData.Builder vprecision(String vprecision) {
      this.vprecision = vprecision;
      return this;
    }

    public LOCData build() {
      return new LOCData(latitude, longitude, altitude, diameter, hprecision, vprecision);
    }

    public LOCData.Builder from(LOCData in) {
      return this.latitude(in.latitude())
          .longitude(in.longitude())
          .altitude(in.altitude())
          .diameter(in.diameter())
          .hprecision(in.hprecision())
          .vprecision(in.vprecision());
    }
  }
}
