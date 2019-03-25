package org.dhis2.usescases.eventsWithoutRegistration.eventInitial;

import android.content.Context;

import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.ProgramStage;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;

/**
 * QUADRAM. Created by Cristian E. on 02/11/2017.
 */
@SuppressWarnings("squid:S00107")
public interface EventInitialRepository {

    @NonNull
    Observable<Event> event(String eventId);

    @NonNull
    Observable<List<OrganisationUnit>> orgUnits(String programId);

    @NonNull
    Observable<CategoryCombo> catComboModel(String programUid);

    @NonNull
    Observable<List<CategoryOptionCombo>> catCombo(String programUid);

    @NonNull
    Observable<List<OrganisationUnit>> filteredOrgUnits(String date, String programId);

    Observable<String> createEvent(String enrollmentUid, @Nullable String trackedEntityInstanceUid,
                                   @NonNull Context context, @NonNull String program,
                                   @NonNull String programStage, @NonNull Date date,
                                   @NonNull String orgUnitUid, @NonNull String catComboUid,
                                   @NonNull String catOptionUid, @NonNull String latitude, @NonNull String longitude);

    Observable<String> scheduleEvent(String enrollmentUid, @Nullable String trackedEntityInstanceUid,
                                     @NonNull Context context, @NonNull String program,
                                     @NonNull String programStage, @NonNull Date dueDate,
                                     @NonNull String orgUnitUid, @NonNull String catComboUid,
                                     @NonNull String catOptionUid, @NonNull String latitude, @NonNull String longitude);

    Observable<String> updateTrackedEntityInstance(String eventId, String trackedEntityInstanceUid, String orgUnitUid);

    @NonNull
    Observable<Event> newlyCreatedEvent(long rowId);

    @NonNull
    Observable<ProgramStage> programStage(String programUid);

    @NonNull
    Observable<ProgramStage> programStageWithId(String programStageUid);

    @NonNull
    Observable<Event> editEvent(String trackedEntityInstance, String eventUid, String date, String orgUnitUid, String catComboUid, String catOptionCombo, String latitude, String longitude);

    @NonNull
    Observable<List<Event>> getEventsFromProgramStage(String programUid, String enrollmentUid, String programStageUid);

    Observable<Boolean> accessDataWrite(String programId);

    void deleteEvent(String eventId, String trackedEntityInstance);

    boolean isEnrollmentOpen();
}
