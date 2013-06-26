package feign.codec;

import java.io.IOException;
import java.io.Reader;

import com.google.common.io.Closer;
import com.google.common.reflect.TypeToken;

import feign.Feign;
import feign.Response;

/**
 * Decodes an HTTP response into a given type. Invoked when
 * {@link Response#status()} is in the 2xx range.
 * <p/>
 * Ex.
 * 
 * <pre>
 * public class GsonDecoder extends Decoder {
 *     private final Gson gson;
 * 
 *     public GsonDecoder(Gson gson) {
 *         this.gson = gson;
 *     }
 * 
 *     &#064;Override
 *     public Object decode(String methodKey, Reader reader, TypeToken&lt;?&gt; type) {
 *         return gson.fromJson(reader, type.getType());
 *     }
 * }
 * </pre>
 * 
 * <h4>Error handling</h4>
 * 
 * Responses where {@link Response#status()} is not in the 2xx range are
 * classified as errors, addressed by the {@link ErrorDecoder}. That said,
 * certain RPC apis return errors defined in the {@link Response#body()} even on
 * a 200 status. For example, in the DynECT api, a job still running condition
 * is returned with a 200 status, encoded in json. When scenarios like this
 * occur, you should raise an application-specific exception (which may be
 * {@link ErrorDecoder.Retryable retryable}) here, noting that it must be a
 * {@code RuntimeException}.
 * 
 */
public abstract class Decoder {

    /**
     * Override this method in order to consider the HTTP {@link Response} as
     * opposed to just the {@link Response.Body} when decoding into a new
     * instance of {@link type}.
     * 
     * @param methodKey
     *            {@link Feign#configKey} of the method that created the
     *            request.
     * @param response
     *            HTTP response.
     * @param type
     *            Target object type.
     * @return instance of {@code type}
     * @throws IOException
     *             if there was a network error reading the response.
     */
    public Object decode(String methodKey, Response response, TypeToken<?> type) throws IOException {
        Response.Body body = response.body().orNull();
        if (body == null)
            return null;
        Closer closer = Closer.create();
        try {
            Reader reader = closer.register(body.asReader());
            return decode(methodKey, reader, type);
        } catch (IOException e) {
            throw closer.rethrow(e, IOException.class);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    /**
     * Implement this to decode a {@code Reader} to an object of the specified
     * type.
     * 
     * @param methodKey
     *            {@link Feign#configKey} of the method that created the
     *            request.
     * @param reader
     *            no need to close this, as {@link #decode(Response, TypeToken)}
     *            manages resources.
     * @param type
     *            Target object type.
     * @return instance of {@code type}
     * @throws Throwable
     *             will be propagated safely to the caller.
     */
    public abstract Object decode(String methodKey, Reader reader, TypeToken<?> type) throws Throwable;
}