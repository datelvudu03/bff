package cz.kb.leon.bff.servicing.app.helper;

import cz.kb.ndch.bff.business.financing.overdraft.servicing.v1.dto.MonetaryAmount;

import java.math.BigDecimal;

public record OverdraftDetailHelper(BigDecimal availableBalance, BigDecimal loanAmount, String currency, String productState) {

    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String SIGNED = "SIGNED";

    public MonetaryAmount evaluateRemainingAmount() {
        var withdrawnAmount = computeWithdrawnAmount();
        if (loanAmount != null && withdrawnAmount != null) {
            var amount = loanAmount.subtract(withdrawnAmount);
            return new MonetaryAmount().amount(amount.toString()).currency(currency);
        }
        return null;
    }

    public MonetaryAmount evaluateWithdrawnAmount() {
        var withdrawnAmount = computeWithdrawnAmount();
        if (withdrawnAmount != null) {
            return new MonetaryAmount().amount(withdrawnAmount.toString()).currency(currency);
        }
        return null;
    }

    private BigDecimal computeWithdrawnAmount() {
        if (loanAmount != null && availableBalance != null) {
            var diff = availableBalance.subtract(loanAmount);
            return loanAmount.min(((BigDecimal.ZERO.min(diff)).abs()));
        }
        return null;
    }

    public boolean hasActiveTerminationCaseFlag() {
        return IN_PROGRESS.equals(productState) || SIGNED.equals(productState);
    }

}
