package denominator.common;

import static java.lang.String.format;

/**
 * cherry-picks from guava {@code com.google.common.base.Preconditions}.
 */
public class Preconditions {

  private Preconditions() { // no instances
  }

  public static void checkArgument(boolean expression, String errorMessageTemplate,
                                   Object... errorMessageArgs) {
    if (!expression) {
      throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
    }
  }

  public static <T> T checkNotNull(T reference, String errorMessageTemplate,
                                   Object... errorMessageArgs) {
    if (reference == null) {
      throw new NullPointerException(format(errorMessageTemplate, errorMessageArgs));
    }
    return reference;
  }

  public static void checkState(boolean expression, String errorMessageTemplate,
                                Object... errorMessageArgs) {
    if (!expression) {
      throw new IllegalStateException(format(errorMessageTemplate, errorMessageArgs));
    }
  }
}
