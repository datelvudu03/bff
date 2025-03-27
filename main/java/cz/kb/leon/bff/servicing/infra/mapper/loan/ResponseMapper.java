package cz.kb.leon.bff.servicing.infra.mapper.loan;

import cz.kb.leon.bc.servicing.v2.dto.GeneratingState;
import cz.kb.leon.bc.servicing.v2.dto.GeneratingStateResp;
import cz.kb.leon.bc.servicing.v2.dto.RequestStatementCmdResp;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.CreateEarlyRepaymentCaseResponse;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.RepaymentAcceptedActionResponse;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.StatementGenerateStateResponse;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.StatementRequestedActionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.*;

@Mapper
public interface ResponseMapper {

    StatementRequestedActionResponse mapRequestStatementCmdRespToBusinessLoanStatementRequestedActionResponse(RequestStatementCmdResp requestStatementCmdResp);

    @Mapping(target = "statementGenerationState", source = "state")
    StatementGenerateStateResponse mapGetStatementGeneratingStateActionResponse(GeneratingStateResp responseEntity);

    @Mapping(target = "accountIban", ignore = true)
    @Mapping(target = "counterpartyIdentification", ignore = true)
    @Mapping(target = "instructedAmount", ignore = true)
    @Mapping(target = "requestedPaymentType", constant = DOMESTIC_PAYMENT)
    @Mapping(target = "repaymentProductType", constant = PARTIAL_REPAYMENT)
    @Mapping(target = "servicingCaseId", source = "servicingCaseId")
    @Mapping(target = "ownDescription", constant = EARLY_REPAYMENT_OWN_AND_COUNTERPARTY_DESC)
    @Mapping(target = "messageForCounterparty", constant = EARLY_REPAYMENT_OWN_AND_COUNTERPARTY_DESC)
    RepaymentAcceptedActionResponse mapCreateEarlyRepaymentCaseResponseToPartialRepaymentAcceptedActionResponse(CreateEarlyRepaymentCaseResponse responseEntity);

    default StatementGenerateStateResponse.StatementGenerationStateEnum map(GeneratingState source) {
        return switch (source) {
            case GENERATING, DRAFT -> StatementGenerateStateResponse.StatementGenerationStateEnum.IN_PROGRESS;
            case SUCCESS -> StatementGenerateStateResponse.StatementGenerationStateEnum.COMPLETED;
            case FAILED, DELETED -> StatementGenerateStateResponse.StatementGenerationStateEnum.FAILED;
        };
    }
}
