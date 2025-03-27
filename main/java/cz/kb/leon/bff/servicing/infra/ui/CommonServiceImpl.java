package cz.kb.leon.bff.servicing.infra.ui;

import cz.kb.leon.assertion.AssertCheck;
import cz.kb.leon.bff.servicing.util.EndUserUtil;
import cz.kb.leon.featureflags.FeatureFlagService;

public abstract class CommonServiceImpl {

    protected boolean checkFeatureFlag(String featureFlag) {
        AssertCheck.notNull(getFeatureFlagService(), "Feature flag service is not set.");
        AssertCheck.notNull(featureFlag, "Feature flag service is not set.");

        return getFeatureFlagService().isFeatureFlagEnabled(featureFlag, EndUserUtil.getPartyId());
    }

    protected abstract FeatureFlagService getFeatureFlagService();

}
