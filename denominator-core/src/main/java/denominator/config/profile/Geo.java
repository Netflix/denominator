package denominator.config.profile;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Qualifier;

import denominator.profile.GeoResourceRecordSetApi;

/**
 * Configuration required in order to support the
 * {@link GeoResourceRecordSetApi}.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface Geo {
}
