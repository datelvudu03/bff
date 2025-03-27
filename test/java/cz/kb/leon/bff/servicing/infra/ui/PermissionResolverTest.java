package cz.kb.leon.bff.servicing.infra.ui;

import cz.kb.leon.bff.servicing.app.command.overdraft.GetOverdraftDetailScreenDataCmd;
import cz.kb.leon.bff.servicing.infra.ui.overdraft.BusinessFinancingOverdraftServiceImpl;
import cz.kb.leon.bff.servicing.infra.integrations.overdraft.ServicingOverdraftBffClient;
import cz.kb.leon.bff.servicing.infra.integrations.plc.PLCClientService;
import cz.kb.leon.bff.servicing.util.EndUserUtil;
import cz.kb.leon.featureflags.FeatureFlagService;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.BusinessFinancingOverdraftService;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.OverdraftTerminationRequest;
import cz.kb.speed.cqrs.api.command.CommandBus;
import cz.kb.speed.messaging.api.model.ResponseMessage;
import cz.kb.speed.messaging.api.model.SimpleResponseMessage;
import cz.kb.speed.security.pep.resolver.PepResolver;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest()
@ContextConfiguration(classes = {BusinessFinancingOverdraftServiceImpl.class, PermissionResolver.class})
@EnableMethodSecurity
class PermissionResolverTest {

    private static final String PRODUCT_ID_ALLOW = "prod_id_1234";

    private static final String PRODUCT_ID_DENY = "prod_id_5678";

    private static final String X_KB_SESSION_ID = "1234";
    private static final String X_KB_FE_PLATFORM = "WEB";
    private static final String X_KB_IDENTITY_SCHEMA = "KBID";
    private static final String X_KB_FE_CHANNEL = "NDB_INTERNET_BANKING_ASSISTED";
    private static final String X_KB_BUS_CHANNEL = "CH0001";
    private static final String USER_ID = "123456789";

    @MockBean
    private PepResolver pepResolver;

    @MockBean
    private ServicingOverdraftBffClient servicingOverdraftBCClient;

    @MockBean
    private CommandBus commandBus;

    @MockBean
    private FeatureFlagService featureFlagService;

    @MockBean
    private PLCClientService plcClientService;

    MockedStatic<EndUserUtil> endUserUtilMockedStatic;

    @Autowired
    private BusinessFinancingOverdraftService businessFinancingOverdraftService;

    @BeforeEach
    void setUp() {
        Mockito.when(pepResolver.allow(PRODUCT_OVERDRAFT_READ, Map.of(REQUEST_PRODUCT_ID, PRODUCT_ID_ALLOW, ATTR_REQUEST_SERVICE, APPLICATION_NAME_LEON))).thenReturn(true);
        Mockito.when(pepResolver.allow(PRODUCT_OVERDRAFT_CHANGE, Map.of(REQUEST_PRODUCT_ID, PRODUCT_ID_ALLOW, ATTR_REQUEST_SERVICE, APPLICATION_NAME_LEON))).thenReturn(true);
        Mockito.when(pepResolver.allow(ACCOUNT_DETAIL_READ, Map.of(REQUEST_ACCOUNT_ID, PRODUCT_ID_ALLOW, ATTR_REQUEST_SERVICE, APPLICATION_NAME_LEON))).thenReturn(true);
        Mockito.when(pepResolver.allow(ACCOUNT_BUSINESS_LOAN_MANAGE, Map.of(REQUEST_ACCOUNT_ID, PRODUCT_ID_ALLOW, ATTR_REQUEST_SERVICE, APPLICATION_NAME_LEON))).thenReturn(true);
        Mockito.when(pepResolver.allow(ACCOUNT_BUSINESS_LOAN_MANAGE, Map.of(REQUEST_ACCOUNT_IBAN, PRODUCT_ID_ALLOW, ATTR_REQUEST_SERVICE, APPLICATION_NAME_LEON))).thenReturn(true);
        Mockito.when(pepResolver.allow(ACCOUNT_BUSINESS_LOAN_MANAGE, Map.of(REQUEST_ACCOUNT_ID, REQUEST_ACCOUNT_IBAN, ATTR_REQUEST_SERVICE, APPLICATION_NAME_LEON))).thenReturn(true);
        Mockito.when(pepResolver.allow(PAYMENT_CREATE, Map.of(REQUEST_ACCOUNT_IBAN, REQUEST_ACCOUNT_IBAN, ATTR_REQUEST_SERVICE, APPLICATION_NAME_LEON))).thenReturn(true);

        GetOverdraftDetailScreenDataCmd.CmdResult result = new GetOverdraftDetailScreenDataCmd.CmdResult(Response.ok().entity(null).build());
        ResponseMessage<Object> response = new SimpleResponseMessage<>(result);

        Mockito.when(commandBus.sendAndGetReply(new GetOverdraftDetailScreenDataCmd(PRODUCT_ID_ALLOW, USER_ID))).thenReturn(response);

        endUserUtilMockedStatic = Mockito.mockStatic(EndUserUtil.class);
        endUserUtilMockedStatic.when(EndUserUtil::getPartyId).thenReturn("123");
    }

    @AfterEach
    void tearDown() {
        endUserUtilMockedStatic.close();
    }

    @Test
    void testAllowGetSigningCaseState() {
        assertDoesNotThrow(() ->
                businessFinancingOverdraftService.getSigningCaseState(X_KB_SESSION_ID, X_KB_FE_PLATFORM, X_KB_IDENTITY_SCHEMA, X_KB_FE_CHANNEL, X_KB_BUS_CHANNEL, USER_ID, PRODUCT_ID_ALLOW, null));

        assertThrows(AccessDeniedException.class, () ->
                businessFinancingOverdraftService.getSigningCaseState(X_KB_SESSION_ID, X_KB_FE_PLATFORM, X_KB_IDENTITY_SCHEMA, X_KB_FE_CHANNEL, X_KB_BUS_CHANNEL, USER_ID, PRODUCT_ID_DENY, null));
    }

    @Test
    void testAllowOverdraftDetailScreenData() {
        assertDoesNotThrow(() ->
                businessFinancingOverdraftService.overdraftDetailScreenData(X_KB_SESSION_ID, X_KB_FE_PLATFORM, X_KB_IDENTITY_SCHEMA, X_KB_FE_CHANNEL, X_KB_BUS_CHANNEL, "CZ", USER_ID, PRODUCT_ID_ALLOW));

        assertThrows(AccessDeniedException.class, () ->
                businessFinancingOverdraftService.overdraftDetailScreenData(X_KB_SESSION_ID, X_KB_FE_PLATFORM, X_KB_IDENTITY_SCHEMA, X_KB_FE_CHANNEL, X_KB_BUS_CHANNEL, "CZ", USER_ID, PRODUCT_ID_DENY));
    }

    @Test
    void testAllowOverdraftTerminatedScreenData() {
        assertDoesNotThrow(() ->
                businessFinancingOverdraftService.overdraftTerminatedScreenData(X_KB_SESSION_ID, X_KB_FE_PLATFORM, X_KB_IDENTITY_SCHEMA, X_KB_FE_CHANNEL, X_KB_BUS_CHANNEL, "CZ", USER_ID, PRODUCT_ID_ALLOW, null));

        assertThrows(AccessDeniedException.class, () ->
                businessFinancingOverdraftService.overdraftTerminatedScreenData(X_KB_SESSION_ID, X_KB_FE_PLATFORM, X_KB_IDENTITY_SCHEMA, X_KB_FE_CHANNEL, X_KB_BUS_CHANNEL, "CZ", USER_ID, PRODUCT_ID_DENY, null));
    }

    @Test
    void testAllowOverdraftTerminationRequestedAction() {
        var overdraftTerminationRequest = createOverdraftTerminationRequest(PRODUCT_ID_ALLOW);
        assertDoesNotThrow(() ->
                businessFinancingOverdraftService.overdraftTerminationRequestedAction(X_KB_SESSION_ID, X_KB_FE_PLATFORM, X_KB_IDENTITY_SCHEMA, X_KB_BUS_CHANNEL, X_KB_FE_CHANNEL, USER_ID, overdraftTerminationRequest));
    }

    @Test
    void testAllowOverdraftTerminationRequestedAction_accessDenied() {
        var overdraftTerminationRequest = createOverdraftTerminationRequest(PRODUCT_ID_DENY);
        assertThrows(AccessDeniedException.class, () ->
                businessFinancingOverdraftService.overdraftTerminationRequestedAction(X_KB_SESSION_ID, X_KB_FE_PLATFORM, X_KB_IDENTITY_SCHEMA, X_KB_BUS_CHANNEL, X_KB_FE_CHANNEL, USER_ID, overdraftTerminationRequest));
    }

    private OverdraftTerminationRequest createOverdraftTerminationRequest(String productId) {
        return new OverdraftTerminationRequest().productId(productId);
    }

}
