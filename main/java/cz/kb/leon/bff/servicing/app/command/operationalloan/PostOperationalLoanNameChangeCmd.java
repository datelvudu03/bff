package cz.kb.leon.bff.servicing.app.command.operationalloan;

import jakarta.ws.rs.core.Response;

import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.ChangeNameAction;

public record PostOperationalLoanNameChangeCmd(String userId, ChangeNameAction changeNameAction) {

    public record CmdResult(Response response) {}

}
