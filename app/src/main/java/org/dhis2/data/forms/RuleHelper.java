package org.dhis2.data.forms;

import android.database.Cursor;

import com.squareup.sqlbrite2.BriteDatabase;

import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.program.ProgramRule;
import org.hisp.dhis.android.core.program.ProgramRuleAction;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleDataValue;
import org.hisp.dhis.rules.models.RuleEvent;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;

public class RuleHelper {

    private RuleHelper() {
        // hide public constructor
    }

    public static Rule trasformToRule(ProgramRule rule) {
        return Rule.create(
                rule.programStage() != null ? rule.programStage().uid() : null,
                rule.priority(),
                rule.condition(),
                transformToRuleAction(rule.programRuleActions()),
                rule.displayName());
    }

    public static List<Rule> trasformToRule(List<ProgramRule> rules) {
        List<Rule> finalRules = new ArrayList<>();
        for (ProgramRule rule : rules) {
            finalRules.add(Rule.create(
                    rule.programStage() != null ? rule.programStage().uid() : null,
                    rule.priority(),
                    rule.condition(),
                    transformToRuleAction(rule.programRuleActions()),
                    rule.displayName()));
        }
        return finalRules;
    }

    public static List<RuleAction> transformToRuleAction(List<ProgramRuleAction> programRuleActions) {
        List<RuleAction> ruleActions = new ArrayList<>();
        if (programRuleActions != null)
            for (ProgramRuleAction programRuleAction : programRuleActions)
                ruleActions.add(createRuleAction(programRuleAction));
        return ruleActions;
    }

    public static RuleAction createRuleAction(ProgramRuleAction programRuleAction) {
        return RulesRepository.create(
                programRuleAction.programRuleActionType(),
                programRuleAction.programStage() != null ? programRuleAction.programStage().uid() : null,
                programRuleAction.programStageSection() != null ? programRuleAction.programStageSection().uid() : null,
                programRuleAction.trackedEntityAttribute() != null ? programRuleAction.trackedEntityAttribute().uid() : null,
                programRuleAction.dataElement() != null ? programRuleAction.dataElement().uid() : null,
                programRuleAction.location(),
                programRuleAction.content(),
                programRuleAction.data(),
                programRuleAction.option() != null ? programRuleAction.option().uid() : null,
                programRuleAction.optionGroup() != null ? programRuleAction.optionGroup().uid() : null);
    }

    public static RuleEvent createRuleEventFromCursor(BriteDatabase briteDatabase, Cursor cursor, @NonNull List<RuleDataValue> dataValues) throws ParseException {
        String eventUidAux = cursor.getString(0);
        String programStageUid = cursor.getString(1);
        RuleEvent.Status status = RuleEvent.Status.valueOf(cursor.getString(2));
        Date eventDate = parseDate(cursor.getString(3));
        Date dueDate = cursor.isNull(4) ? eventDate : parseDate(cursor.getString(4));
        String orgUnit = cursor.getString(5);
        String orgUnitCode = getOrgUnitCode(briteDatabase, orgUnit);
        String programStageName = cursor.getString(6);

        return RuleEvent.builder()
                .event(eventUidAux)
                .programStage(programStageUid)
                .programStageName(programStageName)
                .status(status)
                .eventDate(eventDate)
                .dueDate(dueDate)
                .organisationUnit(orgUnit)
                .organisationUnitCode(orgUnitCode)
                .dataValues(dataValues)
                .build();
    }

    @NonNull
    private static Date parseDate(@NonNull String date) throws ParseException {
        return BaseIdentifiableObject.DATE_FORMAT.parse(date);
    }

    @Nonnull
    private static String getOrgUnitCode(BriteDatabase briteDatabase, String orgUnitUid) {
        String ouCode = "";
        try (Cursor cursor = briteDatabase.query("SELECT code FROM OrganisationUnit WHERE uid = ? LIMIT 1", orgUnitUid)) {
            if (cursor != null && cursor.moveToFirst() && cursor.getString(0) != null) {
                ouCode = cursor.getString(0);
            }
        }
        return ouCode;
    }
}
