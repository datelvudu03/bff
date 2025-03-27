package cz.kb.leon.bff.servicing;

import cz.kb.leon.bff.servicing.util.ObjectUtil;
import cz.kb.leon.featureflags.FeatureFlagService;

import java.util.HashSet;
import java.util.Set;

public class TestFeatureFlagService implements FeatureFlagService {

    private final Set<String> enabledFeatureFlag = new HashSet<>();

    public void reset() {
        enabledFeatureFlag.clear();
    }

    public void enableFeatureFlag(String featureFlag) {
        enabledFeatureFlag.add(featureFlag);
    }

    public void enableFeatureFlag(String featureFlag, String userId) {
        enabledFeatureFlag.add(ObjectUtil.evaluateMessage("{}@{}", featureFlag, userId));
    }


    @Override
    public boolean isFeatureFlagEnabled(String featureFlag) {
        return enabledFeatureFlag.stream()
                .anyMatch(ff -> ff.startsWith(ObjectUtil.evaluateMessage("{}@", featureFlag)));
    }

    @Override
    public boolean isFeatureFlagEnabled(String featureFlag, String userId) {
        return enabledFeatureFlag.contains(ObjectUtil.evaluateMessage("{}@{}", featureFlag, userId));
    }

    @Override
    public Object getFeatureFlagValue(String featureFlag) {
        return null;
    }

    @Override
    public Object getFeatureFlagValue(String featureFlag, String userId) {
        return null;
    }

}