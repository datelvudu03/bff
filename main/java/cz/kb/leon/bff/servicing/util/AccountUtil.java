package cz.kb.leon.bff.servicing.util;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.REGEXP_FOR_ALL_LEADING_ZEROES;
import static cz.kb.leon.bff.servicing.configuration.AppConstants.REGEXP_FOR_IBAN;
import static cz.kb.leon.bff.servicing.configuration.AppConstants.REGEXP_FOR_LEADING_ZEROES;

public class AccountUtil {

    private AccountUtil() {}

    public static String parsingAccountNumber(String iban) {
        if (iban == null) {
            return null;
        }
        return iban.substring(8).replaceFirst(REGEXP_FOR_LEADING_ZEROES, "");
    }

    public static AccountNumber parseIbanToAccountNumber(String iban) {
        if (!isValidIban(iban)) {
            return null;
        }

        return new AccountNumber(
                iban.substring(4, 8),
                iban.substring(8, 14).replaceFirst(REGEXP_FOR_ALL_LEADING_ZEROES, ""),
                iban.substring(14).replaceFirst(REGEXP_FOR_LEADING_ZEROES, "")
        );
    }

    public static boolean isValidIban(String iban) {
        return iban != null && iban.matches(REGEXP_FOR_IBAN);
    }

    public record AccountNumber(String bankCode, String prefix, String core) {
    }

}
