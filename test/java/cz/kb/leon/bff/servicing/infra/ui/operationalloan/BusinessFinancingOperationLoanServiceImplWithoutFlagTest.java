package cz.kb.leon.bff.servicing.infra.ui.operationalloan;

import cz.kb.leon.bff.servicing.contract.ContractTest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.*;

class BusinessFinancingOperationLoanServiceImplWithoutFlagTest extends ContractTest {

    @Test
    void getOperationLoanPartialRepaymentDataTest() {
        Mockito.when(pepResolver.allow(Mockito.anyString(), Mockito.any())).thenReturn(true);
        given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .pathParam(USER_ID, "12345678")
                .queryParam(PRODUCT_INSTANCE_ID, "926893922")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/operational-loan-partial-repayment-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    @Test
    void getOperationLoanEarlyRepaymentInfoData_OK_Test() {
        Mockito.when(pepResolver.allow(Mockito.anyString(), Mockito.any())).thenReturn(true);
        given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .header(CONTENT_TYPE, CONTENT_ENCODING_JSON)
                .pathParam(USER_ID, "12345678")
                .queryParam(PRODUCT_INSTANCE_ID, "1")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/operational-loan-early-repayment-info-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    @Test
    void getOperationalLoanFullRepaymentDataTest() {
        Mockito.when(pepResolver.allow(Mockito.anyString(), Mockito.any())).thenReturn(true);
        given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .pathParam(USER_ID, "12345678")
                .queryParam(PRODUCT_INSTANCE_ID, "926893922")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/operational-loan-full-repayment-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    @Test
    void getOperationalLoanFullRepaymentDataTest_insufficientBalance() {
        Mockito.when(pepResolver.allow(Mockito.anyString(), Mockito.any())).thenReturn(true);
        given(AUTHORIZED_PERSON)
                .header(X_USERID_IDENTITY_SCHEMA_HEADER, KBID_SCHEMA)
                .pathParam(USER_ID, "12345678")
                .queryParam(PRODUCT_INSTANCE_ID, "926893923")
                .when()
                .get("/api/business-financing-operational-loan/servicing/v1/users/{userId}/operational-loan-full-repayment-data")
                .then()
                .assertThat().statusCode(HttpStatus.SC_BAD_REQUEST);
    }
}
