package org.dhis2.usescases.teiDashboard;

import android.content.Context;

import org.dhis2.data.tuples.Pair;
import org.dhis2.data.tuples.Trio;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.enrollment.note.Note;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.program.ProgramIndicator;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.relationship.RelationshipType;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;

import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * QUADRAM. Created by ppajuelo on 30/11/2017.
 */

public interface DashboardRepository {

    Observable<List<ProgramStage>> getProgramStages(String programStages);

    Observable<Enrollment> getEnrollment(String programUid, String teiUid);

    Observable<List<Event>> getTEIEnrollmentEvents(String programUid, String teiUid);

    Observable<List<Event>> getEnrollmentEventsWithDisplay(String programUid, String teiUid);

    Observable<List<TrackedEntityAttributeValue>> getTEIAttributeValues(String programUid, String teiUid);

    Flowable<List<ProgramIndicator>> getIndicators(String programUid);

    boolean setFollowUp(String enrollmentUid);

    Flowable<List<Note>> getNotes(String programUid, String teUid);

    Consumer<Pair<String, Boolean>> handleNote();

    Observable<Boolean> handleNote(Pair<String, Boolean> pair);

    void setDashboardDetails(String teiUid, String programUid);

    Observable<List<TrackedEntityAttributeValue>> mainTrackedEntityAttributes(String teiUid);

    Event updateState(Event eventModel, EventStatus newStatus);

    Flowable<Long> updateEnrollmentStatus(@NonNull String uid, @NonNull EnrollmentStatus value);

    Observable<ProgramStage> displayGenerateEvent(String eventUid);

    Observable<String> generateNewEvent(String lastModifiedEventUid, Integer standardInterval);

    Observable<Trio<ProgramIndicator, String, String>> getLegendColorForIndicator(ProgramIndicator programIndicator, String value);

    Observable<String> generateNewEventFromDate(String lastModifiedEventUid, Calendar chosenDate);

    void updateTeiState();

    String relationshipTeiSync(String teiUid);

    Integer getObjectStyle(Context context, String uid);

    Observable<List<Pair<RelationshipType, String>>> relationshipsForTeiType(String teType);

    Observable<CategoryCombo> catComboForProgram(String program);

    void setDefaultCatOptCombToEvent(String eventUid);
}