package org.dhis2.usescases.eventsWithoutRegistration.eventInitial;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.utils.CodeGenerator;
import org.dhis2.utils.DateUtils;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.Coordinates;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.program.ProgramStageModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private static final String SELECT_ORG_UNITS_FILTERED = SELECT_ALL_FROM + OrganisationUnitModel.TABLE +
            " JOIN OrganisationUnitProgramLink ON OrganisationUnitProgramLink .organisationUnit = OrganisationUnit.uid " +
            " WHERE ("
            + OrganisationUnitModel.Columns.OPENING_DATE + " IS NULL OR " +
            " date(" + OrganisationUnitModel.Columns.OPENING_DATE + ") <= date(?)) AND ("
            + OrganisationUnitModel.Columns.CLOSED_DATE + " IS NULL OR " +
            " date(" + OrganisationUnitModel.Columns.CLOSED_DATE + ") >= date(?)) " +
            "AND OrganisationUnitProgramLink .program = ?";

    private final BriteDatabase briteDatabase;
    private final CodeGenerator codeGenerator;
    private final String eventUid;

    EventInitialRepositoryImpl(CodeGenerator codeGenerator, BriteDatabase briteDatabase, String eventUid) {
        this.briteDatabase = briteDatabase;
        this.codeGenerator = codeGenerator;
        this.eventUid = eventUid;
    }


    @NonNull
    @Override
    public Observable<Event> event(String eventId) {
        String id = eventId == null ? "" : eventId;
        String selectEventWithId = SELECT_ALL_FROM + EventModel.TABLE + WHERE + EventModel.Columns.UID + " = '" + id + "' AND " + EventModel.Columns.STATE + NOT_EQUALS + QUOTE + State.TO_DELETE + QUOTE + LIMIT_1;
        return briteDatabase.createQuery(EventModel.TABLE, selectEventWithId)
                .mapToOne(Event::create);
    }

    @NonNull
    @Override
    public Observable<List<OrganisationUnit>> orgUnits(String programId) {
        return briteDatabase.createQuery(OrganisationUnitModel.TABLE, SELECT_ORG_UNITS, programId == null ? "" : programId)
                .mapToList(OrganisationUnit::create);
    }

    @NonNull
    @Override
    public Observable<CategoryCombo> catComboModel(String programUid) {
        String catComboQuery = "SELECT CategoryCombo.* FROM CategoryCombo JOIN Program ON Program.categoryCombo = CategoryCombo.uid WHERE Program.uid = ?";
        return briteDatabase.createQuery(CategoryComboModel.TABLE, catComboQuery, programUid)
                .mapToOne(CategoryCombo::create);
    }

    @NonNull
    @Override
    public Observable<List<CategoryOptionCombo>> catCombo(String programUid) {
        String catComboQuery = "SELECT CategoryOptionCombo.* FROM CategoryOptionCombo " +
                "JOIN CategoryCombo ON CategoryCombo.uid= CategoryOptionCombo.categoryCombo " +
                "JOIN CategoryOptionComboCategoryOptionLink ON CategoryOptionComboCategoryOptionLink.categoryOptionCombo = CategoryOptionCombo.uid " +
                "JOIN CategoryOption ON CategoryOptionComboCategoryOptionLink.CategoryOption = CategoryOption.uid " +
                "JOIN Program ON Program.categoryCombo = CategoryCombo.uid " +
                "WHERE categoryOption.accessDataWrite AND program.uid = ?";
        return briteDatabase.createQuery(CategoryOptionComboModel.TABLE, catComboQuery, programUid)
                .mapToList(CategoryOptionCombo::create);
    }

    @NonNull
    @Override
    public Observable<List<OrganisationUnit>> filteredOrgUnits(String date, String programId) {
        if (date == null)
            return orgUnits(programId);
        return briteDatabase.createQuery(OrganisationUnitModel.TABLE, SELECT_ORG_UNITS_FILTERED,
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
                                          @Nullable String categoryOptionComboUid, @NonNull String latitude, @NonNull String longitude) {


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
                .coordinate(Coordinates.create(Double.valueOf(latitude), Double.valueOf(longitude)))
                .program(programUid)
                .programStage(programStage)
                .organisationUnit(orgUnitUid)
                .eventDate(cal.getTime())
                .completedDate(null)
                .dueDate(null)
                .state(State.TO_POST)
                .attributeOptionCombo(categoryOptionComboUid)
                .build();

        long row = -1;

        try {
            row = briteDatabase.insert(EventModel.TABLE,
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
            row = briteDatabase.insert(EventModel.TABLE,
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
        program.put(EnrollmentModel.Columns.LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(lastUpdated));
        briteDatabase.update(ProgramModel.TABLE, program, ProgramModel.Columns.UID + " = ?", programUid);*/
    }

    @Override
    public Observable<String> updateTrackedEntityInstance(String eventId, String trackedEntityInstanceUid, String orgUnitUid) {
        String teiQuery = "SELECT * FROM TrackedEntityInstance WHERE TrackedEntityInstance.uid = ? LIMIT 1";
        return briteDatabase.createQuery(TrackedEntityInstanceModel.TABLE, teiQuery, trackedEntityInstanceUid == null ? "" : trackedEntityInstanceUid)
                .mapToOne(TrackedEntityInstance::create).distinctUntilChanged()
                .map(trackedEntityInstanceModel -> {
                    ContentValues contentValues = trackedEntityInstanceModel.toContentValues();
                    contentValues.put(TrackedEntityInstanceModel.Columns.ORGANISATION_UNIT, orgUnitUid);
                    long row = -1;
                    try {
                        row = briteDatabase.update(TrackedEntityInstanceModel.TABLE, contentValues, "TrackedEntityInstance.uid = ?", trackedEntityInstanceUid == null ? "" : trackedEntityInstanceUid);
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
        String selectEventWithRowid = SELECT_ALL_FROM + EventModel.TABLE + WHERE + EventModel.Columns.ID + " = '" + rowId + "'" + " AND " + EventModel.Columns.STATE + NOT_EQUALS + QUOTE + State.TO_DELETE + QUOTE + LIMIT_1;
        return briteDatabase.createQuery(EventModel.TABLE, selectEventWithRowid).mapToOne(Event::create);
    }

    @NonNull
    @Override
    public Observable<ProgramStage> programStage(String programUid) {
        String id = programUid == null ? "" : programUid;
        String selectProgramStage = SELECT_ALL_FROM + ProgramStageModel.TABLE + WHERE + ProgramStageModel.Columns.PROGRAM + " = '" + id + QUOTE + LIMIT_1;
        return briteDatabase.createQuery(ProgramStageModel.TABLE, selectProgramStage)
                .mapToOne(ProgramStage::create);
    }

    @NonNull
    @Override
    public Observable<ProgramStage> programStageWithId(String programStageUid) {
        String id = programStageUid == null ? "" : programStageUid;
        String selectProgramStageWithId = SELECT_ALL_FROM + ProgramStageModel.TABLE + WHERE + ProgramStageModel.Columns.UID + " = '" + id + QUOTE + LIMIT_1;
        return briteDatabase.createQuery(ProgramStageModel.TABLE, selectProgramStageWithId)
                .mapToOne(ProgramStage::create);
    }


    @NonNull
    @Override
    public Observable<Event> editEvent(String trackedEntityInstance, String eventUid, String date, String orgUnitUid, String catComboUid, String catOptionCombo, String latitude, String longitude) {

        Date currentDate = Calendar.getInstance().getTime();
        Date dueDate = null;
        try {
            dueDate = DateUtils.databaseDateFormat().parse(date);
        } catch (ParseException e) {
            Timber.e(e);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(dueDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        ContentValues contentValues = new ContentValues();
        contentValues.put(EventModel.Columns.EVENT_DATE, DateUtils.databaseDateFormat().format(cal.getTime()));
        contentValues.put(EventModel.Columns.ORGANISATION_UNIT, orgUnitUid);
        // TODO CRIS: CHECK IF THESE ARE WORKING...
        contentValues.put(EventModel.Columns.LATITUDE, latitude);
        contentValues.put(EventModel.Columns.LONGITUDE, longitude);
        contentValues.put(EventModel.Columns.ATTRIBUTE_OPTION_COMBO, catComboUid);
        contentValues.put(EventModel.Columns.LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(currentDate));

        long row = -1;

        String id = eventUid == null ? "" : eventUid;

        try {
            row = briteDatabase.update(EventModel.TABLE, contentValues, EventModel.Columns.UID + " = ?", id);
        } catch (Exception e) {
            Timber.e(e);
        }

        if (row <= 0) {
            String message = String.format(Locale.US, "Failed to update event for uid=[%s]", id);
            return Observable.error(new SQLiteConstraintException(message));
        }
        if (trackedEntityInstance != null) {
            updateTei(trackedEntityInstance);
        }
//            updateProgramTable(currentDate, eventModel1.program()); //TODO: (inside the map) This is crashing the app
        return event(id).map(eventModel1 -> eventModel1);
    }

    @NonNull
    @Override
    public Observable<List<Event>> getEventsFromProgramStage(String programUid, String enrollmentUid, String programStageUid) {
        String eventsQuery = String.format(
                "SELECT Event.* FROM %s JOIN %s " +
                        "ON %s.%s = %s.%s " +
                        "WHERE %s.%s = ? " +
                        "AND %s.%s = ? " +
                        "AND %s.%s = ? " +
                        "AND " + EventModel.TABLE + "." + EventModel.Columns.STATE + NOT_EQUALS + QUOTE + State.TO_DELETE + "'" +
                        "AND " + EventModel.TABLE + "." + EventModel.Columns.EVENT_DATE + " > DATE() " +
                        "ORDER BY CASE WHEN %s.%s > %s.%s " +
                        "THEN %s.%s ELSE %s.%s END ASC",
                EventModel.TABLE, EnrollmentModel.TABLE,
                EnrollmentModel.TABLE, EnrollmentModel.Columns.UID, EventModel.TABLE, EventModel.Columns.ENROLLMENT,
                EnrollmentModel.TABLE, EnrollmentModel.Columns.PROGRAM,
                EnrollmentModel.TABLE, EnrollmentModel.Columns.UID,
                EventModel.TABLE, EventModel.Columns.PROGRAM_STAGE,
                EventModel.TABLE, EventModel.Columns.DUE_DATE, EventModel.TABLE, EventModel.Columns.EVENT_DATE,
                EventModel.TABLE, EventModel.Columns.DUE_DATE, EventModel.TABLE, EventModel.Columns.EVENT_DATE);

        return briteDatabase.createQuery(EventModel.TABLE, eventsQuery, programUid == null ? "" : programUid,
                enrollmentUid == null ? "" : enrollmentUid,
                programStageUid == null ? "" : programStageUid)
                .mapToList(Event::create);
    }

    @Override
    public Observable<Boolean> accessDataWrite(String programId) {
        String writePermission = "SELECT ProgramStage.accessDataWrite FROM ProgramStage WHERE ProgramStage.program = ? LIMIT 1";
        String programWritePermission = "SELECT Program.accessDataWrite FROM Program WHERE Program.uid = ? LIMIT 1";
        return briteDatabase.createQuery(ProgramStageModel.TABLE, writePermission, programId == null ? "" : programId)
                .mapToOne(cursor -> cursor.getInt(0) == 1)
                .flatMap(programStageAccessDataWrite ->
                        briteDatabase.createQuery(ProgramModel.TABLE, programWritePermission, programId == null ? "" : programId)
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
                            EventModel.TABLE, EventModel.Columns.UID
                    );
                    briteDatabase.delete(EventModel.TABLE, deleteWhere, eventId);
                } else {
                    ContentValues contentValues = eventModel.toContentValues();
                    contentValues.put(EventModel.Columns.STATE, State.TO_DELETE.name());
                    briteDatabase.update(EventModel.TABLE, contentValues, EventModel.Columns.UID + " = ?", eventId);
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
                cv.put(EnrollmentModel.Columns.LAST_UPDATED, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime()));
                cv.put(EnrollmentModel.Columns.STATE,
                        enrollment.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());
                briteDatabase.update(EnrollmentModel.TABLE, cv, "uid = ?", enrollmentUid);
            }
        }
    }

    private void updateTei(String teiUid) {
        String selectTei = "SELECT * FROM TrackedEntityInstance WHERE uid = ?";
        try (Cursor teiCursor = briteDatabase.query(selectTei, teiUid)) {
            if (teiCursor != null && teiCursor.moveToFirst()) {
                TrackedEntityInstance teiModel = TrackedEntityInstance.create(teiCursor);
                ContentValues cv = teiModel.toContentValues();
                cv.put(TrackedEntityInstanceModel.Columns.LAST_UPDATED, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime()));
                cv.put(TrackedEntityInstanceModel.Columns.STATE,
                        teiModel.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());
                briteDatabase.update(TrackedEntityInstanceModel.TABLE, cv, "uid = ?", teiUid);
            }
        }
    }
}