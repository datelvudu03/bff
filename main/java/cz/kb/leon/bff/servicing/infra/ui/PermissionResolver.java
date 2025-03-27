package cz.kb.leon.bff.servicing.infra.ui;

import cz.kb.leon.bff.servicing.util.EndUserUtil;
import cz.kb.speed.security.pep.resolver.PepResolver;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class PermissionResolver {

    private final PepResolver pepResolver;

    public boolean allowProductOverdraftRead(@NotEmpty String productId, String xKbFeBusChannel, String userId) {
        Map<String, String> attributes = new HashMap<>(Map.of(REQUEST_PRODUCT_ID, productId));
        addCommonAttributes(xKbFeBusChannel, userId, attributes);
        return pepResolver.allow(PRODUCT_OVERDRAFT_READ, attributes);
    }

    public boolean allowProductOverdraftChange(@NotEmpty String productId, String xKbFeBusChannel, String userId) {
        Map<String, String> attributes = new HashMap<>(Map.of(REQUEST_PRODUCT_ID, productId));
        addCommonAttributes(xKbFeBusChannel, userId, attributes);
        return pepResolver.allow(PRODUCT_OVERDRAFT_CHANGE, attributes);
    }

    public boolean allowAccountDetailsRead(@NotEmpty String productId, String xKbFeBusChannel, String userId) {
        Map<String, String> attributes = new HashMap<>(Map.of(REQUEST_ACCOUNT_ID, productId));
        addCommonAttributes(xKbFeBusChannel, userId, attributes);
        return pepResolver.allow(ACCOUNT_DETAIL_READ, attributes);
    }

    public boolean allowAccountBusinessLoanManage(@NotEmpty String productId, String xKbFeBusChannel, String userId) {
        Map<String, String> attributes = new HashMap<>(Map.of(REQUEST_ACCOUNT_ID, productId));
        addCommonAttributes(xKbFeBusChannel, userId, attributes);
        return pepResolver.allow(ACCOUNT_BUSINESS_LOAN_MANAGE, attributes);
    }

    public boolean allowAccountBusinessLoanManageIban(@NotEmpty String iban, String xKbFeBusChannel, String userId) {
        Map<String, String> attributes = new HashMap<>(Map.of(REQUEST_ACCOUNT_IBAN, iban));
        addCommonAttributes(xKbFeBusChannel, userId, attributes);
        return pepResolver.allow(ACCOUNT_BUSINESS_LOAN_MANAGE, attributes);
    }

    public boolean allowPaymentCreateWithFromAccountIban(@NotEmpty String iban, String xKbFeBusChannel, String userId) {
        Map<String, String> attributes = new HashMap<>(Map.of(REQUEST_ACCOUNT_IBAN, iban));
        addCommonAttributes(xKbFeBusChannel, userId, attributes);
        return pepResolver.allow(PAYMENT_CREATE, attributes);
    }

    private void addCommonAttributes(String xKbFeBusChannel, String userId, Map<String, String> attributes) {
        attributes.put(ATTR_REQUEST_SERVICE, APPLICATION_NAME_LEON);

        // Primarily because of assisted channel - KBUID (employee) instead of KBID (customer) is sent in jwt when assisted channel is used.
        if (EndUserUtil.userHasAuthority(FE_SCOPE_MUJ_KLIENT_WEB) && !FE_CHANNEL_OTHER.equals(xKbFeBusChannel)) {
            attributes.put(ATTR_CONTEXT_CUSTOMER_KBID, userId);
        }
    }

}
