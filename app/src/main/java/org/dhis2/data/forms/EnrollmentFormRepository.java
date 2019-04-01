package org.dhis2.data.forms;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.forms.dataentry.fields.FieldViewModel;
import org.dhis2.data.forms.dataentry.fields.FieldViewModelFactoryImpl;
import org.dhis2.data.forms.dataentry.fields.FieldViewModelHelper;
import org.dhis2.data.tuples.Pair;
import org.dhis2.data.tuples.Trio;
import org.dhis2.utils.CodeGenerator;
import org.dhis2.utils.DateUtils;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.common.ObjectStyle;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.common.ValueTypeDeviceRendering;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityType;
import org.hisp.dhis.rules.RuleEngine;
import org.hisp.dhis.rules.RuleEngineContext;
import org.hisp.dhis.rules.RuleExpressionEvaluator;
import org.hisp.dhis.rules.models.TriggerEnvironment;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

import static org.dhis2.utils.SqlConstants.ENROLLMENT_ENROLLMENT_DATE;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_INCIDENT_DATE;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_LATITUDE;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_LONGITUDE;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_ORG_UNIT;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_PROGRAM;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_STATE;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_STATUS;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_TABLE;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_TEI;
import static org.dhis2.utils.SqlConstants.ENROLLMENT_UID;
import static org.dhis2.utils.SqlConstants.EQUAL;
import static org.dhis2.utils.SqlConstants.EVENT_TABLE;
import static org.dhis2.utils.SqlConstants.FROM;
import static org.dhis2.utils.SqlConstants.JOIN;
import static org.dhis2.utils.SqlConstants.LIMIT_1;
import static org.dhis2.utils.SqlConstants.ON;
import static org.dhis2.utils.SqlConstants.PROGRAM_STAGE_TABLE;
import static org.dhis2.utils.SqlConstants.PROGRAM_TABLE;
import static org.dhis2.utils.SqlConstants.PROGRAM_UID;
import static org.dhis2.utils.SqlConstants.QUESTION_MARK;
import static org.dhis2.utils.SqlConstants.SELECT;
import static org.dhis2.utils.SqlConstants.TABLE_FIELD_EQUALS;
import static org.dhis2.utils.SqlConstants.TEI_TABLE;
import static org.dhis2.utils.SqlConstants.TEI_UID;
import static org.dhis2.utils.SqlConstants.TE_ATTR_VALUE_TABLE;
import static org.dhis2.utils.SqlConstants.TE_ATTR_VALUE_TEI;
import static org.dhis2.utils.SqlConstants.WHERE;

@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals"
})
public class EnrollmentFormRepository implements FormRepository {
    private static final List<String> TITLE_TABLES = Arrays.asList(
            ENROLLMENT_TABLE, PROGRAM_TABLE);

    private static final String SELECT_TITLE = "SELECT Program.displayName\n" +
            FROM + ENROLLMENT_TABLE + "\n" +
            "   " + JOIN + PROGRAM_TABLE + ON + ENROLLMENT_TABLE + "." + ENROLLMENT_PROGRAM + EQUAL + PROGRAM_TABLE + "." + PROGRAM_UID + "\n" +
            WHERE + ENROLLMENT_TABLE + "." + ENROLLMENT_UID + EQUAL + QUESTION_MARK +
            LIMIT_1;

    private static final String SELECT_ENROLLMENT_UID = "SELECT Enrollment.uid\n" +
            FROM + ENROLLMENT_TABLE + "\n" +
            "WHERE Enrollment.uid = ?";

    private static final String SELECT_ENROLLMENT_STATUS = "SELECT Enrollment.status\n" +
            FROM + ENROLLMENT_TABLE + "\n" +
            WHERE + ENROLLMENT_TABLE + "." + ENROLLMENT_UID + EQUAL + QUESTION_MARK +
            LIMIT_1;

    private static final String SELECT_ENROLLMENT_DATE = "SELECT Enrollment.*\n" +
            FROM + ENROLLMENT_TABLE + "\n" +
            WHERE + ENROLLMENT_TABLE + "." + ENROLLMENT_UID + EQUAL + QUESTION_MARK +
            LIMIT_1;

    private static final String SELECT_ENROLLMENT_PROGRAM = "SELECT Program.*\n" +
            "FROM Program JOIN Enrollment ON Enrollment.program = Program.uid\n" +
            WHERE + ENROLLMENT_TABLE + "." + ENROLLMENT_UID + EQUAL + QUESTION_MARK +
            LIMIT_1;

    private static final String SELECT_INCIDENT_DATE = "SELECT Enrollment.* FROM Enrollment WHERE Enrollment.uid = ? LIMIT 1";

    private static final String SELECT_AUTO_GENERATE_PROGRAM_STAGE = SELECT +
            "ProgramStage.uid, " +
            "Program.uid, " +
            ENROLLMENT_TABLE + "." + ENROLLMENT_ORG_UNIT + ", " +
            "ProgramStage.minDaysFromStart, " +
            "ProgramStage.reportDateToUse, " +
            "Enrollment.incidentDate, " +
            "Enrollment.enrollmentDate, " +
            "ProgramStage.periodType, \n" +
            "ProgramStage.generatedByEnrollmentDate \n" +
            FROM + ENROLLMENT_TABLE + "\n" +
            "   " + JOIN + PROGRAM_TABLE + ON + ENROLLMENT_TABLE + "." + ENROLLMENT_PROGRAM + EQUAL + PROGRAM_TABLE + "." + PROGRAM_UID + "\n" +
            "  JOIN ProgramStage ON Program.uid = ProgramStage.program \n" +
            "WHERE Enrollment.uid = ? AND ProgramStage.autoGenerateEvent = 1";

    private static final String SELECT_PROGRAM = "SELECT \n" +
            "  program\n" +
            FROM + ENROLLMENT_TABLE + "\n" +
            "WHERE uid = ?\n" +
            "LIMIT 1;";

    private static final String SELECT_TE_TYPE = SELECT +
            "Program.uid," +
            "Enrollment.trackedEntityInstance\n" +
            "FROM Program\n" +
            "JOIN Enrollment ON Enrollment.program = Program.uid\n" +
            "WHERE Enrollment.uid = ? LIMIT 1";

    private static final String QUERY = "SELECT \n" +
            "  Field.id,\n" +
            "  Field.label,\n" +
            "  Field.type,\n" +
            "  Field.mandatory,\n" +
            "  Field.optionSet,\n" +
            "  Value.value,\n" +
            "  Option.displayName,\n" +
            "  Field.allowFutureDate,\n" +
            "  Field.generated,\n" +
            "  Enrollment.organisationUnit,\n" +
            "  Enrollment.status,\n" +
            "  Field.displayDescription\n" +
            "FROM (Enrollment INNER JOIN Program ON Program.uid = Enrollment.program)\n" +
            "  LEFT OUTER JOIN (\n" +
            "      SELECT\n" +
            "        TrackedEntityAttribute.uid AS id,\n" +
            "        TrackedEntityAttribute.displayName AS label,\n" +
            "        TrackedEntityAttribute.valueType AS type,\n" +
            "        TrackedEntityAttribute.optionSet AS optionSet,\n" +
            "        ProgramTrackedEntityAttribute.program AS program,\n" +
            "        ProgramTrackedEntityAttribute.mandatory AS mandatory,\n" +
            "        ProgramTrackedEntityAttribute.allowFutureDate AS allowFutureDate,\n" +
            "        TrackedEntityAttribute.generated AS generated,\n" +
            "        TrackedEntityAttribute.displayDescription AS displayDescription\n" +
            "      FROM ProgramTrackedEntityAttribute INNER JOIN TrackedEntityAttribute\n" +
            "          ON TrackedEntityAttribute.uid = ProgramTrackedEntityAttribute.trackedEntityAttribute\n" +
            "    ) AS Field ON Field.program = Program.uid\n" +
            "  LEFT OUTER JOIN TrackedEntityAttributeValue AS Value ON (\n" +
            "    Value.trackedEntityAttribute = Field.id\n" +
            "        AND Value.trackedEntityInstance = Enrollment.trackedEntityInstance)\n" +
            "  LEFT OUTER JOIN Option ON (\n" +
            "    Field.optionSet = Option.optionSet AND Value.value = Option.code\n" +
            "  )\n" +
            "WHERE Enrollment.uid = ?";
    private static final String CHECK_STAGE_IS_NOT_CREATED = "SELECT * FROM Event JOIN Enrollment ON Event.enrollment = Enrollment.uid WHERE Enrollment.uid = ? AND Event.programStage = ?";
    @NonNull
    private final BriteDatabase briteDatabase;

    @NonNull
    private final CodeGenerator codeGenerator;

    @NonNull
    private final Flowable<RuleEngine> cachedRuleEngineFlowable;

    @NonNull
    private final String enrollmentUid;
    private final D2 d2;

    private String programUid;

    public EnrollmentFormRepository(@NonNull BriteDatabase briteDatabase,
                                    @NonNull RuleExpressionEvaluator expressionEvaluator,
                                    @NonNull RulesRepository rulesRepository,
                                    @NonNull CodeGenerator codeGenerator,
                                    @NonNull String enrollmentUid,
                                    @NonNull D2 d2) {
        this.d2 = d2;
        this.briteDatabase = briteDatabase;
        this.codeGenerator = codeGenerator;
        this.enrollmentUid = enrollmentUid;

        // We don't want to rebuild RuleEngine on each request, since metadata of
        // the event is not changing throughout lifecycle of FormComponent.
        this.cachedRuleEngineFlowable = enrollmentProgram()
                .switchMap(program -> Flowable.zip(
                        rulesRepository.rulesNew(program),
                        rulesRepository.ruleVariables(program),
                        rulesRepository.enrollmentEvents(enrollmentUid),
                        rulesRepository.queryConstants(),
                        (rules, variables, events, constants) -> {
                            RuleEngine.Builder builder = RuleEngineContext.builder(expressionEvaluator)
                                    .rules(rules)
                                    .ruleVariables(variables)
                                    .calculatedValueMap(new HashMap<>())
                                    .supplementaryData(new HashMap<>())
                                    .constantsValue(constants)
                                    .build().toEngineBuilder();
                            builder.triggerEnvironment(TriggerEnvironment.ANDROIDCLIENT);
                            builder.events(events);
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
                .createQuery(TITLE_TABLES, SELECT_TITLE, enrollmentUid)
                .mapToOne(cursor -> cursor.getString(0)).toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged();
    }

    @NonNull
    @Override
    public Flowable<Pair<Program, String>> reportDate() {
        return briteDatabase.createQuery(PROGRAM_TABLE, SELECT_ENROLLMENT_PROGRAM, enrollmentUid)
                .mapToOne(Program::create)
                .flatMap(programModel -> briteDatabase.createQuery(ENROLLMENT_TABLE, SELECT_ENROLLMENT_DATE, enrollmentUid)
                        .mapToOne(Enrollment::create)
                        .map(enrollmentModel -> Pair.create(programModel, enrollmentModel.enrollmentDate() != null ?
                                DateUtils.uiDateFormat().format(enrollmentModel.enrollmentDate()) : "")))
                .toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged();
    }

    @NonNull
    @Override
    public Flowable<Pair<Program, String>> incidentDate() {
        return briteDatabase.createQuery(PROGRAM_TABLE, SELECT_ENROLLMENT_PROGRAM, enrollmentUid)
                .mapToOne(Program::create)
                .flatMap(programModel -> briteDatabase.createQuery(ENROLLMENT_TABLE, SELECT_INCIDENT_DATE, enrollmentUid)
                        .mapToOne(Enrollment::create)
                        .map(enrollmentModel -> Pair.create(programModel, enrollmentModel.incidentDate() != null ?
                                DateUtils.uiDateFormat().format(enrollmentModel.incidentDate()) : "")))
                .toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged();
    }

    @Override
    public Flowable<Program> getAllowDatesInFuture() {
        return briteDatabase.createQuery(PROGRAM_TABLE, SELECT_ENROLLMENT_PROGRAM, enrollmentUid)
                .mapToOne(Program::create)
                .toFlowable(BackpressureStrategy.LATEST);
    }

    @NonNull
    @Override
    public Flowable<ReportStatus> reportStatus() {
        return briteDatabase
                .createQuery(ENROLLMENT_TABLE, SELECT_ENROLLMENT_STATUS, enrollmentUid)
                .mapToOne(cursor ->
                        ReportStatus.fromEnrollmentStatus(EnrollmentStatus.valueOf(cursor.getString(0)))).toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged();
    }

    @NonNull
    @Override
    public Flowable<List<FormSectionViewModel>> sections() {
        return briteDatabase
                .createQuery(ENROLLMENT_TABLE, SELECT_ENROLLMENT_UID, enrollmentUid)
                .mapToList(cursor -> FormSectionViewModel
                        .createForEnrollment(cursor.getString(0))).toFlowable(BackpressureStrategy.LATEST);
    }

    @NonNull
    @Override
    public Consumer<String> storeReportDate() {
        return reportDate -> {
            Calendar cal = DateUtils.getCalendarFromDate(reportDate);
            ContentValues enrollment = new ContentValues();
            enrollment.put(ENROLLMENT_ENROLLMENT_DATE, DateUtils.databaseDateFormat().format(cal.getTime()));
            enrollment.put(ENROLLMENT_STATE, State.TO_UPDATE.name()); // TODO: Check if state is TO_POST
            // TODO: and if so, keep the TO_POST state

            briteDatabase.update(ENROLLMENT_TABLE, enrollment,
                    ENROLLMENT_UID + " = ?", enrollmentUid);
        };
    }

    @NonNull
    @Override
    public Consumer<LatLng> storeCoordinates() {
        return latLng -> {
            ContentValues enrollment = new ContentValues();
            enrollment.put(ENROLLMENT_LATITUDE, latLng.latitude);
            enrollment.put(ENROLLMENT_LONGITUDE, latLng.longitude); // TODO: Check if state is TO_POST
            // TODO: and if so, keep the TO_POST state

            briteDatabase.update(ENROLLMENT_TABLE, enrollment,
                    ENROLLMENT_UID + " = ?", enrollmentUid);
        };
    }

    @NonNull
    @Override
    public Consumer<String> storeIncidentDate() {
        return incidentDate -> {
            Calendar cal = Calendar.getInstance();
            Date date = DateUtils.databaseDateFormat().parse(incidentDate);
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            ContentValues enrollment = new ContentValues();
            enrollment.put(ENROLLMENT_INCIDENT_DATE, DateUtils.databaseDateFormat().format(cal.getTime()));
            enrollment.put(ENROLLMENT_STATE, State.TO_UPDATE.name()); // TODO: Check if state is TO_POST
            // TODO: and if so, keep the TO_POST state

            briteDatabase.update(ENROLLMENT_TABLE, enrollment,
                    ENROLLMENT_UID + " = ?", enrollmentUid);
        };
    }

    @NonNull
    @Override
    public Consumer<ReportStatus> storeReportStatus() {
        return reportStatus -> {
            ContentValues enrollment = new ContentValues();
            enrollment.put(ENROLLMENT_STATUS,
                    ReportStatus.toEnrollmentStatus(reportStatus).name());
            enrollment.put(ENROLLMENT_STATE, State.TO_UPDATE.name()); // TODO: Check if state is TO_POST
            // TODO: and if so, keep the TO_POST state

            briteDatabase.update(ENROLLMENT_TABLE, enrollment,
                    ENROLLMENT_UID + " = ?", enrollmentUid);
        };
    }

    @NonNull
    @Override
    public Observable<String> autoGenerateEvents(String enrollmentUid) {
        try (Cursor cursor = briteDatabase.query(SELECT_AUTO_GENERATE_PROGRAM_STAGE, enrollmentUid == null ? "" : enrollmentUid)) {
            if (cursor != null) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    insertEvent(cursor);
                    cursor.moveToNext();
                }
            }
        }

        return Observable.just(enrollmentUid);
    }

    private void insertEvent(Cursor cursor) {

        Calendar calNow = Calendar.getInstance();
        calNow.set(Calendar.HOUR_OF_DAY, 0);
        calNow.set(Calendar.MINUTE, 0);
        calNow.set(Calendar.SECOND, 0);
        calNow.set(Calendar.MILLISECOND, 0);
        Date now = calNow.getTime();

        String programStage = cursor.getString(0);
        String program = cursor.getString(1);
        String orgUnit = cursor.getString(2);

        Date eventDate = DateUtils.getEventDate(cursor);

        try (Cursor eventCursor = briteDatabase.query(CHECK_STAGE_IS_NOT_CREATED, enrollmentUid, programStage)) {

            if (!eventCursor.moveToFirst()) {

                Event.Builder eventBuilder = Event.builder()
                        .uid(codeGenerator.generate())
                        .created(Calendar.getInstance().getTime())
                        .lastUpdated(Calendar.getInstance().getTime())
//                            .eventDate(eventDate)
//                            .dueDate(eventDate)
                        .enrollment(enrollmentUid)
                        .program(program)
                        .programStage(programStage)
                        .organisationUnit(orgUnit)
                        .status(eventDate.after(now) ? EventStatus.SCHEDULE : EventStatus.ACTIVE)
                        .state(State.TO_POST);
                if (eventDate.after(now)) //scheduling
                    eventBuilder.dueDate(eventDate);
                else
                    eventBuilder.eventDate(eventDate);

                Event event = eventBuilder.build();


                if (briteDatabase.insert(EVENT_TABLE, event.toContentValues()) < 0) {
                    throw new OnErrorNotImplementedException(new Throwable("Unable to store event:" + event));
                }
            }
        }
    }

    @NonNull
    @Override
    public Observable<List<FieldViewModel>> fieldValues() {
        return briteDatabase
                .createQuery(TE_ATTR_VALUE_TABLE, QUERY, enrollmentUid)
                .mapToList(this::transform);
    }


    @Override
    public void deleteTrackedEntityAttributeValues(@NonNull String trackedEntityInstanceId) {
        String deleteWhereRelationship = String.format(
                TABLE_FIELD_EQUALS,
                TE_ATTR_VALUE_TABLE, TE_ATTR_VALUE_TEI);
        briteDatabase.delete(TE_ATTR_VALUE_TABLE, deleteWhereRelationship + "'" + trackedEntityInstanceId + "'");
    }

    @Override
    public void deleteEnrollment(@NonNull String trackedEntityInstanceId) {
        String deleteWhereRelationship = String.format(
                TABLE_FIELD_EQUALS,
                ENROLLMENT_TABLE, ENROLLMENT_TEI);
        briteDatabase.delete(ENROLLMENT_TABLE, deleteWhereRelationship + "'" + trackedEntityInstanceId + "'");
    }

    @Override
    public void deleteEvent() {
        // not necessary
    }

    @Override
    public void deleteTrackedEntityInstance(@NonNull String trackedEntityInstanceId) {
        String deleteWhereRelationship = String.format(
                TABLE_FIELD_EQUALS,
                TEI_TABLE, TEI_UID);
        briteDatabase.delete(TEI_TABLE, deleteWhereRelationship + "'" + trackedEntityInstanceId + "'");
    }

    @NonNull
    @Override
    public Observable<String> getTrackedEntityInstanceUid() {
        String selectTe = SELECT + ENROLLMENT_TABLE + "." + ENROLLMENT_TEI +
                " FROM " + ENROLLMENT_TABLE +
                " WHERE " + ENROLLMENT_UID + " = ?" +
                " LIMIT 1";

        return briteDatabase.createQuery(ENROLLMENT_TABLE, selectTe, enrollmentUid).mapToOne(cursor -> cursor.getString(0));
    }

    @Override
    public Observable<Trio<Boolean, CategoryCombo, List<CategoryOptionCombo>>> getProgramCategoryCombo() {
        return null;
    }

    @Override
    public void saveCategoryOption(CategoryOptionCombo selectedOption) {
        // unused
    }

    @Override
    public Observable<Boolean> captureCoodinates() {
        return briteDatabase.createQuery("Program", "SELECT Program.captureCoordinates FROM Program " +
                "JOIN Enrollment ON Enrollment.program = Program.uid WHERE Enrollment.uid = ?", enrollmentUid)
                .mapToOne(cursor -> cursor.getInt(0) == 1);
    }

    @Override
    public Observable<OrganisationUnit> getOrgUnitDates() {
        return Observable.defer(() -> Observable.just(d2.enrollmentModule().enrollments.uid(enrollmentUid).get()))
                .switchMap(enrollment -> Observable.just(d2.organisationUnitModule().organisationUnits.uid(enrollment.organisationUnit()).get()));
    }

    @NonNull
    private FieldViewModel transform(@NonNull Cursor cursor) {
        FieldViewModelHelper fieldViewModelHelper = FieldViewModelHelper.createFromCursor(cursor);
        EnrollmentStatus status = EnrollmentStatus.valueOf(cursor.getString(10));

        int optionCount = 0;
        if (fieldViewModelHelper.getOptionSetUid() != null) {
            try (Cursor countCursor = briteDatabase.query("SELECT COUNT (uid) FROM Option WHERE optionSet = ?", fieldViewModelHelper.getOptionSetUid())) {
                if (countCursor != null && countCursor.moveToFirst())
                    optionCount = countCursor.getInt(0);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        ValueTypeDeviceRendering fieldRendering = null;
        try (Cursor rendering = briteDatabase.query("SELECT ValueTypeDeviceRendering.* FROM ValueTypeDeviceRendering " +
                "JOIN ProgramTrackedEntityAttribute ON ProgramTrackedEntityAttribute.uid = ValueTypeDeviceRendering.uid WHERE ProgramTrackedEntityAttribute.trackedEntityAttribute = ?", fieldViewModelHelper.getUid())) {
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
            if (objStyleCursor != null && objStyleCursor.moveToFirst())
                objectStyle = ObjectStyle.create(objStyleCursor);
        }

        return fieldFactory.create(fieldViewModelHelper.getUid(), fieldViewModelHelper.getLabel(), fieldViewModelHelper.getValueType(),
                fieldViewModelHelper.isMandatory(), fieldViewModelHelper.getOptionSetUid(), fieldViewModelHelper.getDataValue(),
                fieldViewModelHelper.getSection(), fieldViewModelHelper.getAllowFutureDates(),
                status == EnrollmentStatus.ACTIVE, null, fieldViewModelHelper.getDescription(), fieldRendering,
                optionCount, objectStyle);
    }

    private ProgramStage getStageToOpen(Trio<Boolean, List<ProgramStage>, TrackedEntityType> data) {
        ProgramStage stageToOpen = null;
        if (data.val0() && !data.val1().isEmpty()) {
            stageToOpen = data.val1().get(0);
        } else if (!data.val1().isEmpty()) {
            for (ProgramStage programStage : data.val1()) {
                if (programStage.openAfterEnrollment() && stageToOpen == null)
                    stageToOpen = programStage;
            }
        }
        return stageToOpen;
    }

    @NonNull
    @Override
    public Observable<Trio<String, String, String>> useFirstStageDuringRegistration() { //enrollment uid, trackedEntityType, event uid

        return briteDatabase.createQuery(PROGRAM_TABLE, "SELECT * FROM Program WHERE uid = ?", programUid)
                .mapToOne(Program::create)
                .flatMap(programModel ->
                        briteDatabase.createQuery(PROGRAM_STAGE_TABLE, "SELECT * FROM ProgramStage WHERE program = ? ORDER BY ProgramStage.sortOrder", programModel.uid())
                                .mapToList(ProgramStage::create).map(programstages -> Trio.create(programModel.useFirstStageDuringRegistration(), programstages, programModel.trackedEntityType())))
                .map(data -> {
                    ProgramStage stageToOpen = getStageToOpen(data);
                    if (stageToOpen != null) { //we should check if event exist (if not create) and open
                        return getOrInsertEvent(stageToOpen);
                    } else { //open Dashboard
                        return openDashboard();
                    }
                });
    }

    private Trio<String, String, String> getOrInsertEvent(ProgramStage stageToOpen) {
        try (Cursor eventCursor = briteDatabase.query("SELECT Event.uid FROM Event WHERE Event.programStage = ? AND Event.enrollment = ?", stageToOpen.uid(), enrollmentUid)) {
            if (eventCursor != null && eventCursor.moveToFirst()) {
                String eventUid = eventCursor.getString(0);
                return Trio.create(getTeiUid(), programUid, eventUid);
            } else {
                try (Cursor enrollmentOrgUnitCursor = briteDatabase.query("SELECT Enrollment.organisationUnit FROM Enrollment WHERE Enrollment.uid = ?", enrollmentUid)) {
                    return insertEvent(stageToOpen, enrollmentOrgUnitCursor);
                }
            }
        }
    }

    private Trio<String, String, String> insertEvent(ProgramStage stageToOpen, Cursor enrollmentOrgUnitCursor) {
        if (enrollmentOrgUnitCursor != null && enrollmentOrgUnitCursor.moveToFirst()) {
            Date createdDate = DateUtils.getInstance().getCalendar().getTime();
            Event eventToCreate = Event.builder()
                    .uid(codeGenerator.generate())
                    .created(createdDate)
                    .lastUpdated(createdDate)
                    .eventDate(createdDate)
                    .enrollment(enrollmentUid)
                    .program(stageToOpen.program().uid())
                    .programStage(stageToOpen.uid())
                    .organisationUnit(enrollmentOrgUnitCursor.getString(0))
                    .status(EventStatus.ACTIVE)
                    .state(State.TO_POST)
                    .build();

            if (briteDatabase.insert(EVENT_TABLE, eventToCreate.toContentValues()) < 0) {
                throw new OnErrorNotImplementedException(new Throwable("Unable to store event:" + eventToCreate));
            }

            return Trio.create(getTeiUid(), programUid, eventToCreate.uid());//teiUid, programUio, eventUid
        } else
            throw new IllegalArgumentException("Can't create event in enrollment with null organisation unit");
    }

    private Trio<String, String, String> openDashboard() {
        try (Cursor tetCursor = briteDatabase.query(SELECT_TE_TYPE, enrollmentUid)) {
            String programUidAux = "";
            String teiUid = "";
            if (tetCursor != null && tetCursor.moveToFirst()) {
                programUidAux = tetCursor.getString(0);
                teiUid = tetCursor.getString(1);
            }
            return Trio.create(teiUid, programUidAux, "");
        }
    }

    private String getTeiUid() {
        String teiUid = "";
        try (Cursor teiUidCursor = briteDatabase.query("SELECT DISTINCT TrackedEntityInstance.uid " +
                "FROM TrackedEntityInstance JOIN Enrollment ON Enrollment.trackedEntityInstance = TrackedEntityInstance.uid " +
                "WHERE Enrollment.uid = ? LIMIT 1", enrollmentUid)) {
            if (teiUidCursor != null && teiUidCursor.moveToFirst()) {
                teiUid = teiUidCursor.getString(0);
            }
        }
        return teiUid;
    }

    @NonNull
    private Flowable<String> enrollmentProgram() {
        return briteDatabase
                .createQuery(ENROLLMENT_TABLE, SELECT_PROGRAM, enrollmentUid)
                .mapToOne(cursor -> {
                    programUid = cursor.getString(0);
                    return programUid;
                })
                .toFlowable(BackpressureStrategy.LATEST);
    }
}