package cz.kb.leon.bff.servicing.app.command.operationalloan;

import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.EarlyRepaymentResultActionRequest;
import jakarta.ws.rs.core.Response;

public record OperationalLoanEarlyRepaymentResultActionCmd(EarlyRepaymentResultActionRequest earlyRepaymentResultActionRequest, String userId) {
    public record Result(Response response){}
}
