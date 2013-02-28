package denominator.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import denominator.model.rdata.CNAMEData;

/**
 * Capable of building record sets where rdata types more easily expressed as
 * Strings, such as {@link CNAMEData}
 * 
 * @param <D>
 *            portable type of the rdata in the {@link ResourceRecordSet}
 */
abstract class StringRDataBuilder<D extends Map<String, Object>> extends
        AbstractRecordSetBuilder<String, D, StringRDataBuilder<D>> implements Function<String, D> {

    private ImmutableList.Builder<D> rdata = ImmutableList.builder();

    /**
     * adds a value to the builder.
     * 
     * ex.
     * 
     * <pre>
     * builder.add(&quot;1.1.1.1&quot;);
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
     * builder.addAll(&quot;1.1.1.1&quot;, &quot;1.1.1.2&quot;);
     * </pre>
     */
    public StringRDataBuilder<D> addAll(String... rdata) {
        this.rdata = ImmutableList.<D> builder().addAll(
                Iterables.transform(ImmutableList.<String> copyOf(checkNotNull(rdata, "rdata")), this));
        return this;
    }

    /**
     * adds a value to the builder.
     * 
     * ex.
     * 
     * <pre>
     * builder.addAll(&quot;1.1.1.1&quot;, &quot;1.1.1.2&quot;);
     * </pre>
     */
    public StringRDataBuilder<D> addAll(Iterable<String> rdata) {
        this.rdata = ImmutableList.<D> builder().addAll(Iterables.transform(checkNotNull(rdata, "rdata"), this));
        return this;
    }

    @Override
    protected ImmutableList<D> rdataValues() {
        return rdata.build();
    }
}