package cz.kb.leon.bff.servicing.domain.exception;

import cz.kb.leon.exception.CommonRuntimeException;
import cz.kb.leon.exception.ExceptionCode;
import lombok.*;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DomainException extends CommonRuntimeException {

    /**
     * Optional additional parameters for given exceptionCode.
     */
    private final Map<String, String> exceptionParams;

    public DomainException(ExceptionCode exceptionCode, String message) {
        this(exceptionCode, message, null);
    }

    public DomainException(ExceptionCode exceptionCode, String message, Map<String, String> exceptionParams) {
        super(exceptionCode, message);

        if (MapUtils.isNotEmpty(exceptionParams)) {
            this.exceptionParams = new HashMap<>();
            this.exceptionParams.putAll(exceptionParams);
        } else {
            this.exceptionParams = null;
        }
    }

}
