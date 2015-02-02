package denominator.dynect;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(injects = DynECTGeoResourceRecordSetApi.Factory.class, complete = false)
public class CountryToRegions {

  /**
   * taken from <a href="https://help.dynect.net/predefined-geotm-regions-groups/"
   * >documentation</a> on 2013-04-24.
   */
  @Provides
  @Singleton
  @Named("geo")
  Map<String, Collection<String>> provideCountriesByRegion() {
    Map<String, Collection<String>>
        countriesByRegion =
        new LinkedHashMap<String, Collection<String>>();
    countriesByRegion.put("11",
                          Arrays.asList("AG", "AI", "AN", "AW", "BB", "BL", "BM", "BS", "BZ", "CA",
                                        "CR", "CU", "DM",
                                        "DO", "GD", "GL", "GP", "GT", "HN", "HT", "JM", "KN", "KY",
                                        "LC", "MF", "MQ", "MS",
                                        "MX", "NI", "PA", "PM", "PR", "SV", "TC", "TT", "US", "VC",
                                        "VG", "VI"));
    countriesByRegion.put("United States",
                          Arrays.asList("al", "ak", "as", "az", "ar", "aa", "ae", "ap", "ca", "co",
                                        "ct", "de", "dc",
                                        "fm", "fl", "ga", "gu", "hi", "id", "il", "in", "ia", "ks",
                                        "ky", "la", "me", "mh",
                                        "md", "ma", "mi", "mn", "ms", "mo", "mt", "ne", "nv", "nh",
                                        "nj", "nm", "ny", "nc",
                                        "nd", "mp", "oh", "ok", "or", "pw", "pa", "pr", "ri", "sc",
                                        "sd", "tn", "tx", "ut",
                                        "vt", "vi", "va", "wa", "wv", "wi", "wy"));
    countriesByRegion.put("Canada",
                          Arrays.asList("ab", "bc", "mb", "nb", "nl", "nt", "ns", "nu", "on", "pe",
                                        "qc", "sk", "yt"));
    // Continental South America
    countriesByRegion.put("12",
                          Arrays.asList("AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY", "PE",
                                        "PY", "SR", "UY",
                                        "VE"));
    // Continental Europe
    countriesByRegion.put("13",
                          Arrays.asList("AD", "AL", "AT", "AX", "BA", "BE", "BG", "BY", "CH", "CZ",
                                        "DE", "DK", "EE",
                                        "ES", "EU", "FI", "FO", "FR", "FX", "GB", "GG", "GI", "GR",
                                        "HR", "HU", "IE", "IM",
                                        "IS", "IT", "JE", "LI", "LT", "LU", "LV", "MC", "MD", "ME",
                                        "MK", "MT", "NL", "NO",
                                        "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SJ", "SK", "SM",
                                        "TR", "UA", "VA"));
    // Continental Africa
    countriesByRegion.put("14",
                          Arrays.asList("AO", "BF", "BI", "BJ", "BW", "CD", "CF", "CG", "CI", "CM",
                                        "CV", "DJ", "DZ",
                                        "EG", "EH", "ER", "ET", "GA", "GH", "GM", "GN", "GQ", "GW",
                                        "KE", "KM", "LR", "LS",
                                        "LY", "MA", "MG", "ML", "MR", "MU", "MW", "MZ", "NA", "NE",
                                        "NG", "RE", "RW", "SC",
                                        "SD", "SH", "SL", "SN", "SO", "ST", "SZ", "TD", "TG", "TN",
                                        "TZ", "UG", "YT", "ZA",
                                        "ZM", "ZW"));
    // Continental Asia
    countriesByRegion.put("15",
                          Arrays.asList("AE", "AF", "AM", "AP", "AZ", "BD", "BH", "BN", "BT", "CC",
                                        "CN", "CX", "CY",
                                        "GE", "HK", "ID", "IL", "IN", "IO", "IQ", "IR", "JO", "JP",
                                        "KG", "KH", "KP", "KR",
                                        "KW", "KZ", "LA", "LB", "LK", "MM", "MN", "MO", "MV", "MY",
                                        "NP", "OM", "PH", "PK",
                                        "PS", "QA", "SA", "SG", "SY", "TH", "TJ", "TL", "TM", "TW",
                                        "UZ", "VN", "YE"));
    // Continental Australia
    countriesByRegion.put("16",
                          Arrays.asList("AS", "AU", "CK", "FJ", "FM", "GU", "KI", "MH", "MP", "NC",
                                        "NF", "NR", "NU",
                                        "NZ", "PF", "PG", "PN", "PW", "SB", "TK", "TO", "TV", "UM",
                                        "VU", "WF", "WS"));
    // Continental Antarctica
    countriesByRegion.put("17", Arrays.asList("AQ", "BV", "GS", "HM", "TF"));
    countriesByRegion.put("Fallback", Arrays.asList("@@"));
    countriesByRegion.put("Unknown IP", Arrays.asList("@!"));
    countriesByRegion.put("Anonymous Proxy", Arrays.asList("A1"));
    countriesByRegion.put("Other Country", Arrays.asList("O1"));
    countriesByRegion.put("Satellite Provider", Arrays.asList("A2"));
    return countriesByRegion;
  }
}
