package org.dhis2.utils;

import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramStageModel;
import org.hisp.dhis.android.core.program.ProgramStageSectionModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValueModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;

public class SqlConstants {

    private SqlConstants() {
        // hide public constructor
    }

    public static final String SELECT = " SELECT ";
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String JOIN = " JOIN ";
    public static final String ON = " ON ";
    public static final String AND = " AND ";
    public static final String NOT_EQUALS = " != ";
    public static final String EQUAL = " = ";
    public static final String QUESTION_MARK = " ? ";
    public static final String QUOTE = "'";
    public static final String LIMIT_1 = " LIMIT 1 ";
    public static final String TABLE_FIELD_EQUALS = " %s.%s = ";
    public static final String JOIN_TABLE_ON = " JOIN %s ON %s.%s = %s.%s ";
    public static final String SELECT_ALL_FROM_TABLE_WHERE_TABLE_FIELD_EQUALS = "SELECT * FROM %s WHERE %s.%s = ";
    public static final String SELECT_ALL_FROM = "SELECT * FROM ";
    public static final String UID_EQUALS_QUESTION_MARK = "uid = ?";

    public static final String ENROLLMENT_TABLE = EnrollmentModel.TABLE;
    public static final String ENROLLMENT_UID = EnrollmentModel.Columns.UID;
    public static final String ENROLLMENT_DATE = EnrollmentModel.Columns.ENROLLMENT_DATE;
    public static final String ENROLLMENT_STATE = EnrollmentModel.Columns.STATE;
    public static final String ENROLLMENT_LATITUDE = EnrollmentModel.Columns.LATITUDE;
    public static final String ENROLLMENT_LONGITUDE = EnrollmentModel.Columns.LONGITUDE;
    public static final String ENROLLMENT_INCIDENT_DATE = EnrollmentModel.Columns.INCIDENT_DATE;
    public static final String ENROLLMENT_STATUS = EnrollmentModel.Columns.ENROLLMENT_STATUS;
    public static final String ENROLLMENT_TEI = EnrollmentModel.Columns.TRACKED_ENTITY_INSTANCE;

    public static final String PROGRAM_TABLE = ProgramModel.TABLE;
    public static final String PROGRAM_STAGE_TABLE = ProgramStageModel.TABLE;

    public static final String EVENT_TABLE = EventModel.TABLE;
    public static final String EVENT_PROGRAM = EventModel.Columns.PROGRAM;
    public static final String EVENT_PROGRAM_STAGE = EventModel.Columns.PROGRAM_STAGE;

    public static final String TEI_TABLE = TrackedEntityInstanceModel.TABLE;
    public static final String TEI_UID = TrackedEntityInstanceModel.Columns.UID;

    public static final String PROGRAM_STAGE_SECTION_TABLE = ProgramStageSectionModel.TABLE;

    public static final String TE_ATTR_VALUE_TABLE = TrackedEntityAttributeValueModel.TABLE;
    public static final String TE_ATTR_VALUE_TEI = TrackedEntityAttributeValueModel.Columns.TRACKED_ENTITY_INSTANCE;

}
