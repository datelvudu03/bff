package cz.kb.leon.bff.servicing.app.handler.operationalloan;

import cz.kb.api.accountservicing.v1.dto.GetCurrentAccountDetailRes;
import cz.kb.api.loansapi.v1.dto.*;
import cz.kb.cbs.position_keeping.gen.jaxrs.model.ApplicationResponseCK;
import cz.kb.cbs.position_keeping.gen.jaxrs.model.InstructionResponseBALANCE;
import cz.kb.leon.assertion.AssertCheck;
import cz.kb.leon.bc.pricing.v1.dto.FeesDetail;
import cz.kb.leon.bc.pricing.v1.dto.InterestRateDetailV1;
import cz.kb.leon.bc.productdefinition_private_v1.dto.ProductDefinitionDetail;
import cz.kb.leon.bc.productlifecycle_private_api_v1.dto.Currency;
import cz.kb.leon.bc.productlifecycle_private_api_v1.dto.*;
import cz.kb.leon.bc.servicing.v2.dto.*;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.EarlyRepaymentAuthorizedRequest;
import cz.kb.leon.bc.servicing_operationalloan_private_v1.dto.EarlyRepaymentAvailabilityResult;
import cz.kb.leon.bff.servicing.app.command.operationalloan.*;
import cz.kb.leon.bff.servicing.app.mapper.loan.LoanCommandMapper;
import cz.kb.leon.bff.servicing.domain.enumeration.OperationalLoanEarlyRepaymentResultActionResultEnum;
import cz.kb.leon.bff.servicing.domain.enumeration.RepaymentTypeEnum;
import cz.kb.leon.bff.servicing.domain.exception.DomainException;
import cz.kb.leon.bff.servicing.domain.exception.DomainExceptionCode;
import cz.kb.leon.bff.servicing.infra.mapper.MappingDTO;
import cz.kb.leon.bff.servicing.service.TimeService;
import cz.kb.leon.bff.servicing.infra.integrations.nca.NCAClientService;
import cz.kb.leon.bff.servicing.infra.integrations.operationalloan.ServicingOperationLoanBffClient;
import cz.kb.leon.bff.servicing.infra.integrations.plc.PLCClientService;
import cz.kb.leon.bff.servicing.infra.integrations.positionkeeping.PositionKeepingService;
import cz.kb.leon.bff.servicing.infra.integrations.pricing.PricingClientService;
import cz.kb.leon.bff.servicing.infra.integrations.product_definitions.ProductDefinitionsClientService;
import cz.kb.leon.bff.servicing.infra.integrations.t24.T24ClientService;
import cz.kb.leon.bff.servicing.util.AccountUtil;
import cz.kb.leon.bff.servicing.util.CollectionUtil;
import cz.kb.leon.bff.servicing.util.ObjectUtil;
import cz.kb.ndch.bff.business.financing.operational.loan.servicing.v1.dto.*;
import cz.kb.ndpcommon.phrase.converter.starter.converter.MonetaryAmountConverter;
import cz.kb.ndpcommon.phrase.converter.starter.converter.MonetaryAmountWithoutDecimalConverter;
import cz.kb.speed.cqrs.api.command.CommandHandler;
import cz.kb.speed.messaging.api.handler.Processing;
import jakarta.annotation.security.RunAs;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static cz.kb.leon.bff.servicing.configuration.AppConstants.*;
import static cz.kb.leon.bff.servicing.domain.exception.DomainExceptionCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
@RunAs("bff-leon-servicing-service-user")
public class OperationalLoanCmdHandler {

    private final ServicingOperationLoanBffClient servicingOperationLoanBCClient;

    private final PLCClientService plcClientService;

    private final T24ClientService t24ClientService;

    private final PricingClientService pricingClientService;

    private final PositionKeepingService positionKeepingService;

    private final NCAClientService ncaClientService;

    private final ProductDefinitionsClientService productDefinitionsClientService;

    private final MappingDTO mappingDTO;

    private final LoanCommandMapper loanCommandMapper = Mappers.getMapper(LoanCommandMapper.class);

    private final MessageSource phraseMessages;

    private final TimeService timeService;

    private final MonetaryAmountConverter monetaryAmountConverter = new MonetaryAmountConverter();

    private final MonetaryAmountWithoutDecimalConverter monetaryAmountWithoutDecimalConverter = new MonetaryAmountWithoutDecimalConverter();

    private static @NotNull LocalDate getDeactivationDate(LoanInfo loanInfo) {
        return Optional.ofNullable(loanInfo.operationalLoanProduct())
                .map(OperationalLoanProduct::getDeactivationDate)
                .map(DeactivationDate::getDate)
                .orElse(LocalDate.MIN);
    }

    private static @NotNull String getCurrency(LoanInfo loanInfo) {
        return Optional.of(loanInfo)
                .map(LoanInfo::operationalLoanProduct)
                .map(OperationalLoanProduct::getLoanLimit)
                .map(LoanLimit::getCurrency)
                .map(Currency::getCode)
                .orElse(CURRENCY_CZK);
    }

    private static @NotNull Boolean getLoanActive(LoanInfo loanInfo) {
        return Optional.ofNullable(loanInfo.operationalLoanProduct())
                .map(OperationalLoanProduct::getProduct)
                .map(Product::getProductState)
                .map(VersionState::getState)
                .map(PRODUCT_STATE_PLC_ACTIVATED::equalsIgnoreCase)
                .orElse(false);
    }

    @CommandHandler(processing = Processing.SYNC)
    public void handleCommand(GetOperationalLoanEarlyRepaymentInfoDataCmd cmd) {
        EarlyRepaymentAvailabilityResult response = servicingOperationLoanBCClient.earlyRepaymentAvailability(cmd.productInstanceId(), cmd.userId());

        if (!response.getAvailability()) {
            switch (response.getReasonCode()) {
                case COB ->
                        throw new DomainException(ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_NOT_IN_TIME, ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_NOT_IN_TIME.name(), response.getData());
                case DATE ->
                        throw new DomainException(ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_NOT_TODAY, ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_NOT_TODAY.name(), response.getData());
                case OVERDUE ->
                        throw new DomainException(ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_OVERDUE, ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_OVERDUE.name(), response.getData());
                case ALREADY_PAID ->
                        throw new DomainException(ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_ALREADY_PAID, ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_ALREADY_PAID.name(), response.getData());
                case CONFLICT ->
                        throw new DomainException(ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_IN_PROCESSING, ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_IN_PROCESSING.name(), response.getData());
                case DEBT ->
                        throw new DomainException(ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_KB_DEBT, ERR_OPERATIONAL_LOAN_EARLY_REPAYMENT_IN_PROCESSING.name(), response.getData());
                case OTHER -> throw new DomainException(ERR_OTHER, "");
            }
        }
    }

    @CommandHandler(processing = Processing.SYNC)
    public OperationalLoanEarlyRepaymentResultActionCmd.Result handleCommand(OperationalLoanEarlyRepaymentResultActionCmd cmd) {
        EarlyRepaymentAuthorizedRequest earlyRepaymentAuthorizedRequest = loanCommandMapper.mapEarlyRepaymentAuthorizedRequest(cmd.earlyRepaymentResultActionRequest());

        servicingOperationLoanBCClient.cmdEarlyRepaymentAuthorized(earlyRepaymentAuthorizedRequest, cmd.earlyRepaymentResultActionRequest().getServicingCaseId(), cmd.userId());

        Result result = (ObjectUtil.evaluateBoolean(cmd.earlyRepaymentResultActionRequest().getIsPaymentAuthorizationSuccessful()))
                ? new Result().code(OperationalLoanEarlyRepaymentResultActionResultEnum.OPERATIONAL_LOAN_EARLY_REPAYMENT_SUCCESS.name())
                : new Result().code(OperationalLoanEarlyRepaymentResultActionResultEnum.OPERATIONAL_LOAN_EARLY_REPAYMENT_FAIL.name());

        return new OperationalLoanEarlyRepaymentResultActionCmd.Result(Response.status(Response.Status.CREATED).entity(result).build());
    }

    @CommandHandler(processing = Processing.SYNC)
    public OperationalLoanStatementRequestedActionCmd.CmdResult handleCommand(OperationalLoanStatementRequestedActionCmd cmd) {
        RequestStatementCmdReq requestStatementCmdReq = loanCommandMapper.mapBusinessLoanStatementRequestedActionRequestToRequestStatementCmdReq(cmd.statementRequestedActionRequest());

        Response response = servicingOperationLoanBCClient.requestStatement(requestStatementCmdReq, cmd.xB3TraceId(), cmd.userId());

        return new OperationalLoanStatementRequestedActionCmd.CmdResult(response);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetConsumerLoanStatementDocumentDataCmd.CmdResult handleCommand(GetConsumerLoanStatementDocumentDataCmd cmd) {
        Response response = servicingOperationLoanBCClient.documentData(cmd.statementId(), cmd.productInstanceId(), cmd.userId());

        return new GetConsumerLoanStatementDocumentDataCmd.CmdResult(response);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetOperationalLoanStatementDocumentStateCmd.CmdResult handleCommand(GetOperationalLoanStatementDocumentStateCmd cmd) {
        Response response = servicingOperationLoanBCClient.getStatementGeneratingState(cmd.statementId(), cmd.userId());

        return new GetOperationalLoanStatementDocumentStateCmd.CmdResult(response);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetDetailDataCmd.CmdResult handleCommand(GetDetailDataCmd cmd) {
        String productInstanceId = cmd.productInstanceId();

        LoanInfo loanInfo = retrieveLoanInfo(productInstanceId, true);

        OperationalLoanDetailData responseEntity = loanCommandMapper.mapToOperationalLoanDetailData(loanInfo, cmd.language());

        Locale locale = cmd.language().getLocale();
        Optional.ofNullable(loanInfo.loanDetailResponseBodyInner())
                .map(LoanDetailResponseBodyInner::getSchedules)
                .ifPresent(sList -> {
                    Message message = createErrorMessage(sList, loanInfo, locale, responseEntity.getAccount().getAccountAlias());
                    if (message != null) {
                        responseEntity.addMessageListItem(message);
                    }
                });

        Optional.of(loanInfo)
                .map(LoanInfo::paymentScheduleResponseBodyInner)
                .ifPresent(paymentScheduleResponseBodyInner -> {
                    MonetaryAmount monetaryAmount = loanCommandMapper.createMonetaryAmount(paymentScheduleResponseBodyInner);
                    if (getLoanActive(loanInfo) && monetaryAmount.getAmount() != null && paymentScheduleResponseBodyInner.getPaymentDate() != null) {
                        responseEntity.addMessageListItem(new Message()
                                .severity(Message.SeverityEnum.INFO)
                                .text(phraseMessages.getMessage(NEXT_PAYMENT_KEY,
                                        new Object[]{prepareMonetaryAmount(new BigDecimal(monetaryAmount.getAmount()), getCurrency(loanInfo), cmd.language().getLocale()), ObjectUtil.formatDate(paymentScheduleResponseBodyInner.getPaymentDate())}, cmd.language().getLocale())));
                    }
                });

        Optional.of(loanInfo)
                .map(LoanInfo::operationalLoanProduct)
                .map(OperationalLoanProduct::getProduct)
                .map(Product::getProductState)
                .map(VersionState::getState)
                .ifPresent((String productState) -> {
                    if (PRODUCT_STATE_PLC_TERMINATED.equalsIgnoreCase(productState) || PRODUCT_STATE_PLC_TERMINATING.equalsIgnoreCase(productState)) {
                        responseEntity.addMessageListItem(new Message()
                                .severity(Message.SeverityEnum.SUCCESS)
                                .text(phraseMessages.getMessage(PAYED_LOAN_KEY,
                                        new Object[]{ObjectUtil.formatDate(getDeactivationDate(loanInfo))}, cmd.language().getLocale())));
                    }
                });

        Response response = Response.ok(responseEntity).build();

        return new GetDetailDataCmd.CmdResult(response);
    }

    private String prepareMonetaryAmount(BigDecimal npa, String currency, Locale locale) {
        AssertCheck.notNull(npa, "NextPaymentAmount is null");
        AssertCheck.notNull(currency, "currency is null");
        AssertCheck.notNull(locale, "locale is null");
        if (npa.compareTo(BigDecimal.ZERO) == 0) {
            return monetaryAmountWithoutDecimalConverter.convert(new cz.kb.ndpcommon.common.model.MonetaryAmountWithoutDecimal(npa.longValue(), currency), locale);
        }
        return monetaryAmountConverter.convert(new cz.kb.ndpcommon.common.model.MonetaryAmount(npa, currency), locale);
    }

    private Message createErrorMessage(List<LoanDetailResponseBodyInnerSchedulesInner> sList, LoanInfo loanInfo, Locale locale, String alias) {
        Message message = new Message();
        if (!sList.isEmpty()) {
            Optional.of(sList.getFirst())
                    .map(LoanDetailResponseBodyInnerSchedulesInner::getDueScheduleCount)
                    .map(Integer::parseInt)
                    .ifPresent((Integer dueScheduleCount) -> {
                        if (shouldShowMessage(loanInfo, dueScheduleCount)) {
                            message.setSeverity(Message.SeverityEnum.ERROR);
                            message.setText(phraseMessages.getMessage(OVERDUE_LOAN_KEY, new Object[]{alias}, locale));
                        }
                    });
        }
        return null;
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetInfoDataCmd.CmdResult handleCommand(GetInfoDataCmd cmd) {

        String productInstanceId = cmd.productInstanceId();

        LoanInfo loanInfo = retrieveLoanInfo(productInstanceId, false);

        Optional<OperationalLoanProduct> operationalLoanProduct = Optional.ofNullable(loanInfo.operationalLoanProduct());
        String pricingId = operationalLoanProduct
                .map(OperationalLoanProduct::getPricingId)
                .map(PricingId::getId)
                .map(UUID::toString)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format - missing pricing id."));

        InterestRateDetailV1 interestRateDetailV1 = pricingClientService.getIRDetailByProductInstanceIdV1(productInstanceId);

        FeesDetail feesDetail = pricingClientService.getFeesDetailV1(pricingId);

        OperationalLoanInfoData responseEntity = loanCommandMapper.mapToOperationalLoanInfoData(loanInfo, interestRateDetailV1, feesDetail, cmd.language());

        QryGetServicingCasesV2200Response qryGetServicingCases200Response = servicingOperationLoanBCClient.servicingCaseQuery(productInstanceId,
                Set.of(CaseType.OPERATION_LOAN_EARLY_REPAYMENT), Set.of(CaseState.STARTED, CaseState.IN_PROGRESS), cmd.userId());

        Locale locale = cmd.language().getLocale();

        Optional.ofNullable(qryGetServicingCases200Response)
                .map(QryGetServicingCasesV2200Response::getCases)
                .ifPresent((List<ServicingCase> scList) -> {
                    if (!scList.isEmpty()) {
                        responseEntity.addMessageListItem(new Message()
                                .severity(Message.SeverityEnum.INFO)
                                .text(phraseMessages.getMessage(EARLY_REPAYMENT_IN_PROGRESS_KEY, null, locale)));
                    }
                });

        Optional.ofNullable(loanInfo.loanDetailResponseBodyInner())
                .map(LoanDetailResponseBodyInner::getSchedules)
                .ifPresent(sList -> {
                    Message message = createErrorMessage(sList, loanInfo, locale, responseEntity.getAccount().getAccountAlias());
                    if (message != null) {
                        responseEntity.addMessageListItem(message);
                    }
                });

        Optional.ofNullable(responseEntity)
                .map(OperationalLoanInfoData::getInstalment)
                .ifPresent(instalment -> {
                    if (getLoanActive(loanInfo) && instalment.getInstalmentAmount() != null && instalment.getNextInstalmentDate() != null) {
                        responseEntity.addMessageListItem(new Message()
                                .severity(Message.SeverityEnum.INFO)
                                .text(phraseMessages.getMessage(NEXT_PAYMENT_KEY, new Object[]{prepareMonetaryAmount(new BigDecimal(instalment.getInstalmentAmount().getAmount()), getCurrency(loanInfo), cmd.language().getLocale()), ObjectUtil.formatDate(instalment.getNextInstalmentDate())}, cmd.language().getLocale())));
                    }
                });

        Optional.of(loanInfo.operationalLoanProduct())
                .map(OperationalLoanProduct::getProduct)
                .map(Product::getProductState)
                .map(VersionState::getState)
                .ifPresent((String productState) -> {
                    if (PRODUCT_STATE_PLC_TERMINATED.equalsIgnoreCase(productState) || PRODUCT_STATE_PLC_TERMINATING.equalsIgnoreCase(productState)) {
                        responseEntity.addMessageListItem(new Message()
                                .severity(Message.SeverityEnum.SUCCESS)
                                .text(phraseMessages.getMessage(PAYED_LOAN_KEY, new Object[]{ObjectUtil.formatDate(getDeactivationDate(loanInfo))}, cmd.language().getLocale())));
                    }
                });

        Response response = Response.ok(responseEntity).build();
        return new GetInfoDataCmd.CmdResult(response);
    }

    private boolean shouldShowMessage(LoanInfo loanInfo, Integer dueScheduleCount) {
        LocalDate now = LocalDate.now();
        LocalDate scheduledPayment = evaluateScheduledPayment(loanInfo.operationalLoanProduct());

        if (dueScheduleCount >= 2) {
            return true;
        } else if (dueScheduleCount == 0) {
            return false;
        }

        if (now.isAfter(scheduledPayment.plusDays(5))) {
            return true;
        }

        if (now.getDayOfMonth() < scheduledPayment.getDayOfMonth()) {
            Pair<LocalTime, LocalTime> cobStartAndEnd = timeService.cobParsing();
            LocalTime localTime = LocalTime.now();
            return !(cobStartAndEnd.getLeft().isAfter(localTime) && cobStartAndEnd.getRight().isBefore(localTime));
        }

        return false;
    }

    private LocalDate evaluateScheduledPayment(OperationalLoanProduct operationalLoanProduct) {
        int dayInMonth = Optional.ofNullable(operationalLoanProduct)
                .map(OperationalLoanProduct::getPaymentDueDay)
                .map(PaymentDueDay::getDayOfMonth)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.ARGUMENT_IS_NULL, ObjectUtil.evaluateMessage("dayInMonth is null")));

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int monthsToAdd = day > dayInMonth ? 1 : 0;
        month = (month + monthsToAdd - 1) % 12 + 1;
        year = year + (month == 12 && monthsToAdd > 0 ? 1 : 0);

        dayInMonth = switch (month) {
            case 2 -> Math.min(dayInMonth, IsoChronology.INSTANCE.isLeapYear(year) ? 29 : 28);
            case 4, 6, 9, 11 -> Math.min(dayInMonth, 30);
            default -> dayInMonth;
        };

        return LocalDate.of(year, month, dayInMonth);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetOperationalLoanFullRepaymentAcceptedActionCmd.CmdResult handleCommand(GetOperationalLoanFullRepaymentAcceptedActionCmd cmd) {
        Response response = processAcceptedActionCmd(cmd.productId(), cmd.fromAccountIban(), cmd.amount(), cmd.xB3TraceId(), cmd, RepaymentTypeEnum.FULL, cmd.userId());
        return new GetOperationalLoanFullRepaymentAcceptedActionCmd.CmdResult(response);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetOperationalLoanPartialRepaymentAcceptedActionCmd.CmdResult handleCommand(GetOperationalLoanPartialRepaymentAcceptedActionCmd cmd) {
        Response response = processAcceptedActionCmd(cmd.productInstanceId(), cmd.fromAccountIban(), cmd.amount(), cmd.xB3TraceId(), cmd, RepaymentTypeEnum.PARTIAL, cmd.userId());
        return new GetOperationalLoanPartialRepaymentAcceptedActionCmd.CmdResult(response);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetOperationLoanPartialRepaymentDataCmd.CmdResult handleCommand(GetOperationLoanPartialRepaymentDataCmd cmd) {

        GetOperationalLoanProductData200Response operationalLoanProduct = plcClientService.getOperatingLoanProductData(cmd.productInstanceId());

        String ibanFromRepaymentAccount = Optional.ofNullable(operationalLoanProduct)
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getRepaymentAccount)
                .map(RepaymentAccount::getIban)
                .map(IBAN::getValue)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.ERR_OTHER, "Invalid operationalLoanProduct Data format - missing IBAN from RepaymentAccount."));

        String ibanFromLoanAccount = Optional.of(operationalLoanProduct)
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getLoanAccount)
                .map(LoanAccount::getIban)
                .map(IBAN::getValue)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.ERR_OTHER, "Invalid operationalLoanProduct Data format - missing IBAN from Loan Account."));
        InstructionResponseBALANCE balanceResponse = positionKeepingService.getBalance(ibanFromRepaymentAccount);

        LoanDetailResponse loanDetailResponse = t24ClientService.getLoanArrangementDetail(ibanFromLoanAccount);

        // T24 - getCustomerLoan
        CustomerLoanResponse customerLoanResponse = t24ClientService.getLoanDetail(ibanFromLoanAccount);

        String repaymentAccountId = Optional.of(operationalLoanProduct)
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getRepaymentAccount)
                .map(RepaymentAccount::getId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.ERR_OTHER, "Invalid operationalLoanProduct Data format - missing Id from Repayment Account."));

        GetCurrentAccountDetailRes currentAccountDetailResponse = ncaClientService.getCurrentAccountDetail(repaymentAccountId);

        PaymentScheduleResponse t24PaymentScheduleResponse = t24ClientService.getPaymentSchedule(ibanFromLoanAccount);

        PartialRepaymentDataResponse responseEntity = loanCommandMapper.mapToPartialRepaymentDataResponse(
                checkCollectionAndReturnFirst(customerLoanResponse.getBody()),
                balanceResponse,
                checkCollectionAndReturnFirst(checkCollectionAndReturnFirst(loanDetailResponse.getBody()).getSchedules()),
                currentAccountDetailResponse,
                t24PaymentScheduleResponse);

        Optional.ofNullable(responseEntity).ifPresent(response -> response.setLoanAccountIban(ibanFromLoanAccount));
        Optional.ofNullable(responseEntity)
                .map(PartialRepaymentDataResponse::getRepaymentAccount)
                .ifPresent(r -> {
                    r.setIban(ibanFromRepaymentAccount);
                    AccountUtil.AccountNumber parsedFromIban = AccountUtil.parseIbanToAccountNumber(ibanFromRepaymentAccount);
                    AccountNumber accountNumber = new AccountNumber();
                    if (parsedFromIban != null) {
                        accountNumber.setPrefix(parsedFromIban.prefix());
                        accountNumber.setCore(parsedFromIban.core());
                        accountNumber.setBankCode(parsedFromIban.bankCode());
                        r.setAccountNumber(accountNumber);
                    }
                });

        Response response = Response.ok(responseEntity).build();

        return new GetOperationLoanPartialRepaymentDataCmd.CmdResult(response);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetOperationalLoanPartialRepaymentAmountChangedActionCmd.CmdResult handleCommand(GetOperationalLoanPartialRepaymentAmountChangedActionCmd cmd) {
        String iban = cmd.operationalLoanIban();
        MonetaryAmount amount = cmd.amount();

        CalPayoffSimulationRunnerResponse calPayoffSimulationRunnerResponse = t24ClientService.createCalPayoffSimulationRunner(iban, amount);

        String simulationId = Optional.of(calPayoffSimulationRunnerResponse)
                .map(CalPayoffSimulationRunnerResponse::getHeader)
                .map(ScreenHeader::getId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_T24_RESPONSE, "Missing simulationId in response from T24 service."));

        LoanSimRunnerDetailResponse loanSimRunnerDetailResponse = getSimulationDetail(simulationId, () -> t24ClientService.getLoanSimRunnerDetail(simulationId, iban));

        Optional<LoanSimRunnerDetailResponseBodyInner> loanSimRunnerDetailResponseBodyOptional = Optional.ofNullable(loanSimRunnerDetailResponse)
                .map(LoanSimRunnerDetailResponse::getBody)
                .orElseGet(List::of).stream().findFirst();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        LocalDate maturityDate = loanSimRunnerDetailResponseBodyOptional
                .map(LoanSimRunnerDetailResponseBodyInner::getScheduleDate)
                .map(sd -> LocalDate.parse(sd, formatter))
                .orElse(null);

        // workaround: zatim nemame spravnou hodnotu
        Integer numberOfInstalments = null;

        CustomerLoanResponse customerLoanResponse = t24ClientService.getLoanDetail(iban);

        Optional<CustomerLoanResponseBodyInner> customerLoanResponseBodyOptional = Optional.ofNullable(customerLoanResponse)
                .map(CustomerLoanResponse::getBody)
                .orElseGet(List::of).stream().findFirst();

        PartialRepaymentChangedActionResponse responseEntity = new PartialRepaymentChangedActionResponse()
                .amount(calculateResponseAmount(customerLoanResponseBodyOptional, cmd.amount()))
                .maturityDate(maturityDate)
                .numberOfInstalments(numberOfInstalments);

        Response response = Response.status(Response.Status.CREATED).entity(responseEntity).build();

        return new GetOperationalLoanPartialRepaymentAmountChangedActionCmd.CmdResult(response);
    }

    private MonetaryAmount calculateResponseAmount(Optional<CustomerLoanResponseBodyInner> loanAmount, MonetaryAmount requestAmount) {
        BigDecimal loanBalanceValue = loanAmount.map(CustomerLoanResponseBodyInner::getLoanBalance).orElseThrow(() -> new DomainException(INVALID_T24_RESPONSE, "T24 service (method party/loan/detail) returned empty loan balance."));
        String loanBalanceCurrency = loanAmount.map(CustomerLoanResponseBodyInner::getLoanCurrency).orElseThrow(() -> new DomainException(INVALID_T24_RESPONSE, "T24 service (method party/loan/detail) returned empty loan currency."));

        if (!loanBalanceCurrency.equalsIgnoreCase(requestAmount.getCurrency())) {
            throw new DomainException(CURRENCIES_DO_NOT_MATCH, ObjectUtil.evaluateMessage("Currency from T24 service (method party/loan/detail) returned a currency '{}' but the currency in the request is '{}'.", loanBalanceCurrency, requestAmount.getCurrency()));
        }

        return new MonetaryAmount()
                .amount(loanBalanceValue.abs().subtract(new BigDecimal(requestAmount.getAmount())).toString())
                .currency(loanBalanceCurrency);
    }

    @CommandHandler(processing = Processing.SYNC)
    public PostOperationalLoanNameChangeCmd.CmdResult handleCommand(PostOperationalLoanNameChangeCmd cmd) {

        Response response = plcClientService.updateProductAlias(
                cmd.changeNameAction().getProductInstanceId(),
                cmd.changeNameAction().getNewConsumerLoanAlias());
        return new PostOperationalLoanNameChangeCmd.CmdResult(response);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetOperationLoanFullRepaymentDataCmd.CmdResult handleCommand(GetOperationLoanFullRepaymentDataCmd cmd) {
        GetOperationalLoanProductData200Response plcResponse = plcClientService.getOperatingLoanProductData(cmd.productInstanceId());

        String arrangementId = Optional.ofNullable(plcResponse)
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getLoanAccount)
                .map(LoanAccount::getIban)
                .map(IBAN::getValue)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format - missing IBAN."));

        CalPayoffSimulationResponse calPayoffSimulationRunnerResponse = t24ClientService.createCalPayoffSimulation(arrangementId);

        String simulationId = Optional.of(calPayoffSimulationRunnerResponse)
                .map(CalPayoffSimulationResponse::getHeader)
                .map(ScreenHeader::getId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_T24_RESPONSE, "Missing simulationId in response from T24 service."));

        TodayBillDetailsResponse todayBillDetailsResponse = getSimulationDetail(simulationId, () -> t24ClientService.getTodayBillDetails(arrangementId));

        TodayBillDetailsResponseBodyInner todayBillDetailsResponseBody = Optional.ofNullable(todayBillDetailsResponse)
                .map(TodayBillDetailsResponse::getBody)
                .orElseGet(List::of).stream().findFirst()
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_T24_RESPONSE, "Missing body in response from T24 service."));

        String repaymentAccountId = Optional.of(plcResponse)
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getRepaymentAccount)
                .map(RepaymentAccount::getId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format - missing RepaymentAccount ID."));

        GetCurrentAccountDetailRes ncaRepaymentAccount = ncaClientService.getCurrentAccountDetail(repaymentAccountId);

        var repaymentAccountIban = Optional.of(plcResponse)
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getRepaymentAccount)
                .map(RepaymentAccount::getIban)
                .map(IBAN::getValue)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format - missing RepaymentAccount IBAN."));

        InstructionResponseBALANCE balanceResponse = positionKeepingService.getBalance(repaymentAccountIban);

        BigDecimal availableBalance = Optional.ofNullable(balanceResponse)
                .map(InstructionResponseBALANCE::getApplCK)
                .map(ApplicationResponseCK::getAvailableBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalAmount = Optional.of(todayBillDetailsResponseBody)
                .map(TodayBillDetailsResponseBodyInner::getoSTotalAmt)
                .filter(amt -> amt.matches("^\\d+(?:\\.\\d+)?$"))   // This RE matches all numbers - at least one digit (0-9) (\\d+) optionally followed by ((...)?) decimal point (\\.) and at least one digit
                .map(BigDecimal::new)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_T24_RESPONSE, "Missing 'oSTotalAmt' in response from T24 service."));

        if (availableBalance.compareTo(totalAmount) < 0) {
            throw new DomainException(DomainExceptionCode.ERR_OPERATIONAL_LOAN_FULL_REPAYMENT_INSUFFICIENT_BALANCE, ObjectUtil.evaluateMessage("Available balance ({}) is less than total repayment amount ({}).", availableBalance, totalAmount));
        }

        String pricingId = Optional.of(plcResponse)
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getPricingId)
                .map(PricingId::getId)
                .map(UUID::toString)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format - missing pricing id."));

        FeesDetail feesDetail = pricingClientService.getFeesDetailV1(pricingId);

        FullRepaymentDataResponse responseEntity = loanCommandMapper.mapToFullRepaymentDataResponse(todayBillDetailsResponseBody, ncaRepaymentAccount, feesDetail);

        Response result = Response.ok(responseEntity).build();

        return new GetOperationLoanFullRepaymentDataCmd.CmdResult(result);
    }

    @CommandHandler(processing = Processing.SYNC)
    public GetOperationalLoanStatementAvailabilityCmd.CmdResult handleCommand(GetOperationalLoanStatementAvailabilityCmd cmd) {

        GetOperationalLoanProductData200Response operationalLoanProduct = plcClientService.getOperatingLoanProductData(cmd.productInstanceId());

        LocalDate activationDate = Optional.ofNullable(operationalLoanProduct)
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getActivationDate)
                .map(ActivationDate::getDate)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.ERR_OTHER, "Activation date cannot be null."));

        if (activationDate.isBefore(LocalDate.now())) {
            String loanAccountId = Optional.of(operationalLoanProduct)
                    .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                    .map(OperationalLoanProduct::getLoanAccount)
                    .map(LoanAccount::getId)
                    .orElseThrow(() -> new DomainException(DomainExceptionCode.ERR_OTHER, "Loan account id cannot be null."));
            return new GetOperationalLoanStatementAvailabilityCmd.CmdResult(Response.ok(new GetOperationalLoanStatementAvailability200Response().accountId(loanAccountId)).build());
        } else {
            throw new DomainException(DomainExceptionCode.ERR_OPERATIONAL_LOAN_STATEMENT_NOT_TODAY, "Activation date is in the past.");
        }
    }

    private LoanInfo retrieveLoanInfo(String productInstanceId, boolean isLoanDetailData) {
        GetOperationalLoanProductData200Response plcResponse = plcClientService.getOperatingLoanProductData(productInstanceId);

        Optional<GetOperationalLoanProductData200Response> operationalLoanProduct = Optional.ofNullable(plcResponse);

        UUID productDefinitionId = operationalLoanProduct
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getProduct)
                .map(Product::getProductDefinitionId)
                .map(ProductDefinitionId::getId)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format - missing product definition id."));

        String arrangementId = operationalLoanProduct
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getLoanAccount)
                .map(LoanAccount::getIban)
                .map(IBAN::getValue)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format - missing IBAN."));

        String state = operationalLoanProduct
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getProduct)
                .map(Product::getProductState)
                .map(VersionState::getState)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.INVALID_PLC_DATA, "Invalid PLC Data format - missing state."));

        ProductDefinitionDetail productDefinitionDetail = productDefinitionsClientService.getDefinitionData(productDefinitionId);

        CustomerLoanResponse t24CustomerLoanDetailResponse = new CustomerLoanResponse();

        LoanDetailResponse t24LoanArrangementDetailResponse;

        PaymentScheduleResponseBodyInner paymentScheduleResponseBodyInner;

        // For GetDetailDataCmd
        if (isLoanDetailData) {
            if (PRODUCT_STATE_PLC_ACTIVATED.equalsIgnoreCase(state)) {
                t24CustomerLoanDetailResponse = t24ClientService.getLoanDetail(arrangementId);
            }
            t24LoanArrangementDetailResponse = t24ClientService.getLoanArrangementDetail(arrangementId);
            paymentScheduleResponseBodyInner = getPaymentScheduleResponseBodyInner(arrangementId);
            // For GetInfoDataCmd
        } else {
            if (PRODUCT_STATE_PLC_ACTIVATED.equalsIgnoreCase(state)) {
                t24CustomerLoanDetailResponse = t24ClientService.getLoanDetail(arrangementId);
            }
            paymentScheduleResponseBodyInner = getPaymentScheduleResponseBodyInner(arrangementId);
            t24LoanArrangementDetailResponse = t24ClientService.getLoanArrangementDetail(arrangementId);
        }

        return new LoanInfo(
                operationalLoanProduct.map(GetOperationalLoanProductData200Response::getOperationalLoanProduct).orElse(null),
                productDefinitionDetail,
                CollectionUtil.safelyReturnNthElementValue(t24CustomerLoanDetailResponse.getBody(), 0),
                CollectionUtil.safelyReturnNthElementValue(t24LoanArrangementDetailResponse.getBody(), 0),
                paymentScheduleResponseBodyInner);
    }

    private PaymentScheduleResponseBodyInner getPaymentScheduleResponseBodyInner(String arrangementId) {
        PaymentScheduleResponse t24PaymentScheduleResponse = t24ClientService.getPaymentSchedule(arrangementId);

        if (t24PaymentScheduleResponse.getBody() == null) {
            log.warn("Body of t24PaymentScheduleResponse is null");
            return null;
        }
        return t24PaymentScheduleResponse.getBody().stream()
                .filter(paymentScheduleResponseBodyInner -> List.of(SCHEDULE_TYPE_DUE, SCHEDULE_TYPE_FUTURE).contains(paymentScheduleResponseBodyInner.getScheduleType()) &&
                        (paymentScheduleResponseBodyInner.getPrincipalAmount() != null || paymentScheduleResponseBodyInner.getInterestAmount() != null)
                )
                .findFirst().orElseGet(() -> {
                    log.warn("PaymentScheduleResponseBodyInner is null");
                    return null;
                });
    }

    private void throwProgressException(AtomicInteger attemptCounter, LocalDateTime checkStart, Exception ex) {
        long durationInSeconds = Duration.between(checkStart, LocalDateTime.now()).getSeconds();

        Throwable cause = Optional.ofNullable(ex).map(Exception::getCause).orElse(null);
        String message = (!(cause instanceof T24ClientService.SimulationStatusInProgressException) ? "Simulation status is not successfully retrieved" : cause.getMessage());

        throw new DomainException(DomainExceptionCode.INVALID_T24_RESPONSE, ObjectUtil.evaluateMessage("{} after {} attempts in {} seconds.", message, attemptCounter.get(), durationInSeconds));
    }

    private <T> T getSimulationDetail(String simulationId, Supplier<T> detailSupplier) {
        LocalDateTime checkStart = LocalDateTime.now();
        AtomicInteger attemptCounter = new AtomicInteger(0);
        try {
            boolean simulationSuccessful = t24ClientService.checkSimulationStatusWithRetry(simulationId, attemptCounter);

            if (!simulationSuccessful) {
                throwProgressException(attemptCounter, checkStart, null);
            }
        } catch (Exception ex) {
            throwProgressException(attemptCounter, checkStart, ex);
        }

        return detailSupplier.get();
    }

    private Response processAcceptedActionCmd(String productId, String fromAccountIban, MonetaryAmount monetaryAmount, String xB3TraceId, Record recordCmd, RepaymentTypeEnum repaymentTypeEnum, String userId) {
        AssertCheck.notNull(productId, "Operational Loan IBAN cannot be null.");
        AssertCheck.notNull(monetaryAmount, "Amount cannot be null.");
        AssertCheck.notNull(fromAccountIban, "fromAccountIban cannot be null.");
        AssertCheck.notNull(xB3TraceId, "xB3TraceId cannot be null.");

        BigDecimal amount = Optional.of(monetaryAmount)
                .map(MonetaryAmount::getAmount)
                .map(BigDecimal::new)
                .orElseThrow(() -> new DomainException(DomainExceptionCode.MISSING_REPAYMENT_AMOUNT, "Invalid repayment amount."));
        AssertCheck.isTrue(amount.compareTo(BigDecimal.ONE) >= 0, "Amount cannot be less than 1.");

        String loanAccountIban = Optional.ofNullable(plcClientService.getOperatingLoanProductData(productId))
                .map(GetOperationalLoanProductData200Response::getOperationalLoanProduct)
                .map(OperationalLoanProduct::getLoanAccount)
                .map(LoanAccount::getIban)
                .map(IBAN::getValue)
                .orElseThrow(() -> new DomainException(ERR_OTHER, "Loan account iban cannot be null."));


        Pair<Integer, Object> responsePair = servicingOperationLoanBCClient.createEarlyRepaymentCase(mappingDTO.mapToCreateEarlyRepaymentCaseRequest(monetaryAmount, productId, repaymentTypeEnum), xB3TraceId, userId);

        Integer responseStatus = responsePair.getLeft();
        Object responseEntity = responsePair.getRight();

        if (responseStatus == Response.Status.CREATED.getStatusCode()) {
            ((RepaymentAcceptedActionResponse) responseEntity)
                    .requestedPaymentType(TypeOfPayment.DOMESTIC_PAYMENT)
                    .repaymentProductType((recordCmd instanceof GetOperationalLoanFullRepaymentAcceptedActionCmd) ? RepaymentProductType.EARLY_REPAYMENT : RepaymentProductType.PARTIAL_REPAYMENT)
                    .accountIban(fromAccountIban)
                    .counterpartyIdentification(loanAccountIban)
                    .instructedAmount(monetaryAmount);
        }

        return Response.status(responseStatus).entity(responseEntity).build();
    }

    private <T> T checkCollectionAndReturnFirst(Collection<T> collection) {
        return CollectionUtil.checkAndReturnFirst(collection, () -> new DomainException(DomainExceptionCode.INVALID_T24_RESPONSE, ObjectUtil.evaluateMessage("T24 returned bad response.")));
    }

    public record LoanInfo(OperationalLoanProduct operationalLoanProduct,
                           ProductDefinitionDetail productDefinitionDetail,
                           CustomerLoanResponseBodyInner customerLoanResponseBodyInner,
                           LoanDetailResponseBodyInner loanDetailResponseBodyInner,
                           PaymentScheduleResponseBodyInner paymentScheduleResponseBodyInner) {
    }

}
