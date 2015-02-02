package denominator.model;

import java.util.LinkedHashMap;

/**
 * Ensures we don't accidentally serialize whole numbers as floats.
 */
public class NumbersAreUnsignedIntsLinkedHashMap extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  protected NumbersAreUnsignedIntsLinkedHashMap() {
  }

  // only overriding put, as putAll uses this, and we aren't exposing
  // a ctor that allows passing a map.
  @Override
  public Object put(String key, Object val) {
    val = val != null && val instanceof Number ? Number.class.cast(val).intValue() : val;
    return super.put(key, val);
  }
}
