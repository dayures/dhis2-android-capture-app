package org.dhis2.usescases.eventsWithoutRegistration.eventInitial;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.utils.CodeGenerator;
import org.dhis2.utils.DateUtils;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.category.Category;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryOption;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.Coordinates;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;
import static org.dhis2.utils.SqlConstants.LIMIT_1;
import static org.dhis2.utils.SqlConstants.NOT_EQUALS;
import static org.dhis2.utils.SqlConstants.QUOTE;
import static org.dhis2.utils.SqlConstants.SELECT_ALL_FROM;
import static org.dhis2.utils.SqlConstants.WHERE;


/**
 * QUADRAM. Created by Cristian on 22/03/2018.
 */

public class EventInitialRepositoryImpl implements EventInitialRepository {

    private static final String SELECT_ORG_UNITS = "SELECT * FROM OrganisationUnit " +
            "JOIN OrganisationUnitProgramLink ON OrganisationUnitProgramLink .organisationUnit = OrganisationUnit.uid " +
            "WHERE OrganisationUnitProgramLink .program = ?";

    private static final String SELECT_ORG_UNITS_FILTERED = SELECT_ALL_FROM + SqlConstants.ORG_UNIT_TABLE +
            " JOIN OrganisationUnitProgramLink ON OrganisationUnitProgramLink .organisationUnit = OrganisationUnit.uid " +
            " WHERE ("
            + SqlConstants.ORG_UNIT_OPENING_DATE + " IS NULL OR " +
            " date(" + SqlConstants.ORG_UNIT_OPENING_DATE + ") <= date(?)) AND ("
            + SqlConstants.ORG_UNIT_CLOSED_DATE + " IS NULL OR " +
            " date(" + SqlConstants.ORG_UNIT_CLOSED_DATE + ") >= date(?)) " +
            "AND OrganisationUnitProgramLink .program = ?";

    private final BriteDatabase briteDatabase;
    private final CodeGenerator codeGenerator;
    private final String eventUid;
    private final D2 d2;

    EventInitialRepositoryImpl(CodeGenerator codeGenerator, BriteDatabase briteDatabase, String eventUid, D2 d2) {
        this.briteDatabase = briteDatabase;
        this.codeGenerator = codeGenerator;
        this.eventUid = eventUid;
        this.d2 = d2;
    }


    @NonNull
    @Override
    public Observable<Event> event(String eventId) {
        String id = eventId == null ? "" : eventId;
        String selectEventWithId = SELECT_ALL_FROM + SqlConstants.EVENT_TABLE + WHERE + SqlConstants.EVENT_UID + " = '" + id + "' AND " +
                SqlConstants.EVENT_STATE + NOT_EQUALS + QUOTE + State.TO_DELETE + QUOTE + LIMIT_1;
        return briteDatabase.createQuery(SqlConstants.EVENT_TABLE, selectEventWithId)
                .mapToOne(Event::create);
    }

    @NonNull
    @Override
    public Observable<List<OrganisationUnit>> orgUnits(String programId) {
        return briteDatabase.createQuery(SqlConstants.ORG_UNIT_TABLE, SELECT_ORG_UNITS, programId == null ? "" : programId)
                .mapToList(OrganisationUnit::create);
    }

    @NonNull
    @Override
    public Observable<CategoryCombo> catCombo(String programUid) {
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
    public Flowable<Map<String, CategoryOption>> getOptionsFromCatOptionCombo(String eventId) {
        return Flowable.just(d2.eventModule().events.uid(eventUid).get())
                .flatMap(event -> Flowable.zip(catCombo(event.program()).toFlowable(BackpressureStrategy.LATEST),
                        Flowable.just(d2.categoryModule().categoryOptionCombos.uid(event.attributeOptionCombo()).withAllChildren().get()),
                        (categoryCombo, categoryOptionCombo) -> {
                            List<CategoryOption> selectedCatOptions = categoryOptionCombo.categoryOptions();
                            Map<String, CategoryOption> map = new HashMap<>();
                            for (Category category : categoryCombo.categories()) {
                                for (CategoryOption categoryOption : selectedCatOptions)
                                    if (category.categoryOptions().contains(categoryOption))
                                        map.put(category.uid(), categoryOption);
                            }
                            return map;
                        }));
    }

    @NonNull
    @Override
    public Observable<List<OrganisationUnit>> filteredOrgUnits(String date, String programId) {
        if (date == null)
            return orgUnits(programId);
        return briteDatabase.createQuery(SqlConstants.ORG_UNIT_TABLE, SELECT_ORG_UNITS_FILTERED,
                date,
                date,
                programId == null ? "" : programId)
                .mapToList(OrganisationUnit::create);
    }

    @Override
    public Observable<String> createEvent(String enrollmentUid, @Nullable String trackedEntityInstanceUid,
                                          @NonNull Context context, @NonNull String programUid,
                                          @NonNull String programStage, @NonNull Date date,
                                          @NonNull String orgUnitUid, @Nullable String categoryOptionsUid,
                                          @Nullable String categoryOptionComboUid, @Nullable String latitude, @Nullable String longitude) {


        Date createDate = Calendar.getInstance().getTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String uid = codeGenerator.generate();

        Event eventModel = Event.builder()
                .uid(uid)
                .enrollment(enrollmentUid)
                .created(createDate)
                .lastUpdated(createDate)
                .status(EventStatus.ACTIVE)
                .program(programUid)
                .programStage(programStage)
                .organisationUnit(orgUnitUid)
                .eventDate(cal.getTime())
                .completedDate(null)
                .dueDate(null)
                .state(State.TO_POST)
                .attributeOptionCombo(categoryOptionComboUid)
                .build();

        if (latitude != null && longitude != null) {
            eventModel.toBuilder().coordinate(Coordinates.create(Double.valueOf(latitude), Double.valueOf(longitude))).build();
        }

        long row = -1;

        try {
            row = briteDatabase.insert(SqlConstants.EVENT_TABLE,
                    eventModel.toContentValues());
        } catch (Exception e) {
            Timber.e(e);
        }

        if (row < 0) {
            String message = String.format(Locale.US, "Failed to insert new event " +
                            "instance for organisationUnit=[%s] and programStage=[%s]",
                    orgUnitUid, programStage);
            return Observable.error(new SQLiteConstraintException(message));
        } else {
            if (trackedEntityInstanceUid != null)
                updateTei(trackedEntityInstanceUid);
            updateProgramTable(createDate, programUid);
            return Observable.just(uid);
        }
    }

    @Override
    public Observable<String> scheduleEvent(String enrollmentUid, @Nullable String trackedEntityInstanceUid,
                                            @NonNull Context context, @NonNull String program, @NonNull String programStage,
                                            @NonNull Date dueDate, @NonNull String orgUnitUid, @Nullable String categoryOptionsUid,
                                            @Nullable String categoryOptionComboUid, @NonNull String latitude, @NonNull String longitude) {
        Date createDate = Calendar.getInstance().getTime();
        Calendar cal = Calendar.getInstance();
        cal.setTime(dueDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String uid = codeGenerator.generate();

        Event eventModel = Event.builder()
                .uid(uid)
                .enrollment(enrollmentUid)
                .created(createDate)
                .lastUpdated(createDate)
                .status(EventStatus.SCHEDULE)
                .coordinate(Coordinates.create(Double.valueOf(latitude), Double.valueOf(longitude)))
                .program(program)
                .programStage(programStage)
                .organisationUnit(orgUnitUid)
                .completedDate(null)
                .dueDate(cal.getTime())
                .state(State.TO_POST)
                .attributeOptionCombo(categoryOptionComboUid)
                .build();

        long row = -1;

        try {
            row = briteDatabase.insert(SqlConstants.EVENT_TABLE,
                    eventModel.toContentValues());
        } catch (Exception e) {
            Timber.e(e);
        }

        if (row < 0) {
            String message = String.format(Locale.US, "Failed to insert new event " +
                            "instance for organisationUnit=[%s] and programStage=[%s]",
                    orgUnitUid, programStage);
            return Observable.error(new SQLiteConstraintException(message));
        } else {
            if (trackedEntityInstanceUid != null)
                updateTei(trackedEntityInstanceUid);
            updateTrackedEntityInstance(uid, trackedEntityInstanceUid, orgUnitUid);
            updateProgramTable(createDate, program);
            return Observable.just(uid);
        }
    }

    @SuppressWarnings({"squid:S1172", "squid:CommentedOutCodeLine"})
    private void updateProgramTable(Date lastUpdated, String programUid) {
        //TODO: Update program causes crash
        /* ContentValues program = new ContentValues();
        program.put(SqlConstants.ENROLLMENT_LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(lastUpdated));
        briteDatabase.update(SqlConstants.PROGRAM_TABLE, program, SqlConstants.PROGRAM_UID + " = ?", programUid);*/
    }

    @Override
    public Observable<String> updateTrackedEntityInstance(String eventId, String trackedEntityInstanceUid, String orgUnitUid) {
        String teiQuery = "SELECT * FROM TrackedEntityInstance WHERE TrackedEntityInstance.uid = ? LIMIT 1";
        return briteDatabase.createQuery(SqlConstants.TEI_TABLE, teiQuery, trackedEntityInstanceUid == null ? "" : trackedEntityInstanceUid)
                .mapToOne(TrackedEntityInstance::create).distinctUntilChanged()
                .map(trackedEntityInstanceModel -> {
                    ContentValues contentValues = trackedEntityInstanceModel.toContentValues();
                    contentValues.put(SqlConstants.TEI_ORG_UNIT, orgUnitUid);
                    long row = -1;
                    try {
                        row = briteDatabase.update(SqlConstants.TEI_TABLE, contentValues, "TrackedEntityInstance.uid = ?", trackedEntityInstanceUid == null ? "" : trackedEntityInstanceUid);
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                    if (row != -1) {
                        return eventId; //Event created and referral complete
                    }
                    return eventId;
                });
    }


    @NonNull
    @Override
    public Observable<Event> newlyCreatedEvent(long rowId) {
        String selectEventWithRowid = SELECT_ALL_FROM + SqlConstants.EVENT_TABLE + WHERE + SqlConstants.EVENT_ID + " = '" + rowId + "'" + " AND " + SqlConstants.EVENT_STATE + NOT_EQUALS + QUOTE + State.TO_DELETE + QUOTE + LIMIT_1;
        return briteDatabase.createQuery(SqlConstants.EVENT_TABLE, selectEventWithRowid).mapToOne(Event::create);
    }

    @NonNull
    @Override
    public Observable<ProgramStage> programStage(String programUid) {
        String id = programUid == null ? "" : programUid;
        String selectProgramStage = SELECT_ALL_FROM + SqlConstants.PROGRAM_STAGE_TABLE + WHERE + SqlConstants.PROGRAM_STAGE_PROGRAM + " = '" + id + QUOTE + LIMIT_1;
        return briteDatabase.createQuery(SqlConstants.PROGRAM_STAGE_TABLE, selectProgramStage)
                .mapToOne(ProgramStage::create);
    }

    @NonNull
    @Override
    public Observable<ProgramStage> programStageWithId(String programStageUid) {
        String id = programStageUid == null ? "" : programStageUid;
        String selectProgramStageWithId = SELECT_ALL_FROM + SqlConstants.PROGRAM_STAGE_TABLE + WHERE + SqlConstants.PROGRAM_STAGE_UID + " = '" + id + QUOTE + LIMIT_1;
        return briteDatabase.createQuery(SqlConstants.PROGRAM_STAGE_TABLE, selectProgramStageWithId)
                .mapToOne(ProgramStage::create);
    }


    @NonNull
    @Override
    public Observable<Event> editEvent(String trackedEntityInstance,
                                       String eventUid,
                                       String date,
                                       String orgUnitUid,
                                       String catComboUid,
                                       String catOptionCombo,
                                       String latitude, String longitude) {

        Event event = d2.eventModule().events.uid(eventUid).get();

        boolean hasChanged = false;

        Date currentDate = Calendar.getInstance().getTime();
        Date mDate = null;
        try {
            mDate = DateUtils.databaseDateFormat().parse(date);
        } catch (ParseException e) {
            Timber.e(e);
        }

        if (event.eventDate() != mDate)
            hasChanged = true;
        if (!event.organisationUnit().equals(orgUnitUid))
            hasChanged = true;
        if (!String.valueOf(event.coordinate().latitude()).equals(latitude) || !String.valueOf(event.coordinate().longitude()).equals(longitude))
            hasChanged = true;
        if (!event.attributeOptionCombo().equals(catOptionCombo))
            hasChanged = true;

        if (hasChanged) {

            Calendar cal = Calendar.getInstance();
            cal.setTime(mDate);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            ContentValues contentValues = new ContentValues();
            contentValues.put(SqlConstants.EVENT_DATE, DateUtils.databaseDateFormat().format(cal.getTime()));
            contentValues.put(SqlConstants.EVENT_ORG_UNIT, orgUnitUid);
            contentValues.put(SqlConstants.EVENT_LATITUDE, latitude);
            contentValues.put(SqlConstants.EVENT_LONGITUDE, longitude);
            contentValues.put(SqlConstants.EVENT_ATTR_OPTION_COMBO, catOptionCombo);
            contentValues.put(SqlConstants.EVENT_LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(currentDate));
            contentValues.put(SqlConstants.EVENT_STATE, event.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());

            long row = -1;

            try {
                row = briteDatabase.update(SqlConstants.EVENT_TABLE, contentValues, SqlConstants.EVENT_UID + " = ?", eventUid);
            } catch (Exception e) {
                Timber.e(e);
            }
            if (row <= 0) {
                String message = String.format(Locale.US, "Failed to update event for uid=[%s]", eventUid);
                return Observable.error(new SQLiteConstraintException(message));
            }
            if (trackedEntityInstance != null)
                updateTei(trackedEntityInstance);
        }

        return event(eventUid).map(eventModel1 -> eventModel1);
    }

    @NonNull
    @Override
    public Observable<List<Event>> getEventsFromProgramStage(String programUid, String
            enrollmentUid, String programStageUid) {
        String eventsQuery = String.format(
                "SELECT Event.* FROM %s JOIN %s " +
                        "ON %s.%s = %s.%s " +
                        "WHERE %s.%s = ? " +
                        "AND %s.%s = ? " +
                        "AND %s.%s = ? " +
                        "AND " + SqlConstants.EVENT_TABLE + "." + SqlConstants.EVENT_STATE + NOT_EQUALS + QUOTE + State.TO_DELETE + "'" +
                        "AND " + SqlConstants.EVENT_TABLE + "." + SqlConstants.EVENT_DATE + " > DATE() " +
                        "ORDER BY CASE WHEN %s.%s > %s.%s " +
                        "THEN %s.%s ELSE %s.%s END ASC",
                SqlConstants.EVENT_TABLE, SqlConstants.ENROLLMENT_TABLE,
                SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_UID, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_ENROLLMENT,
                SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_PROGRAM,
                SqlConstants.ENROLLMENT_TABLE, SqlConstants.ENROLLMENT_UID,
                SqlConstants.EVENT_TABLE, SqlConstants.EVENT_PROGRAM_STAGE,
                SqlConstants.EVENT_TABLE, SqlConstants.EVENT_DUE_DATE, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_DATE,
                SqlConstants.EVENT_TABLE, SqlConstants.EVENT_DUE_DATE, SqlConstants.EVENT_TABLE, SqlConstants.EVENT_DATE);

        return briteDatabase.createQuery(SqlConstants.EVENT_TABLE, eventsQuery, programUid == null ? "" : programUid,
                enrollmentUid == null ? "" : enrollmentUid,
                programStageUid == null ? "" : programStageUid)
                .mapToList(Event::create);
    }

    @Override
    public Observable<Boolean> accessDataWrite(String programId) {
        String writePermission = "SELECT ProgramStage.accessDataWrite FROM ProgramStage WHERE ProgramStage.program = ? LIMIT 1";
        String programWritePermission = "SELECT Program.accessDataWrite FROM Program WHERE Program.uid = ? LIMIT 1";
        return briteDatabase.createQuery(SqlConstants.PROGRAM_STAGE_TABLE, writePermission, programId == null ? "" : programId)
                .mapToOne(cursor -> cursor.getInt(0) == 1)
                .flatMap(programStageAccessDataWrite ->
                        briteDatabase.createQuery(SqlConstants.PROGRAM_TABLE, programWritePermission, programId == null ? "" : programId)
                                .mapToOne(cursor -> (cursor.getInt(0) == 1) && programStageAccessDataWrite));
    }

    @Override
    public void deleteEvent(String eventId, String trackedEntityInstance) {
        try (Cursor eventCursor = briteDatabase.query("SELECT Event.* FROM Event WHERE Event.uid = ?", eventId)) {
            if (eventCursor != null && eventCursor.moveToNext()) {
                Event eventModel = Event.create(eventCursor);
                if (eventModel.state() == State.TO_POST) {
                    String deleteWhere = String.format(
                            "%s.%s = ?",
                            SqlConstants.EVENT_TABLE, SqlConstants.EVENT_UID
                    );
                    briteDatabase.delete(SqlConstants.EVENT_TABLE, deleteWhere, eventId);
                } else {
                    ContentValues contentValues = eventModel.toContentValues();
                    contentValues.put(SqlConstants.EVENT_STATE, State.TO_DELETE.name());
                    briteDatabase.update(SqlConstants.EVENT_TABLE, contentValues, SqlConstants.EVENT_UID + " = ?", eventId);
                }

                if (!isEmpty(eventModel.enrollment()))
                    updateEnrollment(eventModel.enrollment());

                if (trackedEntityInstance != null)
                    updateTei(trackedEntityInstance);
            }
        }
    }

    @Override
    public boolean isEnrollmentOpen() {
        boolean isEnrollmentOpen = true;
        if (!isEmpty(eventUid)) {
            try (Cursor enrollmentCursor = briteDatabase.query("SELECT Enrollment.* FROM Enrollment JOIN Event ON Event.enrollment = Enrollment.uid WHERE Event.uid = ?", eventUid)) {
                if (enrollmentCursor != null && enrollmentCursor.moveToFirst()) {
                    Enrollment enrollment = Enrollment.create(enrollmentCursor);
                    isEnrollmentOpen = enrollment.status() == EnrollmentStatus.ACTIVE;
                }
            }
        }
        return isEnrollmentOpen;
    }


    private void updateEnrollment(String enrollmentUid) {
        String selectEnrollment = "SELECT * FROM Enrollment WHERE uid = ?";
        try (Cursor enrollmentCursor = briteDatabase.query(selectEnrollment, enrollmentUid)) {
            if (enrollmentCursor != null && enrollmentCursor.moveToFirst()) {
                Enrollment enrollment = Enrollment.create(enrollmentCursor);
                ContentValues cv = enrollment.toContentValues();
                cv.put(SqlConstants.ENROLLMENT_LAST_UPDATED, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime()));
                cv.put(SqlConstants.ENROLLMENT_STATE,
                        enrollment.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());
                briteDatabase.update(SqlConstants.ENROLLMENT_TABLE, cv, "uid = ?", enrollmentUid);
            }
        }
    }

    private void updateTei(String teiUid) {
        String selectTei = "SELECT * FROM TrackedEntityInstance WHERE uid = ?";
        try (Cursor teiCursor = briteDatabase.query(selectTei, teiUid)) {
            if (teiCursor != null && teiCursor.moveToFirst()) {
                TrackedEntityInstance teiModel = TrackedEntityInstance.create(teiCursor);
                ContentValues cv = teiModel.toContentValues();
                cv.put(SqlConstants.TEI_LAST_UPDATED, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime()));
                cv.put(SqlConstants.TEI_STATE,
                        teiModel.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());
                briteDatabase.update(SqlConstants.TEI_TABLE, cv, "uid = ?", teiUid);
            }
        }
    }
}