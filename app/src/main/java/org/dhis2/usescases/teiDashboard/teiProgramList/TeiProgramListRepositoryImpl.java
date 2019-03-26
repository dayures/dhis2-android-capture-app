package org.dhis2.usescases.teiDashboard.teiProgramList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.usescases.main.program.ProgramViewModel;
import org.dhis2.utils.CodeGenerator;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.ObjectStyleModel;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitProgramLinkModel;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.NonNull;
import io.reactivex.Observable;

import static org.dhis2.utils.SqlConstants.SELECT;

/**
 * QUADRAM. Created by ppajuelo on 02/11/2017.
 */

public class TeiProgramListRepositoryImpl implements TeiProgramListRepository {

    private final BriteDatabase briteDatabase;
    private final CodeGenerator codeGenerator;

    TeiProgramListRepositoryImpl(CodeGenerator codeGenerator, BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
        this.codeGenerator = codeGenerator;
    }

    public static final String PROGRAM_COLOR_QUERY = String.format(
            "SELECT %s FROM %S " +
                    "WHERE %s = 'Program' AND %s = ?",
            ObjectStyleModel.Columns.COLOR, ObjectStyleModel.TABLE,
            ObjectStyleModel.Columns.OBJECT_TABLE,
            ObjectStyleModel.Columns.UID
    );

    @NonNull
    @Override
    public Observable<List<EnrollmentViewModel>> activeEnrollments(String trackedEntityId) {
        String selectActiveEnrollmentWithTeiId = SELECT +
                "Enrollment.uid," +
                "Enrollment.enrollmentDate," +
                "Enrollment.followup," +
                ObjectStyleModel.TABLE + "." + ObjectStyleModel.Columns.ICON + "," +
                "ObjectStyle.color," +
                "Program.displayName AS programName," +
                "Program.uid AS programUid," +
                "OrganisationUnit.displayName AS OrgUnitName FROM Enrollment " +
                "LEFT JOIN ObjectStyle ON ObjectStyle.uid = Enrollment.program " +
                "JOIN Program ON Program.uid = Enrollment.program " +
                "JOIN OrganisationUnit ON OrganisationUnit.uid = Enrollment.organisationUnit " +
                "WHERE Enrollment.trackedEntityInstance = ? AND Enrollment.status = 'ACTIVE'";
        String[] tableNames = new String[]{SqlConstants.PROGRAM_TABLE, ObjectStyleModel.TABLE, OrganisationUnitProgramLinkModel.TABLE};
        Set<String> tableSet = new HashSet<>(Arrays.asList(tableNames));
        return briteDatabase.createQuery(tableSet, selectActiveEnrollmentWithTeiId, trackedEntityId == null ? "" : trackedEntityId)
                .mapToList(EnrollmentViewModel::fromCursor);
    }

    @NonNull
    @Override
    public Observable<List<EnrollmentViewModel>> otherEnrollments(String trackedEntityId) {
        String selectActiveEnrollmentWithTeiId = SELECT +
                "Enrollment.uid," +
                "Enrollment.enrollmentDate," +
                "Enrollment.followup," +
                ObjectStyleModel.TABLE + "." + ObjectStyleModel.Columns.ICON + "," +
                "ObjectStyle.color," +
                "Program.displayName AS programName," +
                "Program.uid AS programUid," +
                "OrganisationUnit.displayName AS OrgUnitName FROM Enrollment " +
                "LEFT JOIN ObjectStyle ON ObjectStyle.uid = Enrollment.program " +
                "JOIN Program ON Program.uid = Enrollment.program " +
                "JOIN OrganisationUnit ON OrganisationUnit.uid = Enrollment.organisationUnit " +
                "WHERE Enrollment.trackedEntityInstance = ? AND Enrollment.status != 'ACTIVE'";
        String[] tableNames = new String[]{SqlConstants.PROGRAM_TABLE, ObjectStyleModel.TABLE, OrganisationUnitProgramLinkModel.TABLE};
        Set<String> tableSet = new HashSet<>(Arrays.asList(tableNames));
        return briteDatabase.createQuery(tableSet, selectActiveEnrollmentWithTeiId, trackedEntityId == null ? "" : trackedEntityId)
                .mapToList(EnrollmentViewModel::fromCursor);
    }


    private static final String PROGRAM_MODELS_FOR_TEI = SELECT +
            "Program.uid, " +
            "Program.displayName, " +
            "ObjectStyle.color, " +
            ObjectStyleModel.TABLE + "." + ObjectStyleModel.Columns.ICON + "," +
            "Program.programType," +
            "Program.trackedEntityType," +
            "Program.description, " +
            "Program.onlyEnrollOnce, " +
            "Program.accessDataWrite " +
            "FROM Program LEFT JOIN ObjectStyle ON ObjectStyle.uid = Program.uid " +
            "JOIN OrganisationUnitProgramLink ON OrganisationUnitProgramLink.program = Program.uid " +
            "JOIN TrackedEntityInstance ON TrackedEntityInstance.trackedEntityType = Program.trackedEntityType " +
            "WHERE TrackedEntityInstance.uid = ? GROUP BY Program.uid";
    private static final String[] TABLE_NAMES = new String[]{SqlConstants.PROGRAM_TABLE, ObjectStyleModel.TABLE, OrganisationUnitProgramLinkModel.TABLE};
    private static final Set<String> TABLE_SET = new HashSet<>(Arrays.asList(TABLE_NAMES));

    @NonNull
    @Override
    public Observable<List<ProgramViewModel>> allPrograms(String trackedEntityId) {
        return briteDatabase.createQuery(TABLE_SET, PROGRAM_MODELS_FOR_TEI, trackedEntityId == null ? "" : trackedEntityId)
                .mapToList(cursor -> {
                    String uid = cursor.getString(0);
                    String displayName = cursor.getString(1);
                    String color = cursor.getString(2);
                    String icon = cursor.getString(3);
                    String programType = cursor.getString(4);
                    String teiType = cursor.getString(5);
                    String description = cursor.getString(6);
                    Boolean onlyEnrollOnce = cursor.getInt(7) == 1;
                    Boolean accessDataWrite = cursor.getInt(8) == 1;

                    return ProgramViewModel.create(uid, displayName, color, icon, 0, teiType, "", programType, description, onlyEnrollOnce, accessDataWrite);
                });
    }

    @NonNull
    @Override
    public Observable<List<Program>> alreadyEnrolledPrograms(String trackedEntityId) {
        String selectEnrolledProgramsWithTeiId = "SELECT * FROM " + SqlConstants.PROGRAM_TABLE + " JOIN " + SqlConstants.ENROLLMENT_TABLE +
                " ON " + SqlConstants.ENROLLMENT_TABLE + "." + EnrollmentModel.Columns.PROGRAM + "=" + SqlConstants.PROGRAM_TABLE + "." + ProgramModel.Columns.UID +
                " WHERE " + SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + "='%s' GROUP BY " + SqlConstants.PROGRAM_TABLE + "." + ProgramModel.Columns.UID;
        return briteDatabase.createQuery(SqlConstants.ENROLLMENT_TABLE, String.format(selectEnrolledProgramsWithTeiId, trackedEntityId == null ? "" : trackedEntityId))
                .mapToList(Program::create);
    }

    @NonNull
    @Override
    public Observable<String> saveToEnroll(@NonNull String orgUnit, @NonNull String programUid, @NonNull String teiUid, Date enrollmentDate) {
        Date currentDate = Calendar.getInstance().getTime();
        return Observable.defer(() -> {

            ContentValues dataValue = new ContentValues();

            // renderSearchResults time stamp
            dataValue.put(TrackedEntityInstanceModel.Columns.LAST_UPDATED,
                    BaseIdentifiableObject.DATE_FORMAT.format(currentDate));
            dataValue.put(TrackedEntityInstanceModel.Columns.STATE,
                    State.TO_POST.toString());

            if (briteDatabase.update(SqlConstants.TEI_TABLE, dataValue,
                    SqlConstants.TEI_UID + " = ? ", teiUid) <= 0) {
                String message = String.format(Locale.US, "Failed to update tracked entity " +
                                "instance for uid=[%s]",
                        teiUid);
                return Observable.error(new SQLiteConstraintException(message));
            }

            Enrollment enrollmentModel = Enrollment.builder()
                    .uid(codeGenerator.generate())
                    .created(currentDate)
                    .lastUpdated(currentDate)
                    .enrollmentDate(enrollmentDate)
                    .program(programUid)
                    .organisationUnit(orgUnit)
                    .trackedEntityInstance(teiUid)
                    .status(EnrollmentStatus.ACTIVE)
                    .followUp(false)
                    .state(State.TO_POST)
                    .build();

            if (briteDatabase.insert(SqlConstants.ENROLLMENT_TABLE, enrollmentModel.toContentValues()) < 0) {
                String message = String.format(Locale.US, "Failed to insert new enrollment " +
                        "instance for organisationUnit=[%s] and program=[%s]", orgUnit, programUid);
                return Observable.error(new SQLiteConstraintException(message));
            }

            return Observable.just(enrollmentModel.uid());
        });
    }

    @Override
    public Observable<List<OrganisationUnit>> getOrgUnits(String programUid) {

        if (programUid != null) {
            String orgUnitQuery = "SELECT * FROM OrganisationUnit " +
                    "JOIN OrganisationUnitProgramLink ON OrganisationUnitProgramLink.organisationUnit = OrganisationUnit.uid " +
                    "WHERE OrganisationUnitProgramLink.program = ?";
            return briteDatabase.createQuery(OrganisationUnitModel.TABLE, orgUnitQuery, programUid)
                    .mapToList(OrganisationUnit::create);
        } else
            return briteDatabase.createQuery(OrganisationUnitModel.TABLE, " SELECT * FROM OrganisationUnit")
                    .mapToList(OrganisationUnit::create);
    }

    @Override
    public String getProgramColor(@NonNull String programUid) {
        try (Cursor cursor = briteDatabase.query(PROGRAM_COLOR_QUERY, programUid)) {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        return null;
    }

    @Override
    public Program getProgram(String programUid) {
        Program programModel = null;
        try (Cursor programCursor = briteDatabase.query("SELECT * FROM Program WHERE uid = ? LIMIT 1", programUid)) {
            if (programCursor != null && programCursor.moveToFirst())
                programModel = Program.create(programCursor);
        }
        return programModel;
    }
}