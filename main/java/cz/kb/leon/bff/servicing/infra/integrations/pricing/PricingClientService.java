package cz.kb.leon.bff.servicing.infra.integrations.pricing;

import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import cz.kb.leon.bc.pricing.v1.dto.FeesDetail;
import cz.kb.leon.bc.pricing.v1.dto.InterestRateDetailV1;
import cz.kb.leon.bff.servicing.infra.integrations.ServicingCommonBffClient;
import cz.kb.leon.exception.translation.JaxRsClientExceptionHandler;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("bff-leon-servicing-service-user")
@JaxRsClientExceptionHandler(serviceIdentification = PricingClientService.PRICING_CLIENT)
public class PricingClientService extends ServicingCommonBffClient {

    @Qualifier(PRICING_CLIENT)
    private final Client pricingClient;
    private final JaxRsRestClientProperties clientProperties;
    protected static final String PRICING_CLIENT = "pricingClient";

    private static final String IR_DETAIL_BY_PRODUCT_INSTANCE_ID_V1_URL = "/v1/pricing/pricing-collection/query/v1/interest-rate-detail";



    private static final String FEES_DETAIL_V1_URL = "/v1/pricing/pricing-collection/{pricingId}/query/v1/fees-detail";
    private static final String PRICING_ID_PARAM = "pricingId";
    private static final String PRODUCT_INSTANCE_ID = "productInstanceId";

    public InterestRateDetailV1 getIRDetailByProductInstanceIdV1(String productInstanceId) {
        var response = pricingClient
                .target(evaluateTarget(IR_DETAIL_BY_PRODUCT_INSTANCE_ID_V1_URL))
                .queryParam(PRODUCT_INSTANCE_ID, productInstanceId)
                .request()
                .get(Response.class);

        return evaluateResponse(response, InterestRateDetailV1.class, "The pricing service returned response status {}.");
    }

    public FeesDetail getFeesDetailV1(String pricingId) {
        var response = pricingClient
                .target(evaluateTarget(FEES_DETAIL_V1_URL))
                .resolveTemplate(PRICING_ID_PARAM, pricingId)
                .request()
                .get(Response.class);

        return evaluateResponse(response, FeesDetail.class, "The pricing service returned response status {}.");
    }

    @Override
    protected String getClientName() {
        return PRICING_CLIENT;
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
