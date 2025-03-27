package cz.kb.leon.bff.servicing.infra.integrations.plc;

import cz.kb.leon.bc.productlifecycle_private_api_v1.dto.*;
import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import cz.kb.leon.bff.servicing.infra.integrations.ServicingCommonBffClient;
import cz.kb.leon.exception.translation.JaxRsClientExceptionHandler;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("bff-leon-servicing-service-user")
@JaxRsClientExceptionHandler(serviceIdentification = PLCClientService.PLC_CLIENT)
public class PLCClientService extends ServicingCommonBffClient {

    @Qualifier(PLC_CLIENT)
    private final Client plcClient;
    private final JaxRsRestClientProperties clientProperties;

    protected static final String PLC_CLIENT = "plcClient";
    private static final String OPERATING_LOAN_PRODUCT_DATA_URL = "/operational-loan/v1/operational-loans/qry/product-data";
    private static final String UPDATE_PRODUCT_ALIAS_URL = "/common/v1/products/{productInstanceId}/cmd/product-alias";
    private static final String BUSINESS_OVERDRAFT_PRODUCT_DATA_URL = "/business-overdraft/v1/overdrafts/qry/product-data";
    private static final String PRODUCT_INSTANCE_ID_PARAM = "productInstanceId";

    private static final String PLC_EXCEPTION_MESSAGE_PATTERN = "The Product Life Cycle service returned response status {}.";

    public GetOperationalLoanProductData200Response getOperatingLoanProductData(String productId) {
        var response = plcClient
                .target(evaluateTarget(OPERATING_LOAN_PRODUCT_DATA_URL))
                .queryParam(PRODUCT_INSTANCE_ID_PARAM, productId)
                .request()
                .get(Response.class);

        return evaluateResponse(response, GetOperationalLoanProductData200Response.class, PLC_EXCEPTION_MESSAGE_PATTERN);
    }

    public GetOverdraftProductData200Response getOverdraftProductData(String productId) {
        var response = plcClient
                .target(evaluateTarget(BUSINESS_OVERDRAFT_PRODUCT_DATA_URL))
                .queryParam(PRODUCT_INSTANCE_ID_PARAM, productId)
                .request()
                .get(Response.class);

        return evaluateResponse(response, GetOverdraftProductData200Response.class, PLC_EXCEPTION_MESSAGE_PATTERN);
    }

    public Response updateProductAlias(String productId, String alias) {
        ProductAlias productAlias = new ProductAlias().name(alias);

        var response = plcClient
                .target(evaluateTarget(UPDATE_PRODUCT_ALIAS_URL))
                .resolveTemplate(PRODUCT_INSTANCE_ID_PARAM, productId)
                .request()
                .put(Entity.json(productAlias), Response.class);

        if (response.getStatus() != 204) {
            return Response.status(response.getStatus()).entity(response.getEntity()).build();
        } else {
            return Response.status(Response.Status.CREATED).build();
        }
    }

    @Override
    protected String getClientName() {
        return PLC_CLIENT;
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
