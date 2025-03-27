package cz.kb.leon.bff.servicing.app.command.operationalloan;

import cz.kb.leon.bff.servicing.util.EndUserUtil;
import jakarta.ws.rs.core.Response;

public record GetDetailDataCmd(String userId, String productInstanceId, EndUserUtil.LanguageEnum language) {

    public record CmdResult(Response response) {}

}
