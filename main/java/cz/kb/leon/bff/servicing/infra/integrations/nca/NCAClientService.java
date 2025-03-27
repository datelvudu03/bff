package cz.kb.leon.bff.servicing.infra.integrations.nca;

import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.client.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import cz.kb.api.accountservicing.v1.dto.GetCurrentAccountDetailRes;
import cz.kb.leon.bff.servicing.infra.integrations.ServicingCommonBffClient;
import cz.kb.leon.exception.translation.JaxRsClientExceptionHandler;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("ncaServiceUser")
@JaxRsClientExceptionHandler(serviceIdentification = NCAClientService.NCA_CLIENT)
public class NCAClientService extends ServicingCommonBffClient {

    @Qualifier(NCA_CLIENT)
    private final Client ncaClient;
    private final JaxRsRestClientProperties clientProperties;
    protected static final String NCA_CLIENT = "ncaClient";

    private static final String CURRENT_ACCOUNT_DETAIL_URL = "/accounts/account-servicing/v1/current-accounts/{repaymentAccountId}";

    public GetCurrentAccountDetailRes getCurrentAccountDetail(String repaymentAccountId) {
        var response = ncaClient
                .target(evaluateTarget(CURRENT_ACCOUNT_DETAIL_URL))
                .resolveTemplate("repaymentAccountId", repaymentAccountId)
                .request()
                .get();

        return evaluateResponse(response, GetCurrentAccountDetailRes.class, "The PLC Client returned response status {}.");
    }

    @Override
    protected String getClientName() {
        return NCA_CLIENT;
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
