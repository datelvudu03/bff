package cz.kb.leon.bff.servicing.infra.ui.overdraft;

import cz.kb.leon.bc.servicing.v2.dto.GeneratingState;
import cz.kb.leon.bff.servicing.contract.ContractTest;
import cz.kb.leon.bff.servicing.infra.mapper.loan.ResponseMapper;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.StatementGenerateStateResponse;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.OverdraftTerminationRequest;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.SigningCaseState;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(MockitoExtension.class)
class BusinessFinancingOverdraftServiceTest extends ContractTest {

    @Autowired
    ResponseMapper responseMapper;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // ignore permission validation.
        Mockito.when(pepResolver.allow(Mockito.anyString(), Mockito.any())).thenReturn(true);
    }

    @Test
    void testOverdraftDetailScreenDataResponseBff() {
        final String productId = "926893922";

        var response = given("KBID=970070122")
                .queryParam("productId", productId)
                .pathParam("userId", "100")
                .when()
                .get("/api/business-financing-overdraft/servicing/v1/users/{userId}/overdraft-detail-screen-data")
                .then()
                .assertThat().statusCode(200);

        response.body("productId", equalTo(productId))
                .body("account.iban", equalTo("CZ2901000000279609820237"))
                .body("account.accountNumber.prefix", equalTo("27"))
                .body("account.accountNumber.core", equalTo("9609820237"))
                .body("account.accountNumber.bankCode", equalTo("0100"))
                .body("account.accountAlias", equalTo("Kontokorent Business"))
                .body("account.accountAliasLong", equalTo("Kontokorent Business"))
                .body("productOwner.id", equalTo("926893922"))
                .body("productOwner.idSchema", equalTo("KBID"))
                .body("withdrawnAmount.amount", equalTo("0"))
                .body("withdrawnAmount.currency", equalTo("CZK"))
                .body("remainingAmount.amount", equalTo("100000"))
                .body("remainingAmount.currency", equalTo("CZK"))
                .body("limitAmount.amount", equalTo("100000"))
                .body("limitAmount.currency", equalTo("CZK"))
                .body("loanRate.percentage", equalTo("17.99"))
                .body("activeTerminationCaseFlag", is(true))
                //.body("terminationDate", is(LocalDate.of(2025, 1, 1))) // TODO: It's embarrassing, but it will be in a future story CSBFS-2330
                .body("withdrawnFlag", is(false));
    }

    @Test
    void testGeneratingState() {
        for (GeneratingState generatingState : List.of(GeneratingState.GENERATING, GeneratingState.DRAFT)) {
            Assertions.assertEquals(StatementGenerateStateResponse.StatementGenerationStateEnum.IN_PROGRESS,
                    responseMapper.map(generatingState));
        }

        Assertions.assertEquals(StatementGenerateStateResponse.StatementGenerationStateEnum.COMPLETED,
                responseMapper.map(GeneratingState.SUCCESS));

        for (GeneratingState generatingState : List.of(GeneratingState.DELETED, GeneratingState.FAILED)) {
            Assertions.assertEquals(StatementGenerateStateResponse.StatementGenerationStateEnum.FAILED,
                    responseMapper.map(generatingState));
        }
    }

    @Test
    void testOverdraftTerminatedScreenData() {
        var response = given("KBID=970070122")
                .queryParam("productId", "99999999999")
                .queryParam("servicingCaseId", "bb9f9f30-3aee-49a5-b6c1-26a51249a5e1")
                .pathParam("userId", "100")
                .when()
                .get("/api/business-financing-overdraft/servicing/v1/users/{userId}/overdraft-terminated-data")
                .then()
                .assertThat().statusCode(200);

        response.assertThat()
                .body("uuid", equalTo("bb9f9f30-3aee-49a5-b6c1-26a51249a5e1"))
                .body("code", equalTo("SIGNED"));
    }

    @Test
    void testOverdraftTerminationRequestedAction() {
        var response = given("KBID=970070122")
                .contentType(ContentType.JSON)
                .queryParam("userId", "123123123")
                .body(new OverdraftTerminationRequest().productId("0102"))
                .when()
                .post("/api/business-financing-overdraft/servicing/v1/actions/overdraftTerminationRequestedAction")
                .then()
                .assertThat().statusCode(200);

        response.assertThat()
                .body("servicingCaseId", equalTo("018dd0cb-befe-7c9b-a627-0190330e950a"));
    }

    @Test
    void testGetSigningCaseState() {
        var body = new SigningCaseState();
        body.setServicingCaseState(SigningCaseState.ServicingCaseStateEnum.STARTED);
        body.setSigningCaseId(UUID.fromString("ff6730bc-4b73-4eb4-bf3a-c7a78d5e4021"));
        var response = given("KBID=970070122")
                .queryParam("servicingCaseId", "ff6730bc-4b73-4eb4-bf3a-c7a78d5e4021")
                .queryParam("productId", "123456")
                .contentType(ContentType.JSON)
                .pathParam("userId", "1")
                .when()
                .get("api/business-financing-overdraft/servicing/v1/users/{userId}/get-signing-case-state")
                .then()
                .assertThat().statusCode(200);

        response.assertThat()
                .body("signingCaseId", equalTo("ff6730bc-4b73-4eb4-bf3a-c7a78d5e4021"))
                .body("servicingCaseState", equalTo("STARTED"));

    }

    @Test
    void testGetSigningCaseState2() {
        var body = new SigningCaseState();
        body.setServicingCaseState(SigningCaseState.ServicingCaseStateEnum.STARTED);
        body.setSigningCaseId(UUID.fromString("ff6730bc-4b73-4eb4-bf3a-c7a78d5e4021"));
        var response = given("KBID=970070122")
                .queryParam("servicingCaseId", "ff6730bc-4b73-4eb4-bf3a-c7a78d5e4022")
                .queryParam("productId", "123456")
                .contentType(ContentType.JSON)
                .pathParam("userId", "1")
                .when()
                .get("api/business-financing-overdraft/servicing/v1/users/{userId}/get-signing-case-state")
                .then()
                .assertThat().statusCode(200);

        response.assertThat()
                .body("signingCaseId", equalTo("ff6730bc-4b73-4eb4-bf3a-c7a78d5e4022"))
                .body("servicingCaseState", equalTo("NOT_STARTED"));

    }

}
