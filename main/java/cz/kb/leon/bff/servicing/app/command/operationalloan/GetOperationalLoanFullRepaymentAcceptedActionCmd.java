package cz.kb.leon.bff.servicing.app.command.operationalloan;

import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.MonetaryAmount;
import jakarta.ws.rs.core.Response;

public record GetOperationalLoanFullRepaymentAcceptedActionCmd(String productId,
                                                               String fromAccountIban,
                                                               MonetaryAmount amount,
                                                               String xB3TraceId,
                                                               String userId) {
    public record CmdResult(Response response) {}
}
