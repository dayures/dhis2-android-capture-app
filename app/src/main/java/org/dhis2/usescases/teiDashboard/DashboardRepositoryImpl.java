package org.dhis2.usescases.teiDashboard;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.R;
import org.dhis2.data.tuples.Pair;
import org.dhis2.data.tuples.Trio;
import org.dhis2.utils.CodeGenerator;
import org.dhis2.utils.DateUtils;
import org.dhis2.utils.SqlConstants;
import org.dhis2.utils.ValueUtils;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.category.Category;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.data.database.DbDateColumnAdapter;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.enrollment.note.Note;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.program.ProgramIndicator;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.relationship.RelationshipType;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

import static org.dhis2.utils.SqlConstants.AND;
import static org.dhis2.utils.SqlConstants.EVENT_ATTR_OPTION_COMBO;
import static org.dhis2.utils.SqlConstants.EVENT_STATE;
import static org.dhis2.utils.SqlConstants.JOIN_TABLE_ON;
import static org.dhis2.utils.SqlConstants.PROGRAM_TE_ATTR_DISPLAY_IN_LIST;
import static org.dhis2.utils.SqlConstants.PROGRAM_TE_ATTR_PROGRAM;
import static org.dhis2.utils.SqlConstants.PROGRAM_TE_ATTR_SORT_ORDER;
import static org.dhis2.utils.SqlConstants.PROGRAM_TE_ATTR_TABLE;
import static org.dhis2.utils.SqlConstants.PROGRAM_TE_ATTR_TE_ATTR;
import static org.dhis2.utils.SqlConstants.QUESTION_MARK;
import static org.dhis2.utils.SqlConstants.SELECT;
import static org.dhis2.utils.SqlConstants.TABLE_FIELD_EQUALS;
import static org.dhis2.utils.SqlConstants.TE_ATTR_TABLE;
import static org.dhis2.utils.SqlConstants.TE_ATTR_UID;
import static org.dhis2.utils.SqlConstants.TE_ATTR_VALUE_TABLE;
import static org.dhis2.utils.SqlConstants.TE_ATTR_VALUE_TEI;
import static org.dhis2.utils.SqlConstants.TE_ATTR_VALUE_TE_ATTR;
import static org.dhis2.utils.SqlConstants.WHERE;
import static org.hisp.dhis.android.core.utils.StoreUtils.sqLiteBind;


/**
 * QUADRAM. Created by ppajuelo on 30/11/2017.
 */

public class DashboardRepositoryImpl implements DashboardRepository {

    private static final String INSERT_NOTE = "INSERT INTO Note ( " +
            "uid, enrollment, value, storedBy, storedDate" +
            ") VALUES (?, ?, ?, ?, ?);";

    private static final String SELECT_NOTES = SELECT +
            "Note.* FROM Note\n" +
            "JOIN Enrollment ON Enrollment.uid = Note.enrollment\n" +
            "WHERE Enrollment.trackedEntityInstance = ? AND Enrollment.program = ?\n" +
            "ORDER BY Note.storedDate DESC";

    private static final String PROGRAM_INDICATORS_QUERY = String.format("SELECT %s.* FROM %s WHERE %s.%s = ",
            SqlConstants.PROGRAM_INDICATOR_TABLE, SqlConstants.PROGRAM_INDICATOR_TABLE, SqlConstants.PROGRAM_INDICATOR_TABLE, SqlConstants.PROGRAM_INDICATOR_PROGRAM);

    private static final String ENROLLMENT_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ? AND %s.%s = ? LIMIT 1",
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_PROGRAM,
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_TEI);

    private static final String PROGRAM_STAGE_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_PROGRAM);

    private static final String PROGRAM_STAGE_FROM_EVENT = String.format(
            "SELECT %s.* FROM %s JOIN %s " +
                    "ON %s.%s = %s.%s " +
                    WHERE + TABLE_FIELD_EQUALS + QUESTION_MARK +
                    "LIMIT 1",
            SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.EVENT_TABLE,
            SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_UID, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_PROGRAM_STAGE,
            SqlConstants.EVENT_TABLE, SqlConstants.EVENT_UID);

    private static final String GET_EVENT_FROM_UID = String.format(
            "SELECT * FROM %s WHERE %s.%s = ? LIMIT 1",
            SqlConstants.EVENT_TABLE, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_UID);

    private static final String EVENTS_QUERY = String.format(
            "SELECT DISTINCT %s.* FROM %s " +
                    JOIN_TABLE_ON +
                    JOIN_TABLE_ON +
                    WHERE + TABLE_FIELD_EQUALS + QUESTION_MARK + //ProgramUid
                    AND + TABLE_FIELD_EQUALS + QUESTION_MARK + //TeiUid
                    "AND %s.%s != '%s' " +
                    "AND %s.%s IN (SELECT %s FROM %s WHERE %s = ?) " +
                    "ORDER BY CASE WHEN ( Event.status IN ('SCHEDULE','SKIPPED','OVERDUE')) " +
                    "THEN %s.%s " +
                    "ELSE %s.%s END DESC, %s.%s ASC",
            SqlConstants.EVENT_TABLE, SqlConstants.EVENT_TABLE,
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_UID, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_ENROLLMENT,
            SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_UID, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_PROGRAM_STAGE,
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_PROGRAM,
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_TEI,
            SqlConstants.EVENT_TABLE, EVENT_STATE, State.TO_DELETE,
            SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_UID, SqlConstants.PROGRAM_STAGE_UID, SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_PROGRAM,
            SqlConstants.EVENT_TABLE, SqlConstants.EVENT_DUE_DATE,
            SqlConstants.EVENT_TABLE, SqlConstants.EVENT_DATE, SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_SORT_ORDER);

    private static final String EVENTS_DISPLAY_BOX = String.format(
            "SELECT Event.* FROM %s " +
                    JOIN_TABLE_ON +
                    JOIN_TABLE_ON +
                    WHERE + TABLE_FIELD_EQUALS + QUESTION_MARK +
                    AND + TABLE_FIELD_EQUALS + QUESTION_MARK +
                    "AND %s.%s = ?",
            SqlConstants.EVENT_TABLE,
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_UID, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_ENROLLMENT,
            SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_UID, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_PROGRAM_STAGE,
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_PROGRAM,
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_TEI,
            SqlConstants.PROGRAM_STAGE_TABLE, SqlConstants.PROGRAM_STAGE_DISPLAY_GENERATE_EVENT_BOX);


    private static final Set<String> EVENTS_TABLE = new HashSet<>(Arrays.asList(SqlConstants.EVENT_TABLE, SqlConstants.ENROLLMENT_TABLE));
    private static final Set<String> EVENTS_PROGRAM_STAGE_TABLE = new HashSet<>(Arrays.asList(SqlConstants.EVENT_TABLE, SqlConstants.ENROLLMENT_TABLE, SqlConstants.PROGRAM_STAGE_TABLE));

    private static final String ATTRIBUTE_VALUES_QUERY = String.format(
            "SELECT TrackedEntityAttributeValue.*, TrackedEntityAttribute.valueType, TrackedEntityAttribute.optionSet FROM %s " +
                    JOIN_TABLE_ON +
                    JOIN_TABLE_ON +
                    WHERE + TABLE_FIELD_EQUALS + QUESTION_MARK +
                    AND + TABLE_FIELD_EQUALS + QUESTION_MARK +
                    "ORDER BY %s.%s",
            TE_ATTR_VALUE_TABLE,
            PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_TE_ATTR, TE_ATTR_VALUE_TABLE, TE_ATTR_VALUE_TE_ATTR,
            TE_ATTR_TABLE, TE_ATTR_TABLE, TE_ATTR_UID, TE_ATTR_VALUE_TABLE, TE_ATTR_VALUE_TE_ATTR,
            PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_PROGRAM,
            TE_ATTR_VALUE_TABLE, TE_ATTR_VALUE_TEI,
            PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_SORT_ORDER);

    private static final String ATTRIBUTE_VALUES_NO_PROGRAM_QUERY = String.format(
                    "JOIN %s ON %s.%s = %s.%s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = ? " +
                    "AND %s.%s = ? " +
                    "AND %s.%s = 1 " +
                    "ORDER BY %s.%s",
            TE_ATTR_VALUE_TABLE,
            PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_TE_ATTR, TE_ATTR_VALUE_TABLE, TE_ATTR_VALUE_TE_ATTR,
            TE_ATTR_TABLE, TE_ATTR_TABLE, TE_ATTR_UID, TE_ATTR_VALUE_TABLE, TE_ATTR_VALUE_TE_ATTR,
            PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_PROGRAM,
            TE_ATTR_VALUE_TABLE, TE_ATTR_VALUE_TEI,
            PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_DISPLAY_IN_LIST,
            PROGRAM_TE_ATTR_TABLE, PROGRAM_TE_ATTR_SORT_ORDER);
   
    private static final Set<String> ATTRIBUTE_VALUES_TABLE = new HashSet<>(Arrays.asList(TE_ATTR_VALUE_TABLE, PROGRAM_TE_ATTR_TABLE));

    private final BriteDatabase briteDatabase;
    private final CodeGenerator codeGenerator;
    private final D2 d2;

    private String teiUid;
    private String programUid;
    private String enrollmentUid;

    private static final String SELECT_USERNAME = SELECT +
            "UserCredentials.displayName FROM UserCredentials";
    private static final String SELECT_ENROLLMENT = SELECT +
            "Enrollment.uid FROM Enrollment JOIN Program ON Program.uid = Enrollment.program\n" +
            "WHERE Program.uid = ? AND Enrollment.status = ? AND Enrollment.trackedEntityInstance = ?";

    private static final String SELECT_TEI_MAIN_ATTR = "SELECT TrackedEntityAttributeValue.*, ProgramTrackedEntityAttribute.sortOrder FROM TrackedEntityAttributeValue " +
            "JOIN ProgramTrackedEntityAttribute ON ProgramTrackedEntityAttribute.trackedEntityAttribute = TrackedEntityAttributeValue.trackedEntityAttribute " +
            "WHERE TrackedEntityAttributeValue.trackedEntityInstance = ? " +
            "ORDER BY ProgramTrackedEntityAttribute.sortOrder";

    private static final String SELECT_LEGEND = String.format("SELECT %s.%s FROM %s\n" +
                    "JOIN %s ON %s.%s = %s.%s\n" +
                    "JOIN %s ON %s.%s = %s.%s\n" +
                    "WHERE %s.%s = ?\n" +
                    "AND %s.%s <= ?\n" +
                    "AND %s.%s > ?",
            SqlConstants.LEGEND_TABLE, SqlConstants.LEGEND_COLOR, SqlConstants.LEGEND_TABLE,
            SqlConstants.PROGRAM_INDICATOR_LEGEND_SET_LINK_TABLE, SqlConstants.PROGRAM_INDICATOR_LEGEND_SET_LINK_TABLE,
            SqlConstants.PROGRAM_INDICATOR_LEGEND_SET_LINK_LEGEND_SET, SqlConstants.LEGEND_TABLE, SqlConstants.LEGEND_LEGEND_SET,
            SqlConstants.PROGRAM_INDICATOR_TABLE, SqlConstants.PROGRAM_INDICATOR_TABLE, SqlConstants.PROGRAM_INDICATOR_UID,
            SqlConstants.PROGRAM_INDICATOR_LEGEND_SET_LINK_TABLE, SqlConstants.PROGRAM_INDICATOR_LEGEND_SET_LINK_PROGRAM_INDICATOR,
            SqlConstants.PROGRAM_INDICATOR_TABLE, SqlConstants.PROGRAM_INDICATOR_UID,
            SqlConstants.LEGEND_TABLE, SqlConstants.LEGEND_START_VALUE,
            SqlConstants.LEGEND_TABLE, SqlConstants.LEGEND_END_VALUE);

    public DashboardRepositoryImpl(CodeGenerator codeGenerator, BriteDatabase briteDatabase, D2 d2) {
        this.briteDatabase = briteDatabase;
        this.codeGenerator = codeGenerator;
        this.d2 = d2;
    }


    @Override
    public void setDashboardDetails(String teiUid, String programUid) {
        this.teiUid = teiUid;
        this.programUid = programUid;
    }

    @Override
    public Observable<List<TrackedEntityAttributeValue>> mainTrackedEntityAttributes(String teiUid) {
        return briteDatabase.createQuery(TE_ATTR_VALUE_TABLE, SELECT_TEI_MAIN_ATTR, teiUid)
                .mapToList(TrackedEntityAttributeValue::create);
    }

    @Override
    public Event updateState(Event eventModel, EventStatus newStatus) {

        Date currentDate = Calendar.getInstance().getTime();
        Event event = Event.builder()
                .id(eventModel.id())
                .uid(eventModel.uid())
                .created(eventModel.created())
                .lastUpdated(currentDate)
                .eventDate(eventModel.eventDate())
                .dueDate(eventModel.dueDate())
                .enrollment(eventModel.enrollment())
                .program(eventModel.program())
                .programStage(eventModel.programStage())
                .organisationUnit(eventModel.organisationUnit())
                .status(newStatus)
                .state(State.TO_UPDATE)
                .build();

        updateProgramTable(currentDate, eventModel.program());
        updateTeiState();

        briteDatabase.update(SqlConstants.EVENT_TABLE, event.toContentValues(), SqlConstants.EVENT_UID + " = ?", event.uid());
        return event;
    }

    @Override
    public Observable<List<ProgramStage>> getProgramStages(String programUid) {
        String id = programUid == null ? "" : programUid;
        return briteDatabase.createQuery(SqlConstants.PROGRAM_STAGE_TABLE, PROGRAM_STAGE_QUERY + "'" + id + "'")
                .mapToList(ProgramStage::create);
    }

    @Override
    public Observable<Enrollment> getEnrollment(String programUid, String teiUid) {
        String progId = programUid == null ? "" : programUid;
        String teiId = teiUid == null ? "" : teiUid;
        return briteDatabase.createQuery(SqlConstants.ENROLLMENT_TABLE, ENROLLMENT_QUERY, progId, teiId)
                .mapToOne(Enrollment::create);
    }

    @Override
    public Observable<List<Event>> getTEIEnrollmentEvents(String programUid, String teiUid) {
        String progId = programUid == null ? "" : programUid;
        String teiId = teiUid == null ? "" : teiUid;
        return briteDatabase.createQuery(EVENTS_TABLE, EVENTS_QUERY, progId, teiId, progId)
                .mapToList(cursor -> {
                    Event eventModel = Event.create(cursor);
                    if (eventModel.status() == EventStatus.SCHEDULE && eventModel.dueDate().before(DateUtils.getInstance().getToday()))
                        eventModel = updateState(eventModel, EventStatus.OVERDUE);

                    return eventModel;
                });
    }

    @Override
    public Observable<List<Event>> getEnrollmentEventsWithDisplay(String programUid, String teiUid) {
        String progId = programUid == null ? "" : programUid;
        String teiId = teiUid == null ? "" : teiUid;
        return briteDatabase.createQuery(EVENTS_PROGRAM_STAGE_TABLE, EVENTS_DISPLAY_BOX, progId, teiId, "1")
                .mapToList(Event::create);
    }

    @Override
    public Observable<ProgramStage> displayGenerateEvent(String eventUid) {
        String id = eventUid == null ? "" : eventUid;
        return briteDatabase.createQuery(SqlConstants.PROGRAM_STAGE_TABLE, PROGRAM_STAGE_FROM_EVENT, id)
                .mapToOne(ProgramStage::create);
    }

    @Override
    public Observable<String> generateNewEvent(String lastModifiedEventUid, Integer standardInterval) {
        return briteDatabase.createQuery(SqlConstants.EVENT_TABLE, GET_EVENT_FROM_UID, lastModifiedEventUid == null ? "" : lastModifiedEventUid)
                .mapToOne(Event::create)
                .flatMap(event -> {
                    ContentValues values = new ContentValues();
                    Calendar createdDate = Calendar.getInstance();
                    Calendar dueDate = Calendar.getInstance();
                    dueDate.set(Calendar.HOUR_OF_DAY, 0);
                    dueDate.set(Calendar.MINUTE, 0);
                    dueDate.set(Calendar.SECOND, 0);
                    dueDate.set(Calendar.MILLISECOND, 0);

                    if (standardInterval != null)
                        dueDate.add(Calendar.DAY_OF_YEAR, standardInterval);

                    values.put(SqlConstants.EVENT_UID, codeGenerator.generate());
                    values.put(SqlConstants.EVENT_ENROLLMENT, event.enrollment());
                    values.put(SqlConstants.EVENT_CREATED, DateUtils.databaseDateFormat().format(createdDate.getTime()));
                    values.put(SqlConstants.EVENT_LAST_UPDATED, DateUtils.databaseDateFormat().format(createdDate.getTime()));
                    values.put(SqlConstants.EVENT_STATUS, EventStatus.SCHEDULE.toString());
                    values.put(SqlConstants.EVENT_PROGRAM, event.program());
                    values.put(SqlConstants.EVENT_PROGRAM_STAGE, event.programStage());
                    values.put(SqlConstants.EVENT_ORG_UNIT, event.organisationUnit());
                    values.put(SqlConstants.EVENT_DUE_DATE, DateUtils.databaseDateFormat().format(dueDate.getTime()));
                    values.put(SqlConstants.EVENT_DATE, DateUtils.databaseDateFormat().format(dueDate.getTime()));
                    values.put(EVENT_STATE, State.TO_POST.toString());

                    if (briteDatabase.insert(SqlConstants.EVENT_TABLE, values) <= 0) {
                        return Observable.error(new IllegalStateException("Event has not been successfully added"));
                    }

                    updateProgramTable(createdDate.getTime(), programUid);

                    return Observable.just("Event Created");
                });
    }

    @Override
    public Observable<Trio<ProgramIndicator, String, String>> getLegendColorForIndicator(ProgramIndicator indicator, String value) {
        String piId = indicator != null && indicator.uid() != null ? indicator.uid() : "";
        String color = "";
        try (Cursor cursor = briteDatabase.query(SELECT_LEGEND, piId, value == null ? "" : value, value == null ? "" : value)) {
            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                color = cursor.getString(0);
            }
        }
        return Observable.just(Trio.create(indicator, value, color));
    }

    @Override
    public Observable<String> generateNewEventFromDate(String lastModifiedEventUid, Calendar chosenDate) {
        return briteDatabase.createQuery(SqlConstants.EVENT_TABLE, GET_EVENT_FROM_UID, lastModifiedEventUid == null ? "" : lastModifiedEventUid)
                .mapToOne(Event::create)
                .flatMap(event -> {
                    ContentValues values = new ContentValues();
                    Calendar createdDate = Calendar.getInstance();

                    chosenDate.set(Calendar.HOUR_OF_DAY, 0);
                    chosenDate.set(Calendar.MINUTE, 0);
                    chosenDate.set(Calendar.SECOND, 0);
                    chosenDate.set(Calendar.MILLISECOND, 0);

                    values.put(SqlConstants.EVENT_UID, codeGenerator.generate());
                    values.put(SqlConstants.EVENT_ENROLLMENT, event.enrollment());
                    values.put(SqlConstants.EVENT_CREATED, DateUtils.databaseDateFormat().format(createdDate.getTime()));
                    values.put(SqlConstants.EVENT_LAST_UPDATED, DateUtils.databaseDateFormat().format(createdDate.getTime()));
                    values.put(SqlConstants.EVENT_STATUS, EventStatus.SCHEDULE.toString());
                    values.put(SqlConstants.EVENT_PROGRAM, event.program());
                    values.put(SqlConstants.EVENT_PROGRAM_STAGE, event.programStage());
                    values.put(SqlConstants.EVENT_ORG_UNIT, event.organisationUnit());
                    values.put(SqlConstants.EVENT_DUE_DATE, DateUtils.databaseDateFormat().format(chosenDate.getTime()));
                    values.put(SqlConstants.EVENT_DATE, DateUtils.databaseDateFormat().format(chosenDate.getTime()));
                    values.put(EVENT_STATE, State.TO_POST.toString());

                    if (briteDatabase.insert(SqlConstants.EVENT_TABLE, values) <= 0) {
                        return Observable.error(new IllegalStateException("Event has not been successfully added"));
                    }

                    updateProgramTable(createdDate.getTime(), programUid);

                    return Observable.just("Event Created");
                });
    }

    @Override
    public void updateTeiState() {
        String getTei = "SELECT * FROM TrackedEntityInstance WHERE uid = ? LIMIT 1";
        try (Cursor teiCursor = briteDatabase.query(getTei, teiUid)) {
            if (teiCursor != null && teiCursor.moveToFirst()) {
                TrackedEntityInstance tei = TrackedEntityInstance.create(teiCursor);
                ContentValues contentValues = tei.toContentValues();
                if (contentValues.get(SqlConstants.TEI_STATE).equals(State.SYNCED.name())) {
                    contentValues.put(SqlConstants.TEI_STATE, State.TO_UPDATE.name());

                    briteDatabase.update(SqlConstants.TEI_TABLE, contentValues, "uid = ?", teiUid);

                }
            }
        }
    }

    @Override
    public Integer getObjectStyle(Context context, String uid) {
        String getObjectStyle = "SELECT * FROM ObjectStyle WHERE uid = ?";
        try (Cursor objectStyleCurosr = briteDatabase.query(getObjectStyle, uid)) {
            if (objectStyleCurosr != null && objectStyleCurosr.moveToNext()) {
                String iconName = objectStyleCurosr.getString(objectStyleCurosr.getColumnIndex("icon"));
                Resources resources = context.getResources();
                iconName = iconName.startsWith("ic_") ? iconName : "ic_" + iconName;
                return resources.getIdentifier(iconName, "drawable", context.getPackageName());
            } else
                return R.drawable.ic_person;
        }
    }

    @Override
    public Observable<List<Pair<RelationshipType, String>>> relationshipsForTeiType(String teType) {
        String relationshipQuery =
                "SELECT FROMTABLE.*, TOTABLE.trackedEntityType AS toTeiType FROM " +
                        "(SELECT RelationshipType.*,RelationshipConstraint.* FROM RelationshipType " +
                        "JOIN RelationshipConstraint ON RelationshipConstraint.relationshipType = RelationshipType.uid WHERE constraintType = 'FROM') " +
                        "AS FROMTABLE " +
                        "JOIN " +
                        "(SELECT RelationshipType.*,RelationshipConstraint.* FROM RelationshipType " +
                        "JOIN RelationshipConstraint ON RelationshipConstraint.relationshipType = RelationshipType.uid WHERE constraintType = 'TO') " +
                        "AS TOTABLE " +
                        "ON TOTABLE.relationshipType = FROMTABLE.relationshipType " +
                        "WHERE FROMTABLE.trackedEntityType = ?";
        String relationshipQuery29 =
                "SELECT RelationshipType.* FROM RelationshipType";
        return briteDatabase.createQuery("SystemInfo", "SELECT version FROM SystemInfo")
                .mapToOne(cursor -> cursor.getString(0))
                .flatMap(version -> {
                    if (version.equals("2.29"))
                        return briteDatabase.createQuery(SqlConstants.RELATIONSHIP_TYPE_TABLE, relationshipQuery29)
                                .mapToList(cursor -> Pair.create(RelationshipType.create(cursor), teType));
                    else
                        return briteDatabase.createQuery(SqlConstants.RELATIONSHIP_TYPE_TABLE, relationshipQuery, teType)
                                .mapToList(cursor -> Pair.create(RelationshipType.create(cursor), cursor.getString(cursor.getColumnIndex("toTeiType"))));
                });

    }

    @Override
    public Observable<CategoryCombo> catComboForProgram(String programUid) {
        return Observable.defer(() -> Observable.just(d2.categoryModule().categoryCombos.uid(d2.programModule().programs.uid(programUid).get().categoryCombo().uid()).withAllChildren().get()))
                .map(categoryCombo -> {
                    List<Category> fullCategories = new ArrayList<>();
                    List<CategoryOptionCombo> fullOptionCombos = new ArrayList<>();
                    for (Category category : categoryCombo.categories()) {
                        fullCategories.add(d2.categoryModule().categories.uid(category.uid()).withAllChildren().get());
                    }
                    for (CategoryOptionCombo categoryOptionCombo : categoryCombo.categoryOptionCombos())
                        fullOptionCombos.add(d2.categoryModule().categoryOptionCombos.uid(categoryOptionCombo.uid()).withAllChildren().get());
                    return categoryCombo.toBuilder().categories(fullCategories).categoryOptionCombos(fullOptionCombos).build();
                });
    }

    @Override
    public void setDefaultCatOptCombToEvent(String eventUid) {
        Event event = d2.eventModule().events.uid(eventUid).get();
        ContentValues cv = event.toContentValues();
        List<CategoryCombo> categoryCombos = d2.categoryModule().categoryCombos.byIsDefault().isTrue().withAllChildren().get();
        cv.put(EVENT_ATTR_OPTION_COMBO, categoryCombos.get(0).categoryOptionCombos().get(0).uid());
        cv.put(EVENT_STATE, event.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());
        briteDatabase.update("Event", cv, "Event.uid = ?", eventUid);
    }

    @Override
    public Observable<List<TrackedEntityAttributeValue>> getTEIAttributeValues(String
                                                                                       programUid, String teiUid) {
        if (programUid != null)
            return briteDatabase.createQuery(ATTRIBUTE_VALUES_TABLE, ATTRIBUTE_VALUES_QUERY, programUid, teiUid == null ? "" : teiUid)
                    .mapToList(cursor -> ValueUtils.transform(briteDatabase, cursor));
        else
            return briteDatabase.createQuery(ATTRIBUTE_VALUES_TABLE, ATTRIBUTE_VALUES_NO_PROGRAM_QUERY, teiUid == null ? "" : teiUid)
                    .mapToList(cursor -> ValueUtils.transform(briteDatabase, cursor));
    }

    @Override
    public Flowable<List<ProgramIndicator>> getIndicators(String programUid) {
        String id = programUid == null ? "" : programUid;
        return briteDatabase.createQuery(SqlConstants.PROGRAM_TABLE, PROGRAM_INDICATORS_QUERY + "'" + id + "'")
                .mapToList(ProgramIndicator::create).toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public boolean setFollowUp(String enrollmentUid) {

        String enrollmentFollowUpQuery = "SELECT Enrollment.followup FROM Enrollment WHERE Enrollment.uid = ?";
        boolean followUp = false;

        try (Cursor cursor = briteDatabase.query(enrollmentFollowUpQuery, enrollmentUid)) {
            if (cursor != null && cursor.moveToFirst()) {
                followUp = cursor.getInt(0) == 1;
            }
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(SqlConstants.ENROLLMENT_FOLLOW_UP, followUp ? "0" : "1");

        briteDatabase.update(SqlConstants.ENROLLMENT_TABLE, contentValues, SqlConstants.ENROLLMENT_UID + " = ?", enrollmentUid == null ? "" : enrollmentUid);

        return !followUp;
    }

    @Override
    public Flowable<List<Note>> getNotes(String programUid, String teUid) {
        return briteDatabase.createQuery(SqlConstants.NOTE_TABLE, SELECT_NOTES, teUid == null ? "" : teUid, programUid == null ? "" : programUid)
                .mapToList(cursor -> {

                    DbDateColumnAdapter dbDateColumnAdapter = new DbDateColumnAdapter();
                    int idColumnIndex = cursor.getColumnIndex("_id");
                    Long id = idColumnIndex != -1 && !cursor.isNull(idColumnIndex) ? cursor.getLong(idColumnIndex) : null;
                    int enrollmentColumnIndex = cursor.getColumnIndex("enrollment");
                    String enrollment = enrollmentColumnIndex != -1 && !cursor.isNull(enrollmentColumnIndex) ? cursor.getString(enrollmentColumnIndex) : null;
                    int valueColumnIndex = cursor.getColumnIndex("value");
                    String value = valueColumnIndex != -1 && !cursor.isNull(valueColumnIndex) ? cursor.getString(valueColumnIndex) : null;
                    int storedByColumnIndex = cursor.getColumnIndex("storedBy");
                    String storedBy = storedByColumnIndex != -1 && !cursor.isNull(storedByColumnIndex) ? cursor.getString(storedByColumnIndex) : null;
                    Date storedDate = dbDateColumnAdapter.fromCursor(cursor, "storedDate");

                    return Note.builder()
                            .id(id)
                            .enrollment(enrollment)
                            .value(value)
                            .storedBy(storedBy)
                            .storedDate(DateUtils.databaseDateFormat().format(storedDate))
                            .build();

                }).toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public Consumer<Pair<String, Boolean>> handleNote() {
        return stringBooleanPair -> {
            if (stringBooleanPair.val1()) {
                try (Cursor cursor1 = briteDatabase.query(SELECT_ENROLLMENT, programUid == null ? "" : programUid, EnrollmentStatus.ACTIVE.name(), teiUid == null ? "" : teiUid);
                     Cursor cursor = briteDatabase.query(SELECT_USERNAME)) {
                    cursor.moveToFirst();
                    cursor1.moveToFirst();
                    SQLiteStatement insetNoteStatement = getInsertNoteStatement(cursor, cursor1, stringBooleanPair);
                    briteDatabase.executeInsert(SqlConstants.NOTE_TABLE, insetNoteStatement);
                    insetNoteStatement.clearBindings();
                }
            }
        };
    }

    private SQLiteStatement getInsertNoteStatement(Cursor cursor, Cursor
            cursor1, Pair<String, Boolean> stringBooleanPair) {
        String userName = cursor.getString(0);
        String enrollmentUidAux = cursor1.getString(0);

        SQLiteStatement insetNoteStatement = briteDatabase.getWritableDatabase()
                .compileStatement(INSERT_NOTE);


        sqLiteBind(insetNoteStatement, 1, codeGenerator.generate()); //enrollment
        sqLiteBind(insetNoteStatement, 2, enrollmentUidAux == null ? "" : enrollmentUidAux); //enrollment
        sqLiteBind(insetNoteStatement, 3, stringBooleanPair.val0()); //value
        sqLiteBind(insetNoteStatement, 4, userName == null ? "" : userName); //storeBy
        sqLiteBind(insetNoteStatement, 5, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime())); //storeDate

        return insetNoteStatement;
    }

    @Override
    public Observable<Boolean> handleNote(Pair<String, Boolean> stringBooleanPair) {
        if (stringBooleanPair.val1()) {

            try (Cursor cursor1 = briteDatabase.query(SELECT_ENROLLMENT, programUid == null ? "" : programUid, EnrollmentStatus.ACTIVE.name(), teiUid == null ? "" : teiUid);
                 Cursor cursor = briteDatabase.query(SELECT_USERNAME)) {

                cursor.moveToFirst();
                String userName = cursor.getString(0);

                cursor1.moveToFirst();
                String enrollmentUidAux = cursor1.getString(0);

                SQLiteStatement insetNoteStatement = briteDatabase.getWritableDatabase()
                        .compileStatement(INSERT_NOTE);


                sqLiteBind(insetNoteStatement, 1, codeGenerator.generate()); //enrollment
                sqLiteBind(insetNoteStatement, 2, enrollmentUidAux == null ? "" : enrollmentUidAux); //enrollment
                sqLiteBind(insetNoteStatement, 3, stringBooleanPair.val0()); //value
                sqLiteBind(insetNoteStatement, 4, userName == null ? "" : userName); //storeBy
                sqLiteBind(insetNoteStatement, 5, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime())); //storeDate

                long success = briteDatabase.executeInsert(SqlConstants.NOTE_TABLE, insetNoteStatement);

                insetNoteStatement.clearBindings();

                return Observable.just(success == 1).flatMap(value -> updateEnrollment(success).toObservable())
                        .map(value -> value == 1);
            }

        } else
            return Observable.just(false);
    }

    @Override
    public Flowable<Long> updateEnrollmentStatus(@NonNull String uid, @NonNull EnrollmentStatus
            value) {
        return Flowable
                .defer(() -> {
                    long updated = update(uid, value);
                    return Flowable.just(updated);
                })
                .switchMap(this::updateEnrollment);
    }

    private long update(String uid, EnrollmentStatus value) {
        this.enrollmentUid = uid;
        String update = "UPDATE Enrollment\n" +
                "SET lastUpdated = ?, status = ?\n" +
                "WHERE uid = ?;";

        SQLiteStatement updateStatement = briteDatabase.getWritableDatabase()
                .compileStatement(update);
        sqLiteBind(updateStatement, 1, BaseIdentifiableObject.DATE_FORMAT
                .format(Calendar.getInstance().getTime()));
        sqLiteBind(updateStatement, 2, value);
        sqLiteBind(updateStatement, 3, enrollmentUid == null ? "" : enrollmentUid);

        long updated = briteDatabase.executeUpdateDelete(
                TE_ATTR_VALUE_TABLE, updateStatement);
        updateStatement.clearBindings();

        updateTeiState();

        return updated;
    }

    @NonNull
    private Flowable<Long> updateEnrollment(long status) {
        String selectTei = "SELECT *\n" +
                "FROM TrackedEntityInstance\n" +
                "WHERE uid IN (\n" +
                "  SELECT trackedEntityInstance\n" +
                "  FROM Enrollment\n" +
                "  WHERE Enrollment.uid = ?\n" +
                ") LIMIT 1;";
        return briteDatabase.createQuery(SqlConstants.TEI_TABLE, selectTei, enrollmentUid == null ? "" : enrollmentUid)
                .mapToOne(TrackedEntityInstance::create).take(1).toFlowable(BackpressureStrategy.LATEST)
                .switchMap(tei -> {
                    if (State.SYNCED.equals(tei.state()) || State.TO_DELETE.equals(tei.state()) ||
                            State.ERROR.equals(tei.state())) {
                        ContentValues values = tei.toContentValues();
                        values.put(SqlConstants.TEI_STATE, State.TO_UPDATE.toString());

                        if (tei.uid() != null && briteDatabase.update(SqlConstants.TEI_TABLE, values,
                                SqlConstants.TEI_UID + " = ?", tei.uid()) <= 0) {

                            throw new IllegalStateException(String.format(Locale.US, "Tei=[%s] " +
                                    "has not been successfully updated", tei.uid()));
                        }
                    }
                    return Flowable.just(status);
                });
    }

    @SuppressWarnings({"squid:S1172", "squid:CommentedOutCodeLine"})
    private void updateProgramTable(Date lastUpdated, String programUid) {
        /*ContentValues program = new ContentValues();TODO: Crash if active
        program.put(SqlConstants.ENROLLMENT_LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(lastUpdated));
        briteDatabase.update(SqlConstants.PROGRAM_TABLE, program, SqlConstants.PROGRAM_UID + " = ?", programUid);*/
    }
}