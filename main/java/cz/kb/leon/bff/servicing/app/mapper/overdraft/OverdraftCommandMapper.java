package cz.kb.leon.bff.servicing.app.mapper.overdraft;

import cz.kb.cbs.position_keeping.gen.jaxrs.model.InstructionResponseBALANCE;
import cz.kb.leon.bc.productlifecycle_private_api_v1.dto.*;
import cz.kb.leon.bff.servicing.app.helper.OverdraftDetailHelper;
import cz.kb.leon.bff.servicing.util.AccountUtil;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.AccountNumber;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.AccountInformation;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.MonetaryAmount;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.OverdraftDetail;
import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.SubjectIdentity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Mapper
public interface OverdraftCommandMapper {

    @Mapping(target = "productId", source = "overdraftProduct.product.productInstanceId")
    @Mapping(target = "account.iban", source = "overdraftProduct.currentAccount.iban.value")
    @Mapping(target = "account.accountId", source = "overdraftProduct.currentAccount.id")
    @Mapping(target = "account.accountAlias", source = "accountAlias")
    @Mapping(target = "account.accountAliasLong", source = "accountAlias")
    @Mapping(target = "productOwner", source = "overdraftProduct.debtor")
    @Mapping(target = "limitAmount", source = "overdraftProduct.loanLimit")
    @Mapping(target = "loanRate.percentage", constant = "17.99")
    @Mapping(target = "withdrawnFlag", expression = "java(evaluateWithdravnFlag(withdrawnAmount))")
    OverdraftDetail mapToOverdraftDetail(OverdraftProduct overdraftProduct,
                                         InstructionResponseBALANCE instructionResponseBalance,
                                         MonetaryAmount remainingAmount,
                                         MonetaryAmount withdrawnAmount,
                                         boolean activeTerminationCaseFlag,
                                         String accountAlias);

    @Mapping(target = "idSchema", source = "idType")
    SubjectIdentity mapDebtorToSubjectIdentity(Debtor debtor);

    @Mapping(target = "currency", source = "currency.code")
    MonetaryAmount mapLoanLimitToMonetaryAmount(LoanLimit loanLimit);

    @Mapping(target = "accountId", source = "id")
    @Mapping(target = "iban", source = "iban.value")
    AccountInformation mapCurrentAccountToAccountInformation(CurrentAccount currentAccount);

    @Mapping(target = "availableBalance", source = "instructionResponseBALANCE.applCK.availableBalance")
    @Mapping(target = "loanAmount", source = "overdraftProduct.loanLimit.amount")
    @Mapping(target = "currency", source = "overdraftProduct.loanLimit.currency.code")
    @Mapping(target = "productState", source = "overdraftProduct.product.productState.state")
    OverdraftDetailHelper mapToOverdraftDetailDto(InstructionResponseBALANCE instructionResponseBALANCE, OverdraftProduct overdraftProduct);

    default boolean evaluateWithdravnFlag(MonetaryAmount withdrawnAmount) {
        return !Optional.ofNullable(withdrawnAmount)
                .map(MonetaryAmount::getAmount)
                .map(String::trim)
                .map("0"::equals)
                .orElse(false);
    }

    @Mapping(target = "availableBalance", source = "instructionResponseBALANCE.applCK.availableBalance")
    @Mapping(target = "loanAmount", source = "overdraftProductData200Response.overdraftProduct.loanLimit.amount")
    @Mapping(target = "currency", source = "overdraftProductData200Response.overdraftProduct.loanLimit.currency.code")
    @Mapping(target = "productState", source = "overdraftProductData200Response.overdraftProduct.product.productState.state")
    OverdraftDetailHelper mapToOverdraftDetailDto(InstructionResponseBALANCE instructionResponseBALANCE, GetOverdraftProductData200Response overdraftProductData200Response);

    @Mapping(target = "productId", source = "overdraftProduct.product.productInstanceId")
    @Mapping(target = "account.iban", ignore = true)
    @Mapping(target = "account.accountNumber", ignore = true)
    @Mapping(target = "account.accountId", ignore = true)
    @Mapping(target = "account.accountAlias", ignore = true)
    @Mapping(target = "account.accountAliasLong", ignore = true)
    @Mapping(target = "productOwner.id", source = "overdraftProduct.debtor.id")
    @Mapping(target = "productOwner.idSchema", source = "overdraftProduct.debtor.idType")
    @Mapping(target = "limitAmount.amount", source = "overdraftProduct.loanLimit.amount")
    @Mapping(target = "limitAmount.currency", source = "overdraftProduct.loanLimit.currency.code")
    @Mapping(target = "loanRate.percentage", constant = "17.99")
    @Mapping(target = "withdrawnFlag", expression = "java(evaluateWithdrawnFlag(withdrawnAmount))")
    OverdraftDetail mapToDetailScreenResp(OverdraftProduct overdraftProduct,
                                          MonetaryAmount remainingAmount,
                                          MonetaryAmount withdrawnAmount,
                                          boolean activeTerminationCaseFlag,
                                          String accountAlias,
                                          LocalDate terminationDate);

    @AfterMapping
    default void mapToAccount(@MappingTarget OverdraftDetail overdraftDetail, OverdraftProduct overdraftProduct, String accountAlias) {
        Optional<CurrentAccount> currentAccoutntOptional = Optional.ofNullable(overdraftProduct).map(OverdraftProduct::getCurrentAccount);

        AccountInformation account = new AccountInformation();
        account.setAccountAlias(accountAlias);
        account.setAccountAliasLong(accountAlias);
        account.setAccountId(currentAccoutntOptional.map(CurrentAccount::getId).orElse(null));
        account.setIban(currentAccoutntOptional.map(CurrentAccount::getIban).map(IBAN::getValue).orElse(""));
        account.setAccountNumber(evaluateAccountNumber(currentAccoutntOptional));
        overdraftDetail.setAccount(account);
    }

    default AccountNumber evaluateAccountNumber(Optional<CurrentAccount> currentAccount) {
        var iban = currentAccount
                .map(CurrentAccount::getIban)
                .map(IBAN::getValue)
                .orElse("");
        return ibanToAccountNumber(iban);
    }

    default AccountNumber ibanToAccountNumber(String iban) {
        return mapToAccountNumber(AccountUtil.parseIbanToAccountNumber(iban));
    }

    AccountNumber mapToAccountNumber(AccountUtil.AccountNumber accountNumber);

    default boolean evaluateWithdrawnFlag(MonetaryAmount withdrawnAmount) {
        return !Optional.ofNullable(withdrawnAmount)
                .map(MonetaryAmount::getAmount)
                .map(String::trim)
                .map("0"::equals)
                .orElse(false);
    }

}
