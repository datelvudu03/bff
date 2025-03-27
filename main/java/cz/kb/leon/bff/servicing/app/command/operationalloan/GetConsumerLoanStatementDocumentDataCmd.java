package cz.kb.leon.bff.servicing.app.command.operationalloan;

import jakarta.ws.rs.core.Response;

import java.util.UUID;

public record GetConsumerLoanStatementDocumentDataCmd(UUID statementId, String productInstanceId, String userId) {

    public record CmdResult(Response response) {}

}
