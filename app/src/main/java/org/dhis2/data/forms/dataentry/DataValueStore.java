package org.dhis2.data.forms.dataentry;

import android.content.ContentValues;
import android.database.Cursor;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.tuples.Pair;
import org.dhis2.data.user.UserRepository;
import org.dhis2.utils.DateUtils;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.android.core.user.UserCredentials;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import static org.dhis2.data.forms.dataentry.DataEntryStore.valueType.ATTR;
import static org.dhis2.data.forms.dataentry.DataEntryStore.valueType.DATA_ELEMENT;
import static org.dhis2.utils.SqlConstants.AND;
import static org.dhis2.utils.SqlConstants.EQUAL;
import static org.dhis2.utils.SqlConstants.FROM;
import static org.dhis2.utils.SqlConstants.JOIN;
import static org.dhis2.utils.SqlConstants.ON;
import static org.dhis2.utils.SqlConstants.QUESTION_MARK;
import static org.dhis2.utils.SqlConstants.SELECT;
import static org.dhis2.utils.SqlConstants.WHERE;

public final class DataValueStore implements DataEntryStore {
    private static final String SELECT_EVENT = "SELECT * FROM " + SqlConstants.EVENT_TABLE +
            " WHERE " + SqlConstants.EVENT_UID + EQUAL + QUESTION_MARK + AND + SqlConstants.EVENT_STATE + " != '" + State.TO_DELETE + "' LIMIT 1";

    @NonNull
    private final BriteDatabase briteDatabase;
    @NonNull
    private final Flowable<UserCredentials> userCredentials;

    @NonNull
    private final String eventUid;

    public DataValueStore(@NonNull BriteDatabase briteDatabase,
                          @NonNull UserRepository userRepository,
                          @NonNull String eventUid) {
        this.briteDatabase = briteDatabase;
        this.eventUid = eventUid;

        // we want to re-use results of the user credentials query
        this.userCredentials = userRepository.credentials()
                .cacheWithInitialCapacity(1);
    }

    @NonNull
    @Override
    public Flowable<Long> save(@NonNull String uid, @Nullable String value) {
        return userCredentials
                .map(userCredentialsModel -> Pair.create(userCredentialsModel, getValueType(uid)))
                .filter(userCredentialAndType -> {
                    String currentValue = currentValue(uid, userCredentialAndType.val1());
                    return !Objects.equals(currentValue, value);
                })
                .switchMap(userCredentialAndType -> {
                    if (value == null)
                        return Flowable.just(delete(uid, userCredentialAndType.val1()));

                    long updated = update(uid, value, userCredentialAndType.val1());
                    if (updated > 0) {
                        return Flowable.just(updated);
                    }

                    return Flowable.just(insert(uid, value, userCredentialAndType.val0().username(), userCredentialAndType.val1()));
                })
                .switchMap(this::updateEvent);
    }


    private long update(@NonNull String uid, @Nullable String value, valueType valueType) {
        ContentValues dataValue = new ContentValues();
        if (valueType == DATA_ELEMENT) {
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
                    SqlConstants.TEI_DATA_VALUE_DATA_ELEMENT + EQUAL + QUESTION_MARK + AND +
                            SqlConstants.TEI_DATA_VALUE_EVENT + " = ?", uid, eventUid);
        } else {
            dataValue.put(SqlConstants.TE_ATTR_VALUE_LAST_UPDATED,
                    BaseIdentifiableObject.DATE_FORMAT.format(Calendar.getInstance().getTime()));
            if (value == null) {
                dataValue.putNull(SqlConstants.TE_ATTR_VALUE_VALUE);
            } else {
                dataValue.put(SqlConstants.TE_ATTR_VALUE_VALUE, value);
            }

            String teiUid = "";
            try (Cursor enrollmentCursor = briteDatabase.query(
                    SELECT + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + FROM + SqlConstants.TE_ATTR_VALUE_TABLE + " " +
                            JOIN + SqlConstants.ENROLLMENT_TABLE + ON + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + EQUAL + SqlConstants.TE_ATTR_VALUE_TABLE + "." + SqlConstants.TE_ATTR_VALUE_TEI +
                            WHERE + SqlConstants.TE_ATTR_VALUE_TABLE + "." + SqlConstants.TE_ATTR_VALUE_TE_ATTR + EQUAL + QUESTION_MARK, uid)) {
                if (enrollmentCursor.moveToFirst()) {
                    teiUid = enrollmentCursor.getString(0);
                }
            }

            return (long) briteDatabase.update(SqlConstants.TE_ATTR_VALUE_TABLE, dataValue,
                    SqlConstants.TE_ATTR_VALUE_TE_ATTR + EQUAL + QUESTION_MARK + AND +
                            SqlConstants.TE_ATTR_VALUE_TEI + " = ? ",
                    uid, teiUid);
        }
    }

    private valueType getValueType(@Nonnull String uid) {
        String attrUid = null;
        try (Cursor attrCursor = briteDatabase.query("SELECT TrackedEntityAttribute.uid FROM TrackedEntityAttribute " +
                "WHERE TrackedEntityAttribute.uid = ?", uid)) {
            if (attrCursor.moveToFirst()) {
                attrUid = attrCursor.getString(0);
            }
        }

        return attrUid != null ? ATTR : valueType.DATA_ELEMENT;
    }

    private String currentValue(@NonNull String uid, valueType valueType) {
        String value = "";
        if (valueType == DATA_ELEMENT) {
            try (Cursor cursor = briteDatabase.query("SELECT TrackedEntityDataValue.value FROM TrackedEntityDataValue " +
                    "WHERE dataElement = ? AND event = ?", uid, eventUid)) {
                if (cursor.moveToFirst())
                    value = cursor.getString(0);
            }
        } else {
            try (Cursor cursor = briteDatabase.query("SELECT TrackedEntityAttributeValue.value FROM TrackedEntityAttributeValue " +
                    JOIN + SqlConstants.ENROLLMENT_TABLE + ON + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + EQUAL + SqlConstants.TE_ATTR_VALUE_TABLE + "." + SqlConstants.TE_ATTR_VALUE_TEI +
                    "JOIN Event ON Event.enrollment = Enrollment.uid " +
                    "WHERE TrackedEntityAttributeValue.trackedEntityAttribute = ? " +
                    "AND Event.uid = ?", uid, eventUid)) {
                if (cursor.moveToFirst())
                    value = cursor.getString(0);
            }
        }
        return value;
    }

    private long insert(@NonNull String uid, @Nullable String value, @NonNull String storedBy, valueType valueType) {
        Date created = Calendar.getInstance().getTime();
        if (valueType == DATA_ELEMENT) {
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
        } else {
            String teiUid = null;
            try (Cursor enrollmentCursor = briteDatabase.query(
                    SELECT + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + FROM + SqlConstants.TE_ATTR_VALUE_TABLE + " " +
                            JOIN + SqlConstants.ENROLLMENT_TABLE + ON + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + EQUAL + SqlConstants.TE_ATTR_VALUE_TABLE + "." + SqlConstants.TE_ATTR_VALUE_TEI +
                            WHERE + SqlConstants.TE_ATTR_VALUE_TABLE + "." + SqlConstants.TE_ATTR_VALUE_TE_ATTR + EQUAL + QUESTION_MARK, uid)) {
                if (enrollmentCursor.moveToFirst()) {
                    teiUid = enrollmentCursor.getString(0);
                }
            }

            TrackedEntityAttributeValue attributeValueModel =
                    TrackedEntityAttributeValue.builder()
                            .created(created)
                            .lastUpdated(created)
                            .trackedEntityAttribute(uid)
                            .trackedEntityInstance(teiUid)
                            .build();
            return briteDatabase.insert(SqlConstants.TE_ATTR_VALUE_TABLE,
                    attributeValueModel.toContentValues());
        }
    }

    private long delete(@NonNull String uid, valueType valueType) {
        if (valueType == DATA_ELEMENT)
            return (long) briteDatabase.delete(SqlConstants.TEI_DATA_VALUE_TABLE,
                    SqlConstants.TEI_DATA_VALUE_DATA_ELEMENT + EQUAL + QUESTION_MARK + AND +
                            SqlConstants.TEI_DATA_VALUE_EVENT + " = ?",
                    uid, eventUid);
        else {
            String teiUid = "";
            try (Cursor enrollmentCursor = briteDatabase.query(
                    SELECT + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + FROM + SqlConstants.TE_ATTR_VALUE_TABLE + " " +
                            JOIN + SqlConstants.ENROLLMENT_TABLE + ON + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + EQUAL + SqlConstants.TE_ATTR_VALUE_TABLE + "." + SqlConstants.TE_ATTR_VALUE_TEI +
                            WHERE + SqlConstants.TE_ATTR_VALUE_TABLE + "." + SqlConstants.TE_ATTR_VALUE_TE_ATTR + EQUAL + QUESTION_MARK, uid)) {
                if (enrollmentCursor.moveToFirst()) {
                    teiUid = enrollmentCursor.getString(0);
                }
            }

            return (long) briteDatabase.delete(SqlConstants.TE_ATTR_VALUE_TABLE,
                    SqlConstants.TE_ATTR_VALUE_TE_ATTR + EQUAL + QUESTION_MARK + AND +
                            SqlConstants.TE_ATTR_VALUE_TEI + " = ? ",
                    uid, teiUid);
        }
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
                    }

                    if (eventModel.enrollment() != null) {
                        updateTei(eventModel);
                    }

                    return Flowable.just(status);
                });
    }

    private void updateTei(Event event) {
        TrackedEntityInstance tei = null;
        try (Cursor teiCursor = briteDatabase.query("SELECT TrackedEntityInstance .* FROM TrackedEntityInstance " +
                "JOIN Enrollment ON Enrollment.trackedEntityInstance = TrackedEntityInstance.uid WHERE Enrollment.uid = ?", event.enrollment())) {
            if (teiCursor.moveToFirst())
                tei = TrackedEntityInstance.create(teiCursor);
        } finally {
            if (tei != null) {
                ContentValues cv = tei.toContentValues();
                cv.put(SqlConstants.TEI_STATE, tei.state() == State.TO_POST ? State.TO_POST.name() : State.TO_UPDATE.name());
                cv.put(SqlConstants.TEI_LAST_UPDATED, DateUtils.databaseDateFormat().format(Calendar.getInstance().getTime()));

                briteDatabase.update(SqlConstants.TEI_TABLE, cv, "uid = ?", tei.uid());
            }
        }
    }

}
