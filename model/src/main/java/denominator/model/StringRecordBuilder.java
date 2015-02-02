package denominator.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import denominator.model.rdata.CNAMEData;

import static denominator.common.Preconditions.checkNotNull;

/**
 * Capable of building record sets where rdata types more easily expressed as Strings, such as
 * {@link CNAMEData}
 *
 * @param <D> portable type of the rdata in the {@link ResourceRecordSet}
 */
abstract class StringRecordBuilder<D extends Map<String, Object>> extends
                                                                  AbstractRecordSetBuilder<String, D, StringRecordBuilder<D>> {

  private List<D> records = new ArrayList<D>();

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
    return addAll(Arrays.asList(checkNotNull(records, "records")));
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
  public StringRecordBuilder<D> addAll(Collection<String> records) {
    for (String value : checkNotNull(records, "records")) {
      add(value);
    }
    return this;
  }

  @Override
  protected List<D> records() {
    return records;
  }

  /**
   * Override to properly convert the input to a string-based RData value;
   */
  protected abstract D apply(String in);
}
