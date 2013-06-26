package denominator.dynect;

import static com.google.common.collect.Multimaps.index;

import java.util.List;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;

@Module(injects = GeoResourceRecordSetsDecoder.class)
public class CountryToRegions {

    /**
     * taken from <a
     * href="https://help.dynect.net/predefined-geotm-regions-groups/"
     * >documentation</a> on 2013-04-24.
     */
    @Provides
    @Singleton
    @Named("geo")
    Multimap<String, String> provideCountriesByRegion() {
        return ImmutableMultimap
                .<String, String> builder()
                // Continental North America
                .putAll("11",
                        ImmutableSet.of("AG", "AI", "AN", "AW", "BB", "BL", "BM", "BS", "BZ", "CA", "CR", "CU", "DM",
                                "DO", "GD", "GL", "GP", "GT", "HN", "HT", "JM", "KN", "KY", "LC", "MF", "MQ", "MS",
                                "MX", "NI", "PA", "PM", "PR", "SV", "TC", "TT", "US", "VC", "VG", "VI"))
                .putAll("United States",
                        ImmutableSet.of("al", "ak", "as", "az", "ar", "aa", "ae", "ap", "ca", "co", "ct", "de", "dc",
                                "fm", "fl", "ga", "gu", "hi", "id", "il", "in", "ia", "ks", "ky", "la", "me", "mh",
                                "md", "ma", "mi", "mn", "ms", "mo", "mt", "ne", "nv", "nh", "nj", "nm", "ny", "nc",
                                "nd", "mp", "oh", "ok", "or", "pw", "pa", "pr", "ri", "sc", "sd", "tn", "tx", "ut",
                                "vt", "vi", "va", "wa", "wv", "wi", "wy"))
                .putAll("Canada",
                        ImmutableSet.of("ab", "bc", "mb", "nb", "nl", "nt", "ns", "nu", "on", "pe", "qc", "sk", "yt"))
                // Continental South America
                .putAll("12",
                        ImmutableSet.of("AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY", "PE", "PY", "SR", "UY",
                                "VE"))
                // Continental Europe
                .putAll("13",
                        ImmutableSet.of("AD", "AL", "AT", "AX", "BA", "BE", "BG", "BY", "CH", "CZ", "DE", "DK", "EE",
                                "ES", "EU", "FI", "FO", "FR", "FX", "GB", "GG", "GI", "GR", "HR", "HU", "IE", "IM",
                                "IS", "IT", "JE", "LI", "LT", "LU", "LV", "MC", "MD", "ME", "MK", "MT", "NL", "NO",
                                "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SJ", "SK", "SM", "TR", "UA", "VA"))
                // Continental Africa
                .putAll("14",
                        ImmutableSet.of("AO", "BF", "BI", "BJ", "BW", "CD", "CF", "CG", "CI", "CM", "CV", "DJ", "DZ",
                                "EG", "EH", "ER", "ET", "GA", "GH", "GM", "GN", "GQ", "GW", "KE", "KM", "LR", "LS",
                                "LY", "MA", "MG", "ML", "MR", "MU", "MW", "MZ", "NA", "NE", "NG", "RE", "RW", "SC",
                                "SD", "SH", "SL", "SN", "SO", "ST", "SZ", "TD", "TG", "TN", "TZ", "UG", "YT", "ZA",
                                "ZM", "ZW"))
                // Continental Asia
                .putAll("15",
                        ImmutableSet.of("AE", "AF", "AM", "AP", "AZ", "BD", "BH", "BN", "BT", "CC", "CN", "CX", "CY",
                                "GE", "HK", "ID", "IL", "IN", "IO", "IQ", "IR", "JO", "JP", "KG", "KH", "KP", "KR",
                                "KW", "KZ", "LA", "LB", "LK", "MM", "MN", "MO", "MV", "MY", "NP", "OM", "PH", "PK",
                                "PS", "QA", "SA", "SG", "SY", "TH", "TJ", "TL", "TM", "TW", "UZ", "VN", "YE"))
                // Continental Australia
                .putAll("16",
                        ImmutableSet.of("AS", "AU", "CK", "FJ", "FM", "GU", "KI", "MH", "MP", "NC", "NF", "NR", "NU",
                                "NZ", "PF", "PG", "PN", "PW", "SB", "TK", "TO", "TV", "UM", "VU", "WF", "WS"))
                // Continental Antarctica
                .putAll("17", ImmutableSet.of("AQ", "BV", "GS", "HM", "TF")).put("Fallback", "@@")
                .put("Unknown IP", "@!").put("Anonymous Proxy", "A1").put("Other Country", "O1")
                .put("Satellite Provider", "A2").build();
    }

    /**
     * {@link org.jclouds.dynect.v3.domain.GeoRegionGroup#getCountries()} isn't
     * organized, so we need a lookup table.
     */
    @Provides
    @Singleton
    @Named("geo")
    Function<List<String>, Multimap<String, String>> provideCountryToRegionIndexer(
            @Named("geo") final Multimap<String, String> regions) {
        Builder<String, String> builder = ImmutableMap.<String, String> builder();
        for (Entry<String, String> region : regions.entries()) {
            builder.put(region.getValue(), region.getKey());
        }
        // special case the "all countries" condition
        for (String key : regions.keySet()) {
            builder.put(key, key);
        }
        final Function<String, String> countryToRegion = Functions.forMap(builder.build());
        return new Function<List<String>, Multimap<String, String>>() {
            @Override
            public Multimap<String, String> apply(List<String> input) {
                return index(input, countryToRegion);
            }
        };
    }
}
