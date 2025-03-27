package cz.kb.leon.bff.servicing.app.command.overdraft;

import jakarta.ws.rs.core.Response;

public record GetOverdraftDetailScreenDataCmd(String productId, String userId) {

    public record CmdResult(Response response) {}

}
