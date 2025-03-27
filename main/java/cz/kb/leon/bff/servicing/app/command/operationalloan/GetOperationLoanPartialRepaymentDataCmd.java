package cz.kb.leon.bff.servicing.app.command.operationalloan;

import jakarta.ws.rs.core.Response;

public record GetOperationLoanPartialRepaymentDataCmd(String userId, String productInstanceId) {

    public record CmdResult(Response response) {}

}
