package cz.kb.leon.bff.servicing.infra.ui.operationalloan;

import cz.kb.leon.bff.servicing.app.command.operationalloan.*;
import cz.kb.leon.bff.servicing.domain.exception.DomainException;
import cz.kb.leon.bff.servicing.domain.exception.DomainExceptionCode;
import cz.kb.leon.bff.servicing.infra.annotation.TranslateExceptionToFeObject;
import cz.kb.leon.bff.servicing.infra.ui.CommonServiceImpl;
import cz.kb.leon.bff.servicing.util.EndUserUtil;
import cz.kb.leon.exception.StructuredItoLog;
import cz.kb.leon.featureflags.FeatureFlagService;
import cz.kb.leon.featureflags.annotations.EnabledOnFeatureFlag;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.BusinessFinancingOperationalLoanService;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.*;
import cz.kb.speed.cqrs.api.command.CommandBus;
import cz.kb.speed.messaging.api.model.ResponseMessage;
import cz.kb.speed.tracing.profiler.Profiler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.UUID;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.FF_OPERATIONAL_LOAN;

@ConstrainedTo(RuntimeType.SERVER)
@Component
@RequiredArgsConstructor
@Validated
@TranslateExceptionToFeObject
@Profiler(useKebabCaseForDefaultOperationName = true)
@StructuredItoLog(logSpeedExceptions = true)
public class BusinessFinancingOperationLoanServiceImpl extends CommonServiceImpl implements BusinessFinancingOperationalLoanService {

    private final CommandBus commandBus;

    private final FeatureFlagService featureFlagService;

    @Override
    protected FeatureFlagService getFeatureFlagService() {
        return featureFlagService;
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManage(#productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response getOperationalLoanEarlyRepaymentInfoData(@NotNull @NotEmpty String xKbSessionId,
                                                             @NotNull @NotEmpty String xKbFePlatform,
                                                             @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                             @NotNull @NotEmpty String xKbFeChannel,
                                                             @NotNull @NotEmpty String xKbBusChannel,
                                                             @NotNull @NotEmpty String acceptLanguage,
                                                             String userId,
                                                             @NotNull String productInstanceId) {
        commandBus.dispatch(new GetOperationalLoanEarlyRepaymentInfoDataCmd(productInstanceId, userId));

        return Response.ok().build();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountDetailsRead(#productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response getOperationalLoanStatementDocumentData(@NotNull @NotEmpty String xKbSessionId,
                                                            @NotNull @NotEmpty String xKbFePlatform,
                                                            @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                            @NotNull @NotEmpty String xKbFeChannel,
                                                            @NotNull @NotEmpty String xKbBusChannel,
                                                            String userId,
                                                            @NotNull UUID statementId,
                                                            @NotNull String productInstanceId) {
        ResponseMessage<GetConsumerLoanStatementDocumentDataCmd.CmdResult> response = commandBus.sendAndGetReply(new GetConsumerLoanStatementDocumentDataCmd(statementId, productInstanceId, userId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountDetailsRead(#productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response getOperationalLoanStatementDocumentState(@NotNull @NotEmpty String xKbSessionId,
                                                             @NotNull @NotEmpty String xKbFePlatform,
                                                             @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                             @NotNull @NotEmpty String xKbFeChannel,
                                                             @NotNull @NotEmpty String xKbBusChannel,
                                                             String userId,
                                                             @NotNull UUID statementId,
                                                             @NotNull String productInstanceId) {
        ResponseMessage<GetOperationalLoanStatementDocumentStateCmd.CmdResult> responseMessage = commandBus.sendAndGetReply(new GetOperationalLoanStatementDocumentStateCmd(statementId, userId));

        return responseMessage.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountDetailsRead(#productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response operationalLoanDetailData(@NotNull @NotEmpty String xKbSessionId,
                                              @NotNull @NotEmpty String xKbFePlatform,
                                              @NotNull @NotEmpty String xUserIdIdentitySchema,
                                              @NotNull @NotEmpty String xKbFeChannel,
                                              @NotNull @NotEmpty String xKbBusChannel,
                                              @NotNull @NotEmpty String acceptLanguage,
                                              String userId,
                                              @NotNull String productInstanceId) {
        ResponseMessage<GetDetailDataCmd.CmdResult> response = commandBus.sendAndGetReply(new GetDetailDataCmd(userId, productInstanceId, EndUserUtil.usedLanguage(acceptLanguage)));

        return response.getPayload().response();
    }


    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManage(#earlyRepaymentResultActionRequest.productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response operationalLoanEarlyRepaymentResultAction(@NotNull @NotEmpty String xKbSessionId,
                                                              @NotNull @NotEmpty String xKbFePlatform,
                                                              @NotNull @NotEmpty String xKbFeChannel,
                                                              @NotNull @NotEmpty String xKbBusChannel,
                                                              @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                              @NotNull @NotEmpty String acceptLanguage,
                                                              @NotNull @Pattern(regexp = "^\\d{8,12}$") String userId,
                                                              @Valid @NotNull EarlyRepaymentResultActionRequest earlyRepaymentResultActionRequest) {
        ResponseMessage<OperationalLoanEarlyRepaymentResultActionCmd.Result> response = commandBus.sendAndGetReply(new OperationalLoanEarlyRepaymentResultActionCmd(earlyRepaymentResultActionRequest, userId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManage(#repaymentAcceptedActionRequest.productInstanceId, #xKbBusChannel, #userId) " +
            "and @permissionResolver.allowPaymentCreateWithFromAccountIban(#repaymentAcceptedActionRequest.fromAccountIban, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response operationalLoanFullRepaymentAcceptedAction(@NotNull @NotEmpty String xKbSessionId,
                                                               @NotNull @NotEmpty String xKbFePlatform,
                                                               @NotNull @NotEmpty String xKbFeChannel,
                                                               @NotNull @NotEmpty String xKbBusChannel,
                                                               @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                               @NotNull @NotEmpty String acceptLanguage,
                                                               @NotNull @Pattern(regexp = "^\\d{8,12}$") String userId,
                                                               @Valid @NotNull RepaymentAcceptedActionRequest repaymentAcceptedActionRequest) {
        ResponseMessage<GetOperationalLoanFullRepaymentAcceptedActionCmd.CmdResult> response = commandBus.sendAndGetReply(
                new GetOperationalLoanFullRepaymentAcceptedActionCmd(
                        repaymentAcceptedActionRequest.getProductInstanceId(),
                        repaymentAcceptedActionRequest.getFromAccountIban(),
                        getMonetaryAmount(repaymentAcceptedActionRequest),
                        xKbSessionId, userId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountDetailsRead(#productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response operationalLoanInfoData(@NotNull @NotEmpty String xKbSessionId,
                                            @NotNull @NotEmpty String xKbFePlatform,
                                            @NotNull @NotEmpty String xUserIdIdentitySchema,
                                            @NotNull @NotEmpty String xKbFeChannel,
                                            @NotNull @NotEmpty String xKbBusChannel,
                                            @NotNull @NotEmpty String acceptLanguage,
                                            String userId,
                                            @NotNull String productInstanceId) {
        ResponseMessage<GetInfoDataCmd.CmdResult> response = commandBus.sendAndGetReply(new GetInfoDataCmd(userId, productInstanceId, EndUserUtil.usedLanguage(acceptLanguage)));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManage(#changeNameAction.productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response operationalLoanNameChangeRequestedAction(@NotNull @NotEmpty String xKbSessionId,
                                                             @NotNull @NotEmpty String xKbFePlatform,
                                                             @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                             @NotNull @NotEmpty String xKbFeChannel,
                                                             @NotNull @NotEmpty String xKbBusChannel,
                                                             @NotNull @NotEmpty String acceptLanguage,
                                                             @NotNull @Pattern(regexp = "^\\d{8,12}$") String userId,
                                                             @Valid @NotNull ChangeNameAction changeNameAction) {
        ResponseMessage<PostOperationalLoanNameChangeCmd.CmdResult> response = commandBus.sendAndGetReply(new PostOperationalLoanNameChangeCmd(userId, changeNameAction));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManage(#repaymentAcceptedActionRequest.productInstanceId, #xKbBusChannel, #userId)" +
            " and @permissionResolver.allowPaymentCreateWithFromAccountIban(#repaymentAcceptedActionRequest.fromAccountIban, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response operationalLoanPartialRepaymentAcceptedAction(@NotNull @NotEmpty String xKbSessionId,
                                                                  @NotNull @NotEmpty String xKbFePlatform,
                                                                  @NotNull @NotEmpty String xKbFeChannel,
                                                                  @NotNull @NotEmpty String xKbBusChannel,
                                                                  @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                                  @NotNull @NotEmpty String acceptLanguage,
                                                                  @NotNull @Pattern(regexp = "^\\d{8,12}$") String userId,
                                                                  @Valid @NotNull RepaymentAcceptedActionRequest repaymentAcceptedActionRequest) {
        ResponseMessage<GetOperationalLoanPartialRepaymentAcceptedActionCmd.CmdResult> response = commandBus.sendAndGetReply(
                new GetOperationalLoanPartialRepaymentAcceptedActionCmd(
                        repaymentAcceptedActionRequest.getProductInstanceId(),
                        repaymentAcceptedActionRequest.getFromAccountIban(),
                        getMonetaryAmount(repaymentAcceptedActionRequest),
                        xKbSessionId, userId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManageIban(#partialRepaymentChangedActionRequest.iban, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response operationalLoanPartialRepaymentAmountChangedAction(@NotNull @NotEmpty String xKbSessionId,
                                                                       @NotNull @NotEmpty String xKbFePlatform,
                                                                       @NotNull @NotEmpty String xKbFeChannel,
                                                                       @NotNull @NotEmpty String xKbBusChannel,
                                                                       @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                                       @NotNull @Pattern(regexp = "^\\d{8,12}$") String userId,
                                                                       @Valid @NotNull PartialRepaymentChangedActionRequest partialRepaymentChangedActionRequest) {
        ResponseMessage<GetOperationalLoanPartialRepaymentAmountChangedActionCmd.CmdResult> response = commandBus.sendAndGetReply(new GetOperationalLoanPartialRepaymentAmountChangedActionCmd(partialRepaymentChangedActionRequest.getIban(), partialRepaymentChangedActionRequest.getAmount(), xKbSessionId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountDetailsRead(#statementRequestedActionRequest.accountId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response operationalLoanStatementRequestedAction(@NotNull @NotEmpty String xKbSessionId,
                                                            @NotNull @NotEmpty String xKbFePlatform,
                                                            @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                            @NotNull @NotEmpty String xKbFeChannel,
                                                            @NotNull @NotEmpty String xKbBusChannel,
                                                            @NotNull @Pattern(regexp = "^\\d{8,12}$") String userId,
                                                            @Valid @NotNull StatementRequestedActionRequest statementRequestedActionRequest) {
        ResponseMessage<OperationalLoanStatementRequestedActionCmd.CmdResult> response = commandBus.sendAndGetReply(new OperationalLoanStatementRequestedActionCmd(statementRequestedActionRequest, xKbSessionId, userId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManage(#productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response getOperationalLoanFullRepaymentData(@NotNull @NotEmpty String xKbSessionId,
                                                        @NotNull @NotEmpty String xKbFePlatform,
                                                        @NotNull @NotEmpty String xKbFeChannel,
                                                        @NotNull @NotEmpty String xKbBusChannel,
                                                        @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                        @NotNull @NotEmpty String acceptLanguage,
                                                        String userId,
                                                        @NotNull String productInstanceId) {
        ResponseMessage<GetOperationLoanFullRepaymentDataCmd.CmdResult> response = commandBus.sendAndGetReply(new GetOperationLoanFullRepaymentDataCmd(userId, productInstanceId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManage(#productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response getOperationalLoanPartialRepaymentData(@NotNull @NotEmpty String xKbSessionId,
                                                           @NotNull @NotEmpty String xKbFePlatform,
                                                           @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                           @NotNull @NotEmpty String xKbFeChannel,
                                                           @NotNull @NotEmpty String xKbBusChannel,
                                                           @NotNull @NotEmpty String acceptLanguage,
                                                           String userId,
                                                           @NotNull String productInstanceId) {
        ResponseMessage<GetOperationLoanPartialRepaymentDataCmd.CmdResult> response = commandBus.sendAndGetReply(new GetOperationLoanPartialRepaymentDataCmd(userId, productInstanceId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowAccountBusinessLoanManage(#productInstanceId, #xKbBusChannel, #userId)")
    @EnabledOnFeatureFlag(featureFlag = FF_OPERATIONAL_LOAN, clientIdSelector = "#userId")
    public Response getOperationalLoanStatementAvailability(@NotNull @NotEmpty String xKbSessionId,
                                                            @NotNull @NotEmpty String xKbFePlatform,
                                                            @NotNull @NotEmpty String xUserIdIdentitySchema,
                                                            @NotNull @NotEmpty String xKbFeChannel,
                                                            @NotNull @NotEmpty String xKbBusChannel,
                                                            String userId,
                                                            @NotNull String productInstanceId) {
        ResponseMessage<GetOperationalLoanStatementAvailabilityCmd.CmdResult> response = commandBus.sendAndGetReply(new GetOperationalLoanStatementAvailabilityCmd(userId, productInstanceId));

        return response.getPayload().response();
    }

    private MonetaryAmount getMonetaryAmount(RepaymentAcceptedActionRequest repaymentAcceptedActionRequest) {
        return Optional.of(repaymentAcceptedActionRequest)
                .map(RepaymentAcceptedActionRequest::getRepaymentAmount)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.MISSING_REPAYMENT_AMOUNT, "No repayment amount found in request."));
    }

}
