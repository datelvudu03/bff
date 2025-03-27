package cz.kb.leon.bff.servicing.util;

import cz.kb.leon.bff.servicing.domain.exception.DomainException;
import cz.kb.leon.bff.servicing.domain.exception.DomainExceptionCode;
import cz.kb.speed.headers.rest.v2.PartyIdentityInService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EndUserUtil {

    private static final String PARTY_IDENTITY_USAGE = "BA";
    private static final String DELIMITER = "=";

    public static Optional<PartyIdentityInService.PartyId> partyIdentityInService() {
        Optional<PartyIdentityInService.PartyId> result = Optional.empty();

        String userIdentity = authenticatedUser();
        if (Objects.nonNull(userIdentity)) {
            String[] splitted = userIdentity.split(DELIMITER);
            if (splitted.length == 2) {
                result = Optional.of(partyIdentityInService(splitted[0], splitted[1]));
            }
        }
        return result;
    }

    public static String getPartyId() {
        return partyIdentityInService().orElseThrow(() -> new DomainException(DomainExceptionCode.UNKNOWN_PARTY_ID, "Unknown party id."))
                .getPartyId()
                .getId();
    }

    private static PartyIdentityInService.PartyId partyIdentityInService(String identitySchema, String identityId) {
        return new PartyIdentityInService.PartyId(
                new PartyIdentityInService.PartyId.Id(identityId, identitySchema),
                PARTY_IDENTITY_USAGE
        );
    }

    private static String authenticatedUser() {
        String result = null;
        SecurityContext context = SecurityContextHolder.getContext();
        if (Objects.nonNull(context)) {
            Authentication authentication = context.getAuthentication();
            if (Objects.nonNull(authentication)) {
                result = authentication.getName();
            }
        }
        return result;
    }

    public static boolean userHasAuthority(String authority) {
        SecurityContext context = SecurityContextHolder.getContext();

        if (Objects.nonNull(context)) {
            Authentication authentication = context.getAuthentication();
            if (Objects.nonNull(authentication) && Objects.nonNull(authentication.getAuthorities())) {
                return authentication.getAuthorities().stream()
                        .anyMatch(e -> StringUtils.equals(e.getAuthority(), authority));
            }
        }

        return false;
    }

    public static LanguageEnum usedLanguage(String language) {
        if (StringUtils.isBlank(language)) {
            return LanguageEnum.CZ;
        }

        return LanguageEnum.evaluateLanguage(language);
    }

    public enum LanguageEnum {
        CZ(Locale.of("cs"), "cs", "cz"), EN(Locale.ENGLISH, "en");

        @Getter
        private final Locale locale;
        private final List<String> codes;

        LanguageEnum(Locale locale, String... codes) {
            this.locale = locale;
            this.codes = Arrays.stream(codes).toList();
        }

        static LanguageEnum evaluateLanguage(String code) {
            return Arrays.stream(LanguageEnum.values())
                    .filter(le -> le.codes.contains(ObjectUtil.toLowerCase(code)))
                    .findFirst()
                    .orElse(LanguageEnum.CZ);
        }

    }

}
