package denominator;

/**
 * Answers the question:
 *
 * Are operations likely to succeed, given context of the {@link Provider provider} and currently
 * configured {@link Credentials credentials}?
 *
 * Implementations should make a remote connection, or consult a trusted source to derive the
 * result. They should use least resources possible to establish a meaningful result, and be safe to
 * call many times, possibly concurrently.
 */
public interface CheckConnection {

  boolean ok();
}
