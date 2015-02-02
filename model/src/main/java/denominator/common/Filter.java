package denominator.common;

/**
 * adapted from guava's {@code com.google.common.base.Predicate}.
 */
public interface Filter<T> {

  /**
   * @param in to evaluate, could be null
   * @return true if not null and should be retained.
   */
  boolean apply(T in);
}
