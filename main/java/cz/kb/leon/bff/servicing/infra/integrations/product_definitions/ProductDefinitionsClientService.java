package cz.kb.leon.bff.servicing.infra.integrations.product_definitions;

import cz.kb.leon.bc.productdefinition_private_v1.dto.ProductDefinitionDetail;
import cz.kb.leon.bff.servicing.infra.integrations.ServicingCommonBffClient;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;
import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("bff-leon-servicing-service-user")
public class ProductDefinitionsClientService extends ServicingCommonBffClient  {

    @Qualifier(PRODUCT_DEFINITIONS_CLIENT)
    private final Client productDefinitionsClient;
    private final JaxRsRestClientProperties clientProperties;

    private static final String PRODUCT_DEFINITION_ID_PARAM = "productDefinitionId";

    private static final String DEFINITION_DATA_URL = "/product-definitions/{productDefinitionId}/query/definition-data";

    private static final String PRODUCT_DEFINITIONS_CLIENT = "productDefinitionsClient";

    public ProductDefinitionDetail getDefinitionData(UUID productDefinitionId) {
        var response = productDefinitionsClient
                .target(evaluateTarget(DEFINITION_DATA_URL))
                .resolveTemplate(PRODUCT_DEFINITION_ID_PARAM, productDefinitionId)
                .request()
                .get(Response.class);

        return evaluateResponse(response, ProductDefinitionDetail.class, "The Product Definitions service returned response status {}.");
    }

    @Override
    protected String getClientName() {
        return PRODUCT_DEFINITIONS_CLIENT;
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
