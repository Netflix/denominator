package denominator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableBiMap;

/**
 * Some apis use numerical type value of a resource record rather than their
 * names. This class helps convert the numerical values to what people more
 * commonly use. Note that this does not complain a complete mapping and may
 * need updates over time.
 */
@Beta
public class ResourceTypeToValue extends ForwardingMap<String, Integer> implements Function<Object, String>,
        BiMap<String, Integer> {

    /**
     * look up the value (ex. {@code 28}) for the mnemonic name (ex.
     * {@code AAAA} ).
     * 
     * @param type
     *            type to look up. ex {@code AAAA}
     * @throws IllegalArgumentException
     *             if the type was not configured.
     */
    public static Integer lookup(String type) throws IllegalArgumentException {
        checkNotNull(type, "resource type was null");
        checkArgument(lookup.containsKey(type), "%s do not include %s; types: %s", ResourceTypes.class.getSimpleName(),
                type, EnumSet.allOf(ResourceTypes.class));
        return lookup.get(type);
    }

    /**
     * Taken from <a href=
     * "http://www.iana.org/assignments/dns-parameters/dns-parameters.xml#dns-parameters-3"
     * >iana types</a>.
     * 
     */
    // enum only to look and format prettier than fluent bimap builder calls
    private static enum ResourceTypes {
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
         * SSH Public Key Fingerprint
         */
        SSHFP(44),

        /**
         * Sender Policy Framework
         */
        SPF(99),

        /**
         * Server Selection
         */
        SRV(33);

        private final int value;

        private ResourceTypes(int value) {
            this.value = value;
        }
    }

    @Override
    protected ImmutableBiMap<String, Integer> delegate() {
        return lookup;
    }

    private static final ImmutableBiMap<String, Integer> lookup;

    static {
        ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
        for (ResourceTypes r : EnumSet.allOf(ResourceTypes.class)) {
            builder.put(r.name(), r.value);
        }
        lookup = builder.build();
    }

    /**
     * @see ImmutableBiMap#forcePut(Object, Object)
     */
    @Deprecated
    @Override
    public Integer forcePut(String key, Integer value) {
        return lookup.forcePut(key, value);
    }

    @Override
    public Set<Integer> values() {
        return lookup.values();
    }

    @Override
    public BiMap<Integer, String> inverse() {
        return lookup.inverse();
    }

    @Override
    public String apply(Object input) {
        return lookup(checkNotNull(input, "resource type was null").toString()).toString();
    }
}
