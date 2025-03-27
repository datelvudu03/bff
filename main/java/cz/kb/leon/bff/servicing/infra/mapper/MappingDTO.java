package cz.kb.leon.bff.servicing.infra.mapper;

import cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.ConfirmationScreenResp;
import cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.DetailScreenResp;
import cz.kb.leon.bc.servicing_businessoverdraft_private_v1.dto.TerminationActionResp;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.CreateEarlyRepaymentCaseRequest;
import cz.kb.leon.bff.servicing.domain.enumeration.RepaymentTypeEnum;
import cz.kb.leon.bff.servicing.util.AccountUtil;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.MonetaryAmount;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.AccountNumber;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.OverdraftTerminationActionResponse;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.Result;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MappingDTO {

    OverdraftTerminationActionResponse toOverdraftTerminationActionResponse(TerminationActionResp terminationActionResp);

    @Mapping(source = "servicingCaseId", target = "uuid")
    Result getOverdraftTerminationScreenDataResponseToResult(ConfirmationScreenResp confirmationScreenResp);

    @Mapping(source = "loanRate", target = "loanRate.percentage")
    @Mapping(source = "debtorId", target = "productOwner.id")
    @Mapping(source = "debtorIdScheme", target = "productOwner.idSchema")
    @Mapping(source = "iban", target = "account.iban")
    @Mapping(expression = "java(ibanToAccountNumber(detailScreenResp.getIban()))", target = "account.accountNumber")
    @Mapping(source = "accountAlias", target = "account.accountAlias")
    @Mapping(source = "accountAliasLong", target = "account.accountAliasLong")
    @Mapping(source = "accountId", target = "account.accountId")
    cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.OverdraftDetail toOverdraftDetail(DetailScreenResp detailScreenResp);

    AccountNumber mapToAccountNumber(AccountUtil.AccountNumber accountNumber);
    @Mapping(target = "repaymentAmount", source = "monetaryAmount")
    @Mapping(target = "productInstanceId", source = "productId")
    @Mapping(target = "repaymentType", source = "repaymentTypeEnum")
    CreateEarlyRepaymentCaseRequest mapToCreateEarlyRepaymentCaseRequest(MonetaryAmount monetaryAmount, String productId, RepaymentTypeEnum repaymentTypeEnum);

    default AccountNumber ibanToAccountNumber(String iban) {
        return mapToAccountNumber(AccountUtil.parseIbanToAccountNumber(iban));
    }

}
