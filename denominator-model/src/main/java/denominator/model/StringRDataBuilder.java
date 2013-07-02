package denominator.model;

import static denominator.common.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import denominator.model.rdata.CNAMEData;

/**
 * Capable of building record sets where rdata types more easily expressed as
 * Strings, such as {@link CNAMEData}
 * 
 * @param <D>
 *            portable type of the rdata in the {@link ResourceRecordSet}
 */
abstract class StringRDataBuilder<D extends Map<String, Object>> extends
        AbstractRecordSetBuilder<String, D, StringRDataBuilder<D>> {

    private List<D> rdata = new ArrayList<D>();

    /**
     * adds a value to the builder.
     * 
     * ex.
     * 
     * <pre>
     * builder.add(&quot;192.0.2.1&quot;);
     * </pre>
     */
    public StringRDataBuilder<D> add(String rdata) {
        this.rdata.add(apply(checkNotNull(rdata, "rdata")));
        return this;
    }

    /**
     * adds values to the builder
     * 
     * ex.
     * 
     * <pre>
     * builder.addAll(&quot;192.0.2.1&quot;, &quot;192.0.2.2&quot;);
     * </pre>
     */
    public StringRDataBuilder<D> addAll(String... rdata) {
        return addAll(Arrays.asList(checkNotNull(rdata, "rdata")));
    }

    /**
     * adds a value to the builder.
     * 
     * ex.
     * 
     * <pre>
     * builder.addAll(&quot;192.0.2.1&quot;, &quot;192.0.2.2&quot;);
     * </pre>
     */
    public StringRDataBuilder<D> addAll(Collection<String> rdata) {
        for (String value : checkNotNull(rdata, "rdata")) {
            add(value);
        }
        return this;
    }

    @Override
    protected List<D> rdataValues() {
        return rdata;
    }

    /**
     * Override to properly convert the input to a string-based RData value;
     */
    protected abstract D apply(String in);
}
