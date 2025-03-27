package cz.kb.leon.bff.servicing.infra.integrations.t24;

import cz.kb.api.loansapi.v1.dto.*;
import cz.kb.leon.bff.servicing.infra.integrations.ServicingCommonBffClient;
import cz.kb.leon.bff.servicing.util.ObjectUtil;
import cz.kb.leon.exception.ServiceCallException;
import cz.kb.leon.exception.translation.JaxRsClientExceptionHandler;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.MonetaryAmount;
import cz.kb.speed.rest.config.JaxRsRestClientProperties;
import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("bff-leon-servicing-service-user")
@JaxRsClientExceptionHandler(serviceIdentification = T24ClientService.T24_CLIENT)
public class T24ClientService extends ServicingCommonBffClient {

    @Qualifier(T24_CLIENT)
    private final Client t24Client;
    private final JaxRsRestClientProperties clientProperties;

    protected static final String T24_CLIENT = "t24Client";

    private static final String GET_LOAN_DETAIL_URL_SUFFIX = "/v1.0.0/party/loan/detail";
    private static final String GET_LOAN_ARRANGEMENT_DETAIL_URL_SUFFIX = "/v1.0.0/party/loan/arrangement/detail";
    private static final String POST_PARTY_LOAN_PREPAY_SIMULATION_SUFFIX = "/v1.0.0/party/loan/prepay/simulation";
    private static final String POST_PARTY_LOAN_PAYOFF_SIMULATION_SUFFIX = "/v1.0.0/party/loan/payoff/simulation";
    private static final String GET_PARTY_LOAN_GET_SIM_STATUS_SUFFIX = "/v1.0.0/party/loan/get/sim/status/{simulationId}";
    private static final String GET_PARTY_LOAN_SIMULATION_SCHEDULE_DETAILS_SUFFIX = "/v1.0.0/party/loan/simulation/schedule/details";
    private static final String GET_PARTY_LOAN_BILL_DETAILS_BY_IBAN_SUFFIX = "/v1.0.0/party/loan/bill/details/{loanAccountIban}";
    private static final String GET_PAYMENT_SCHEDULE_SUFFIX = "/v1.0.0/party/loan/schedule/{id}";

    private static final String ARRANGEMENT_ID_PARAMETER = "arrangementId";
    private static final String SIMULATION_ID_PARAMETER = "simulationId";
    private static final String LOAN_ACCOUNT_IBAN_PARAMETER = "loanAccountIban";
    private static final String ID_PARAMETER = "id";

    public CustomerLoanResponse getLoanDetail(String arrangementId) {
        var response = t24Client
                .target(evaluateTarget(GET_LOAN_DETAIL_URL_SUFFIX))
                .queryParam(ARRANGEMENT_ID_PARAMETER, arrangementId)
                .request()
                .get();

        return evaluateResponse(response, CustomerLoanResponse.class, "The method getCustomerLoan of the T24 service returned response status {}.");
    }

    public LoanDetailResponse getLoanArrangementDetail(String arrangementId) {
        var response = t24Client
                .target(evaluateTarget(GET_LOAN_ARRANGEMENT_DETAIL_URL_SUFFIX))
                .queryParam(ARRANGEMENT_ID_PARAMETER, arrangementId)
                .request()
                .get();

        return evaluateResponse(response, LoanDetailResponse.class, "The method getLoanDetail of the T24 service returned response status {}.");
    }

    public CalPayoffSimulationRunnerResponse createCalPayoffSimulationRunner(String iban, MonetaryAmount amount) {
        var amountValueOptional = Optional.ofNullable(amount)
                .map(MonetaryAmount::getAmount);
        var calPayoffSimulationRunnerBody = new CalPayoffSimulationRunnerBody()
                .arrangement(iban);
        amountValueOptional.ifPresent(calPayoffSimulationRunnerBody::transactionAmount);
        var calPayoffSimulationRunner = new CalPayoffSimulationRunner().body(calPayoffSimulationRunnerBody);
        var response = t24Client
                .target(evaluateTarget(POST_PARTY_LOAN_PREPAY_SIMULATION_SUFFIX))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(calPayoffSimulationRunner));

        return evaluateResponse(response, CalPayoffSimulationRunnerResponse.class, "The method createCalPayoffSimulationRunner of the T24 service returned response status {}.");
    }

    public CalPayoffSimulationResponse createCalPayoffSimulation(String iban) {
        var calPayoffSimulationBody = new CalPayoffSimulationBody().arrangementId(iban);
        var calPayoffSimulationRunner = new CalPayoffSimulation().body(calPayoffSimulationBody);
        var response = t24Client
                .target(evaluateTarget(POST_PARTY_LOAN_PAYOFF_SIMULATION_SUFFIX))
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(calPayoffSimulationRunner));

        return evaluateResponse(response, CalPayoffSimulationResponse.class, "The method createCalPayoffSimulation of the T24 service returned response status {}.");
    }

    public SimulationStatusResponse getSimulationStatus(String simulationId) {
        var response = t24Client
                .target(evaluateTarget(GET_PARTY_LOAN_GET_SIM_STATUS_SUFFIX))
                .resolveTemplate(SIMULATION_ID_PARAMETER, simulationId)
                .request()
                .get();

        var simulationStatusResponse = evaluateResponse(response, SimulationStatusResponse.class, "The method getSimulationStatus of the T24 service returned response status {}.");

        var simulationStatus = Optional.ofNullable(simulationStatusResponse)
                .map(SimulationStatusResponse::getBody)
                .orElseGet(List::of).stream().findFirst()
                .map(SimulationStatusResponseBodyInner::getSimulationStatus)
                .map(String::trim)
                .orElse("UNKNOWN");

        var status = simulationFinished(simulationStatus);
        if (!status) {
            throw new SimulationStatusInProgressException(ObjectUtil.evaluateMessage("Simulation progress is in state {}", simulationStatus));
        }

        return simulationStatusResponse;
    }

    @Retryable(retryFor = {SimulationStatusInProgressException.class, ServiceCallException.class}, maxAttempts = 8, backoff = @Backoff(delay = 5000L, multiplier = 1.2))
    public boolean checkSimulationStatusWithRetry(String simulationId, AtomicInteger attemptCounter) {
        attemptCounter.incrementAndGet();
        var simulationStatusResponse = this.getSimulationStatus(simulationId);

        return checkSuccess(simulationStatusResponse);
    }

    public LoanSimRunnerDetailResponse getLoanSimRunnerDetail(String simulationId, String iban) {
        var response = t24Client
                .target(evaluateTarget(GET_PARTY_LOAN_SIMULATION_SCHEDULE_DETAILS_SUFFIX))
                .queryParam(SIMULATION_ID_PARAMETER, simulationId)
                .queryParam(ARRANGEMENT_ID_PARAMETER, iban)
                .request()
                .get();

        return evaluateResponse(response, LoanSimRunnerDetailResponse.class, "The method getLoanSimRunnerDetail of the T24 service returned response status {}.");
    }

    public TodayBillDetailsResponse getTodayBillDetails(String loanAccountIban) {
        var response = t24Client
                .target(evaluateTarget(GET_PARTY_LOAN_BILL_DETAILS_BY_IBAN_SUFFIX))
                .resolveTemplate(LOAN_ACCOUNT_IBAN_PARAMETER, loanAccountIban)
                .request()
                .get();

        return evaluateResponse(response, TodayBillDetailsResponse.class, "The method getTodayBillDetails of the T24 service returned response status {}.");
    }

    public PaymentScheduleResponse getPaymentSchedule(String loanAccountIban) {
        var response = t24Client
                .target(evaluateTarget(GET_PAYMENT_SCHEDULE_SUFFIX))
                .resolveTemplate(ID_PARAMETER, loanAccountIban)
                .request()
                .get();

        return evaluateResponse(response, PaymentScheduleResponse.class, "The method getPaymentSchedule of the T24 service returned response status {}.");
    }

    private boolean simulationFinished(String simulationStatus) {
        return simulationStatus.toUpperCase().matches("^(COMPLETED|EXECUTED).*$");
    }

    private boolean checkSuccess(SimulationStatusResponse simulationStatusResponse) {
        var simulationStatus = Optional.ofNullable(simulationStatusResponse)
                .map(SimulationStatusResponse::getBody)
                .orElseGet(List::of).stream().findFirst()
                .map(SimulationStatusResponseBodyInner::getSimulationStatus)
                .map(String::trim)
                .orElse("UNKNOWN");

        return simulationStatus.toUpperCase().matches("^.*(SUCCESSFULLY)$");
    }

    @Override
    protected String getClientName() {
        return T24_CLIENT;
    }

    @Override
    protected JaxRsRestClientProperties getClientProperties() {
        return clientProperties;
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    public static class SimulationStatusInProgressException extends RuntimeException {
        public SimulationStatusInProgressException(String message) {
            super(message);
        }
    }
}
