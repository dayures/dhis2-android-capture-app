package org.dhis2.usescases.teiDashboard.eventDetail;

import android.content.ContentValues;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.tuples.Pair;
import org.dhis2.utils.DateUtils;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.program.ProgramStageDataElement;
import org.hisp.dhis.android.core.program.ProgramStageDataElementModel;
import org.hisp.dhis.android.core.program.ProgramStageModel;
import org.hisp.dhis.android.core.program.ProgramStageSection;
import org.hisp.dhis.android.core.program.ProgramStageSectionModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValueModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * QUADRAM. Created by ppajuelo on 02/11/2017.
 */

public class EventDetailRepositoryImpl implements EventDetailRepository {

    private static final String ORG_UNIT_NAME = "SELECT OrganisationUnit.displayName FROM OrganisationUnit " +
            "JOIN Event ON Event.organisationUnit = OrganisationUnit.uid " +
            "WHERE Event.uid = ? " +
            "LIMIT 1";
    private static final String ORG_UNIT = "SELECT OrganisationUnit.* FROM OrganisationUnit " +
            "JOIN Event ON Event.organisationUnit = OrganisationUnit.uid " +
            "WHERE Event.uid = ? " +
            "LIMIT 1";

    private final BriteDatabase briteDatabase;
    private final String eventUid;
    private final String teiUid;


    EventDetailRepositoryImpl(BriteDatabase briteDatabase, String eventUid, String teiUid) {
        this.briteDatabase = briteDatabase;
        this.eventUid = eventUid;
        this.teiUid = teiUid;
    }

    @NonNull
    @Override
    public Observable<Event> eventModelDetail(String uid) {
        String selectEventWithUid = "SELECT * FROM " + EventModel.TABLE + " WHERE " + EventModel.Columns.UID + "='" + uid + "' " +
                "AND " + EventModel.TABLE + "." + EventModel.Columns.STATE + " != '" + State.TO_DELETE + "' LIMIT 1";
        return briteDatabase.createQuery(EventModel.TABLE, selectEventWithUid)
                .mapToOne(Event::create);
    }

    @NonNull
    @Override
    public Observable<List<ProgramStageSection>> programStageSection(String eventUid) {
        String selectProgramStageSections = String.format(
                "SELECT %s.* FROM %s " +
                        "JOIN %s ON %s.%s = %s.%s " +
                        "WHERE %s.%s = ? " +
                        "AND " + EventModel.TABLE + "." + EventModel.Columns.STATE + " != '" + State.TO_DELETE + "' " +
                        "ORDER BY %s.%s",
                ProgramStageSectionModel.TABLE, ProgramStageSectionModel.TABLE,
                EventModel.TABLE, EventModel.TABLE, EventModel.Columns.PROGRAM_STAGE, ProgramStageSectionModel.TABLE, ProgramStageSectionModel.Columns.PROGRAM_STAGE,
                EventModel.TABLE, EventModel.Columns.UID,
                ProgramStageSectionModel.TABLE, ProgramStageSectionModel.Columns.SORT_ORDER
        );
        return briteDatabase.createQuery(EventModel.TABLE, selectProgramStageSections, eventUid == null ? "" : eventUid)
                .mapToList(ProgramStageSection::create);
    }

    @NonNull
    @Override
    public Observable<List<ProgramStageDataElement>> programStageDataElement(String eventUid) {
        String selectProgramStageDe = String.format(
                "SELECT %s.* FROM %s " +
                        "JOIN %s ON %s.%s =%s.%s " +
                        "WHERE %s.%s = ? " +
                        "AND " + EventModel.TABLE + "." + EventModel.Columns.STATE + " != '" + State.TO_DELETE + "'",
                ProgramStageDataElementModel.TABLE, ProgramStageDataElementModel.TABLE,
                EventModel.TABLE, EventModel.TABLE, EventModel.Columns.PROGRAM_STAGE, ProgramStageDataElementModel.TABLE, ProgramStageDataElementModel.Columns.PROGRAM_STAGE,
                EventModel.TABLE, EventModel.Columns.UID
        );
        return briteDatabase.createQuery(EventModel.TABLE, selectProgramStageDe, eventUid == null ? "" : eventUid)
                .mapToList(ProgramStageDataElement::create);
    }

    @NonNull
    @Override
    public Observable<List<TrackedEntityDataValue>> dataValueModelList(String eventUid) {
        String selectTrackedEntityDataValueWithEventUid = "SELECT * FROM " + TrackedEntityDataValueModel.TABLE + " WHERE " + TrackedEntityDataValueModel.Columns.EVENT + "=";
        String uid = eventUid == null ? "" : eventUid;
        return briteDatabase.createQuery(TrackedEntityDataValueModel.TABLE, selectTrackedEntityDataValueWithEventUid + "'" + uid + "'")
                .mapToList(TrackedEntityDataValue::create);
    }

    @NonNull
    @Override
    public Observable<ProgramStage> programStage(String eventUid) {
        String query = "SELECT ProgramStage.* FROM ProgramStage " +
                "JOIN Event ON Event.programStage = ProgramStage.uid " +
                "WHERE Event.uid = ? LIMIT 1";
        return briteDatabase.createQuery(ProgramStageModel.TABLE, query, eventUid == null ? "" : eventUid)
                .mapToOne(ProgramStage::create);
    }

    @Override
    public void deleteNotPostedEvent(String eventUid) {
        String deleteWhere = String.format(
                "%s.%s = ",
                EventModel.TABLE, EventModel.Columns.UID
        );
        String id = eventUid == null ? "" : eventUid;
        briteDatabase.delete(EventModel.TABLE, deleteWhere + "'" + id + "'");
    }

    @Override
    public void deletePostedEvent(Event eventModel) {
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
                .status(eventModel.status())
                .state(State.TO_DELETE)
                .build();

        if (event != null) {
            briteDatabase.update(EventModel.TABLE, event.toContentValues(), EventModel.Columns.UID + " = ?", event.uid());
            updateTEi();
        }


        updateProgramTable(currentDate, eventModel.program());
    }

    @NonNull
    @Override
    public Observable<String> orgUnitName(String eventUid) {
        return briteDatabase.createQuery(OrganisationUnitModel.TABLE, ORG_UNIT_NAME, eventUid == null ? "" : eventUid)
                .mapToOne(cursor -> cursor.getString(0));
    }

    @NonNull
    @Override
    public Observable<OrganisationUnit> orgUnit(String eventUid) {
        return briteDatabase.createQuery(OrganisationUnitModel.TABLE, ORG_UNIT, eventUid == null ? "" : eventUid)
                .mapToOne(OrganisationUnit::create);
    }

    @Override
    public Observable<List<OrganisationUnit>> getOrgUnits() {
        String eventOrgUnits = "SELECT OrganisationUnit.* FROM OrganisationUnit " +
                "JOIN OrganisationUnitProgramLink ON OrganisationUnitProgramLink.organisationUnit = OrganisationUnit.uid " +
                "JOIN Event ON Event.program = OrganisationUnitProgramLink.program " +
                "WHERE Event.uid = ?";
        return briteDatabase.createQuery(OrganisationUnitModel.TABLE, eventOrgUnits, eventUid).mapToList(OrganisationUnit::create);
    }

    @Override
    public Observable<Pair<String, List<CategoryOptionCombo>>> getCategoryOptionCombos() {
        String getCatComboFromEvent = "SELECT CategoryCombo.* FROM CategoryCombo " +
                "WHERE CategoryCombo.uid IN (" +
                "SELECT Program.categoryCombo FROM Program " +
                "JOIN Event ON Event.program = Program.uid " +
                "WHERE Event.uid = ? LIMIT 1)";
        String selectCategoryCombo = String.format("SELECT * FROM %s WHERE %s.%s = ?",
                CategoryOptionComboModel.TABLE, CategoryOptionComboModel.TABLE, CategoryOptionComboModel.Columns.CATEGORY_COMBO);
        return briteDatabase.createQuery(CategoryComboModel.TABLE, getCatComboFromEvent, eventUid == null ? "" : eventUid)
                .mapToOne(CategoryCombo::create)
                .flatMap(catCombo -> {
                    if (catCombo != null && !catCombo.isDefault())
                        return briteDatabase.createQuery(CategoryOptionComboModel.TABLE, selectCategoryCombo, catCombo.uid()).mapToList(CategoryOptionCombo::create)
                                .map(list -> Pair.create(catCombo.name(), list));
                    else
                        return Observable.just(Pair.create("", new ArrayList<>()));
                });
    }

    @NonNull
    @Override
    public Flowable<EventStatus> eventStatus(String eventUid) {
        return briteDatabase.createQuery(EventModel.TABLE, "SELECT Event.status FROM Event WHERE Event.uid = ? LIMIT 1", eventUid == null ? "" : eventUid)
                .mapToOne(cursor -> EventStatus.valueOf(cursor.getString(0))).toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public Observable<Program> getProgram(String eventUid) {
        return briteDatabase.createQuery(ProgramModel.TABLE, "SELECT Program.* FROM Program JOIN Event ON Event.program = Program.uid WHERE Event.uid = ? LIMIT 1", eventUid == null ? "" : eventUid)
                .mapToOne(Program::create);
    }

    @Override
    public void saveCatOption(CategoryOptionCombo selectedOption) {
        ContentValues event = new ContentValues();
        event.put(EventModel.Columns.ATTRIBUTE_OPTION_COMBO, selectedOption.uid());
        event.put(EventModel.Columns.STATE, State.TO_UPDATE.name()); // TODO: Check if state is TO_POST
        // TODO: and if so, keep the TO_POST state

        briteDatabase.update(EventModel.TABLE, event, EventModel.Columns.UID + " = ?", eventUid == null ? "" : eventUid);
        updateTEi();
    }

    @Override
    public Observable<Boolean> isEnrollmentActive(String eventUid) {
        return briteDatabase.createQuery(EnrollmentModel.TABLE,
                "SELECT Enrollment.* FROM Enrollment " +
                        "JOIN Event ON Event.enrollment = Enrollment.uid " +
                        "WHERE Event.uid = ?", eventUid)
                .mapToOne(Enrollment::create)
                .map(enrollment -> enrollment.status() == EnrollmentStatus.ACTIVE);
    }

    @SuppressWarnings({"squid:S1172", "squid:CommentedOutCodeLine"})
    private void updateProgramTable(Date lastUpdated, String programUid) {
       /* ContentValues program = new ContentValues();  TODO: Crash if active
        program.put(EnrollmentModel.Columns.LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(lastUpdated));
        briteDatabase.update(ProgramModel.TABLE, program, ProgramModel.Columns.UID + " = ?", programUid);*/
    }

    private void updateTEi() {

        ContentValues tei = new ContentValues();
        tei.put(TrackedEntityInstanceModel.Columns.LAST_UPDATED, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime()));
        tei.put(TrackedEntityInstanceModel.Columns.STATE, State.TO_UPDATE.name());// TODO: Check if state is TO_POST
        // TODO: and if so, keep the TO_POST state
        briteDatabase.update(TrackedEntityInstanceModel.TABLE, tei, "uid = ?", teiUid);
    }
}