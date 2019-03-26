package org.dhis2.usescases.teiDashboard.teiDataDetail;

import android.content.ContentValues;
import android.database.sqlite.SQLiteStatement;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.tuples.Pair;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;

import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import static org.hisp.dhis.android.core.utils.StoreUtils.sqLiteBind;

public final class EnrollmentStatusStore implements EnrollmentStatusEntryStore {

    private static final String UPDATE = "UPDATE Enrollment\n" +
            "SET lastUpdated = ?, status = ?\n" +
            "WHERE uid = ?;";

    private static final String INSERT = "INSERT INTO TrackedEntityAttributeValue ( " +
            "created, lastUpdated, value, trackedEntityAttribute, trackedEntityInstance" +
            ") VALUES (?, ?, ?, ?, (\n" +
            "  SELECT trackedEntityInstance FROM Enrollment WHERE uid = ? LIMIT 1\n" +
            "));";

    private static final String SELECT_TEI = "SELECT *\n" +
            "FROM TrackedEntityInstance\n" +
            "WHERE uid IN (\n" +
            "  SELECT trackedEntityInstance\n" +
            "  FROM Enrollment\n" +
            "  WHERE Enrollment.uid = ?\n" +
            ") LIMIT 1;";

    @NonNull
    private final BriteDatabase briteDatabase;

    @NonNull
    private final SQLiteStatement updateStatement;

    @NonNull
    private final SQLiteStatement insertStatement;

    @NonNull
    private final String enrollment;

    public EnrollmentStatusStore(@NonNull BriteDatabase briteDatabase, @NonNull String enrollment) {
        this.enrollment = enrollment;
        this.briteDatabase = briteDatabase;

        updateStatement = briteDatabase.getWritableDatabase()
                .compileStatement(UPDATE);
        insertStatement = briteDatabase.getWritableDatabase()
                .compileStatement(INSERT);
    }

    @NonNull
    @Override
    public Flowable<Long> save(@NonNull String uid, @NonNull EnrollmentStatus value) {
        return Flowable
                .defer(() -> {
                    long updated = update(value);
                    return Flowable.just(updated);
                })
                .switchMap(this::updateEnrollment);
    }

    @NonNull
    @Override
    public Flowable<EnrollmentStatus> enrollmentStatus(@NonNull String enrollmentUid) {
        String query = "SELECT Enrollment.* FROM Enrollment WHERE Enrollment.uid = ? LIMIT 1";
        return briteDatabase.createQuery(SqlConstants.ENROLLMENT_TABLE, query, enrollmentUid)
                .mapToOne(Enrollment::create)
                .map(Enrollment::status).toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public Flowable<Pair<Double, Double>> enrollmentCoordinates() {
        return briteDatabase.createQuery(SqlConstants.ENROLLMENT_TABLE, "SELECT * FROM Enrollment WHERE uid = ? LIMIT 1", enrollment)
                .mapToOne(Enrollment::create)
                .filter(enrollmentModel -> enrollmentModel.coordinate().latitude() != null && enrollmentModel.coordinate().longitude() != null)
                .map(enrollmentModel ->
                        Pair.create(enrollmentModel.coordinate().latitude(), enrollmentModel.coordinate().longitude()))
                .toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public Flowable<Long> saveCoordinates(double latitude, double longitude) {
        return Flowable.defer(() -> {
            ContentValues cv = new ContentValues();
            cv.put(SqlConstants.ENROLLMENT_LATITUDE, latitude);
            cv.put(SqlConstants.ENROLLMENT_LONGITUDE, longitude);
            long updated = briteDatabase.update(SqlConstants.ENROLLMENT_TABLE, cv, "uid = ?", enrollment);
            return Flowable.just(updated);
        })
                .switchMap(this::updateEnrollment);

    }

    private long update(EnrollmentStatus value) {
        sqLiteBind(updateStatement, 1, BaseIdentifiableObject.DATE_FORMAT
                .format(Calendar.getInstance().getTime()));
        sqLiteBind(updateStatement, 2, value);
        sqLiteBind(updateStatement, 3, enrollment);

        long updated = briteDatabase.executeUpdateDelete(
                SqlConstants.TE_ATTR_VALUE_TABLE, updateStatement);
        updateStatement.clearBindings();

        return updated;
    }


    @NonNull
    private Flowable<Long> updateEnrollment(long status) {
        return briteDatabase.createQuery(SqlConstants.TEI_TABLE, SELECT_TEI, enrollment)
                .mapToOne(TrackedEntityInstance::create).take(1).toFlowable(BackpressureStrategy.LATEST)
                .switchMap(tei -> {
                    if (State.SYNCED.equals(tei.state()) || State.TO_DELETE.equals(tei.state()) ||
                            State.ERROR.equals(tei.state())) {
                        ContentValues values = tei.toContentValues();
                        values.put(TrackedEntityInstanceModel.Columns.STATE, State.TO_UPDATE.toString());

                        if (briteDatabase.update(SqlConstants.TEI_TABLE, values,
                                SqlConstants.TEI_UID + " = ?", tei.uid()) <= 0) {

                            throw new IllegalStateException(String.format(Locale.US, "Tei=[%s] " +
                                    "has not been successfully updated", tei.uid()));
                        }
                    }

                    return Flowable.just(status);
                });
    }
}
