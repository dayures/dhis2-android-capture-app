package org.dhis2.usescases.eventsWithoutRegistration.eventInitial;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.utils.CodeGenerator;
import org.dhis2.utils.DateUtils;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.category.Category;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryOption;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.program.ProgramStageModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;

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

/**
 * QUADRAM. Created by Cristian on 22/03/2018.
 */

public class EventInitialRepositoryImpl implements EventInitialRepository {

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
        return Observable.just(d2.eventModule().events.byUid().eq(eventId == null ? "" : eventId).byState().notIn(State.TO_DELETE).withAllChildren().one().get());
    }

    @NonNull
    @Override
    public Observable<List<OrganisationUnit>> orgUnits(String programId) {
        return Observable.fromIterable(d2.organisationUnitModule().organisationUnits.withPrograms().get())
                .flatMapIterable(organisationUnit -> {
                    List<OrganisationUnit> orgUnits = new ArrayList<>();
                    for (Program program : organisationUnit.programs()) {
                        if (program.uid().equals(programId))
                            orgUnits.add(organisationUnit);
                    }
                    return orgUnits;
                })
                .toList()
                .toObservable();
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
        else {
            Date formattedDate = null;
            try {
                formattedDate = DateUtils.uiDateFormat().parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return Observable.fromIterable(d2.organisationUnitModule().organisationUnits
                    .byOpeningDate().after(formattedDate)
                    .byClosedDate().before(formattedDate)
                    .withPrograms().get())
                    .flatMapIterable(organisationUnit -> {
                        List<OrganisationUnit> orgUnits = new ArrayList<>();
                        for (Program program : organisationUnit.programs()) {
                            if (program.uid().equals(programId))
                                orgUnits.add(organisationUnit);
                        }
                        return orgUnits;
                    })
                    .toList()
                    .toObservable();
        }
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

        EventModel eventModel = EventModel.builder()
                .uid(uid)
                .enrollment(enrollmentUid)
                .created(createDate)
                .lastUpdated(createDate)
                .status(EventStatus.ACTIVE)
                .latitude(latitude)
                .longitude(longitude)
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

        EventModel eventModel = EventModel.builder()
                .uid(uid)
                .enrollment(enrollmentUid)
                .created(createDate)
                .lastUpdated(createDate)
                .status(EventStatus.SCHEDULE)
                .latitude(latitude)
                .longitude(longitude)
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

    private void updateProgramTable(Date lastUpdated, String programUid) {
        //TODO: Update program causes crash
        /* ContentValues program = new ContentValues();
        program.put(EnrollmentModel.Columns.LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(lastUpdated));
        briteDatabase.update(ProgramModel.TABLE, program, ProgramModel.Columns.UID + " = ?", programUid);*/
    }

    @Override
    public Observable<String> updateTrackedEntityInstance(String eventId, String trackedEntityInstanceUid, String orgUnitUid) {
        String TEI_QUERY = "SELECT * FROM TrackedEntityInstance WHERE TrackedEntityInstance.uid = ? LIMIT 1";
        return briteDatabase.createQuery(TrackedEntityInstanceModel.TABLE, TEI_QUERY, trackedEntityInstanceUid == null ? "" : trackedEntityInstanceUid)
                .mapToOne(TrackedEntityInstanceModel::create).distinctUntilChanged()
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


   /* @NonNull
    @Override
    public Observable<EventModel> newlyCreatedEvent(long rowId) {
        String SELECT_EVENT_WITH_ROWID = "SELECT * FROM " + EventModel.TABLE + " WHERE " + EventModel.Columns.ID + " = '" + rowId + "'" + " AND " + EventModel.Columns.STATE + " != '" + State.TO_DELETE + "' LIMIT 1";
        return briteDatabase.createQuery(EventModel.TABLE, SELECT_EVENT_WITH_ROWID).mapToOne(EventModel::create);
    }*/

    @NonNull
    @Override
    public Observable<ProgramStage> programStage(String programUid) {
        return Observable.just(d2.programModule().programStages.byProgramUid().eq(programUid == null ? "" : programUid).one().get());
    }

    @NonNull
    @Override
    public Observable<ProgramStage> programStageWithId(String programStageUid) {
        return Observable.just(d2.programModule().programStages.byUid().eq( programStageUid == null ? "" : programStageUid).one().get());
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
        if ((event.coordinate() == null && (!isEmpty(latitude) && !isEmpty(longitude))) ||
                (event.coordinate() != null && (!String.valueOf(event.coordinate().latitude()).equals(latitude) || !String.valueOf(event.coordinate().longitude()).equals(longitude))))
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
            contentValues.put(EventModel.Columns.EVENT_DATE, DateUtils.databaseDateFormat().format(cal.getTime()));
            if(!isEmpty(orgUnitUid))
                contentValues.put(EventModel.Columns.ORGANISATION_UNIT, orgUnitUid);
            contentValues.put(EventModel.Columns.LATITUDE, latitude);
            contentValues.put(EventModel.Columns.LONGITUDE, longitude);
            contentValues.put(EventModel.Columns.ATTRIBUTE_OPTION_COMBO, catOptionCombo);
            contentValues.put(EventModel.Columns.LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(currentDate));
            contentValues.put(EventModel.Columns.STATE, event.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());

            long row = -1;

            try {
                row = briteDatabase.update(EventModel.TABLE, contentValues, EventModel.Columns.UID + " = ?", eventUid);
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
        return event(eventUid);
    }

    @NonNull
    @Override
    public Observable<List<EventModel>> getEventsFromProgramStage(String programUid, String enrollmentUid, String programStageUid) {
        String EVENTS_QUERY = String.format(
                "SELECT Event.* FROM %s JOIN %s " +
                        "ON %s.%s = %s.%s " +
                        "WHERE %s.%s = ? " +
                        "AND %s.%s = ? " +
                        "AND %s.%s = ? " +
                        "AND " + EventModel.TABLE + "." + EventModel.Columns.STATE + " != '" + State.TO_DELETE + "'" +
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

        return briteDatabase.createQuery(EventModel.TABLE, EVENTS_QUERY, programUid == null ? "" : programUid,
                enrollmentUid == null ? "" : enrollmentUid,
                programStageUid == null ? "" : programStageUid)
                .mapToList(EventModel::create);
    }

    @Override
    public Observable<Boolean> accessDataWrite(String programId) {
        String WRITE_PERMISSION = "SELECT ProgramStage.accessDataWrite FROM ProgramStage WHERE ProgramStage.program = ? LIMIT 1";
        String PROGRAM_WRITE_PERMISSION = "SELECT Program.accessDataWrite FROM Program WHERE Program.uid = ? LIMIT 1";
        return briteDatabase.createQuery(ProgramStageModel.TABLE, WRITE_PERMISSION, programId == null ? "" : programId)
                .mapToOne(cursor -> cursor.getInt(0) == 1)
                .flatMap(programStageAccessDataWrite ->
                        briteDatabase.createQuery(ProgramModel.TABLE, PROGRAM_WRITE_PERMISSION, programId == null ? "" : programId)
                                .mapToOne(cursor -> (cursor.getInt(0) == 1) && programStageAccessDataWrite));
    }

    @Override
    public void deleteEvent(String eventId, String trackedEntityInstance) {
        try (Cursor eventCursor = briteDatabase.query("SELECT Event.* FROM Event WHERE Event.uid = ?", eventId)) {
            if (eventCursor != null && eventCursor.moveToNext()) {
                EventModel eventModel = EventModel.create(eventCursor);
                if (eventModel.state() == State.TO_POST) {
                    String DELETE_WHERE = String.format(
                            "%s.%s = ?",
                            EventModel.TABLE, EventModel.Columns.UID
                    );
                    briteDatabase.delete(EventModel.TABLE, DELETE_WHERE, eventId);
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
                    EnrollmentModel enrollment = EnrollmentModel.create(enrollmentCursor);
                    isEnrollmentOpen = enrollment.enrollmentStatus() == EnrollmentStatus.ACTIVE;
                }
            }
        }
        return isEnrollmentOpen;
    }


    private void updateEnrollment(String enrollmentUid) {
        String selectEnrollment = "SELECT * FROM Enrollment WHERE uid = ?";
        try (Cursor enrollmentCursor = briteDatabase.query(selectEnrollment, enrollmentUid)) {
            if (enrollmentCursor != null && enrollmentCursor.moveToFirst()) {
                EnrollmentModel enrollment = EnrollmentModel.create(enrollmentCursor);
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
                TrackedEntityInstanceModel teiModel = TrackedEntityInstanceModel.create(teiCursor);
                ContentValues cv = teiModel.toContentValues();
                cv.put(TrackedEntityInstanceModel.Columns.LAST_UPDATED, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime()));
                cv.put(TrackedEntityInstanceModel.Columns.STATE,
                        teiModel.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());
                briteDatabase.update(TrackedEntityInstanceModel.TABLE, cv, "uid = ?", teiUid);
            }
        }
    }
}