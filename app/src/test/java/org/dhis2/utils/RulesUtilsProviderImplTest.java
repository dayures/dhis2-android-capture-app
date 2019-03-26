package org.dhis2.utils;

import org.dhis2.data.forms.dataentry.fields.FieldViewModel;
import org.dhis2.data.forms.dataentry.fields.FieldViewModelFactoryImpl;
import org.hisp.dhis.android.core.common.ObjectStyle;
import org.hisp.dhis.android.core.common.ValueType;
import org.hisp.dhis.rules.models.RuleActionDisplayText;
import org.hisp.dhis.rules.models.RuleActionHideField;
import org.hisp.dhis.rules.models.RuleActionShowError;
import org.hisp.dhis.rules.models.RuleActionShowWarning;
import org.hisp.dhis.rules.models.RuleEffect;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.google.common.truth.Truth.assertThat;


/**
 * QUADRAM. Created by ppajuelo on 07/11/2018.
 */
public class RulesUtilsProviderImplTest {

    private String testUid = "XXXXXX";
    private RulesUtilsProviderImpl ruleUtils = new RulesUtilsProviderImpl();
    private FieldViewModelFactoryImpl fieldFactory = new FieldViewModelFactoryImpl(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "");

    private List<RuleEffect> testRuleEffects = new ArrayList<>();

    private HashMap<String, FieldViewModel> testFieldViewModels = new HashMap<>();

    private RulesActionCallbacks actionCallbacks = new RulesActionCallbacks() {

        @Override
        public void setCalculatedValue(String calculatedValueVariable, String value) {
            // unused
        }

        @Override
        public void setShowError(@NonNull RuleActionShowError showError, FieldViewModel model) {
            // unused
        }

        @Override
        public void unsupportedRuleAction() {
            // unused
        }

        @Override
        public void save(@NonNull String uid, @Nullable String value) {
            // unused
        }

        @Override
        public void setDisplayKeyValue(String label, String value) {
            // unused
        }

        @Override
        public void sethideSection(String sectionUid) {
            // unused
        }

        @Override
        public void setMessageOnComplete(String content, boolean canComplete) {
            // unused
        }

        @Override
        public void setHideProgramStage(String programStageUid) {
            // unused
        }

        @Override
        public void setOptionToHide(String optionUid) {
            // unused
        }

        @Override
        public void setOptionGroupToHide(String optionGroupUid) {
            // unused
        }
    };

    private void putFieldViewModel() {
        testFieldViewModels.put(testUid, fieldFactory.create(testUid, "label",
                ValueType.TEXT, false, "optionSet", "test", "section",
                null, true, null, null, null, 1,
                ObjectStyle.builder().build()));
    }

    @Test
    public void showWarningRuleActionTest() {
        testFieldViewModels.put(testUid, fieldFactory.create(testUid, "label",
                ValueType.TEXT, false, "", "test", null,
                null, true, null, null, null, null, null));

        putFieldViewModel();

        testRuleEffects.add(RuleEffect.create(
                RuleActionShowWarning.create("content", "action_data", testUid),
                "data")
        );
        Result<RuleEffect> ruleEffect = Result.success(testRuleEffects);

        ruleUtils.applyRuleEffects(testFieldViewModels, ruleEffect, actionCallbacks);

        Assert.assertNotNull(testFieldViewModels.get(testUid).warning());
    }

    @Test
    public void showErrorRuleActionTest() {

        putFieldViewModel();

        testRuleEffects.add(RuleEffect.create(
                RuleActionShowError.create("content", "action_data", testUid),
                "data")
        );
        Result<RuleEffect> ruleEffect = Result.success(testRuleEffects);

        ruleUtils.applyRuleEffects(testFieldViewModels, ruleEffect, actionCallbacks);

        Assert.assertNotNull(testFieldViewModels.get(testUid).error());
    }

    @Test
    public void hideFieldRuleActionTest() {

        putFieldViewModel();

        testRuleEffects.add(RuleEffect.create(
                RuleActionHideField.create("content", testUid),
                "data")
        );
        Result<RuleEffect> ruleEffect = Result.success(testRuleEffects);

        ruleUtils.applyRuleEffects(testFieldViewModels, ruleEffect, actionCallbacks);

        assertThat(testFieldViewModels).doesNotContainKey(testUid);

    }

    @Test
    public void displayTextRuleActionTest() {

        putFieldViewModel();

        testRuleEffects.add(RuleEffect.create(
                RuleActionDisplayText.createForIndicators("content", "data"),
                "data")
        );
        Result<RuleEffect> ruleEffect = Result.success(testRuleEffects);

        ruleUtils.applyRuleEffects(testFieldViewModels, ruleEffect, actionCallbacks);

        assertThat(testFieldViewModels).containsKey("content");
    }

    /*@Test
    public void displayKeyValuePairRuleActionTest() {

        putFieldViewModel();

        testRuleEffects.add(RuleEffect.create(
                RuleActionDisplayKeyValuePair.createForIndicators("content", "data"),
                "data")
        );
        Result<RuleEffect> ruleEffect = Result.success(testRuleEffects);

        ruleUtils.applyRuleEffects(testFieldViewModels, ruleEffect, actionCallbacks);

    }

    @Test
    public void hideSectionRuleActionTest() {

        putFieldViewModel();

        testRuleEffects.add(RuleEffect.create(
                RuleActionHideSection.create("section"),
                "data")
        );
        Result<RuleEffect> ruleEffect = Result.success(testRuleEffects);

        ruleUtils.applyRuleEffects(testFieldViewModels, ruleEffect, actionCallbacks);

        assertThat(testFieldViewModels).doesNotContainKey(testUid);

    }*/


}
