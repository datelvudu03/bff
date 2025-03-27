package cz.kb.leon.bff.servicing.domain.exception;

import cz.kb.leon.exception.AssertionException;
import cz.kb.leon.exception.CommonExceptionCode;
import cz.kb.leon.exception.ExceptionCode;
import cz.kb.leon.exception.LogCodePrefix;
import cz.kb.speed.exception.error.ErrorCategory;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This enum is implementation of {@link ExceptionCode} and contains all microservice's exceptional codes.
 * Codes which are adepts for raising alerts are prefixed by {@link DomainExceptionCode.ALERTING_BASE_CODE}
 */
@Getter
public enum DomainExceptionCode implements ExceptionCode {

    UNKNOWN_PARTY_ID(2, ErrorCategory.ILLEGAL_STATE, "Unknown party ID."),
    INVALID_AUTHENTICATION_TOKEN(1, ErrorCategory.ILLEGAL_STATE, "Invalid authentication token."),
    INVALID_PLC_DATA(3, ErrorCategory.ILLEGAL_STATE, "Contract data not found."),
    MISSING_REPAYMENT_AMOUNT(4, ErrorCategory.ILLEGAL_STATE, "Missing repayment amount."),
    INVALID_T24_DATA(5, ErrorCategory.ILLEGAL_STATE, "Invalid response from T24 service."),
    ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_NOT_IN_TIME(6, ErrorCategory.CONSTRAINT_VIOLATION, "ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_NOT_IN_TIME"),
    ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_NOT_TODAY(7, ErrorCategory.CONSTRAINT_VIOLATION, "ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_NOT_TODAY"),
    ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_ALREADY_PAID(8, ErrorCategory.CONSTRAINT_VIOLATION, "ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_ALREADY_PAID"),
    ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_IN_PROCESSING(9, ErrorCategory.CONSTRAINT_VIOLATION, "ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_IN_PROCESSING"),
    ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_OVERDUE(10, ErrorCategory.CONSTRAINT_VIOLATION, "ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_OVERDUE"),
    ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_KB_DEBT(11, ErrorCategory.CONSTRAINT_VIOLATION, "ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_KB_DEBT"),
    ERR_OTHER(12, ErrorCategory.CONSTRAINT_VIOLATION, ""),
    INVALID_AMOUNT(13, ErrorCategory.ILLEGAL_STATE, "Invalid amount value."),
    INVALID_T24_RESPONSE(14, ErrorCategory.ILLEGAL_STATE, "Invalid response from T24 service."),
    ERR_OPERATIONAL_LOAN_STATEMENT_NOT_TODAY(15, ErrorCategory.ILLEGAL_STATE, "Activation date is in the past."),
    CURRENCIES_DO_NOT_MATCH(16, ErrorCategory.ILLEGAL_STATE, "Compared currencies are not the same."),
    ERR_OPERATIONAL_LOAN_FULL_REPAYMENT_INSUFFICIENT_BALANCE(17, ErrorCategory.ILLEGAL_STATE, "Insufficient balance on account for fully repayment."),
    INVALID_PRODUCT_DEFINITION_DATA(18, ErrorCategory.ILLEGAL_STATE, "Invalid product definition data."),
    INVALID_IBAN(19, ErrorCategory.ILLEGAL_STATE, "Invalid IBAN"),
    INVALID_DATE_FORMAT(20, ErrorCategory.ILLEGAL_STATE, "Invalid date format"),
    ARGUMENT_IS_NULL(21, ErrorCategory.ILLEGAL_STATE, "Argument is null"),
    /**
     * Adepts for Alerting/Monitoring - candidate to rieman alerting configuration
     */
    FEATURE_FLAG_INACTIVE(27, ErrorCategory.UNAVAILABLE_EXCEPTION_ERROR, "Feature flag is not active", true);

    // Check constraints
    static {
        List<DomainExceptionCode> missingPrefixes = Arrays.stream(values()).filter(ExceptionCode::isInvalid).toList();
        if (!missingPrefixes.isEmpty()) {
            throw new AssertionException(CommonExceptionCode.ERROR_INITIALIZING_CODE_ENUM_MISSING_PREFIX_OR_CODE, "Invalid enum codes found, null or empty attributes: %s".formatted(missingPrefixes));
        }
        Map<String, Long> codeMap = Arrays.stream(values()).collect(Collectors.groupingBy(ExceptionCode::getCode, Collectors.counting()));
        if (codeMap.size() < values().length) {
            throw new AssertionException(CommonExceptionCode.ERROR_INITIALIZING_CODE_ENUM_DUPLICITY, "Duplicate enum codes found: %s".formatted(codeMap.entrySet().stream().filter(v -> v.getValue() > 1).collect(Collectors.toSet())));
        }
    }

    private static final String CODE_PREFIX = "SERVICING";
    private static final int ALERTING_BASE_CODE = 10000;
    private final String internalCode;
    private final ErrorCategory errorCategory;
    private final String defaultDescription;
    /* This MUST contain enum value for specific microservice */
    private final LogCodePrefix prefix = LogCodePrefix.LEON_SERV_BC_SERVICING;

    DomainExceptionCode(int internalCode, ErrorCategory errorCategory, String defaultDescription) {
        this(internalCode, errorCategory, defaultDescription, false);
    }

    DomainExceptionCode(int internalCode, ErrorCategory errorCategory, String defaultDescription, boolean alertingCandidate) {
        this.errorCategory = errorCategory;
        this.internalCode = String.format("%s-%05d", CODE_PREFIX, alertingCandidate ? ALERTING_BASE_CODE + internalCode : internalCode);
        this.defaultDescription = defaultDescription;
    }

    @Override
    public int getHttpStatus() {
        return errorCategory.getHttpStatusCode();
    }

}
