package denominator.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import denominator.model.rdata.CNAMEData;

/**
 * Capable of building record sets where rdata types more easily expressed as
 * Strings, such as {@link CNAMEData}
 * 
 * @param <D>
 *            portable type of the rdata in the {@link ResourceRecordSet}
 */
abstract class StringRecordBuilder<D extends Map<String, Object>> extends
        AbstractRecordSetBuilder<String, D, StringRecordBuilder<D>> implements Function<String, D> {

    private ImmutableList.Builder<D> records = ImmutableList.builder();

    /**
     * adds a value to the builder.
     * 
     * ex.
     * 
     * <pre>
     * builder.add(&quot;192.0.2.1&quot;);
     * </pre>
     */
    public StringRecordBuilder<D> add(String record) {
        this.records.add(apply(checkNotNull(record, "record")));
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
    public StringRecordBuilder<D> addAll(String... records) {
        this.records.addAll(transform(ImmutableList.<String> copyOf(checkNotNull(records, "records")), this));
        return this;
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
    public StringRecordBuilder<D> addAll(Iterable<String> records) {
        this.records.addAll(transform(checkNotNull(records, "records"), this));
        return this;
    }

    @Override
    protected ImmutableList<D> records() {
        return records.build();
    }
}
