/**
 * DNS record data ({@code rdata}) in <a href="http://tools.ietf.org/html/rfc1035">Master File Format</a> are represented as {@code Map<String, Object>}, preferring values to be {@link com.google.common.primitives.int} or {@code String}.
 *
 * <h4>Implementation Notes</h4>
 * {@link java.beans.ConstructorProperties} are assigned to constructors to help deserializing from json or otherwise where parameter names are required.  This approach avoids skipping constructor validation.
 */
package denominator.model.rdata;