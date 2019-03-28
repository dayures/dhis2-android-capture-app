package org.dhis2.usescases.searchTrackEntity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.tuples.Trio;
import org.dhis2.usescases.searchTrackEntity.adapters.SearchTeiModel;
import org.dhis2.utils.CodeGenerator;
import org.dhis2.utils.Constants;
import org.dhis2.utils.SqlConstants;
import org.dhis2.utils.ValueUtils;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.State;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.option.Option;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityTypeAttributeTableInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Flowable;
import io.reactivex.Observable;

import static android.text.TextUtils.isEmpty;
import static org.dhis2.utils.SqlConstants.AND;
import static org.dhis2.utils.SqlConstants.FROM;
import static org.dhis2.utils.SqlConstants.JOIN_TABLE_ON;
import static org.dhis2.utils.SqlConstants.SELECT;

/**
 * QUADRAM. Created by ppajuelo on 02/11/2017.
 */

public class SearchRepositoryImpl implements SearchRepository {

    private static final String ATTR_QUERY = "ATTR_QUERY";
    private static final String T1_TEI = "t1.trackedEntityInstance";

    private final BriteDatabase briteDatabase;

    private static final String SELECT_PROGRAM_WITH_REGISTRATION = "SELECT * FROM " + SqlConstants.PROGRAM_TABLE + " WHERE Program.programType='WITH_REGISTRATION' AND Program.trackedEntityType = ";
    private static final String SELECT_PROGRAM_ATTRIBUTES = "SELECT TrackedEntityAttribute.* FROM " + SqlConstants.TE_ATTR_TABLE +
            " INNER JOIN " + SqlConstants.PROGRAM_TE_ATTR_TABLE +
            " ON " + SqlConstants.TE_ATTR_TABLE + "." + SqlConstants.TE_ATTR_UID + " = " + SqlConstants.PROGRAM_TE_ATTR_TABLE + "." + SqlConstants.PROGRAM_TE_ATTR_TE_ATTR +
            " WHERE (" + SqlConstants.PROGRAM_TE_ATTR_TABLE + "." + SqlConstants.PROGRAM_TE_ATTR_SEARCHABLE + " = 1 OR TrackedEntityAttribute.uniqueProperty = '1')" +
            AND + SqlConstants.PROGRAM_TE_ATTR_TABLE + "." + SqlConstants.PROGRAM_TE_ATTR_PROGRAM + " = ";
    private static final String SELECT_OPTION_SET = "SELECT * FROM " + SqlConstants.OPTION_TABLE + " WHERE Option.optionSet = ";

    private static final String SEARCH =
            "SELECT TrackedEntityInstance.*" +
                    " FROM ((" + SqlConstants.TEI_TABLE + " JOIN " + SqlConstants.ENROLLMENT_TABLE + " ON " +
                    SqlConstants.ENROLLMENT_TABLE + "." + SqlConstants.ENROLLMENT_TEI + " = " +
                    SqlConstants.TEI_TABLE + "." + SqlConstants.TEI_UID + ") " +
                    "%s)" +
                    " WHERE ";
    private static final String SEARCH_ATTR = " JOIN (ATTR_QUERY) tabla ON tabla.trackedEntityInstance = TrackedEntityInstance.uid";

    private static final String PROGRAM_TRACKED_ENTITY_ATTRIBUTES_VALUES_PROGRAM_QUERY = String.format(
            "SELECT %s.*, %s.%s, %s.%s FROM %s " +
                    JOIN_TABLE_ON +
                    JOIN_TABLE_ON +
                    "WHERE %s.%s = ? AND %s.%s = ? AND " +
                    "%s.%s = 1 " +
                    "ORDER BY %s.%s ASC",
            SqlConstants.TE_ATTR_VALUE_TABLE, SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_VALUE_TYPE, SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_OPTION_SET, SqlConstants.TE_ATTR_VALUE_TABLE,
            SqlConstants.PROGRAM_TE_ATTR_TABLE, SqlConstants.PROGRAM_TE_ATTR_TABLE, SqlConstants.PROGRAM_TE_ATTR_TE_ATTR, SqlConstants.TE_ATTR_VALUE_TABLE, SqlConstants.TE_ATTR_VALUE_TE_ATTR,
            SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_UID, SqlConstants.TE_ATTR_VALUE_TABLE, SqlConstants.TE_ATTR_VALUE_TE_ATTR,
            SqlConstants.PROGRAM_TE_ATTR_TABLE, SqlConstants.PROGRAM_TE_ATTR_PROGRAM, SqlConstants.TE_ATTR_VALUE_TABLE, SqlConstants.TE_ATTR_VALUE_TEI,
            SqlConstants.PROGRAM_TE_ATTR_TABLE, SqlConstants.PROGRAM_TE_ATTR_DISPLAY_IN_LIST,
            SqlConstants.PROGRAM_TE_ATTR_TABLE, SqlConstants.PROGRAM_TE_ATTR_SORT_ORDER);

    private static final String PROGRAM_TRACKED_ENTITY_ATTRIBUTES_VALUES_QUERY = String.format(
            "SELECT DISTINCT %s.*, TrackedEntityAttribute.valueType, TrackedEntityAttribute.optionSet, ProgramTrackedEntityAttribute.displayInList FROM %s " +
                    JOIN_TABLE_ON +
                    "LEFT JOIN ProgramTrackedEntityAttribute ON ProgramTrackedEntityAttribute.trackedEntityAttribute = TrackedEntityAttribute.uid " +
                    "WHERE %s.%s = ? AND %s.%s = 1 ORDER BY %s.%s ASC",
            SqlConstants.TE_ATTR_VALUE_TABLE, SqlConstants.TE_ATTR_VALUE_TABLE,
            SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_UID, SqlConstants.TE_ATTR_VALUE_TABLE, SqlConstants.TE_ATTR_VALUE_TE_ATTR,
            SqlConstants.TE_ATTR_VALUE_TABLE, SqlConstants.TE_ATTR_VALUE_TEI,
            SqlConstants.PROGRAM_TE_ATTR_TABLE, SqlConstants.PROGRAM_TE_ATTR_DISPLAY_IN_LIST,
            SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_SORT_ORDER_IN_LIST_NO_PROGRAM
    );

    private static final String PROGRAM_COLOR_QUERY = String.format(
            "SELECT %s FROM %S " +
                    "WHERE %s = 'Program' AND %s = ?",
            SqlConstants.OBJECT_STYLE_COLOR, SqlConstants.OBJECT_STYLE_TABLE,
            SqlConstants.OBJECT_STYLE_OBJECT_TABLE,
            SqlConstants.OBJECT_STYLE_UID
    );

    private static final String PROGRAM_INFO = String.format(
            "SELECT %s.%s, %s.%s, %s.%s FROM %s " +
                    "LEFT JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = ?",
            SqlConstants.PROGRAM_TABLE, SqlConstants.PROGRAM_DISPLAY_NAME,
            SqlConstants.OBJECT_STYLE_TABLE, SqlConstants.OBJECT_STYLE_COLOR,
            SqlConstants.OBJECT_STYLE_TABLE, SqlConstants.OBJECT_STYLE_ICON, SqlConstants.PROGRAM_TABLE,
            SqlConstants.OBJECT_STYLE_TABLE, SqlConstants.OBJECT_STYLE_TABLE, SqlConstants.OBJECT_STYLE_UID, SqlConstants.PROGRAM_TABLE, SqlConstants.PROGRAM_UID,
            SqlConstants.PROGRAM_TABLE, SqlConstants.PROGRAM_UID
    );

    private static final String SELECT_TRACKED_ENTITY_TYPE_ATTRIBUTES = String.format(
            "SELECT %s.* FROM %s " +
                    "JOIN %s ON %s.trackedEntityAttribute = %s.%s " +
                    "WHERE %s.trackedEntityType = ? AND %s.searchable = 1",
            SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_TABLE,
            TrackedEntityTypeAttributeTableInfo.TABLE_INFO.name(), TrackedEntityTypeAttributeTableInfo.TABLE_INFO.name(), SqlConstants.TE_ATTR_TABLE, SqlConstants.TE_ATTR_UID,
            TrackedEntityTypeAttributeTableInfo.TABLE_INFO.name(), TrackedEntityTypeAttributeTableInfo.TABLE_INFO.name());

    private static final String[] TABLE_NAMES = new String[]{SqlConstants.TE_ATTR_TABLE, SqlConstants.PROGRAM_TE_ATTR_TABLE};
    private static final Set<String> TABLE_SET = new HashSet<>(Arrays.asList(TABLE_NAMES));
    private static final String[] TEI_TABLE_NAMES = new String[]{SqlConstants.TEI_TABLE,
            SqlConstants.ENROLLMENT_TABLE, SqlConstants.TE_ATTR_VALUE_TABLE};
    private static final Set<String> TEI_TABLE_SET = new HashSet<>(Arrays.asList(TEI_TABLE_NAMES));
    private final CodeGenerator codeGenerator;
    private final String teiType;


    SearchRepositoryImpl(CodeGenerator codeGenerator, BriteDatabase briteDatabase, String teiType) {
        this.codeGenerator = codeGenerator;
        this.briteDatabase = briteDatabase;
        this.teiType = teiType;
    }


    @NonNull
    @Override
    public Observable<List<TrackedEntityAttribute>> programAttributes(String programId) {
        String id = programId == null ? "" : programId;
        return briteDatabase.createQuery(TABLE_SET, SELECT_PROGRAM_ATTRIBUTES + "'" + id + "'")
                .mapToList(TrackedEntityAttribute::create);
    }

    @Override
    public Observable<List<TrackedEntityAttribute>> programAttributes() {
        String selectAttributes = "SELECT DISTINCT TrackedEntityAttribute.* FROM TrackedEntityAttribute " +
                "JOIN ProgramTrackedEntityAttribute " +
                "ON ProgramTrackedEntityAttribute.trackedEntityAttribute = TrackedEntityAttribute " +
                "JOIN Program ON Program.uid = ProgramTrackedEntityAttribute.program " +
                "WHERE Program.trackedEntityType = ? AND ProgramTrackedEntityAttribute.searchable = 1";
        return briteDatabase.createQuery(SqlConstants.TE_ATTR_TABLE, selectAttributes, teiType)
                .mapToList(TrackedEntityAttribute::create);
    }

    @Override
    public Observable<List<Option>> optionSet(String optionSetId) {
        String id = optionSetId == null ? "" : optionSetId;
        return briteDatabase.createQuery(SqlConstants.OPTION_TABLE, SELECT_OPTION_SET + "'" + id + "'")
                .mapToList(Option::create);
    }

    @Override
    public Observable<List<Program>> programsWithRegistration(String programTypeId) {
        String id = programTypeId == null ? "" : programTypeId;
        return briteDatabase.createQuery(SqlConstants.PROGRAM_TABLE, SELECT_PROGRAM_WITH_REGISTRATION + "'" + id + "'")
                .mapToList(Program::create);
    }

    @Override
    public Observable<List<TrackedEntityInstance>> trackedEntityInstances(@NonNull String teType,
                                                                          @Nullable Program selectedProgram,
                                                                          @Nullable HashMap<String, String> queryData, Integer page) {


        String attrQuery = "(SELECT TrackedEntityAttributeValue.trackedEntityInstance FROM TrackedEntityAttributeValue WHERE " +
                "TrackedEntityAttributeValue.trackedEntityAttribute = 'ATTR_ID' AND TrackedEntityAttributeValue.value LIKE 'ATTR_VALUE%') t";
        StringBuilder attr = new StringBuilder("");

        for (int i = 0; i < queryData.keySet().size(); i++) {
            String dataId = queryData.keySet().toArray()[i].toString();
            String dataValue = queryData.get(dataId);

            if (dataValue.contains("_os_"))
                dataValue = dataValue.split("_os_")[1];

            if (i > 0)
                attr.append(" INNER JOIN  ");

            attr.append(attrQuery.replace("ATTR_ID", dataId).replace("ATTR_VALUE", dataValue));
            attr.append(i + 1);
            if (i > 0)
                attr.append(" ON t" + (i) + ".trackedEntityInstance = t" + (i + 1) + ".trackedEntityInstance ");
        }

        String search = getSearchString(queryData, attr, teType, selectedProgram);

        if (selectedProgram != null && !selectedProgram.displayFrontPageList() && selectedProgram.maxTeiCountToReturn() != 0) {
            String maxResults = String.format(" LIMIT %s", selectedProgram.maxTeiCountToReturn());
            search += maxResults;
        } else {
            search += String.format(Locale.US, " LIMIT %d,%d", page * 20, 20);
        }

        return briteDatabase.createQuery(TEI_TABLE_SET, search)
                .mapToList(TrackedEntityInstance::create);
    }

    private String getSearchString(Map<String, String> queryData,
                                   StringBuilder attr,
                                   @NonNull String teType,
                                   @Nullable Program selectedProgram) {
        String teiTypeWHERE = "TrackedEntityInstance.trackedEntityType = '" + teType + "'";
        String teiRelationship = "TrackedEntityInstance.state <> '" + State.RELATIONSHIP.name() + "'";

        String enrollmentDateWHERE = null;
        String incidentDateWHERE = null;
        if (queryData != null && !isEmpty(queryData.get(Constants.ENROLLMENT_DATE_UID))) {
            enrollmentDateWHERE = " Enrollment.enrollmentDate LIKE '" + queryData.get(Constants.ENROLLMENT_DATE_UID) + "%'";
            queryData.remove(Constants.ENROLLMENT_DATE_UID);
        }
        if (queryData != null && !isEmpty(queryData.get(Constants.INCIDENT_DATE_UID))) {
            incidentDateWHERE = " Enrollment.incidentDate LIKE '" + queryData.get(Constants.INCIDENT_DATE_UID) + "%'";
            queryData.remove(Constants.INCIDENT_DATE_UID);
        }

        String search = String.format(SEARCH, queryData.size() == 0 ? "" : SEARCH_ATTR);
        search = search.replace(ATTR_QUERY, SELECT + T1_TEI + FROM + attr) + teiTypeWHERE + AND + teiRelationship;
        if (selectedProgram != null && !selectedProgram.uid().isEmpty()) {
            String programWHERE = "Enrollment.program = '" + selectedProgram.uid() + "'";
            search += AND + programWHERE;
        }
        if (enrollmentDateWHERE != null)
            search += " AND" + enrollmentDateWHERE;
        if (incidentDateWHERE != null)
            search += " AND" + incidentDateWHERE;
        search += " GROUP BY TrackedEntityInstance.uid";

        return search;
    }

    private String getTeiToUpdateSearch(@NonNull String teType,
                                        @Nullable Program selectedProgram,
                                        @Nullable Map<String, String> queryData,
                                        int listSize,
                                        StringBuilder attr) {
        String teiTypeWHERE = "TrackedEntityInstance.trackedEntityType = '" + teType + "'";
        String teiRelationship = "TrackedEntityInstance.state <> '" + State.RELATIONSHIP.name() + "'";

        String enrollmentDateWHERE = null;
        String incidentDateWHERE = null;
        if (queryData != null && !isEmpty(queryData.get(Constants.ENROLLMENT_DATE_UID))) {
            enrollmentDateWHERE = " Enrollment.enrollmentDate LIKE '" + queryData.get(Constants.ENROLLMENT_DATE_UID) + "%'";
            queryData.remove(Constants.ENROLLMENT_DATE_UID);
        }
        if (queryData != null && !isEmpty(queryData.get(Constants.INCIDENT_DATE_UID))) {
            incidentDateWHERE = " Enrollment.incidentDate LIKE '" + queryData.get(Constants.INCIDENT_DATE_UID) + "%'";
            queryData.remove(Constants.INCIDENT_DATE_UID);
        }

        String search = String.format(SEARCH, queryData.size() == 0 ? "" : SEARCH_ATTR);
        if (listSize > 0)
            search = search.replace(ATTR_QUERY, SELECT + T1_TEI + FROM + attr) + teiTypeWHERE + AND + teiRelationship + " AND (TrackedEntityInstance.state = 'TO_POST' OR TrackedEntityInstance.state = 'TO_UPDATE')";
        else
            search = search.replace(ATTR_QUERY, SELECT + T1_TEI + FROM + attr) + teiTypeWHERE + AND + teiRelationship;
        if (selectedProgram != null && !selectedProgram.uid().isEmpty()) {
            String programWHERE = "Enrollment.program = '" + selectedProgram.uid() + "'";
            search += AND + programWHERE;
        }
        if (enrollmentDateWHERE != null)
            search += " AND" + enrollmentDateWHERE;
        if (incidentDateWHERE != null)
            search += " AND" + incidentDateWHERE;
        search += " GROUP BY TrackedEntityInstance.uid";

        if (selectedProgram != null && !selectedProgram.displayFrontPageList() && selectedProgram.maxTeiCountToReturn() != 0) {
            String maxResults = String.format(" LIMIT %s", selectedProgram.maxTeiCountToReturn());
            search += maxResults;
        }

        return search;
    }

    @Override
    public Observable<List<TrackedEntityInstance>> trackedEntityInstancesToUpdate(@NonNull String teType,
                                                                                  @Nullable Program selectedProgram,
                                                                                  @Nullable HashMap<String, String> queryData, int listSize) {


        String attrQuery = "(SELECT TrackedEntityAttributeValue.trackedEntityInstance FROM TrackedEntityAttributeValue WHERE " +
                "TrackedEntityAttributeValue.trackedEntityAttribute = 'ATTR_ID' AND TrackedEntityAttributeValue.value LIKE 'ATTR_VALUE%') t";
        StringBuilder attr = new StringBuilder("");

        for (int i = 0; i < queryData.keySet().size(); i++) {
            String dataId = queryData.keySet().toArray()[i].toString();
            String dataValue = queryData.get(dataId);
            if (dataValue.contains("_os_"))
                dataValue = dataValue.split("_os_")[1];

            if (i > 0)
                attr.append(" INNER JOIN  ");

            attr.append(attrQuery.replace("ATTR_ID", dataId).replace("ATTR_VALUE", dataValue));
            attr.append(i + 1);
            if (i > 0)
                attr.append(" ON t" + (i) + ".trackedEntityInstance = t" + (i + 1) + ".trackedEntityInstance ");
        }

        String search = getTeiToUpdateSearch(teType, selectedProgram, queryData, listSize, attr);

        return briteDatabase.createQuery(TEI_TABLE_SET, search)
                .mapToList(TrackedEntityInstance::create);
    }


    private TrackedEntityInstance insertTei(Date currentDate, String orgUnit, Map<String, String> queryData) {
        String generatedUid = codeGenerator.generate();
        TrackedEntityInstance trackedEntityInstanceModel =
                TrackedEntityInstance.builder()
                        .uid(generatedUid)
                        .created(currentDate)
                        .lastUpdated(currentDate)
                        .organisationUnit(orgUnit)
                        .trackedEntityType(teiType)
                        .state(State.TO_POST)
                        .build();

        if (briteDatabase.insert(SqlConstants.TEI_TABLE,
                trackedEntityInstanceModel.toContentValues()) < 0) {
            String message = String.format(Locale.US, "Failed to insert new tracked entity " +
                            "instance for organisationUnit=[%s] and trackedEntity=[%s]",
                    orgUnit, teiType);
            throw new SQLiteConstraintException(message);
        }

        for (Map.Entry<String, String> entry : queryData.entrySet()) {
            String dataValue = entry.getValue();
            if (dataValue.contains("_os_"))
                dataValue = dataValue.split("_os_")[1];
            TrackedEntityAttributeValue attributeValueModel =
                    TrackedEntityAttributeValue.builder()
                            .created(currentDate)
                            .lastUpdated(currentDate)
                            .value(dataValue)
                            .trackedEntityAttribute(entry.getKey())
                            .trackedEntityInstance(generatedUid)
                            .build();
            if (briteDatabase.insert(SqlConstants.TE_ATTR_VALUE_TABLE,
                    attributeValueModel.toContentValues()) < 0) {
                String message = String.format(Locale.US, "Failed to insert new trackedEntityAttributeValue " +
                                "instance for organisationUnit=[%s] and trackedEntity=[%s]",
                        orgUnit, teiType);
                throw new SQLiteConstraintException(message);
            }
        }

        return trackedEntityInstanceModel;
    }

    private void updateTei(Date currentDate, String teiUid) {
        ContentValues dataValue = new ContentValues();

        // renderSearchResults time stamp
        dataValue.put(SqlConstants.TEI_LAST_UPDATED,
                BaseIdentifiableObject.DATE_FORMAT.format(currentDate));
        dataValue.put(SqlConstants.TEI_STATE,
                State.TO_POST.toString());

        if (briteDatabase.update(SqlConstants.TEI_TABLE, dataValue,
                SqlConstants.TEI_UID + " = ? ", teiUid) <= 0) {
            String message = String.format(Locale.US, "Failed to update tracked entity " +
                            "instance for uid=[%s]",
                    teiUid);
            throw new SQLiteConstraintException(message);
        }
    }

    private Enrollment insertEnrollment(Date currentDate, Date enrollmentDate, String programUid,
                                        String orgUnit, String teiUid, TrackedEntityInstance trackedEntityInstance) {
        Enrollment enrollmentModel = Enrollment.builder()
                .uid(codeGenerator.generate())
                .created(currentDate)
                .lastUpdated(currentDate)
                .enrollmentDate(enrollmentDate)
                .program(programUid)
                .organisationUnit(orgUnit)
                .trackedEntityInstance(teiUid != null ? teiUid : trackedEntityInstance.uid())
                .status(EnrollmentStatus.ACTIVE)
                .followUp(false)
                .state(State.TO_POST)
                .build();

        if (briteDatabase.insert(SqlConstants.ENROLLMENT_TABLE, enrollmentModel.toContentValues()) < 0) {
            String message = String.format(Locale.US, "Failed to insert new enrollment " +
                    "instance for organisationUnit=[%s] and program=[%s]", orgUnit, programUid);
            throw new SQLiteConstraintException(message);
        }

        return enrollmentModel;
    }

    @NotNull
    @Override
    public Observable<String> saveToEnroll(@NonNull String teiType, @NonNull String orgUnit, @NonNull String programUid,
                                           @Nullable String teiUid, Map<String, String> queryData, Date enrollmentDate) {
        Date currentDate = Calendar.getInstance().getTime();
        return Observable.defer(() -> {
            try {
                TrackedEntityInstance trackedEntityInstance = null;

                if (teiUid == null) {
                    trackedEntityInstance = insertTei(currentDate, orgUnit, queryData);

                } else {
                    updateTei(currentDate, teiUid);
                }

                Enrollment enrollment = insertEnrollment(currentDate, enrollmentDate, programUid,
                        orgUnit, teiUid, trackedEntityInstance);

                return Observable.just(enrollment.uid());

            } catch (SQLiteConstraintException e) {
                return Observable.error(e);
            }
        });
    }

    @Override
    public Observable<List<OrganisationUnit>> getOrgUnits(@Nullable String selectedProgramUid) {


        if (selectedProgramUid != null) {
            String orgUnitQuery = "SELECT * FROM OrganisationUnit " +
                    "JOIN OrganisationUnitProgramLink ON OrganisationUnitProgramLink.organisationUnit = OrganisationUnit.uid " +
                    "JOIN UserOrganisationUnit ON UserOrganisationUnit.organisationUnit = OrganisationUnit.uid " +
                    "WHERE OrganisationUnitProgramLink.program = ? AND UserOrganisationUnit.organisationUnitScope = 'SCOPE_DATA_CAPTURE'";
            return briteDatabase.createQuery(SqlConstants.ORG_UNIT_TABLE, orgUnitQuery, selectedProgramUid)
                    .mapToList(OrganisationUnit::create);
        } else
            return briteDatabase.createQuery(SqlConstants.ORG_UNIT_TABLE, " SELECT * FROM OrganisationUnit")
                    .mapToList(OrganisationUnit::create);
    }

    @Override
    public Flowable<List<SearchTeiModel>> transformIntoModel(List<SearchTeiModel> teiList, @Nullable Program selectedProgram) {
        return Flowable.fromIterable(teiList)
                .map(tei -> {
                    try (Cursor teiCursor = briteDatabase.query("SELECT uid FROM TrackedEntityInstance WHERE uid = ?", tei.getTei().uid())) {
                        if (teiCursor != null && teiCursor.moveToFirst()) {
                            tei.setOnline(false);
                            setEnrollmentInfo(tei);
                            setAttributesInfo(tei, selectedProgram);
                            setOverdueEvents(tei, selectedProgram);
                        }
                    }
                    return tei;
                })
                .toList().toFlowable();
    }

    private void setEnrollmentInfo(SearchTeiModel tei) {
        try (Cursor enrollmentCursor = briteDatabase.query("SELECT * FROM Enrollment " +
                "WHERE Enrollment.trackedEntityInstance = ? AND Enrollment.STATUS = 'ACTIVE' " +
                "GROUP BY Enrollment.program", tei.getTei().uid())) {

            if (enrollmentCursor != null) {
                enrollmentCursor.moveToFirst();
                for (int i = 0; i < enrollmentCursor.getCount(); i++) {
                    Enrollment enrollment = Enrollment.create(enrollmentCursor);
                    if (i == 0)
                        tei.resetEnrollments();
                    tei.addEnrollment(Enrollment.create(enrollmentCursor));
                    tei.addEnrollmentInfo(getProgramInfo(enrollment.program()));
                    enrollmentCursor.moveToNext();
                }
            }
        }
    }

    private Trio<String, String, String> getProgramInfo(String programUid) {
        try (Cursor cursor = briteDatabase.query(PROGRAM_INFO, programUid)) {
            if (cursor != null) {
                cursor.moveToFirst();
                String programName = cursor.getString(0);
                String programColor = cursor.getString(1) != null ? cursor.getString(1) : "";
                String programIcon = cursor.getString(2) != null ? cursor.getString(2) : "";
                return Trio.create(programName, programColor, programIcon);
            }
        }
        return null;
    }

    private void getAttr(SearchTeiModel tei) {
        String id = tei != null && tei.getTei() != null && tei.getTei().uid() != null ? tei.getTei().uid() : "";
        try (Cursor attributes = briteDatabase.query(PROGRAM_TRACKED_ENTITY_ATTRIBUTES_VALUES_QUERY,
                id)) {
            if (attributes != null) {
                attributes.moveToFirst();
                for (int i = 0; i < attributes.getCount(); i++) {
                    if (tei != null)
                        tei.addAttributeValues(ValueUtils.transform(briteDatabase, attributes));
                    attributes.moveToNext();
                }
            }
        }
    }

    private void getProgramAttr(SearchTeiModel tei, Program selectedProgram) {
        String teiId = tei != null && tei.getTei() != null && tei.getTei().uid() != null ? tei.getTei().uid() : "";
        String progId = selectedProgram.uid() != null ? selectedProgram.uid() : "";
        try (Cursor attributes = briteDatabase.query(PROGRAM_TRACKED_ENTITY_ATTRIBUTES_VALUES_PROGRAM_QUERY,
                progId,
                teiId)) {
            if (attributes != null) {
                attributes.moveToFirst();
                for (int i = 0; i < attributes.getCount(); i++) {
                    if (tei != null)
                        tei.addAttributeValues(ValueUtils.transform(briteDatabase, attributes));
                    attributes.moveToNext();
                }
            }
        }
    }

    private void setAttributesInfo(SearchTeiModel tei, Program selectedProgram) {
        if (selectedProgram == null) {
            getAttr(tei);
        } else {
            getProgramAttr(tei, selectedProgram);
        }
    }

    private void setOverdue(@NonNull SearchTeiModel tei, String overdueQuery) {
        String teiId = tei.getTei() != null && tei.getTei().uid() != null ? tei.getTei().uid() : "";
        try (Cursor hasOverdueCursor = briteDatabase.query(overdueQuery,
                teiId, EventStatus.SKIPPED.name())) {
            if (hasOverdueCursor != null && hasOverdueCursor.moveToNext()) {
                tei.setHasOverdue(true);
            }
        }
    }

    private void setProgramOverdue(@NonNull SearchTeiModel tei, Program selectedProgram, String overdueQuery) {
        String overdueProgram = " AND Enrollment.program = ?";
        String teiId = tei.getTei() != null && tei.getTei().uid() != null ? tei.getTei().uid() : "";
        String progId = selectedProgram.uid() != null ? selectedProgram.uid() : "";
        try (Cursor hasOverdueCursor = briteDatabase.query(overdueQuery + overdueProgram, teiId,
                EventStatus.SKIPPED.name(), progId)) {
            if (hasOverdueCursor != null && hasOverdueCursor.moveToNext()) {
                tei.setHasOverdue(true);
            }

        }
    }

    private void setOverdueEvents(@NonNull SearchTeiModel tei, Program selectedProgram) {
        String overdueQuery = "SELECT * FROM EVENT JOIN Enrollment ON Enrollment.uid = Event.enrollment " +
                "JOIN TrackedEntityInstance ON TrackedEntityInstance.uid = Enrollment.trackedEntityInstance " +
                "WHERE TrackedEntityInstance.uid = ? AND Event.status = ?";

        if (selectedProgram == null) {
            setOverdue(tei, overdueQuery);
        } else {
            setProgramOverdue(tei, selectedProgram, overdueQuery);
        }
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
    public Observable<List<TrackedEntityAttribute>> trackedEntityTypeAttributes() {
        return briteDatabase.createQuery(SqlConstants.TE_ATTR_TABLE, SELECT_TRACKED_ENTITY_TYPE_ATTRIBUTES, teiType)
                .mapToList(TrackedEntityAttribute::create);
    }
}
