package cz.kb.leon.bff.servicing.infra.integrations.overdraft;

import cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.ConfirmationScreenResp;
import cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.DetailScreenResp;
import cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.TerminationActionResp;
import cz.kb.leon.bff.servicing.infra.mapper.MappingDTO;
import cz.kb.leon.bff.servicing.infra.integrations.ServicingCommonBffClient;
import cz.kb.leon.exception.translation.JaxRsClientExceptionHandler;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.SigningCaseState;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;
import jakarta.annotation.security.RunAs;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.PRODUCT_INSTANCE_ID;
import static cz.kb.leon.bff.servicing.configuration.AppConstants.SERVICING_CASE_STATE_IN_PROGRESS;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("bff-leon-servicing-service-user")
@JaxRsClientExceptionHandler(serviceIdentification = ServicingOverdraftBffClient.SERVICING_BC_CLIENT)
public class ServicingOverdraftBffClient extends ServicingCommonBffClient {

    private static final String PRODUCT_ID = "productId";
    private static final String LANGUAGE = "language";

    private static final String OVERDRAFT_TERMINATION_SCREEN_DATA_URL_SUFFIX = "/servicing-businessoverdraft-private/termination/v2/query/confirmation-screen-data";
    private static final String OVERDRAFT_REQUEST_TERMINATION_ACTION_URL_SUFFIX = "/servicing-businessoverdraft-private/termination/v2/cmd/request-termination";
    private static final String SIGN_CASE_STATE_URL_SUFFIX = "/servicing-businessoverdraft-private/termination/v2/query/sign-case-state";
    private static final String PRODUCT_DETAIL_SCREEN_DATA_URL = "/servicing-businessoverdraft-private/screenData/v1/query/product-detail-screen-data";
    private static final String SERVICING_CASE_ID_IDENTIFIER = "servicingCaseId";
    protected static final String SERVICING_BC_CLIENT = "servicingBcClient";

    @Qualifier(SERVICING_BC_CLIENT)
    private final Client servicingBcClient;
    private final JaxRsRestClientProperties clientProperties;
    private final MappingDTO mapper = Mappers.getMapper(MappingDTO.class);

    public Response overdraftTerminationRequestedActionResponse(@Valid @NotNull String productId) {
        var jaxClientProperties = getServicingBcClient();

        var target = jaxClientProperties.getBaseUri() + OVERDRAFT_REQUEST_TERMINATION_ACTION_URL_SUFFIX;

        var response = servicingBcClient
                .target(target)
                .queryParam(PRODUCT_INSTANCE_ID, productId)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(null), Response.class);

        return parseResponse(response, () -> mapper.toOverdraftTerminationActionResponse(getResponseEntity(response, TerminationActionResp.class)));
    }

    public Response getSigningCaseState(@NotNull @NotEmpty String productId, @NotNull UUID servicingCaseId) {
        var jaxClientProperties = getServicingBcClient();
        var target = jaxClientProperties.getBaseUri() + SIGN_CASE_STATE_URL_SUFFIX;

        Response response = servicingBcClient
                .target(target)
                .queryParam(SERVICING_CASE_ID_IDENTIFIER, servicingCaseId)
                .queryParam(PRODUCT_INSTANCE_ID, productId)
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);

        if (response.getStatus() != 200) {
            return Response.status(response.getStatus()).entity(response.getEntity()).build();
        }
        cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.SigningCaseState overdraftTerminationStatus = response.readEntity(cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.SigningCaseState.class);
        SigningCaseState signingCaseState = mapToSigningCaseState(overdraftTerminationStatus);
        return Response.ok(signingCaseState).build();
    }

    public Response overdraftTerminatedScreenDataResponse(UUID servicingCaseId, String productId) {
        var jaxClientProperties = getServicingBcClient();
        var target = jaxClientProperties.getBaseUri() + OVERDRAFT_TERMINATION_SCREEN_DATA_URL_SUFFIX;

        var response = servicingBcClient
                .target(target)
                .queryParam(PRODUCT_INSTANCE_ID, productId)
                .queryParam(SERVICING_CASE_ID_IDENTIFIER, servicingCaseId)
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);

        return parseResponse(response, () -> mapper.getOverdraftTerminationScreenDataResponseToResult(getResponseEntity(response, ConfirmationScreenResp.class)));
    }

    public Response getConfirmationScreenData(String productId, String language) {
        var response = servicingBcClient
                .target(evaluateTarget(PRODUCT_DETAIL_SCREEN_DATA_URL))
                .queryParam(PRODUCT_ID, productId)
                .queryParam(LANGUAGE, language)
                .request(MediaType.APPLICATION_JSON)
                .get(Response.class);

        return parseResponse(response, () -> mapper.toOverdraftDetail(getResponseEntity(response, DetailScreenResp.class)));
    }

    private JaxRsRestClientProperties.Client getServicingBcClient() {
        return this.clientProperties.getClient().get(SERVICING_BC_CLIENT);
    }

    private SigningCaseState mapToSigningCaseState(cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.SigningCaseState signingCaseStateFromBc) {
        SigningCaseState signingCaseState = new SigningCaseState();
        signingCaseState.setSigningCaseId(signingCaseStateFromBc.getSigningCaseId());
        signingCaseState.setServicingCaseState(signingCaseStateFromBc.getServicingCaseState().equals(SERVICING_CASE_STATE_IN_PROGRESS) ?
                SigningCaseState.ServicingCaseStateEnum.STARTED :
                SigningCaseState.ServicingCaseStateEnum.NOT_STARTED);
        return signingCaseState;
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
