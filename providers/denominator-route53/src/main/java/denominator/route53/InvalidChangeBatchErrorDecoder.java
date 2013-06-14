package denominator.route53;

import static feign.codec.Decoders.eachFirstGroup;

import java.io.IOException;
import java.util.List;

import com.google.common.reflect.TypeToken;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;

class InvalidChangeBatchErrorDecoder implements ErrorDecoder {
    Decoder messagesDecoder = eachFirstGroup("<Message>([^<]+)</Message>");

    @Override
    public Object decode(String methodKey, Response response, TypeToken<?> type) {
        try {
            @SuppressWarnings("unchecked")
            List<String> messages = List.class.cast(messagesDecoder.decode(methodKey, response, type));
            throw new InvalidChangeBatchException(methodKey, messages);
        } catch (IOException e) {
            throw FeignException.errorStatus(methodKey, response);
        }
    }
}
