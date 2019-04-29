package org.dhis2.data.forms;

import android.content.ContentValues;
import android.database.Cursor;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.forms.dataentry.fields.FieldViewModel;
import org.dhis2.data.forms.dataentry.fields.FieldViewModelFactoryImpl;
import org.dhis2.data.forms.dataentry.fields.FieldViewModelHelper;
import org.dhis2.data.tuples.Pair;
import org.dhis2.data.tuples.Trio;
import org.dhis2.utils.DateUtils;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.common.ObjectStyle;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.common.ValueTypeDeviceRendering;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.rules.RuleEngine;
import org.hisp.dhis.rules.RuleEngineContext;
import org.hisp.dhis.rules.RuleExpressionEvaluator;
import org.hisp.dhis.rules.models.TriggerEnvironment;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;
import static org.dhis2.utils.SqlConstants.CAT_COMBO_TABLE;
import static org.dhis2.utils.SqlConstants.CAT_OPTION_COMBO_TABLE;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_TABLE;
import static org.dhis2.utils.SqlConstants.EQUAL;
import static org.dhis2.utils.SqlConstants.EVENT_ATTR_OPTION_COMBO;
import static org.dhis2.utils.SqlConstants.EVENT_DATE;
import static org.dhis2.utils.SqlConstants.EVENT_PROGRAM;
import static org.dhis2.utils.SqlConstants.EVENT_STATE;
import static org.dhis2.utils.SqlConstants.EVENT_STATUS;
import static org.dhis2.utils.SqlConstants.EVENT_TABLE;
import static org.dhis2.utils.SqlConstants.EVENT_TEI;
import static org.dhis2.utils.SqlConstants.EVENT_UID;
import static org.dhis2.utils.SqlConstants.FROM;
import static org.dhis2.utils.SqlConstants.LIMIT_1;
import static org.dhis2.utils.SqlConstants.PROGRAM_STAGE_SECTION_TABLE;
import static org.dhis2.utils.SqlConstants.PROGRAM_STAGE_TABLE;
import static org.dhis2.utils.SqlConstants.PROGRAM_TABLE;
import static org.dhis2.utils.SqlConstants.PROGRAM_UID;
import static org.dhis2.utils.SqlConstants.QUESTION_MARK;
import static org.dhis2.utils.SqlConstants.SELECT;
import static org.dhis2.utils.SqlConstants.TEI_DATA_VALUE_TABLE;
import static org.dhis2.utils.SqlConstants.WHERE;

@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals"
})
public class EventRepository implements FormRepository {
    private static final List<String> TITLE_TABLES = Arrays.asList(
            PROGRAM_TABLE, PROGRAM_STAGE_TABLE);

    private static final List<String> SECTION_TABLES = Arrays.asList(
            EVENT_TABLE, PROGRAM_TABLE, PROGRAM_STAGE_TABLE, PROGRAM_STAGE_SECTION_TABLE);

    private static final String SELECT_PROGRAM = "SELECT Program.*\n" +
            "FROM Program JOIN Event ON Event.program = Program.uid \n" +
            "WHERE Event.uid =?\n" +
            "LIMIT 1;";

    private static final String SELECT_PROGRAM_FROM_EVENT = String.format(
            "SELECT %s.* from %s JOIN %s " +
                    "ON %s.%s = %s.%s " +
                    "WHERE %s.%s = ? LIMIT 1",
            PROGRAM_TABLE, PROGRAM_TABLE, EVENT_TABLE,
            EVENT_TABLE, EVENT_PROGRAM, PROGRAM_TABLE, PROGRAM_UID,
            EVENT_TABLE, EVENT_UID);

    private static final String SELECT_TITLE = SELECT + "\n" +
            "  Program.displayName,\n" +
            "  ProgramStage.displayName\n" +
            FROM + EVENT_TABLE + "\n" +
            "  JOIN Program ON Event.program = Program.uid\n" +
            "  JOIN ProgramStage ON Event.programStage = ProgramStage.uid\n" +
            WHERE + EVENT_TABLE + "." + EVENT_UID + EQUAL + QUESTION_MARK +
            LIMIT_1;

    private static final String SELECT_SECTIONS = SELECT + "\n" +
            "  Program.uid AS programUid,\n" +
            "  ProgramStage.uid AS programStageUid,\n" +
            "  ProgramStageSection.uid AS programStageSectionUid,\n" +
            "  ProgramStageSection.displayName AS programStageDisplayName,\n" +
            "  ProgramStageSection.mobileRenderType AS renderType,\n" +
            "  ProgramStageSection.sortOrder AS sectionOrder\n" +
            FROM + EVENT_TABLE + "\n" +
            "  JOIN Program ON Event.program = Program.uid\n" +
            "  JOIN ProgramStage ON Event.programStage = ProgramStage.uid\n" +
            "  LEFT OUTER JOIN ProgramStageSection ON ProgramStageSection.programStage = Event.programStage\n" +
            "WHERE Event.uid = ? ORDER BY ProgramStageSection.sortOrder";

    private static final String SELECT_EVENT_STATUS = SELECT + "\n" +
            "  Event.status\n" +
            FROM + EVENT_TABLE + "\n" +
            WHERE + EVENT_TABLE + "." + EVENT_UID + EQUAL + QUESTION_MARK +
            LIMIT_1;

    private static final String QUERY = SELECT + "\n" +
            "  Field.id,\n" +
            "  Field.label,\n" +
            "  Field.type,\n" +
            "  Field.mandatory,\n" +
            "  Field.optionSet,\n" +
            "  Value.value,\n" +
            "  Option.displayName,\n" +
            "  Field.section,\n" +
            "  Field.allowFutureDate,\n" +
            "  Event.status,\n" +
            "  Field.formLabel,\n" +
            "  Field.displayDescription,\n" +
            "  Field.formOrder,\n" +
            "  Field.sectionOrder\n" +
            FROM + EVENT_TABLE + "\n" +
            "  LEFT OUTER JOIN (\n" +
            "      SELECT\n" +
            "        DataElement.displayName AS label,\n" +
            "        DataElement.displayFormName AS formLabel,\n" +
            "        DataElement.valueType AS type,\n" +
            "        DataElement.uid AS id,\n" +
            "        DataElement.optionSet AS optionSet,\n" +
            "        ProgramStageDataElement.sortOrder AS formOrder,\n" +
            "        ProgramStageDataElement.programStage AS stage,\n" +
            "        ProgramStageDataElement.compulsory AS mandatory,\n" +
            "        ProgramStageSectionDataElementLink.programStageSection AS section,\n" +
            "        ProgramStageDataElement.allowFutureDate AS allowFutureDate,\n" +
            "        DataElement.displayDescription AS displayDescription,\n" +
            "        ProgramStageSectionDataElementLink.sortOrder AS sectionOrder\n" +
            "      FROM ProgramStageDataElement\n" +
            "        INNER JOIN DataElement ON DataElement.uid = ProgramStageDataElement.dataElement\n" +
            "        LEFT JOIN ProgramStageSection ON ProgramStageSection.programStage = ProgramStageDataElement.programStage\n" +
            "        LEFT JOIN ProgramStageSectionDataElementLink ON ProgramStageSectionDataElementLink.programStageSection = ProgramStageSection.uid AND ProgramStageSectionDataElementLink.dataElement = DataElement.uid\n" +
            "    ) AS Field ON (Field.stage = Event.programStage)\n" +
            "  LEFT OUTER JOIN TrackedEntityDataValue AS Value ON (\n" +
            "    Value.event = Event.uid AND Value.dataElement = Field.id\n" +
            "  )\n" +
            "  LEFT OUTER JOIN Option ON (\n" +
            "    Field.optionSet = Option.optionSet AND Value.value = Option.code\n" +
            "  )\n" +
            " %s  " +
            "ORDER BY CASE" +
            " WHEN Field.sectionOrder IS NULL THEN Field.formOrder" +
            " WHEN Field.sectionOrder IS NOT NULL THEN Field.sectionOrder" +
            " END ASC;";

    @NonNull
    private final BriteDatabase briteDatabase;

    @NonNull
    private Flowable<RuleEngine> cachedRuleEngineFlowable;

    @Nullable
    private final String eventUid;
    private final D2 d2;
    private final RulesRepository rulesRepository;
    private final RuleExpressionEvaluator evaluator;
    private String programUid;

    public EventRepository(@NonNull BriteDatabase briteDatabase,
                           @NonNull RuleExpressionEvaluator evaluator,
                           @NonNull RulesRepository rulesRepository,
                           @Nullable String eventUid,
                           @NonNull D2 d2) {
        this.d2 = d2;
        this.briteDatabase = briteDatabase;
        this.eventUid = eventUid;
        this.rulesRepository = rulesRepository;
        this.evaluator = evaluator;
        // We don't want to rebuild RuleEngine on each request, since metadata of
        // the event is not changing throughout lifecycle of FormComponent.
        this.cachedRuleEngineFlowable = eventProgram()
                .switchMap(program -> Flowable.zip(
                        rulesRepository.rulesNew(program),
                        rulesRepository.ruleVariables(program),
                        rulesRepository.otherEvents(eventUid),
                        rulesRepository.enrollment(eventUid),
                        rulesRepository.queryConstants(),
                        (rules, variables, events, enrollment, constants) -> {

                            RuleEngine.Builder builder = RuleEngineContext.builder(evaluator)
                                    .rules(rules)
                                    .ruleVariables(variables)
                                    .constantsValue(constants)
                                    .calculatedValueMap(new HashMap<>())
                                    .supplementaryData(new HashMap<>())
                                    .build().toEngineBuilder();
                            builder.triggerEnvironment(TriggerEnvironment.ANDROIDCLIENT);
                            builder.events(events);
                            if (!isEmpty(enrollment.enrollment()))
                                builder.enrollment(enrollment);
                            return builder.build();
                        }))
                .cacheWithInitialCapacity(1);
    }


    @Override
    public Flowable<RuleEngine> restartRuleEngine() {
        return this.cachedRuleEngineFlowable = eventProgram()
                .switchMap(program -> Flowable.zip(
                        rulesRepository.rulesNew(program),
                        rulesRepository.ruleVariables(program),
                        rulesRepository.otherEvents(eventUid),
                        rulesRepository.enrollment(eventUid),
                        rulesRepository.queryConstants(),
                        (rules, variables, events, enrollment, constants) -> {

                            RuleEngine.Builder builder = RuleEngineContext.builder(evaluator)
                                    .rules(rules)
                                    .ruleVariables(variables)
                                    .constantsValue(constants)
                                    .calculatedValueMap(new HashMap<>())
                                    .supplementaryData(new HashMap<>())
                                    .build().toEngineBuilder();
                            builder.triggerEnvironment(TriggerEnvironment.ANDROIDCLIENT);
                            builder.events(events);
                            if (!isEmpty(enrollment.enrollment()))
                                builder.enrollment(enrollment);
                            return builder.build();
                        }))
                .cacheWithInitialCapacity(1);
    }

    @NonNull
    @Override
    public Flowable<RuleEngine> ruleEngine() {
        return cachedRuleEngineFlowable;
    }

    @NonNull
    @Override
    public Flowable<String> title() {
        return briteDatabase
                .createQuery(TITLE_TABLES, SELECT_TITLE, eventUid == null ? "" : eventUid)
                .mapToOne(cursor -> cursor.getString(0) + " - " + cursor.getString(1)).toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged();
    }

    @NonNull
    @Override
    public Flowable<Pair<Program, String>> reportDate() {
        return briteDatabase.createQuery(PROGRAM_TABLE, SELECT_PROGRAM, eventUid == null ? "" : eventUid)
                .mapToOne(Program::create)
                .map(programModel -> Pair.create(programModel, ""))
                .toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged();
    }

    @NonNull
    @Override
    public Flowable<Pair<Program, String>> incidentDate() {
        return briteDatabase.createQuery(PROGRAM_TABLE, SELECT_PROGRAM, eventUid == null ? "" : eventUid)
                .mapToOne(Program::create)
                .map(programModel -> Pair.create(programModel, ""))
                .toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged();
    }

    @Override
    public Flowable<Program> getAllowDatesInFuture() {
        return briteDatabase.createQuery(PROGRAM_TABLE, SELECT_PROGRAM_FROM_EVENT, eventUid == null ? "" : eventUid)
                .mapToOne(Program::create)
                .toFlowable(BackpressureStrategy.LATEST);
    }


    @NonNull
    @Override
    public Flowable<ReportStatus> reportStatus() {
        return briteDatabase
                .createQuery(EVENT_TABLE, SELECT_EVENT_STATUS, eventUid == null ? "" : eventUid)
                .mapToOne(cursor -> ReportStatus.fromEventStatus(EventStatus.valueOf(cursor.getString(0)))).toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged();
    }

    @NonNull
    @Override
    public Flowable<List<FormSectionViewModel>> sections() {
        return briteDatabase
                .createQuery(SECTION_TABLES, SELECT_SECTIONS, eventUid == null ? "" : eventUid)
                .mapToList(cursor -> mapToFormSectionViewModels(eventUid == null ? "" : eventUid, cursor))
                .distinctUntilChanged().toFlowable(BackpressureStrategy.LATEST);
    }

    @NonNull
    @Override
    public Consumer<String> storeReportDate() {
        return reportDate -> {
            Calendar cal = DateUtils.getCalendarFromDate(reportDate);
            ContentValues event = new ContentValues();
            event.put(EVENT_DATE, DateUtils.databaseDateFormat().format(cal.getTime()));
            event.put(EVENT_STATE, State.TO_UPDATE.name()); // TODO: Check if state is TO_POST
            // TODO: and if so, keep the TO_POST state

            updateProgramTable(Calendar.getInstance().getTime(), programUid);

            briteDatabase.update(EVENT_TABLE, event, EVENT_UID + " = ?", eventUid == null ? "" : eventUid);
        };
    }

    @NonNull
    @Override
    public Consumer<String> storeIncidentDate() {
        return data -> {
            //incident date is only for tracker events
        };
    }

    @NonNull
    @Override
    public Consumer<LatLng> storeCoordinates() {
        return data -> {
            //coordinates are only for tracker events
        };
    }

    @NonNull
    @Override
    public Consumer<ReportStatus> storeReportStatus() {
        return reportStatus -> {
            ContentValues event = new ContentValues();
            event.put(EVENT_STATUS, ReportStatus.toEventStatus(reportStatus).name());
            event.put(EVENT_STATE, State.TO_UPDATE.name()); // TODO: Check if state is TO_POST
            // TODO: and if so, keep the TO_POST state

            updateProgramTable(Calendar.getInstance().getTime(), programUid);

            briteDatabase.update(EVENT_TABLE, event, EVENT_UID + " = ?", eventUid == null ? "" : eventUid);
        };
    }

    @Nullable
    @Override
    public Observable<Trio<String, String, String>> useFirstStageDuringRegistration() {
        return Observable.just(null);
    }

    @Nullable
    @Override
    public Observable<String> autoGenerateEvents(String enrollmentUid) {
        return null;
    }

    @NonNull
    @Override
    public Observable<List<FieldViewModel>> fieldValues() {
        String where = String.format(Locale.US, "WHERE Event.uid = '%s'", eventUid == null ? "" : eventUid);
        return briteDatabase.createQuery(TEI_DATA_VALUE_TABLE, String.format(Locale.US, QUERY, where))
                .mapToList(this::transform);
    }

    @Override
    public void deleteTrackedEntityAttributeValues(@NonNull String trackedEntityInstanceId) {
        // not necessary
    }

    @Override
    public void deleteEnrollment(@NonNull String trackedEntityInstanceId) {
        // not necessary
    }

    @Override
    public void deleteEvent() {
        String deleteWhereRelationship = String.format(
                "%s.%s = ",
                EVENT_TABLE, EVENT_UID);
        String id = eventUid == null ? "" : eventUid;
        briteDatabase.delete(EVENT_TABLE, deleteWhereRelationship + "'" + id + "'");
    }

    @Override
    public void deleteTrackedEntityInstance(@NonNull String trackedEntityInstanceId) {
        // not necessary
    }

    @NonNull
    @Override
    public Observable<String> getTrackedEntityInstanceUid() {
        String selectTe = "SELECT " + EVENT_TABLE + "." + EVENT_TEI +
                " FROM " + EVENT_TABLE +
                " WHERE " + EVENT_UID + " = ? LIMIT 1";
        return briteDatabase.createQuery(ENROLLMENT_TABLE, selectTe, eventUid == null ? "" : eventUid).mapToOne(cursor -> cursor.getString(0));
    }

    @Override
    public Observable<Trio<Boolean, CategoryCombo, List<CategoryOptionCombo>>> getProgramCategoryCombo() {
        return briteDatabase.createQuery(EVENT_TABLE, "SELECT * FROM Event WHERE Event.uid = ?", eventUid)
                .mapToOne(Event::create)
                .flatMap(eventModel -> briteDatabase.createQuery(CAT_COMBO_TABLE, "SELECT CategoryCombo.* FROM CategoryCombo " +
                        "JOIN Program ON Program.categoryCombo = CategoryCombo.uid WHERE Program.uid = ?", eventModel.program())
                        .mapToOne(CategoryCombo::create)
                        .flatMap(categoryComboModel ->
                                briteDatabase.createQuery(CAT_OPTION_COMBO_TABLE, "SELECT * FROM CategoryOptionCombo " +
                                        "WHERE categoryCombo = ?", categoryComboModel.uid())
                                        .mapToList(CategoryOptionCombo::create)
                                        .map(categoryOptionComboModels -> {
                                            boolean eventHastOptionSelected = false;
                                            for (CategoryOptionCombo options : categoryOptionComboModels) {
                                                if (eventModel.attributeOptionCombo() != null && eventModel.attributeOptionCombo().equals(options.uid()))
                                                    eventHastOptionSelected = true;
                                            }
                                            return Trio.create(eventHastOptionSelected, categoryComboModel, categoryOptionComboModels);
                                        })
                        )
                );

    }

    @Override
    public void saveCategoryOption(CategoryOptionCombo selectedOption) {
        ContentValues event = new ContentValues();
        event.put(EVENT_ATTR_OPTION_COMBO, selectedOption.uid());
        event.put(EVENT_STATE, State.TO_UPDATE.name()); // TODO: Check if state is TO_POST
        // TODO: and if so, keep the TO_POST state

        briteDatabase.update(EVENT_TABLE, event, EVENT_UID + " = ?", eventUid == null ? "" : eventUid);
    }

    @Override
    public Observable<Boolean> captureCoodinates() {
        return briteDatabase.createQuery("ProgramStage", "SELECT ProgramStage.captureCoordinates FROM ProgramStage " +
                "JOIN Event ON Event.programStage = ProgramStage.uid WHERE Event.uid = ?", eventUid)
                .mapToOne(cursor -> cursor.getInt(0) == 1);
    }

    @Override
    public Observable<OrganisationUnit> getOrgUnitDates() {
        return Observable.defer(() -> Observable.just(d2.eventModule().events.uid(eventUid).get()))
                .switchMap(event -> Observable.just(d2.organisationUnitModule().organisationUnits.uid(event.organisationUnit()).get()));
    }

    @NonNull
    private FieldViewModel transform(@NonNull Cursor cursor) {
        FieldViewModelHelper fieldViewModelHelper = FieldViewModelHelper.createFromCursor(cursor);
        EventStatus status = EventStatus.valueOf(cursor.getString(9));

        int optionCount = 0;
        if (fieldViewModelHelper.getOptionSetUid() != null) {
            try (Cursor countCursor = briteDatabase.query("SELECT COUNT (uid) FROM Option WHERE optionSet = ?", fieldViewModelHelper.getOptionSetUid())) {
                if (countCursor != null && countCursor.moveToFirst()) {
                    optionCount = countCursor.getInt(0);
                }
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        ValueTypeDeviceRendering fieldRendering = null;
        try (Cursor rendering = briteDatabase.query("SELECT * FROM ValueTypeDeviceRendering WHERE uid = ?", fieldViewModelHelper.getUid())) {
            if (rendering != null && rendering.moveToFirst()) {
                fieldRendering = ValueTypeDeviceRendering.create(rendering);
            }
        }

        FieldViewModelFactoryImpl fieldFactory = new FieldViewModelFactoryImpl(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "");
        ObjectStyle objectStyle = ObjectStyle.builder().build();
        try (Cursor objStyleCursor = briteDatabase.query("SELECT * FROM ObjectStyle WHERE uid = ?", fieldViewModelHelper.getUid())) {
            if (objStyleCursor.moveToFirst())
                objectStyle = ObjectStyle.create(objStyleCursor);
        }
        return fieldFactory.create(fieldViewModelHelper.getUid(), isEmpty(fieldViewModelHelper.getFormLabel()) ?
                        fieldViewModelHelper.getLabel() : fieldViewModelHelper.getFormLabel(), fieldViewModelHelper.getValueType(),
                fieldViewModelHelper.isMandatory(), fieldViewModelHelper.getOptionSetUid(), fieldViewModelHelper.getDataValue(),
                fieldViewModelHelper.getSection(), fieldViewModelHelper.getAllowFutureDates(),
                status == EventStatus.ACTIVE, null, fieldViewModelHelper.getDescription(), fieldRendering, optionCount, objectStyle);
    }

    @NonNull
    private Flowable<String> eventProgram() {
        return briteDatabase.createQuery(EVENT_TABLE, SELECT_PROGRAM, eventUid == null ? "" : eventUid)
                .mapToOne(Program::create)
                .map(programModel -> {
                    programUid = programModel.uid();
                    return programUid;
                }).toFlowable(BackpressureStrategy.LATEST);
    }

    @NonNull
    private FormSectionViewModel mapToFormSectionViewModels(@NonNull String eventUid, @NonNull Cursor cursor) {
        if (cursor.getString(2) == null) {
            // This programstage has no sections
            return FormSectionViewModel.createForProgramStage(
                    eventUid, cursor.getString(1));
        } else {
            // This programstage has sections
            return FormSectionViewModel.createForSection(
                    eventUid, cursor.getString(2), cursor.getString(3), cursor.getString(4));
        }
    }

    @SuppressWarnings({"squid:S1172", "squid:CommentedOutCodeLine"})
    private void updateProgramTable(Date lastUpdated, String programUid) {
        /*ContentValues program = new ContentValues();TODO: Crash if active
        program.put(EnrollmentModel.Columns.LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(lastUpdated));
        briteDatabase.update(PROGRAM_TABLE, program, PROGRAM_UID + " = ?", programUid);*/
    }
}