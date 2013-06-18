package denominator.dynect;

import static java.lang.String.format;

import java.util.List;

import com.google.common.base.Optional;

import feign.FeignException;

public class DynECTException extends FeignException {

    private final String status;
    private final List<Message> messages;

    DynECTException(String methodKey, String status, List<Message> messages) {
        super(format("%s status %s: %s", methodKey, status, messages));
        this.status = status;
        this.messages = messages;
    }

    /**
     * For example: {@code running}.
     */
    public String status() {
        return status;
    }

    /**
     * Messages corresponding to the changes.
     */
    public List<Message> messages() {
        return messages;
    }

    private static final long serialVersionUID = 1L;

    public static class Message {
        Optional<String> code = Optional.absent();
        String info;

        public Optional<String> code() {
            return code;
        }

        public String info() {
            return info;
        }

        @Override
        public String toString() {
            if (code.isPresent()) {
                return code.get() + ": " + info;
            }
            return info;
        }
    }
}
