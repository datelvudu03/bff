package cz.kb.leon.bff.servicing.infra.ui.operationalloan;

import cz.kb.leon.bff.servicing.contract.ContractTest;
import cz.kb.leon.bff.servicing.domain.enumeration.OperationalLoanEarlyRepaymentResultActionResultEnum;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.*;
import org.apache.http.HttpStatus;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.UUID;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class BusinessFinancingOperationLoanServiceImplTest extends ContractTest {

    private static final String USER_ID_PARAM = "12345678";
    private static final String PRODUCT_ID_PARAM = "123456";

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();
        featureFlagService.enableFeatureFlag(FF_OPERATIONAL_LOAN, USER_ID_PARAM);
        Mockito.when(pepResolver.allow(Mockito.anyString(), Mockito.any())).thenReturn(true);
    }

    @Test
    void businessLoanNameChangeRequestedActionTest() {
        var changeNameAction = new ChangeNameAction()
                .productInstanceId("926893922")
                .newConsumerLoanAlias("Překlenovací úvěr 7/2024");
        given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .queryParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "926893922")
                .body(changeNameAction)
                .when()
                .post("/api/business-financing-operational-loan/servicing/v1/actions/nameChangeRequestedAction")
                .then()
                .assertThat().statusCode(HttpStatus.SC_NOT_FOUND);
    }


    @Test
    void operationalLoanStatementRequestedActionTest() {
        var businessLoanStatementRequestedActionRequest = new StatementRequestedActionRequest();
        businessLoanStatementRequestedActionRequest.accountId("926893922").startDate(LocalDate.now().minusDays(30L)).endDate(LocalDate.now());
        given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .queryParam(USER_ID, USER_ID_PARAM)
                .body(businessLoanStatementRequestedActionRequest)
                .when()
                .post("/api/business-financing-operational-loan/servicing/v1/actions/statementRequestedAction")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    @Test
    void getConsumerLoanStatementDocumentDataTest() {
        var statementId = UUID.fromString("32e5c17f-7b5c-4090-89e4-88ea9fb455fc");
        given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "926893922")
                .queryParam(STATEMENT_ID, statementId)
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/business-loan-statement-document-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    @Test
    void getOperationalLoanStatementDocumentStateTest() {
        StatementGenerateStateResponse response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "926893922")
                .queryParam(STATEMENT_ID, "01907751-6fc8-7047-9766-ac92dfe451cf")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/statement-document-state")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract().response().as(StatementGenerateStateResponse.class);

        assertNotNull(response);
        Assertions.assertEquals(StatementGenerateStateResponse.StatementGenerationStateEnum.IN_PROGRESS, response.getStatementGenerationState());
    }

    @Test
    void getOperationalLoanDetailDataTest() {
        OperationalLoanDetailData response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "926893922")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/detail-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract().response().as(OperationalLoanDetailData.class);


        assertNotNull(response);
        assertEquals("1234567890321", response.getProductInstanceId());
        assertNotNull(response.getAccount());
        var account = response.getAccount();
        assertEquals("CZ7601000000000885551183", account.getIban());
        assertEquals("927570893747058703", account.getAccountId());
        assertEquals("Provozní úvěr Business 222", account.getAccountAlias());
        assertEquals("Provozní úvěr Business 222", account.getAccountAliasLong());
        var accountNumber = account.getAccountNumber();
        assertNotNull(accountNumber);
        assertEquals("", accountNumber.getPrefix());
        assertEquals("885551183", accountNumber.getCore());
        assertEquals("0100", accountNumber.getBankCode());
        assertEquals("134108.67", response.getCurrentPrincipalAmount().getAmount());
        String text = response.getMessageList().stream().filter(message -> Message.SeverityEnum.INFO == message.getSeverity()).map(Message::getText).findFirst().orElse(null);
        assertEquals("Příští splátka ve výši 100,00 Kč má splatnost 18. 3. 2025.", text);
    }

    @Test
    void getOperationalLoanDetailDataTest_terminated() {
        OperationalLoanDetailData response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "123456789")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/detail-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract().response().as(OperationalLoanDetailData.class);

        assertNotNull(response);
        assertEquals("1234567890321", response.getProductInstanceId());
        assertNotNull(response.getAccount());
        var account = response.getAccount();
        assertEquals("CZ7601000000000885551183", account.getIban());
        assertEquals("927570893747058703", account.getAccountId());
        assertEquals("Provozní úvěr Business 222", account.getAccountAlias());
        assertEquals("Provozní úvěr Business 222", account.getAccountAliasLong());
        assertEquals("0", response.getCurrentPrincipalAmount().getAmount());
        var accountNumber = account.getAccountNumber();
        assertNotNull(accountNumber);
        assertEquals("", accountNumber.getPrefix());
        assertEquals("885551183", accountNumber.getCore());
        assertEquals("0100", accountNumber.getBankCode());
    }

    @Test
    void operationalLoanPartialRepaymentAcceptedActionTest() {
        var repaymentAmount = new MonetaryAmount()
                .amount("100000")
                .currency("CZK");
        var repaymentAcceptedActionRequest = new RepaymentAcceptedActionRequest()
                .productInstanceId("926893922")
                .fromAccountIban("CZ7601000000000885551183")
                .repaymentAmount(repaymentAmount);
        var response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .queryParam(USER_ID, USER_ID_PARAM)
                .body(repaymentAcceptedActionRequest)
                .when()
                .post("/api/business-financing-operational-loan/servicing/v1/actions/operationalLoanPartialRepaymentAcceptedAction")
                .then()
                .assertThat().statusCode(HttpStatus.SC_CREATED)
                .extract().response().as(RepaymentAcceptedActionResponse.class);

        assertNotNull(response);
        assertEquals("CZ7601000000000885551183", response.getAccountIban());
        assertEquals("CZ7601000000000885551183", response.getCounterpartyIdentification());
        assertEquals(RepaymentProductType.PARTIAL_REPAYMENT, response.getRepaymentProductType());
        assertEquals(TypeOfPayment.DOMESTIC_PAYMENT, response.getRequestedPaymentType());
        assertEquals("0192b4c5-7a04-715e-8b4c-2881a685d7de", response.getServicingCaseId().toString());
        assertEquals(EARLY_REPAYMENT_OWN_AND_COUNTERPARTY_DESC, response.getOwnDescription());
        assertEquals(EARLY_REPAYMENT_OWN_AND_COUNTERPARTY_DESC, response.getMessageForCounterparty());
    }

    @Test
    void operationalLoanFullRepaymentAcceptedActionTest() {
        var repaymentAmount = new MonetaryAmount()
                .amount("134108.67")
                .currency("CZK");
        var repaymentAcceptedActionRequest = new RepaymentAcceptedActionRequest()
                .productInstanceId("926893922")
                .fromAccountIban("CZ7601000000000885551183")
                .repaymentAmount(repaymentAmount);
        var response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .queryParam(USER_ID, USER_ID_PARAM)
                .body(repaymentAcceptedActionRequest)
                .when()
                .post("/api/business-financing-operational-loan/servicing/v1/actions/operationalLoanFullRepaymentAcceptedAction")
                .then()
                .assertThat().statusCode(HttpStatus.SC_CREATED)
                .extract().response().as(RepaymentAcceptedActionResponse.class);

        assertNotNull(response);
        assertEquals("CZ7601000000000885551183", response.getAccountIban());
        assertEquals("CZ7601000000000885551183", response.getCounterpartyIdentification());
        assertEquals(RepaymentProductType.EARLY_REPAYMENT, response.getRepaymentProductType());
        assertEquals(TypeOfPayment.DOMESTIC_PAYMENT, response.getRequestedPaymentType());
        assertEquals("0192b4c5-7a04-715e-8b4c-2881a685d7de", response.getServicingCaseId().toString());
        assertEquals(EARLY_REPAYMENT_OWN_AND_COUNTERPARTY_DESC, response.getOwnDescription());
        assertEquals(EARLY_REPAYMENT_OWN_AND_COUNTERPARTY_DESC, response.getMessageForCounterparty());
    }

    @Test
    void getOperationLoanEarlyRepaymentInfoData_OK_Test() {
        given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "1")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/operational-loan-early-repayment-info-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "3", "4", "5", "6", "7", "8"})
    void getOperationLoanEarlyRepaymentInfoData_Error_Test(String productVersionId) {
        UserErrors userErrors = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, productVersionId)
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/operational-loan-early-repayment-info-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().response().as(UserErrors.class);

        Assertions.assertEquals(1, userErrors.getErrors().size());
        UserError userError = userErrors.getErrors().getFirst();
        Assertions.assertNotNull(userError);

        if ("3".equals(productVersionId)) { // ERR_COB
            Map<String, String> expectedData = Map.of("start", "10:00", "end", "14:00");
            Assertions.assertEquals(userError.getData(), expectedData);
        } // else - az bude wiremock ve verzi kdy podporuje toJson helper, tak bude mozny udelat ruzny varianty naplneni response.data podle vstupu
    }

    @Test
    void operationalLoanEarlyRepaymentResultAction_201() {
        var uuid = UUID.fromString("2f15ffcd-ddc4-47ef-a175-37ea54c97ae9");
        var response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .body(new EarlyRepaymentResultActionRequest().isPaymentAuthorizationSuccessful(true).paymentId("7B4D9G2H1F8K6P3Q").servicingCaseId(uuid).productInstanceId("1"))
                .queryParam(USER_ID, USER_ID_PARAM)
                .when()
                .post("/api/business-financing-operational-loan/servicing/v1/actions/operationalLoanEarlyRepaymentResultAction")
                .then()
                .assertThat().statusCode(HttpStatus.SC_CREATED).extract().as(Result.class);

        assertEquals(OperationalLoanEarlyRepaymentResultActionResultEnum.OPERATIONAL_LOAN_EARLY_REPAYMENT_SUCCESS.name(), response.getCode());
    }

    @Test
    void operationalLoanPartialRepaymentAmountChangedActionTest() {
        var partialRepaymentChangedActionRequest = new PartialRepaymentChangedActionRequest()
                .amount(new MonetaryAmount().amount("8000").currency("CZK"))
                .iban("CZ3401000000000121938548");
        var response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header("Content-Type", "application/json")
                .queryParam("userId", USER_ID_PARAM)
                .body(partialRepaymentChangedActionRequest)
                .when()
                .post("/api/business-financing-operational-loan/servicing/v1/actions/operationalLoanPartialRepaymentAmountChangedAction")
                .then()
                .assertThat().statusCode(HttpStatus.SC_CREATED)
                .extract().response().as(PartialRepaymentChangedActionResponse.class);

        assertNotNull(response);
        assertNull(response.getNumberOfInstalments());
        assertEquals(LocalDate.of(2025, Month.JANUARY, 15), response.getMaturityDate());
        assertNotNull(response.getAmount());
        assertEquals("126108.67", response.getAmount().getAmount());
        assertEquals("CZK", response.getAmount().getCurrency());
    }

    @Test
    void getOperationLoanPartialRepaymentData_Test() {
        PartialRepaymentDataResponse response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "1")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/operational-loan-partial-repayment-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract().response().as(PartialRepaymentDataResponse.class);
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response).isNotNull();
            softAssertions.assertThat(response.getRepaymentAmount()).isNotNull();
            softAssertions.assertThat(response.getRepaymentAccount()).isNotNull();
            softAssertions.assertThat(response.getMaxPossibleRepaymentAmount()).isNotNull();
            softAssertions.assertThat(response.getMinPossibleRepaymentAmount()).isNotNull();
            softAssertions.assertThat(response.getCurrentNumberOfInstalments()).isNotNull();
            softAssertions.assertThat(response.getCurrentNumberOfInstalments()).isEqualTo(3);
            softAssertions.assertThat(response.getCurrentMaturityDate()).isNotNull();
            softAssertions.assertThat(response.getCurrentMaturityDate()).isEqualTo(LocalDate.of(2028, 1, 4));

        });
    }

    @Test
    void operationalLoanNameChangeRequestedAction_Test() {
        var changeNameAction = new ChangeNameAction().productInstanceId("11111").newConsumerLoanAlias("newName");
        var response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header("Content-Type", "application/json")
                .queryParam("userId", USER_ID_PARAM)
                .body(changeNameAction)
                .when()
                .post("/api/business-financing-operational-loan/servicing/v1/actions/nameChangeRequestedAction")
                .then()
                .assertThat().statusCode(HttpStatus.SC_CREATED);
        assertNotNull(response);
    }

    @Test
    void getOperationalLoanStatementAvailability_Test() {
        var statementId = UUID.fromString("32e5c17f-7b5c-4090-89e4-88ea9fb455fc");
        var response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header("Content-Type", "application/json")
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, PRODUCT_ID_PARAM)
                .queryParam(STATEMENT_ID, statementId)
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/statement-availability")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
        assertNotNull(response);
    }

    @Test
    void getOperationalLoanInfoData_Test() {
        OperationalLoanInfoData response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header("Content-Type", "application/json")
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "926893922")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/info-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract().response().as(OperationalLoanInfoData.class);

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(response).isNotNull();
            softAssertions.assertThat(response.getProductOwner()).isNotNull();
            softAssertions.assertThat(response.getProductState()).isNotNull();
            softAssertions.assertThat(response.getReferenceInterestRate()).isNotNull();
            softAssertions.assertThat(response.getFloatingInterestRate()).isNotNull();
            softAssertions.assertThat(response.getLoanMaintenanceFee()).isNotNull();
            softAssertions.assertThat(response.getAccount().getAccountAlias()).isNotNull();
            softAssertions.assertThat(response.getAccount().getAccountAlias()).isEqualTo("Provozní úvěr Business 222");
            softAssertions.assertThat(response.getAccount().getAccountAliasLong()).isEqualTo("Provozní úvěr Business 222");
            softAssertions.assertThat(response.getAccount().getAccountId()).isEqualTo("927570893747058703");
            softAssertions.assertThat(response.getAccount().getIban()).isEqualTo("CZ7601000000000885551183");
            softAssertions.assertThat(response.getAccount().getAccountNumber().getCore()).isEqualTo("885551183");
            softAssertions.assertThat(response.getAccount().getAccountNumber().getBankCode()).isEqualTo("0100");
            softAssertions.assertThat(response.getInstalment()).isNotNull();
            softAssertions.assertThat(response.getInstalment().getInstalmentAmount()).isNotNull();
            softAssertions.assertThat(response.getInstalment().getFirstInstalmentFlag()).isNotNull();
        });

        assertNotNull(response);
        assertNotNull(response.getProductOwner());
        assertNotNull(response.getProductState());
        assertNotNull(response.getReferenceInterestRate());
        assertNotNull(response.getFloatingInterestRate());
        assertNotNull(response.getLoanMaintenanceFee());
        assertNotNull(response.getAccount().getAccountAlias());

    }

    @Test
    void getOperationalLoanInfoData_Test_Terminated() {
        OperationalLoanInfoData response = given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header("Content-Type", "application/json")
                .pathParam(USER_ID, USER_ID_PARAM)
                .queryParam(PRODUCT_INSTANCE_ID, "123456789")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/info-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract().response().as(OperationalLoanInfoData.class);
        assertNotNull(response);
        assertNotNull(response.getProductOwner());
        assertNotNull(response.getProductState());
        assertNotNull(response.getReferenceInterestRate());
        assertNotNull(response.getFloatingInterestRate());
        assertNotNull(response.getLoanMaintenanceFee());
        assertNotNull(response.getCurrentPrincipalAmount());
        assertEquals("0", response.getCurrentPrincipalAmount().getAmount());

    }

}
