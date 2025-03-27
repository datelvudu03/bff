package cz.kb.leon.bff.servicing.infra.ui.overdraft;


import cz.kb.leon.bff.servicing.app.command.overdraft.GetOverdraftDetailScreenDataCmd;
import cz.kb.leon.bff.servicing.infra.ui.CommonServiceImpl;
import cz.kb.leon.bff.servicing.infra.integrations.overdraft.ServicingOverdraftBffClient;
import cz.kb.leon.exception.StructuredItoLog;
import cz.kb.leon.featureflags.FeatureFlagService;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.BusinessFinancingOverdraftService;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.OverdraftTerminationRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@ConstrainedTo(RuntimeType.SERVER)
@RequiredArgsConstructor
@Validated
@Profiler(useKebabCaseForDefaultOperationName = true)
@StructuredItoLog(logSpeedExceptions = true)
public class BusinessFinancingOverdraftServiceImpl extends CommonServiceImpl implements BusinessFinancingOverdraftService {

    private final ServicingOverdraftBffClient servicingOverdraftBCClient;

    private final FeatureFlagService featureFlagService;

    private final CommandBus commandBus;

    @Override
    protected FeatureFlagService getFeatureFlagService() {
        return featureFlagService;
    }

    @Override
    @PreAuthorize("@permissionResolver.allowProductOverdraftChange(#productId, #xKbBusChannel, #userId)")
    public Response getSigningCaseState(
            @NotNull @NotEmpty String xKbSessionId,
            @NotNull @NotEmpty String xKbFePlatform,
            @NotNull @NotEmpty String xKbIdentitySchema,
            @NotNull @NotEmpty String xKbFeChannel,
            @NotNull @NotEmpty String xKbBusChannel,
            String userId,
            @NotNull String productId,
            @NotNull UUID servicingCaseId) {
        return servicingOverdraftBCClient.getSigningCaseState(productId, servicingCaseId);
    }

    @Override
    @PreAuthorize("@permissionResolver.allowProductOverdraftRead(#productId, #xKbBusChannel, #userId)")
    public Response overdraftDetailScreenData(
            @NotNull @NotEmpty String xKbSessionId,
            @NotNull @NotEmpty String xKbFePlatform,
            @NotNull @NotEmpty String xKbIdentitySchema,
            @NotNull @NotEmpty String xKbFeChannel,
            @NotNull @NotEmpty String xKbBusChannel,
            @NotNull @NotEmpty String acceptLanguage,
            String userId,
            @NotNull String productId) {
        ResponseMessage<GetOverdraftDetailScreenDataCmd.CmdResult> response = commandBus.sendAndGetReply(new GetOverdraftDetailScreenDataCmd(productId, userId));

        return response.getPayload().response();
    }

    @Override
    @PreAuthorize("@permissionResolver.allowProductOverdraftChange(#productId, #xKbBusChannel, #userId)")
    public Response overdraftTerminatedScreenData(
            @NotNull @NotEmpty String xKbSessionId,
            @NotNull @NotEmpty String xKbFePlatform,
            @NotNull @NotEmpty String xKbIdentitySchema,
            @NotNull @NotEmpty String xKbFeChannel,
            @NotNull @NotEmpty String xKbBusChannel,
            @NotNull @NotEmpty String acceptLanguage,
            String userId,
            @NotNull String productId,
            @NotNull UUID servicingCaseId) {
        return servicingOverdraftBCClient.overdraftTerminatedScreenDataResponse(servicingCaseId, productId);
    }

    @Override
    @PreAuthorize("@permissionResolver.allowProductOverdraftChange(#overdraftTerminationRequest.productId, #xKbBusChannel, #userId)")
    public Response overdraftTerminationRequestedAction(
            @NotNull @NotEmpty String xKbSessionId,
            @NotNull @NotEmpty String xKbFePlatform,
            @NotNull @NotEmpty String xKbIdentitySchema,
            @NotNull @NotEmpty String xKbBusChannel,
            @NotNull @NotEmpty String xKbFeChannel,
            @NotNull @Pattern(regexp = "^\\d{8,12}$") String userId,
            @Valid @NotNull OverdraftTerminationRequest overdraftTerminationRequest) {
        return servicingOverdraftBCClient.overdraftTerminationRequestedActionResponse(overdraftTerminationRequest.getProductId());
    }

}
