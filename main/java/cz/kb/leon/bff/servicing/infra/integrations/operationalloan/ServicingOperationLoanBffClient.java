package cz.kb.leon.bff.servicing.infra.integrations.operationalloan;

import cz.kb.leon.assertion.AssertCheck;
import cz.kb.leon.bc.servicing.v2.dto.*;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.CreateEarlyRepaymentCaseRequest;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.CreateEarlyRepaymentCaseResponse;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.EarlyRepaymentAuthorizedRequest;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.EarlyRepaymentAvailabilityResult;
import cz.kb.leon.bff.servicing.infra.mapper.loan.ResponseMapper;
import cz.kb.leon.bff.servicing.infra.integrations.ServicingCommonBffClient;
import cz.kb.leon.exception.CommonExceptionCode;
import cz.kb.leon.exception.translation.JaxRsClientExceptionHandler;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;
import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("bff-leon-servicing-service-user")
@JaxRsClientExceptionHandler(serviceIdentification = ServicingOperationLoanBffClient.SERVICING_BC_CLIENT)
public class ServicingOperationLoanBffClient extends ServicingCommonBffClient {

    private final @Qualifier(SERVICING_BC_CLIENT) Client servicingBcClient;
    private final JaxRsRestClientProperties clientProperties;

    protected static final String SERVICING_BC_CLIENT = "servicingBcClient";
    private static final String REQUEST_STATEMENT_URL_SUFFIX = "/servicing-private/v2/cmd/request-statement";
    private static final String GET_STATEMENT_GENERATING_STATE_URL_SUFFIX = "/servicing-private/v1/statements/{statementId}/qry/generating-state";
    private static final String DOCUMENT_DATA_URL_SUFFIX = "/servicing-private/v1/statements/{statementId}/qry/document-data";
    private static final String SERVICING_CASE_QUERY_SUFFIX = "/servicing-private/v2/servicing-cases/qry/servicing-cases";
    private static final String CREATE_EARLY_REPAYMENT_CASE_SUFFIX = "/servicing-operatinalloan-private/v1/cases/cmd/create-early-repayment-case";
    private static final String EARLY_REPAYMENT_AVAILABILITY_SUFFIX = "/servicing-operatinalloan-private/v1/query/early-repayment-availability";
    private static final String EARLY_REPAYMENT_AUTHORIZED = "/servicing-operatinalloan-private/v1/cases/{servicingCaseId}/cmd/early-repayment-authorized";
    private static final String X_B3_TRACE_ID_HEADER_PARAM = "X-B3-TraceId";
    private static final String STATEMENT_ID_PATH_PARAM = "statementId";
    private static final String TIMESTAMP_PARAM = "timestamp";

    private final ResponseMapper responseMapper = Mappers.getMapper(ResponseMapper.class);

    public EarlyRepaymentAvailabilityResult earlyRepaymentAvailability(String productId, String userId) {
        var response = servicingBcClient
                .target(evaluateTarget(EARLY_REPAYMENT_AVAILABILITY_SUFFIX))
                .queryParam(PRODUCT_INSTANCE_ID, productId)
                .queryParam(CLIENT_ID, userId)
                .queryParam(TIMESTAMP_PARAM, LocalDateTime.now(ZoneOffset.UTC).toString())
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);

        AssertCheck.isTrue(Response.Status.OK.getStatusCode() == response.getStatus(), String.format("LEON_SERVICING service returned state %s", response.getStatus()), CommonExceptionCode.ASSERTION_ERROR_ILLEGAL_STATE);

        return getResponseEntity(response, EarlyRepaymentAvailabilityResult.class);
    }

    public Response requestStatement(RequestStatementCmdReq requestStatementCmdReq, String xB3TraceId, String userId) {
        var response = servicingBcClient
                .target(evaluateTarget(REQUEST_STATEMENT_URL_SUFFIX))
                .queryParam(CLIENT_ID, userId)
                .request(MediaType.APPLICATION_JSON)
                .header(X_B3_TRACE_ID_HEADER_PARAM, xB3TraceId)
                .post(Entity.json(requestStatementCmdReq), Response.class);

        return parseResponse(response, () -> responseMapper.mapRequestStatementCmdRespToBusinessLoanStatementRequestedActionResponse(getResponseEntity(response, RequestStatementCmdResp.class)));
    }

    public void cmdEarlyRepaymentAuthorized(EarlyRepaymentAuthorizedRequest earlyRepaymentAuthorizedRequest, UUID serviceCaseId, String userId) {
        try (var response = servicingBcClient
                .target(evaluateTarget(EARLY_REPAYMENT_AUTHORIZED))
                .resolveTemplate(SERVICING_CASE_ID, serviceCaseId)
                .queryParam(CLIENT_ID, userId)
                .request()
                .post(Entity.json(earlyRepaymentAuthorizedRequest), Response.class)) {
            AssertCheck.isTrue(Response.Status.CREATED.getStatusCode() == response.getStatus(), String.format("BC service returned state %s for cmdEarlyRepaymentAuthorized(), but OK is expected.", response.getStatus()), CommonExceptionCode.ASSERTION_ERROR_ILLEGAL_STATE);
        }

    }

    public Response getStatementGeneratingState(UUID statementId, String userId) {
        var response = servicingBcClient
                .target(evaluateTarget(GET_STATEMENT_GENERATING_STATE_URL_SUFFIX))
                .resolveTemplate(STATEMENT_ID_PATH_PARAM, statementId)
                .queryParam(CLIENT_ID, userId)
                .request()
                .get(Response.class);

        return parseResponse(response, () -> responseMapper.mapGetStatementGeneratingStateActionResponse(getResponseEntity(response, GeneratingStateResp.class)));
    }

    public Response documentData(UUID statementId, String productInstanceId, String userId) {
        var response = servicingBcClient
                .target(evaluateTarget(DOCUMENT_DATA_URL_SUFFIX))
                .resolveTemplate(STATEMENT_ID_PATH_PARAM, statementId)
                .queryParam(PRODUCT_INSTANCE_ID, productInstanceId)
                .queryParam(CLIENT_ID, userId)
                .request("application/pdf")
                .get(Response.class);

        return parseResponse(response, () -> getResponseEntity(response, File.class));
    }

    public QryGetServicingCasesV2200Response servicingCaseQuery(String productInstanceId, String userId) {
        return servicingCaseQuery(productInstanceId, null, null, userId);
    }

    public QryGetServicingCasesV2200Response servicingCaseQuery(String productInstanceId, Set<CaseType> caseTypes, Set<CaseState> states, String userId) {
        ServicingCasesReq request = new ServicingCasesReq()
                .productInstanceId(productInstanceId)
                .caseTypes(caseTypes)
                .caseStates(states);

        Response response = servicingBcClient
                .target(evaluateTarget(SERVICING_CASE_QUERY_SUFFIX))
                .queryParam(CLIENT_ID, userId)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request), Response.class);

        return getResponseEntity(response, QryGetServicingCasesV2200Response.class);
    }

    public Pair<Integer, Object> createEarlyRepaymentCase(CreateEarlyRepaymentCaseRequest createEarlyRepaymentCaseRequest,
                                                          String xB3TraceId, String userId) {
        var response = servicingBcClient
                .target(evaluateTarget(CREATE_EARLY_REPAYMENT_CASE_SUFFIX))
                .queryParam(CLIENT_ID, userId)
                .request(MediaType.APPLICATION_JSON)
                .header(X_B3_TRACE_ID_HEADER_PARAM, xB3TraceId)
                .post(Entity.json(createEarlyRepaymentCaseRequest), Response.class);

        return parseResponse(response, Response.Status.CREATED, Response.Status.CREATED,
                () -> responseMapper.mapCreateEarlyRepaymentCaseResponseToPartialRepaymentAcceptedActionResponse(getResponseEntity(response, CreateEarlyRepaymentCaseResponse.class)));
    }

    @Override
    protected String getClientName() {
        return SERVICING_BC_CLIENT;
    }

    @Override
    protected JaxRsRestClientProperties getClientProperties() {
        return clientProperties;
    }

    @Override
    protected Logger getLog() {
        return log;
    }
}
