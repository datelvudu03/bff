package cz.kb.leon.bff.servicing.app.command.operationalloan;

import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.MonetaryAmount;
import jakarta.ws.rs.core.Response;

public record GetOperationalLoanPartialRepaymentAmountChangedActionCmd(String operationalLoanIban,
                                                                       MonetaryAmount amount,
                                                                       String xB3TraceId) {

    public record CmdResult(Response response) {
    }

}
