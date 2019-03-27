package org.dhis2.usescases.teiDashboard.eventDetail;

import android.content.ContentValues;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.user.UserRepository;
import org.dhis2.utils.DateUtils;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue;
import org.hisp.dhis.android.core.user.UserCredentials;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

final class DataValueStore implements DataEntryStore {
    private static final String SELECT_EVENT = "SELECT * FROM " + SqlConstants.EVENT_TABLE +
            " WHERE " + SqlConstants.EVENT_UID + " = ? " +
            "AND " + SqlConstants.EVENT_TABLE + "." + SqlConstants.EVENT_STATE + " != '" + State.TO_DELETE + "' LIMIT 1";

    @NonNull
    private final BriteDatabase briteDatabase;

    @NonNull
    private final Flowable<UserCredentials> userCredentials;

    @NonNull
    private final String eventUid;
    private final String teiUid;

    DataValueStore(@NonNull BriteDatabase briteDatabase,
                   @NonNull UserRepository userRepository,
                   @NonNull String eventUid, String teiUid) {
        this.briteDatabase = briteDatabase;
        this.eventUid = eventUid;
        this.teiUid = teiUid;
        // we want to re-use results of the user credentials query
        this.userCredentials = userRepository.credentials()
                .cacheWithInitialCapacity(1);
    }

    @NonNull
    @Override
    public Flowable<Long> save(@NonNull String uid, @Nullable String value) {
        return userCredentials
                .switchMap(userCredentialsResult -> {
                    long updated = update(uid, value);
                    if (updated > 0) {
                        updateTEi();
                        return Flowable.just(updated);
                    }

                    return Flowable.just(insert(uid, value, userCredentialsResult.username()));
                })
                .switchMap(this::updateEvent);
    }

    @Override
    public void updateEventStatus(Event eventModel) {
        ContentValues contentValues = new ContentValues();
        Date currentDate = Calendar.getInstance().getTime();
        contentValues.put(SqlConstants.EVENT_LAST_UPDATED, DateUtils.databaseDateFormat().format(currentDate));
        String eventStatus = null;
        switch (eventModel.status()) {
            case SCHEDULE:
            case COMPLETED:
                eventStatus = EventStatus.ACTIVE.name(); //TODO: should check if visited/skiped/overdue
                contentValues.putNull(SqlConstants.EVENT_COMPLETE_DATE);
                break;
            default:
                eventStatus = EventStatus.COMPLETED.name();
                contentValues.put(SqlConstants.EVENT_COMPLETE_DATE, DateUtils.databaseDateFormat().format(currentDate));
                break;

        }
        contentValues.put(SqlConstants.EVENT_STATUS, eventStatus);
        contentValues.put(SqlConstants.EVENT_STATE, eventModel.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());
        updateProgramTable(currentDate, eventModel.program());

        briteDatabase.update(SqlConstants.EVENT_TABLE, contentValues, SqlConstants.EVENT_UID + "= ?", eventModel.uid());
        updateTEi();
    }

    @Override
    public void updateEvent(@NonNull Date eventDate, @NonNull Event eventModel) {
        ContentValues contentValues = new ContentValues();
        Date currentDate = Calendar.getInstance().getTime();
        contentValues.put(SqlConstants.EVENT_LAST_UPDATED, DateUtils.databaseDateFormat().format(currentDate));
        contentValues.put(SqlConstants.EVENT_DATE, DateUtils.databaseDateFormat().format(eventDate));
        if (eventDate.before(currentDate))
            contentValues.put(SqlConstants.EVENT_STATUS, EventStatus.ACTIVE.name());
        briteDatabase.update(SqlConstants.EVENT_TABLE, contentValues, SqlConstants.EVENT_UID + "= ?", eventModel.uid());
        updateTEi();
    }

    @SuppressWarnings({"squid:S1172", "squid:CommentedOutCodeLine"})
    private void updateProgramTable(Date lastUpdated, String programUid) {
        /*ContentValues program = new ContentValues(); //TODO: Crash if active
        program.put(SqlConstants.ENROLLMENT_LAST_UPDATED, BaseIdentifiableObject.DATE_FORMAT.format(lastUpdated));
        briteDatabase.update(SqlConstants.PROGRAM_TABLE, program, SqlConstants.PROGRAM_UID + " = ?", programUid);*/
    }

    private long update(@NonNull String uid, @Nullable String value) {
        ContentValues dataValue = new ContentValues();

        // renderSearchResults time stamp
        dataValue.put(SqlConstants.TEI_DATA_VALUE_LAST_UPDATED,
                BaseIdentifiableObject.DATE_FORMAT.format(Calendar.getInstance().getTime()));
        if (value == null) {
            dataValue.putNull(SqlConstants.TEI_DATA_VALUE_VALUE);
        } else {
            dataValue.put(SqlConstants.TEI_DATA_VALUE_VALUE, value);
        }

        // ToDo: write test cases for different events
        return (long) briteDatabase.update(SqlConstants.TEI_DATA_VALUE_TABLE, dataValue,
                SqlConstants.TEI_DATA_VALUE_DATA_ELEMENT + " = ? AND " +
                        SqlConstants.TEI_DATA_VALUE_EVENT + " = ?",
                uid,
                eventUid);
    }

    private long insert(@NonNull String uid, @Nullable String value, @NonNull String storedBy) {
        Date created = Calendar.getInstance().getTime();
        TrackedEntityDataValue dataValueModel =
                TrackedEntityDataValue.builder()
                        .created(created)
                        .lastUpdated(created)
                        .dataElement(uid)
                        .event(eventUid)
                        .value(value)
                        .storedBy(storedBy)
                        .build();
        return briteDatabase.insert(SqlConstants.TEI_DATA_VALUE_TABLE,
                dataValueModel.toContentValues());
    }

    private Flowable<Long> updateEvent(long status) {
        return briteDatabase.createQuery(SqlConstants.EVENT_TABLE, SELECT_EVENT, eventUid)
                .mapToOne(Event::create).take(1).toFlowable(BackpressureStrategy.LATEST)
                .switchMap(eventModel -> {
                    if (State.SYNCED.equals(eventModel.state()) || State.TO_DELETE.equals(eventModel.state()) ||
                            State.ERROR.equals(eventModel.state())) {

                        ContentValues values = eventModel.toContentValues();
                        values.put(SqlConstants.EVENT_STATE, State.TO_UPDATE.toString());

                        if (briteDatabase.update(SqlConstants.EVENT_TABLE, values,
                                SqlConstants.EVENT_UID + " = ?", eventUid) <= 0) {

                            throw new IllegalStateException(String.format(Locale.US, "Event=[%s] " +
                                    "has not been successfully updated", eventUid));
                        }

                        updateTEi();
                    }

                    return Flowable.just(status);
                });
    }


    private void updateTEi() {

        ContentValues tei = new ContentValues();
        tei.put(SqlConstants.TEI_LAST_UPDATED, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime()));
        tei.put(SqlConstants.TEI_STATE, State.TO_UPDATE.name());// TODO: Check if state is TO_POST
        // TODO: and if so, keep the TO_POST state
        briteDatabase.update(SqlConstants.TEI_TABLE, tei, "uid = ?", teiUid);
    }
}
