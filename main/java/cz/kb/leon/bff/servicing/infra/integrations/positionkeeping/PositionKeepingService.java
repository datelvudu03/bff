package cz.kb.leon.bff.servicing.infra.integrations.positionkeeping;

import cz.kb.cbs.position_keeping.gen.jaxrs.model.InstructionResponseBALANCE;
import cz.kb.leon.bff.servicing.infra.integrations.ServicingCommonBffClient;
import cz.kb.leon.exception.translation.JaxRsClientExceptionHandler;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;
import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.client.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@JaxRsClientExceptionHandler(serviceIdentification = PositionKeepingService.POSITION_KEEPING_CLIENT)
public class PositionKeepingService extends ServicingCommonBffClient {

    @Qualifier(POSITION_KEEPING_CLIENT)
    private final Client positionKeepingClient;
    private final JaxRsRestClientProperties clientProperties;

    protected static final String POSITION_KEEPING_CLIENT = "positionKeepingClient";

    private static final String PK_BALANCE_URL_SUFFIX = "{accountId}/position/balance";

    private static final String BANK_CODE_IDENTIFIER_PARAMETER = "bankCode";
    private static final String ACCOUNT_ID_PARAMETER = "accountId";

    private static final String BANK_CODE_IDENTIFIER_VALUE = "100";

    public InstructionResponseBALANCE getBalance(String accountId) {
        var response = positionKeepingClient
                .target(getBaseUri() + PK_BALANCE_URL_SUFFIX)
                .resolveTemplate(ACCOUNT_ID_PARAMETER, accountId)
                .queryParam(BANK_CODE_IDENTIFIER_PARAMETER, BANK_CODE_IDENTIFIER_VALUE)
                .request()
                .get();

        return response.readEntity(InstructionResponseBALANCE.class);

    }

    @Override
    protected String getClientName() {
        return POSITION_KEEPING_CLIENT;
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
