package cz.kb.leon.bff.servicing.app.command.operationalloan;

import jakarta.ws.rs.core.Response;

import java.util.UUID;

public record GetOperationalLoanStatementDocumentStateCmd(UUID statementId, String userId) {

    public record CmdResult(Response response) {}

}
