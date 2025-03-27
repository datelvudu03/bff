package cz.kb.leon.bff.servicing.app.command.operationalloan;

import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.StatementRequestedActionRequest;
import jakarta.ws.rs.core.Response;

public record OperationalLoanStatementRequestedActionCmd(StatementRequestedActionRequest statementRequestedActionRequest, String xB3TraceId, String userId) {

    public record CmdResult(Response response) {}

}
