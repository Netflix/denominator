package feign.codec;

import java.io.Reader;

import com.google.common.io.CharStreams;
import com.google.common.reflect.TypeToken;

public class ToStringDecoder extends Decoder {
    @Override
    public Object decode(String methodKey, Reader reader, TypeToken<?> type) throws Throwable {
        return CharStreams.toString(reader);
    }
}