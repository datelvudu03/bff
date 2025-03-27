package cz.kb.leon.bff.servicing.configuration;

import java.math.BigDecimal;

public class AppConstants {

    private AppConstants() {
    }

    public static final String APPLICATION_NAME_LEON = "LEON";

    public static final String FF_OPERATIONAL_LOAN = "feature.leon.operational_loan";

    public static final String REGEXP_FOR_LEADING_ZEROES = "^0+(?!$)";
    public static final String REGEXP_FOR_ALL_LEADING_ZEROES = "^0+";
    public static final String REGEXP_FOR_IBAN = "^(?:CZ)\\d{22}$";

    public static final String DOMESTIC_PAYMENT = "DOMESTIC_PAYMENT";
    public static final String PARTIAL_REPAYMENT = "PARTIAL_REPAYMENT";
    public static final String EARLY_REPAYMENT_OWN_AND_COUNTERPARTY_DESC = "MIMOŘÁDNÁ SPLÁTKA";

    public static final String PRODUCT_ID = "productId";
    public static final String PRODUCT_INSTANCE_ID = "productInstanceId";
    public static final String USER_ID = "userId";
    public static final String CLIENT_ID = "clientId";
    public static final String STATEMENT_ID = "statementId";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_ENCODING_JSON = "application/json";

    public static final String SERVICING_CASE_ID = "servicingCaseId";

    // Pep politics common attributes
    public static final String ATTR_CONTEXT_CUSTOMER_KBID = "context.Customer.KBID";
    public static final String ATTR_REQUEST_SERVICE = "request.service";

    // Pep politics and attributes for overdraft
    public static final String PRODUCT_OVERDRAFT_READ = "Product.Overdraft.Read";
    public static final String PRODUCT_OVERDRAFT_CHANGE = "Product.Overdraft.Change";
    public static final String REQUEST_PRODUCT_ID = "Request.Product.Id";

    // Pep politics and attributes for operational loan
    public static final String ACCOUNT_DETAIL_READ = "Account.Detail.Read";
    public static final String ACCOUNT_BUSINESS_LOAN_MANAGE = "Account.BusinessLoan.Manage";
    public static final String REQUEST_ACCOUNT_ID = "Request.Account.Id";
    public static final String REQUEST_ACCOUNT_IBAN = "Request.Account.IBAN";
    public static final String PAYMENT_CREATE = "Payment.Create";

    // Frontend channels
    public static final String FE_CHANNEL_OTHER = "CH0999";

    // Frontend scopes
    public static final String FE_SCOPE_MUJ_KLIENT_WEB = "SCOPE_mujklient_web";

    // Servicing case states
    public static final String SERVICING_CASE_STATE_IN_PROGRESS = "IN_PROGRESS";

    // Phrase locales keys
    public static final String EARLY_REPAYMENT_IN_PROGRESS_KEY = "business_finance.operational_loan.message_list.early_repayment_in_progress";
    public static final String NEXT_PAYMENT_KEY = "business_finance.operational_loan.message_list.next_payment";
    public static final String PAYED_LOAN_KEY = "business_finance.operational_loan.message_list.payed_loan";
    public static final String OVERDUE_LOAN_KEY = "business_finance.operational_loan.message_list.overdue_loan";

    // Pricing
    public static final String PRICING_FEE_CODE = "MAINTENANCE_FEE";

    // Frontend error types
    public static final String FE_ERROR_TYPE_GET = "error_page";
    public static final String FE_ERROR_TYPE_POST = "error_alert";

    // Number constants
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    // Product state cons - from PLC
    public static final String PRODUCT_STATE_PLC_ACTIVATED = "ACTIVATED";
    public static final String PRODUCT_STATE_PLC_TERMINATED = "TERMINATED";
    public static final String PRODUCT_STATE_PLC_TERMINATING = "TERMINATING";

    // CZK currency
    public static final String CURRENCY_CZK = "CZK";

    //SCHEDULE_TYPE
    public static final String SCHEDULE_TYPE_FUTURE = "FUTURE";
    public static final String SCHEDULE_TYPE_DUE = "DUE";

}
