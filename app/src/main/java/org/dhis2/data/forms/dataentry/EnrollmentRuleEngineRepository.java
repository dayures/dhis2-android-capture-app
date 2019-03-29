package org.dhis2.data.forms.dataentry;

import android.database.Cursor;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.forms.FormRepository;
import org.dhis2.data.forms.RuleHelper;
import org.dhis2.utils.Result;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.option.Option;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramRule;
import org.hisp.dhis.android.core.program.ProgramRuleAction;
import org.hisp.dhis.android.core.program.ProgramRuleActionType;
import org.hisp.dhis.android.core.program.ProgramRuleVariable;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleAttributeValue;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleEnrollment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import static android.text.TextUtils.isEmpty;

public final class EnrollmentRuleEngineRepository implements RuleEngineRepository {
    private static final String QUERY_ENROLLMENT = "SELECT\n" +
            "  Enrollment.uid,\n" +
            "  Enrollment.incidentDate,\n" +
            "  Enrollment.enrollmentDate,\n" +
            "  Enrollment.status,\n" +
            "  Enrollment.organisationUnit,\n" +
            "  Program.displayName\n" +
            "FROM Enrollment\n" +
            "JOIN Program ON Program.uid = Enrollment.program\n" +
            "WHERE Enrollment.uid = ? \n" +
            "LIMIT 1;";

    private static final String QUERY_ATTRIBUTE_VALUES = "SELECT\n" +
            "  Field.id,\n" +
            "  Value.value,\n" +
            "  ProgramRuleVariable.useCodeForOptionSet,\n" +
            "  Option.code,\n" +
            "  Option.name\n" +
            "FROM (Enrollment INNER JOIN Program ON Program.uid = Enrollment.program)\n" +
            "  INNER JOIN (\n" +
            "      SELECT\n" +
            "        TrackedEntityAttribute.uid AS id,\n" +
            "        TrackedEntityAttribute.optionSet AS optionSet,\n" +
            "        ProgramTrackedEntityAttribute.program AS program\n" +
            "      FROM ProgramTrackedEntityAttribute INNER JOIN TrackedEntityAttribute\n" +
            "          ON TrackedEntityAttribute.uid = ProgramTrackedEntityAttribute.trackedEntityAttribute\n" +
            "    ) AS Field ON Field.program = Program.uid\n" +
            "  INNER JOIN TrackedEntityAttributeValue AS Value ON (\n" +
            "    Value.trackedEntityAttribute = Field.id\n" +
            "        AND Value.trackedEntityInstance = Enrollment.trackedEntityInstance)\n" +
            "  LEFT JOIN ProgramRuleVariable ON ProgramRuleVariable.trackedEntityAttribute = Field.id " +
            "  LEFT JOIN Option ON (Option.optionSet = Field.optionSet AND Option.code = Value.value) " +
            "WHERE Enrollment.uid = ? AND Value.value IS NOT NULL;";

    @NonNull
    private final BriteDatabase briteDatabase;

    @NonNull
    private final FormRepository formRepository;

    @NonNull
    private final String enrollmentUid;
    private final RuleEnrollment.Builder ruleEnrollmentBuilder;
    @NonNull
    private final D2 d2;

    private Map<String, ProgramRuleVariable> attrRuleVariableMap;
    private Map<String, RuleAttributeValue> ruleAttributeValueMap;
    private Map<String, List<Rule>> attributeRules = new HashMap<>();
    private String lastUpdatedAttr = null;

    private String getAttrValue(TrackedEntityAttributeValue attributeValue) {
        String value = attributeValue.value();
        TrackedEntityAttribute attr = d2.trackedEntityModule().trackedEntityAttributes.uid(attributeValue.trackedEntityAttribute()).withAllChildren().get();
        if (attr.optionSet() != null) {
            List<Option> options = d2.optionModule().optionSets.uid(attr.optionSet().uid()).withAllChildren().get().options();
            ProgramRuleVariable ruleVariable = attrRuleVariableMap.get(attr.uid());
            if (ruleVariable != null && (ruleVariable.useCodeForOptionSet() == null || !ruleVariable.useCodeForOptionSet())) {
                for (Option option : options) {
                    if (value.equals(option.code()))
                        value = option.displayName();
                }
            }
        }

        return value;
    }

    private Map<String, String> getAttrValueMap(List<TrackedEntityAttributeValue> attributeValueList, Program program) {
        Map<String, String> attrValueMap = new HashMap<>();
        for (TrackedEntityAttributeValue attributeValue : attributeValueList) {
            String uid = attributeValue.trackedEntityAttribute();
            String value = getAttrValue(attributeValue);
            attrValueMap.put(uid, value);
        }
        for (ProgramTrackedEntityAttribute prgAttr : program.programTrackedEntityAttributes()) {
            if (!attrValueMap.containsKey(prgAttr.uid()))
                attrValueMap.put(prgAttr.uid(), "");
        }
        return attrValueMap;
    }

    private void getAttrRuleVariableMap(Program program) {
        attrRuleVariableMap = new HashMap<>();
        for (ProgramRuleVariable ruleVariable : program.programRuleVariables()) {
            if (ruleVariable.trackedEntityAttribute() != null)
                attrRuleVariableMap.put(ruleVariable.trackedEntityAttribute().uid(), ruleVariable);
        }
    }

    public EnrollmentRuleEngineRepository(
            @NonNull BriteDatabase briteDatabase,
            @NonNull FormRepository formRepository,
            @NonNull String enrollmentUid, D2 d2) {
        this.d2 = d2;
        this.briteDatabase = briteDatabase;
        this.formRepository = formRepository;
        this.enrollmentUid = enrollmentUid;
        this.ruleAttributeValueMap = new HashMap<>();

        Enrollment enrollment = d2.enrollmentModule().enrollments.uid(enrollmentUid).withAllChildren().get();
        OrganisationUnit ou = d2.organisationUnitModule().organisationUnits.uid(enrollment.organisationUnit()).get();
        Program program = d2.programModule().programs.uid(enrollment.program()).withAllChildren().get();
        List<TrackedEntityAttributeValue> attributeValueList = d2.trackedEntityModule().trackedEntityAttributeValues
                .byTrackedEntityInstance().eq(enrollment.trackedEntityInstance())
                .withAllChildren().get();

        getAttrRuleVariableMap(program);

        Map<String, String> attrValueMap = getAttrValueMap(attributeValueList, program);

        initRuleAttributeMap(attrValueMap);

        ruleEnrollmentBuilder = RuleEnrollment.builder()
                .enrollment(enrollment.uid())
                .incidentDate(enrollment.incidentDate() == null ? enrollment.enrollmentDate() : enrollment.incidentDate())
                .enrollmentDate(enrollment.enrollmentDate())
                .status(RuleEnrollment.Status.valueOf(enrollment.status().name()))
                .organisationUnit(enrollment.organisationUnit())
                .organisationUnitCode(ou.code())
                .programName(program.displayName());

        loadAttrRules(program.uid());
    }

    private List<ProgramRule> getMandatoryRules(Iterator<ProgramRule> ruleIterator) {
        List<ProgramRule> mandatoryRules = new ArrayList<>();
        while (ruleIterator.hasNext()) {
            ProgramRule rule = ruleIterator.next();
            if (rule.condition() == null)
                ruleIterator.remove();
            for (ProgramRuleAction action : rule.programRuleActions())
                if ((action.programRuleActionType() == ProgramRuleActionType.HIDEFIELD ||
                        action.programRuleActionType() == ProgramRuleActionType.HIDESECTION ||
                        action.programRuleActionType() == ProgramRuleActionType.ASSIGN ||
                        action.programRuleActionType() == ProgramRuleActionType.SHOWWARNING ||
                        action.programRuleActionType() == ProgramRuleActionType.SHOWERROR ||
                        action.programRuleActionType() == ProgramRuleActionType.HIDEOPTIONGROUP ||
                        action.programRuleActionType() == ProgramRuleActionType.HIDEOPTION)
                        && (!mandatoryRules.contains(rule))) {
                    mandatoryRules.add(rule);
                }
        }
        return mandatoryRules;
    }

    private void getAttributeRules(List<ProgramRule> rules, List<ProgramRuleVariable> variables, List<ProgramRule> mandatoryRules) {
        for (ProgramRuleVariable variable : variables) {
            if (variable.trackedEntityAttribute() != null && !attributeRules.containsKey(variable.trackedEntityAttribute().uid()))
                attributeRules.put(variable.trackedEntityAttribute().uid(), RuleHelper.trasformToRule(mandatoryRules));
            for (ProgramRule rule : rules) {
                if (rule.condition().contains(variable.displayName()) || actionsContainsAttr(rule.programRuleActions(), variable.displayName())) {
                    if (attributeRules.get(variable.trackedEntityAttribute().uid()) == null)
                        attributeRules.put(variable.trackedEntityAttribute().uid(), RuleHelper.trasformToRule(mandatoryRules));
                    attributeRules.get(variable.trackedEntityAttribute().uid()).add(RuleHelper.trasformToRule(rule));
                }
            }
        }
    }

    private void loadAttrRules(String programUid) {
        List<ProgramRule> rules = d2.programModule().programRules.byProgramUid().eq(programUid).withAllChildren().get();
        Iterator<ProgramRule> ruleIterator = rules.iterator();
        List<ProgramRule> mandatoryRules = getMandatoryRules(ruleIterator);
        List<ProgramRuleVariable> variables = d2.programModule().programRuleVariables
                .byProgramUid().eq(programUid)
                .withAllChildren().get();
        Iterator<ProgramRuleVariable> variableIterator = variables.iterator();
        while (variableIterator.hasNext()) {
            ProgramRuleVariable variable = variableIterator.next();
            if (variable.trackedEntityAttribute() == null)
                variableIterator.remove();
        }
        getAttributeRules(rules, variables, mandatoryRules);
    }

    private boolean actionsContainsAttr(List<ProgramRuleAction> programRuleActions, String variableName) {
        boolean actionContainsDe = false;
        for (ProgramRuleAction ruleAction : programRuleActions) {
            if (ruleAction.data() != null && ruleAction.data().contains(variableName))
                actionContainsDe = true;

        }
        return actionContainsDe;
    }

    private void initRuleAttributeMap(Map<String, String> attrValueMap) {
        for (Map.Entry<String, String> attrValueEntry : attrValueMap.entrySet()) {
            ruleAttributeValueMap.put(attrValueEntry.getKey(),
                    RuleAttributeValue.create(attrValueEntry.getKey(), attrValueEntry.getValue()));
        }
    }

    @Override
    public void updateRuleAttributeMap(String uid, String value) {
        lastUpdatedAttr = uid;
        TrackedEntityAttribute attr = d2.trackedEntityModule().trackedEntityAttributes.uid(uid).withAllChildren().get();
        if (attr.optionSet() != null) {
            ProgramRuleVariable ruleVariable = attrRuleVariableMap.get(attr.uid());
            if (ruleVariable != null && (ruleVariable.useCodeForOptionSet() == null || !ruleVariable.useCodeForOptionSet())) {
                for (Option option : attr.optionSet().options()) {
                    if (value.equals(option.code()))
                        value = option.displayName();
                }
            }
        }
        ruleAttributeValueMap.put(uid, RuleAttributeValue.create(uid, value));
    }


    @NonNull
    @Override
    public Flowable<Result<RuleEffect>> calculate() {
        return Flowable.defer(() -> Flowable.just(new ArrayList<>(ruleAttributeValueMap.values())))
                .map(attrs -> ruleEnrollmentBuilder.attributeValues(attrs).build())
                .switchMap(enrollment -> formRepository.ruleEngine()
                        .switchMap(ruleEngine -> {
                            if (isEmpty(lastUpdatedAttr))
                                return Flowable.fromCallable(ruleEngine.evaluate(enrollment));
                            else if (attributeRules.get(lastUpdatedAttr) != null && !attributeRules.get(lastUpdatedAttr).isEmpty())
                                return Flowable.fromCallable(ruleEngine.evaluate(enrollment, getRulesFor(lastUpdatedAttr)));
                            else
                                return Flowable.just(new ArrayList<RuleEffect>());
                        })
                        .map(Result::success)
                        .onErrorReturn(error -> Result.failure(new Exception(error)))
                );
    }

    private List<Rule> getRulesFor(String lastUpdatedAttr) {
        return attributeRules.get(lastUpdatedAttr);
    }

    @NonNull
    private Flowable<RuleEnrollment> queryEnrollment(
            @NonNull List<RuleAttributeValue> attributeValues) {
        return briteDatabase.createQuery(SqlConstants.ENROLLMENT_TABLE, QUERY_ENROLLMENT, enrollmentUid)
                .mapToOne(cursor -> {
                    Date enrollmentDate = parseDate(cursor.getString(2));
                    Date incidentDate = cursor.isNull(1) ?
                            enrollmentDate : parseDate(cursor.getString(1));
                    RuleEnrollment.Status status = RuleEnrollment.Status
                            .valueOf(cursor.getString(3));
                    String orgUnit = cursor.getString(4);
                    String programName = cursor.getString(5);
                    String ouCode = getOrgUnitCode(orgUnit);

                    return RuleEnrollment.create(cursor.getString(0),
                            incidentDate, enrollmentDate, status, orgUnit, ouCode, attributeValues, programName);
                }).toFlowable(BackpressureStrategy.LATEST);
    }

    @NonNull
    private Flowable<List<RuleAttributeValue>> queryAttributeValues() {
        return briteDatabase.createQuery(Arrays.asList(SqlConstants.ENROLLMENT_TABLE,
                SqlConstants.TE_ATTR_VALUE_TABLE), QUERY_ATTRIBUTE_VALUES, enrollmentUid)
                .mapToList(cursor -> {
                            String value = cursor.getString(1);
                            boolean useCode = cursor.getInt(2) == 1;
                            String optionCode = cursor.getString(3);
                            String optionName = cursor.getString(4);
                            if (!isEmpty(optionCode) && !isEmpty(optionName))
                                value = useCode ? optionCode : optionName;
                            return RuleAttributeValue.create(cursor.getString(0), value);
                        }
                ).toFlowable(BackpressureStrategy.LATEST);
    }

    @NonNull
    private static Date parseDate(@NonNull String date) throws ParseException {
        return BaseIdentifiableObject.DATE_FORMAT.parse(date);
    }

    @Nonnull
    private String getOrgUnitCode(String orgUnitUid) {
        String ouCode = "";
        try (Cursor cursor = briteDatabase.query("SELECT code FROM OrganisationUnit WHERE uid = ? LIMIT 1", orgUnitUid)) {
            if (cursor != null && cursor.moveToFirst()) {
                ouCode = cursor.getString(0);
            }
        }

        return ouCode;
    }
}
