package cz.kb.leon.bff.servicing.app.handler.overdraft;

import cz.kb.cbs.position_keeping.gen.jaxrs.model.InstructionResponseBALANCE;
import cz.kb.leon.bc.productdefinition_private_v1.dto.ProductDefinitionDetail;
import cz.kb.leon.bc.productlifecycle_private_api_v1.dto.*;
import cz.kb.leon.bc.servicing.v2.dto.CaseState;
import cz.kb.leon.bc.servicing.v2.dto.CaseType;
import cz.kb.leon.bc.servicing.v2.dto.QryGetServicingCasesV2200Response;
import cz.kb.leon.bc.servicing.v2.dto.ServicingCase;
import cz.kb.leon.bff.servicing.app.command.overdraft.GetOverdraftDetailScreenDataCmd;
import cz.kb.leon.bff.servicing.app.helper.OverdraftDetailHelper;
import cz.kb.leon.bff.servicing.app.mapper.overdraft.OverdraftCommandMapper;
import cz.kb.leon.bff.servicing.domain.exception.DomainException;
import cz.kb.leon.bff.servicing.domain.exception.DomainExceptionCode;
import cz.kb.leon.bff.servicing.infra.integrations.operationalloan.ServicingOperationLoanBffClient;
import cz.kb.leon.bff.servicing.infra.integrations.overdraft.ServicingOverdraftBffClient;
import cz.kb.leon.bff.servicing.infra.integrations.plc.PLCClientService;
import cz.kb.leon.bff.servicing.infra.integrations.positionkeeping.PositionKeepingService;
import cz.kb.leon.bff.servicing.infra.integrations.product_definitions.ProductDefinitionsClientService;
import cz.kb.leon.bff.servicing.util.AccountUtil;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.MonetaryAmount;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.OverdraftDetail;
import cz.kb.speed.cqrs.api.command.CommandHandler;
import cz.kb.speed.messaging.api.handler.Processing;
import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("bff-leon-servicing-service-user")
public class OverdraftCmdHandler {

    private final PLCClientService productLifeCycleService;
    private final PositionKeepingService positionKeepingService;
    private final ProductDefinitionsClientService productDefinitionsService;
    private final ServicingOverdraftBffClient servicingOverdraftBffClient;
    private final ServicingOperationLoanBffClient servicingOperationLoanBCClient;

    private final OverdraftCommandMapper overdraftCommandMapper = Mappers.getMapper(OverdraftCommandMapper.class);

    @CommandHandler(processing = Processing.SYNC)
    public GetOverdraftDetailScreenDataCmd.CmdResult handleCommand(GetOverdraftDetailScreenDataCmd cmd) {
        GetOverdraftProductData200Response overdraftProductData200Response = productLifeCycleService.getOverdraftProductData(cmd.productId());

        Optional<OverdraftProduct> overdraftProductOptional = Optional.of(overdraftProductData200Response).map(GetOverdraftProductData200Response::getOverdraftProduct);

        String iban = overdraftProductOptional
                .map(OverdraftProduct::getCurrentAccount)
                .map(CurrentAccount::getIban)
                .map(IBAN::getValue)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_IBAN, "Product LifeCycle returned invalid IBAN."));
        String accountNumber = AccountUtil.parsingAccountNumber(iban);
        InstructionResponseBALANCE instructionResponseBalance = positionKeepingService.getBalance(accountNumber);

        OverdraftDetailHelper overdraftDetailDto = overdraftCommandMapper.mapToOverdraftDetailDto(instructionResponseBalance, overdraftProductData200Response);

        MonetaryAmount remainingAmount = overdraftDetailDto.evaluateRemainingAmount();
        MonetaryAmount withdrawnAmount = overdraftDetailDto.evaluateWithdrawnAmount();

        UUID productDefinitionId = overdraftProductOptional
                .map(OverdraftProduct::getProduct)
                .map(Product::getProductDefinitionId)
                .map(ProductDefinitionId::getId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format."));
        ProductDefinitionDetail productDefinitionDetail = productDefinitionsService.getDefinitionData(productDefinitionId);

        String accountAlias = Optional.ofNullable(productDefinitionDetail)
                .map(ProductDefinitionDetail::getBasicParams)
                .map(pd -> StringUtils.isNotBlank(pd.getProductNameCz()) ? pd.getProductNameCz() : pd.getProductNameEn())
                .orElse("");

        String productInstanceId = overdraftProductOptional
                .map(OverdraftProduct::getProduct)
                .map(Product::getProductInstanceId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid Product Id."));

        QryGetServicingCasesV2200Response getServicingResponse = servicingOperationLoanBCClient.servicingCaseQuery(productInstanceId, cmd.userId());
        boolean activeTerminationCaseFlag = getServicingResponse.getCases().stream()
                .anyMatch(servicingCase -> servicingCase.getType() == CaseType.CLIENT_OVERDRAFT_TERMINATION &&
                        (servicingCase.getState() == CaseState.TERMINATING || servicingCase.getState() == CaseState.SIGNED));

        LocalDate terminationDate = getServicingResponse.getCases().stream()
                .filter(servicingCase -> servicingCase.getType() == CaseType.BANK_OVERDRAFT_TERMINATION
                        && servicingCase.getState() == CaseState.WAITING_FOR_REALIZATION)
                .findFirst()
                .map(ServicingCase::getTerminationDate)
                .orElse(null);
        log.info("servicingCaseQuery called: terminationDate for productInstanceId {} is {}", productInstanceId, terminationDate);

        OverdraftDetail responseEntity = overdraftCommandMapper.mapToDetailScreenResp(
                overdraftProductOptional.orElse(null),
                remainingAmount,
                withdrawnAmount,
                activeTerminationCaseFlag,
                accountAlias,
                terminationDate);

        var response = Response.ok()
                .entity(responseEntity)
                .build();

        return new GetOverdraftDetailScreenDataCmd.CmdResult(response);
    }

}
