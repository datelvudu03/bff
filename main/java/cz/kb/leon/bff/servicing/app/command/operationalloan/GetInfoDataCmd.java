package cz.kb.leon.bff.servicing.app.command.operationalloan;

import jakarta.ws.rs.core.Response;

import cz.kb.leon.bff.servicing.util.EndUserUtil;

public record GetInfoDataCmd(String userId, String productInstanceId, EndUserUtil.LanguageEnum language) {

    public record CmdResult(Response response) {

    }
}