package cz.kb.leon.bff.servicing.app.mapper.loan;

import cz.kb.api.accountservicing.v1.dto.GetCurrentAccountDetailRes;
import cz.kb.api.loansapi.v1.dto.*;
import cz.kb.cbs.position_keeping.gen.jaxrs.model.InstructionResponseBALANCE;
import cz.kb.leon.bc.pricing.v1.dto.FeeV1;
import cz.kb.leon.bc.pricing.v1.dto.FeesDetail;
import cz.kb.leon.bc.pricing.v1.dto.InterestRate;
import cz.kb.leon.bc.pricing.v1.dto.InterestRateDetailV1;
import cz.kb.leon.bc.productdefinition_private_v1.dto.ProductDefinitionBasic;
import cz.kb.leon.bc.productdefinition_private_v1.dto.ProductDefinitionDetail;
import cz.kb.leon.bc.productlifecycle_private_api_v1.dto.*;
import cz.kb.leon.bc.servicing.v2.dto.RequestStatementCmdReq;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.EarlyRepaymentAuthorizedRequest;
import cz.kb.leon.bff.servicing.app.handler.operationalloan.OperationalLoanCmdHandler;
import cz.kb.leon.bff.servicing.domain.exception.DomainException;
import cz.kb.leon.bff.servicing.domain.exception.DomainExceptionCode;
import cz.kb.leon.bff.servicing.util.AccountUtil;
import cz.kb.leon.bff.servicing.util.EndUserUtil;
import cz.kb.leon.bff.servicing.util.ObjectUtil;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.*;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.*;

@Mapper
public interface LoanCommandMapper {

    default String getAccountAlias(EndUserUtil.LanguageEnum language, OperationalLoanCmdHandler.LoanInfo loanInfo) {
        String accountAlias = Optional.ofNullable(loanInfo.operationalLoanProduct())
                .map(OperationalLoanProduct::getProduct)
                .map(Product::getProductAlias)
                .map(ProductAlias::getName)
                .orElse("");

        if (!accountAlias.isEmpty()) {
            return accountAlias;
        }

        Optional<ProductDefinitionBasic> basicParams = Optional.of(loanInfo)
                .map(OperationalLoanCmdHandler.LoanInfo::productDefinitionDetail)
                .map(ProductDefinitionDetail::getBasicParams);

        return switch (language) {
            case CZ -> basicParams.map(ProductDefinitionBasic::getProductNameCz).orElse("");
            case EN -> basicParams.map(ProductDefinitionBasic::getProductNameEn).orElse("");
        };
    }

    AccountNumber getAccountNumber(AccountUtil.AccountNumber parsedFromIban);

    default MonetaryAmount createMonetaryAmount(PaymentScheduleResponseBodyInner paymentScheduleResponseBodyInner) {
        BigDecimal totalAmount = Optional.of(paymentScheduleResponseBodyInner)
                .map(PaymentScheduleResponseBodyInner::getTotalAmount)
                .orElse(BigDecimal.ZERO);

        BigDecimal chargeAmount = Optional.of(paymentScheduleResponseBodyInner)
                .map(PaymentScheduleResponseBodyInner::getChargeAmount)
                .orElse(BigDecimal.ZERO);

        return new MonetaryAmount().amount(totalAmount.subtract(chargeAmount).abs().toString()).currency(CURRENCY_CZK);
    }

    default AccountInformation mapAccountInformationFromPlc(OperationalLoanCmdHandler.LoanInfo loanInfo,
                                                            EndUserUtil.LanguageEnum language) {
        String accountAlias = getAccountAlias(language, loanInfo);

        String iban = Optional.of(loanInfo)
                .map(OperationalLoanCmdHandler.LoanInfo::operationalLoanProduct)
                .map(OperationalLoanProduct::getLoanAccount)
                .map(LoanAccount::getIban)
                .map(IBAN::getValue)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "product LifeCycle service returned response with missing account information."));

        AccountNumber accountNumber = getAccountNumber(AccountUtil.parseIbanToAccountNumber(iban));

        String accountId = Optional.of(loanInfo)
                .map(OperationalLoanCmdHandler.LoanInfo::operationalLoanProduct)
                .map(OperationalLoanProduct::getLoanAccount)
                .map(LoanAccount::getId)
                .orElse(null);

        return new AccountInformation()
                .accountAlias(accountAlias)
                .accountAliasLong(accountAlias)
                .iban(iban)
                .accountId(accountId)
                .accountNumber(accountNumber);
    }

    @Mapping(target = "repaymentAmount.amount", expression = "java(java.util.Optional.of(customerLoanResponseBodyInner).map(CustomerLoanResponseBodyInner::getLoanBalance).map(java.math.BigDecimal::abs).map(java.math.BigDecimal::toString).orElse(null))")
    @Mapping(target = "repaymentAmount.currency", expression = "java(java.util.Optional.of(customerLoanResponseBodyInner).map(CustomerLoanResponseBodyInner::getLoanCurrency).orElse(null))")
    @Mapping(target = "currentMaturityDate", source = "customerLoanResponseBodyInner.loanEndDate")
    @Mapping(target = "currentNumberOfInstalments", ignore = true)
    @Mapping(target = "repaymentAccount.accountAlias", source = "currentAccountDetailResponse.currentAccount.name")
    @Mapping(target = "repaymentAccountBalance.amount", source = "balanceResponse.applCK.availableBalance")
    @Mapping(target = "minPossibleRepaymentAmount.amount", constant = "1")
    @Mapping(target = "minPossibleRepaymentAmount.currency", expression = "java(java.util.Optional.of(customerLoanResponseBodyInner).map(CustomerLoanResponseBodyInner::getLoanCurrency).orElse(null))")
    PartialRepaymentDataResponse mapToPartialRepaymentDataResponse(CustomerLoanResponseBodyInner customerLoanResponseBodyInner,
                                                                   InstructionResponseBALANCE balanceResponse,
                                                                   LoanDetailResponseBodyInnerSchedulesInner loanDetailResponseBodyInnerSchedulesInner,
                                                                   GetCurrentAccountDetailRes currentAccountDetailResponse,
                                                                   PaymentScheduleResponse paymentScheduleResponse);

    @AfterMapping
    default void parseMaxPossibleRepaymentAmount(@MappingTarget PartialRepaymentDataResponse response,
                                                 CustomerLoanResponseBodyInner customerLoanResponseBodyInner,
                                                 PaymentScheduleResponse paymentScheduleResponse) {

        Optional.ofNullable(paymentScheduleResponse)
                .map(PaymentScheduleResponse::getBody)
                .ifPresent(paymentScheduleResponseBodyInners -> response.setCurrentNumberOfInstalments((int) paymentScheduleResponseBodyInners.stream()
                        .filter(paymentScheduleResponseBodyInner -> List.of(SCHEDULE_TYPE_DUE, SCHEDULE_TYPE_FUTURE).contains((paymentScheduleResponseBodyInner.getScheduleType())) &&
                                (paymentScheduleResponseBodyInner.getPrincipalAmount() != null || paymentScheduleResponseBodyInner.getInterestAmount() != null)
                        ).count()));

        var amount = Optional.ofNullable(customerLoanResponseBodyInner)
                .map(CustomerLoanResponseBodyInner::getLoanBalance)
                .map(bigDecimal -> bigDecimal.abs().subtract(BigDecimal.ONE))
                .map(BigDecimal::toString)
                .orElse(null);


        var currency = Optional.ofNullable(customerLoanResponseBodyInner)
                .map(CustomerLoanResponseBodyInner::getLoanCurrency)
                .orElse(null);
        response.setMaxPossibleRepaymentAmount(new MonetaryAmount().amount(amount).currency(currency));

        Optional.of(response).map(PartialRepaymentDataResponse::getRepaymentAccountBalance).ifPresent(r -> r.setCurrency(currency));
    }

    @Mapping(target = "productInstanceId", source = "accountId")
    RequestStatementCmdReq mapBusinessLoanStatementRequestedActionRequestToRequestStatementCmdReq(StatementRequestedActionRequest businessLoanStatementRequestedActionRequest);

    @Mapping(target = "productInstanceId", source = "loanInfo.operationalLoanProduct.product.productInstanceId")
    @Mapping(target = "account.accountId", ignore = true)
    @Mapping(target = "account.iban", ignore = true)
    @Mapping(target = "account.accountAlias", ignore = true)        // See after mapping
    @Mapping(target = "account.accountAliasLong", ignore = true)    // See after mapping
    @Mapping(target = "currentPrincipalAmount.amount", expression = "java(java.util.Optional.of(customerLoanResponseBodyInner).map(CustomerLoanResponseBodyInner::getLoanBalance).map(java.math.BigDecimal::abs).map(java.math.BigDecimal::toString).orElse(\"0\"))")
    @Mapping(target = "currentPrincipalAmount.currency", source = "loanInfo.customerLoanResponseBodyInner.loanCurrency")
    @Mapping(target = "originalPrincipalAmount.amount", expression = "java(java.util.Optional.of(operationalLoanProduct).map(OperationalLoanProduct::getLoanLimit).map(LoanLimit::getAmount).map(java.math.BigDecimal::abs).map(java.math.BigDecimal::toString).orElse(\"0\"))")
    @Mapping(target = "originalPrincipalAmount.currency", source = "loanInfo.operationalLoanProduct.loanLimit.currency.code")
    @Mapping(target = "totalAmountPercentage", ignore = true)
    @Mapping(target = "messageList", ignore = true)
    OperationalLoanDetailData mapToOperationalLoanDetailData(OperationalLoanCmdHandler.LoanInfo loanInfo, EndUserUtil.LanguageEnum language);

    @AfterMapping
    default void afterMappingOperationalLoanDetailData(@MappingTarget OperationalLoanDetailData operationalLoanDetailData,
                                                       OperationalLoanCmdHandler.LoanInfo loanInfo,
                                                       EndUserUtil.LanguageEnum language) {
        //account
        operationalLoanDetailData.setAccount(mapAccountInformationFromPlc(loanInfo, language));

        BigDecimal currentPrincipalAmount = Optional.of(operationalLoanDetailData)
                .map(OperationalLoanDetailData::getCurrentPrincipalAmount)
                .map(MonetaryAmount::getAmount)
                .map(BigDecimal::new)
                .map(BigDecimal::abs)
                .orElse(null);

        BigDecimal originalPrincipalAmount = Optional.of(operationalLoanDetailData)
                .map(OperationalLoanDetailData::getOriginalPrincipalAmount)
                .map(MonetaryAmount::getAmount)
                .map(BigDecimal::new)
                .map(BigDecimal::abs)
                .orElse(null);

        if (currentPrincipalAmount != null && originalPrincipalAmount != null) {
            BigDecimal totalAmountPercentage = HUNDRED.multiply(currentPrincipalAmount).divide(originalPrincipalAmount, RoundingMode.CEILING);
            operationalLoanDetailData.setTotalAmountPercentage(new Percentage().percentage(totalAmountPercentage.toString()));
        } else {
            operationalLoanDetailData.setTotalAmountPercentage(new Percentage().percentage("0"));
        }

        if (currentPrincipalAmount == null) {
            operationalLoanDetailData.setCurrentPrincipalAmount(new MonetaryAmount().amount("0").currency(CURRENCY_CZK));
        }

        if (originalPrincipalAmount == null) {
            operationalLoanDetailData.setOriginalPrincipalAmount(new MonetaryAmount().amount("0").currency(CURRENCY_CZK));
        }

    }

    @Mapping(target = "productOwner.id", source = "loanInfo.operationalLoanProduct.debtor.id")
    @Mapping(target = "productOwner.idSchema", source = "loanInfo.operationalLoanProduct.debtor.idType")
    @Mapping(target = "productInstanceId", source = "loanInfo.operationalLoanProduct.product.productInstanceId")
    @Mapping(target = "productState", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "currentPrincipalAmount.amount", ignore = true)
    @Mapping(target = "currentPrincipalAmount.currency", ignore = true)
    @Mapping(target = "originalPrincipalAmount.amount", ignore = true)
    @Mapping(target = "originalPrincipalAmount.currency", ignore = true)
    @Mapping(target = "instalment.instalmentAmount", ignore = true)
    @Mapping(target = "instalment.nextInstalmentDate", source = "loanInfo.paymentScheduleResponseBodyInner.paymentDate")
    @Mapping(target = "instalment.dayInMonth", source = "loanInfo.operationalLoanProduct.paymentDueDay.dayOfMonth")
    @Mapping(target = "instalment.firstInstalmentFlag", constant = "false")
    @Mapping(target = "signatureDate", ignore = true)
    @Mapping(target = "activationDate", source = "loanInfo.operationalLoanProduct.activationDate.date")
    @Mapping(target = "maturityDate", source = "loanInfo.loanDetailResponseBodyInner.maturityDate")
    @Mapping(target = "interestRate.percentage", source = "interestRateDetailV1.deviation.deviationTotal")
    @Mapping(target = "referenceInterestRate.name", source = "interestRateDetailV1.referenceInterestRate.name")
    @Mapping(target = "referenceInterestRate.value", ignore = true)
    @Mapping(target = "floatingInterestRate.percentage", source = "interestRateDetailV1.interestRateTotal")
    @Mapping(target = "loanMaintenanceFee", ignore = true)
    @Mapping(target = "messageList", ignore = true)
    OperationalLoanInfoData mapToOperationalLoanInfoData(OperationalLoanCmdHandler.LoanInfo loanInfo,
                                                         InterestRateDetailV1 interestRateDetailV1,
                                                         FeesDetail feesDetail,
                                                         EndUserUtil.LanguageEnum language);


    @AfterMapping
    default void afterMappingOperationalLoanInfoData(@MappingTarget OperationalLoanInfoData mappingTarget,
                                                     FeesDetail feesDetail,
                                                     OperationalLoanCmdHandler.LoanInfo loanInfo,
                                                     InterestRateDetailV1 interestRateDetailV1,
                                                     EndUserUtil.LanguageEnum language) {
        //account
        mappingTarget.setAccount(mapAccountInformationFromPlc(loanInfo, language));

        PaymentScheduleResponseBodyInner paymentScheduleResponseBodyInner = loanInfo.paymentScheduleResponseBodyInner();
        if (paymentScheduleResponseBodyInner != null) {
            BigDecimal totalAmount = (paymentScheduleResponseBodyInner.getTotalAmount() != null) ? paymentScheduleResponseBodyInner.getTotalAmount() : null;
            BigDecimal interestAmount = (paymentScheduleResponseBodyInner.getInterestAmount() != null) ? paymentScheduleResponseBodyInner.getInterestAmount() : null;
            BigDecimal chargeAmount = (paymentScheduleResponseBodyInner.getChargeAmount() != null) ? paymentScheduleResponseBodyInner.getChargeAmount() : BigDecimal.ZERO;

            if (totalAmount != null && interestAmount != null) {
                mappingTarget.getInstalment().setFirstInstalmentFlag(totalAmount.equals(interestAmount));
            }
            totalAmount = (totalAmount == null) ? BigDecimal.ZERO : totalAmount;
            mappingTarget.getInstalment().setInstalmentAmount(new MonetaryAmount().amount(totalAmount.subtract(chargeAmount).abs().toString()).currency(CURRENCY_CZK) );
        }

        //referenceInterestRate.value.percentage
        Optional.of(interestRateDetailV1)
                .map(InterestRateDetailV1::getReferenceInterestRate)
                .map(InterestRate::getValue)
                .map(BigDecimal::abs)
                .map(BigDecimal::toString)
                .ifPresent(percentage -> mappingTarget.getReferenceInterestRate().setValue(new Percentage().percentage(percentage)));

        //currentPrincipalAmount.amount and currentPrincipalAmount.currency
        mappingTarget.setCurrentPrincipalAmount(new MonetaryAmount().amount("0").currency(CURRENCY_CZK));
        Optional.of(loanInfo)
                .map(OperationalLoanCmdHandler.LoanInfo::customerLoanResponseBodyInner)
                .ifPresent(customerLoanResponseBodyInner -> {
                    String amount = Optional.of(customerLoanResponseBodyInner)
                            .map(CustomerLoanResponseBodyInner::getLoanBalance)
                            .map(BigDecimal::abs)
                            .map(BigDecimal::toString)
                            .orElse("0");
                    String currency = Optional.of(customerLoanResponseBodyInner)
                            .map(CustomerLoanResponseBodyInner::getLoanCurrency)
                            .orElse(CURRENCY_CZK);
                    mappingTarget.setCurrentPrincipalAmount(new MonetaryAmount().amount(amount).currency(currency));
                });

        //originalPrincipalAmount.amount and originalPrincipalAmount.currency
        mappingTarget.setOriginalPrincipalAmount(new MonetaryAmount().amount("0").currency(CURRENCY_CZK));
        Optional.of(loanInfo)
                .map(OperationalLoanCmdHandler.LoanInfo::operationalLoanProduct)
                .map(OperationalLoanProduct::getLoanLimit)
                .ifPresent(loanLimit -> {
                    String amount = Optional.of(loanLimit)
                            .map(LoanLimit::getAmount)
                            .map(BigDecimal::abs)
                            .map(BigDecimal::toString)
                            .orElse("0");
                    String currency = Optional.of(loanLimit)
                            .map(LoanLimit::getCurrency)
                            .map(Currency::getCode)
                            .orElse(CURRENCY_CZK);
                    mappingTarget.setOriginalPrincipalAmount(new MonetaryAmount().amount(amount).currency(currency));
                });

        //loanMaintenanceFee
        Optional.of(feesDetail)
                .map(FeesDetail::getFees)
                .orElse(List.of())
                .stream()
                .filter(fee -> fee.getFeeCode().equals(PRICING_FEE_CODE))
                .findFirst()
                .map(FeeV1::getFeeAmount)
                .map(cz.kb.leon.bc.pricing.v1.dto.MonetaryAmount::getAmount)
                .map(BigDecimal::toString)
                .ifPresent(fee -> mappingTarget.setLoanMaintenanceFee(new MonetaryAmount().amount(fee).currency(CURRENCY_CZK)));

        //productState
        Optional.of(loanInfo)
                .map(OperationalLoanCmdHandler.LoanInfo::operationalLoanProduct)
                .map(OperationalLoanProduct::getProduct)
                .map(Product::getProductState)
                .map(VersionState::getState)
                .map(String::toUpperCase)
                .ifPresent(ps -> {
                    switch (ps) {
                        case PRODUCT_STATE_PLC_ACTIVATED ->
                                mappingTarget.setProductState(OperationalLoanInfoData.ProductStateEnum.ACTIVE);
                        case PRODUCT_STATE_PLC_TERMINATED, PRODUCT_STATE_PLC_TERMINATING ->
                                mappingTarget.setProductState(OperationalLoanInfoData.ProductStateEnum.CLOSED);
                        default ->
                                throw new IllegalStateException(ObjectUtil.evaluateMessage("Unexpected value of product state from PLC: {}.", ps));
                    }
                });

        //signatureDate
        Optional.of(loanInfo)
                .map(OperationalLoanCmdHandler.LoanInfo::operationalLoanProduct)
                .map(OperationalLoanProduct::getContractDocuments)
                .map(List::getFirst)
                .map(ContractDocument::getSignatureDate)
                .ifPresent(signatureDate -> mappingTarget.setSignatureDate(signatureDate.toLocalDate()));
    }

    @Mapping(target = "principalAmount.amount", source = "todayBillDetailsResponseBodyBody.acOsAmt")
    @Mapping(target = "principalAmount.currency", source = "todayBillDetailsResponseBodyBody.currency")
    @Mapping(target = "dailyInterestAmount.amount", source = "todayBillDetailsResponseBodyBody.principalInt")
    @Mapping(target = "dailyInterestAmount.currency", source = "todayBillDetailsResponseBodyBody.currency")
    @Mapping(target = "totalAmount.amount", source = "todayBillDetailsResponseBodyBody.oSTotalAmt")
    @Mapping(target = "totalAmount.currency", source = "todayBillDetailsResponseBodyBody.currency")
    @Mapping(target = "monthlyLoanManagementFee.currency", source = "todayBillDetailsResponseBodyBody.currency")
    @Mapping(target = "repaymentAccount.accountId", source = "ncaRepaymentAccount.currentAccount.currentAccountInstanceId.id")
    @Mapping(target = "repaymentAccount.accountAlias", source = "ncaRepaymentAccount.currentAccount.name")
    @Mapping(target = "repaymentAccount.accountAliasLong", source = "ncaRepaymentAccount.currentAccount.name")
    @Mapping(target = "repaymentAccount.accountNumber.bankCode", source = "ncaRepaymentAccount.currentAccount.account.accountNumber.bankCode")
    @Mapping(target = "repaymentAccount.accountNumber.core", source = "ncaRepaymentAccount.currentAccount.account.accountNumber.core")
    @Mapping(target = "repaymentAccount.accountNumber.prefix", source = "ncaRepaymentAccount.currentAccount.account.accountNumber.prefix")
    @Mapping(target = "repaymentAccount.iban", source = "ncaRepaymentAccount.currentAccount.account.accountNumber.iban")
    FullRepaymentDataResponse mapToFullRepaymentDataResponse(TodayBillDetailsResponseBodyInner todayBillDetailsResponseBodyBody, GetCurrentAccountDetailRes ncaRepaymentAccount, FeesDetail feesDetail);

    @AfterMapping
    default void parseMonthlyLoanManagementFee(@MappingTarget FullRepaymentDataResponse response, FeesDetail feesDetail) {
        Optional.ofNullable(feesDetail)
                .map(FeesDetail::getFees)
                .orElse(List.of())
                .stream()
                .filter(fee -> fee.getFeeCode().equals(PRICING_FEE_CODE))
                .findFirst()
                .map(FeeV1::getFeeAmount)
                .map(cz.kb.leon.bc.pricing.v1.dto.MonetaryAmount::getAmount)
                .map(BigDecimal::toString)
                .ifPresent(fee -> response.setMonthlyLoanManagementFee(new MonetaryAmount().amount(fee)));

        // monthlyLoanManagementFee currency
        Optional.of(response)
                .map(FullRepaymentDataResponse::getMonthlyLoanManagementFee)
                .ifPresent(monetaryAmount -> monetaryAmount.setCurrency(CURRENCY_CZK));

    }

    @Mapping(target = "isPaymentAuthorized", source = "isPaymentAuthorizationSuccessful")
    EarlyRepaymentAuthorizedRequest mapEarlyRepaymentAuthorizedRequest(EarlyRepaymentResultActionRequest earlyRepaymentAuthorizedRequest);

}
