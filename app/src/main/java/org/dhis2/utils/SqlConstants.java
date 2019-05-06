package org.dhis2.utils;

import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryModel;
import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionModel;
import org.hisp.dhis.android.core.common.ObjectStyleModel;
import org.hisp.dhis.android.core.dataelement.DataElementModel;
import org.hisp.dhis.android.core.dataset.DataSetModel;
import org.hisp.dhis.android.core.datavalue.DataValueModel;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.enrollment.note.NoteModel;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.legendset.LegendModel;
import org.hisp.dhis.android.core.legendset.ProgramIndicatorLegendSetLinkModel;
import org.hisp.dhis.android.core.option.OptionModel;
import org.hisp.dhis.android.core.option.OptionSetModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitProgramLinkModel;
import org.hisp.dhis.android.core.period.PeriodModel;
import org.hisp.dhis.android.core.program.ProgramIndicatorModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramRuleActionModel;
import org.hisp.dhis.android.core.program.ProgramRuleModel;
import org.hisp.dhis.android.core.program.ProgramRuleVariableModel;
import org.hisp.dhis.android.core.program.ProgramStageDataElementModel;
import org.hisp.dhis.android.core.program.ProgramStageModel;
import org.hisp.dhis.android.core.program.ProgramStageSectionModel;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttributeModel;
import org.hisp.dhis.android.core.relationship.RelationshipTypeModel;
import org.hisp.dhis.android.core.resource.ResourceModel;
import org.hisp.dhis.android.core.settings.SystemSettingModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValueModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValueModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityTypeModel;
import org.hisp.dhis.android.core.user.AuthenticatedUserModel;
import org.hisp.dhis.android.core.user.UserCredentialsModel;
import org.hisp.dhis.android.core.user.UserModel;
import org.hisp.dhis.android.core.user.UserOrganisationUnitLinkModel;

@SuppressWarnings("squid:CallToDeprecatedMethod")
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
    public static final String ENROLLMENT_CREATED = EnrollmentModel.Columns.CREATED;
    public static final String ENROLLMENT_PROGRAM = EnrollmentModel.Columns.PROGRAM;
    public static final String ENROLLMENT_LAST_UPDATED = EnrollmentModel.Columns.LAST_UPDATED;
    public static final String ENROLLMENT_FOLLOW_UP = EnrollmentModel.Columns.FOLLOW_UP;
    public static final String ENROLLMENT_ENROLLMENT_DATE = EnrollmentModel.Columns.ENROLLMENT_DATE;
    public static final String ENROLLMENT_ORG_UNIT = EnrollmentModel.Columns.ORGANISATION_UNIT;

    public static final String PROGRAM_TABLE = ProgramModel.TABLE;
    public static final String PROGRAM_UID = ProgramModel.Columns.UID;
    public static final String PROGRAM_LAST_UPDATED = ProgramModel.Columns.LAST_UPDATED;
    public static final String PROGRAM_DISPLAY_NAME = ProgramModel.Columns.DISPLAY_NAME;

    public static final String PROGRAM_STAGE_DATA_ELEMENT_TABLE = ProgramStageDataElementModel.TABLE;
    public static final String PROGRAM_STAGE_DATA_ELEMENT_PROGRAM_STAGE = ProgramStageDataElementModel.Columns.PROGRAM_STAGE;

    public static final String PROGRAM_STAGE_TABLE = ProgramStageModel.TABLE;
    public static final String PROGRAM_STAGE_UID = ProgramStageModel.Columns.UID;
    public static final String PROGRAM_STAGE_DISPLAY_NAME = ProgramStageModel.Columns.DISPLAY_NAME;
    public static final String PROGRAM_STAGE_PROGRAM = ProgramStageModel.Columns.PROGRAM;
    public static final String PROGRAM_STAGE_SORT_ORDER = ProgramStageModel.Columns.SORT_ORDER;
    public static final String PROGRAM_STAGE_HIDE_DUE_DATE = ProgramStageModel.Columns.HIDE_DUE_DATE;
    public static final String PROGRAM_STAGE_DISPLAY_GENERATE_EVENT_BOX = ProgramStageModel.Columns.DISPLAY_GENERATE_EVENT_BOX;

    public static final String EVENT_TABLE = EventModel.TABLE;
    public static final String EVENT_UID = EventModel.Columns.UID;
    public static final String EVENT_ID = EventModel.Columns.ID;
    public static final String EVENT_PROGRAM = EventModel.Columns.PROGRAM;
    public static final String EVENT_PROGRAM_STAGE = EventModel.Columns.PROGRAM_STAGE;
    public static final String EVENT_STATE = EventModel.Columns.STATE;
    public static final String EVENT_STATUS = EventModel.Columns.STATUS;
    public static final String EVENT_ATTR_OPTION_COMBO = EventModel.Columns.ATTRIBUTE_OPTION_COMBO;
    public static final String EVENT_DATE = EventModel.Columns.EVENT_DATE;
    public static final String EVENT_DUE_DATE = EventModel.Columns.DUE_DATE;
    public static final String EVENT_ORG_UNIT = EventModel.Columns.ORGANISATION_UNIT;
    public static final String EVENT_TEI = EventModel.Columns.TRACKED_ENTITY_INSTANCE;
    public static final String EVENT_ENROLLMENT = EventModel.Columns.ENROLLMENT;
    public static final String EVENT_COMPLETE_DATE = EventModel.Columns.COMPLETE_DATE;
    public static final String EVENT_LAST_UPDATED = EventModel.Columns.LAST_UPDATED;
    public static final String EVENT_LATITUDE = EventModel.Columns.LATITUDE;
    public static final String EVENT_LONGITUDE = EventModel.Columns.LONGITUDE;
    public static final String EVENT_CREATED = EventModel.Columns.CREATED;

    public static final String TEI_DATA_VALUE_TABLE = TrackedEntityDataValueModel.TABLE;
    public static final String TEI_DATA_VALUE_VALUE = TrackedEntityDataValueModel.Columns.VALUE;
    public static final String TEI_DATA_VALUE_DATA_ELEMENT = TrackedEntityDataValueModel.Columns.DATA_ELEMENT;
    public static final String TEI_DATA_VALUE_LAST_UPDATED = TrackedEntityDataValueModel.Columns.LAST_UPDATED;
    public static final String TEI_DATA_VALUE_EVENT = TrackedEntityDataValueModel.Columns.EVENT;

    public static final String TEI_TABLE = TrackedEntityInstanceModel.TABLE;
    public static final String TEI_UID = TrackedEntityInstanceModel.Columns.UID;
    public static final String TEI_STATE = TrackedEntityInstanceModel.Columns.STATE;
    public static final String TEI_LAST_UPDATED = TrackedEntityInstanceModel.Columns.LAST_UPDATED;
    public static final String TEI_ORG_UNIT = TrackedEntityInstanceModel.Columns.ORGANISATION_UNIT;

    public static final String TE_ATTR_TABLE = TrackedEntityAttributeModel.TABLE;
    public static final String TE_ATTR_UID = TrackedEntityAttributeModel.Columns.UID;
    public static final String TE_ATTR_DISPLAY_IN_LIST_NO_PROGRAM = TrackedEntityAttributeModel.Columns.DISPLAY_IN_LIST_NO_PROGRAM;
    public static final String TE_ATTR_DISPLAY_NAME = TrackedEntityAttributeModel.Columns.DISPLAY_NAME;
    public static final String TE_ATTR_VALUE_TYPE = TrackedEntityAttributeModel.Columns.VALUE_TYPE;
    public static final String TE_ATTR_OPTION_SET = TrackedEntityAttributeModel.Columns.OPTION_SET;
    public static final String TE_ATTR_SORT_ORDER_IN_LIST_NO_PROGRAM = TrackedEntityAttributeModel.Columns.SORT_ORDER_IN_LIST_NO_PROGRAM;

    public static final String PROGRAM_STAGE_SECTION_TABLE = ProgramStageSectionModel.TABLE;
    public static final String PROGRAM_STAGE_SECTION_SORT_ORDER = ProgramStageSectionModel.Columns.SORT_ORDER;
    public static final String PROGRAM_STAGE_SECTION_PROGRAM_STAGE = ProgramStageSectionModel.Columns.PROGRAM_STAGE;

    public static final String CATEGORY_TABLE = CategoryModel.TABLE;

    public static final String RELATIONSHIP_TYPE_TABLE = RelationshipTypeModel.TABLE;

    public static final String OPTION_TABLE = OptionModel.TABLE;

    public static final String OPTION_SET_TABLE = OptionSetModel.TABLE;

    public static final String SYSTEM_SETTING_TABLE = SystemSettingModel.TABLE;

    public static final String OBJECT_STYLE_TABLE = ObjectStyleModel.TABLE;
    public static final String OBJECT_STYLE_COLOR = ObjectStyleModel.Columns.COLOR;
    public static final String OBJECT_STYLE_OBJECT_TABLE = ObjectStyleModel.Columns.OBJECT_TABLE;
    public static final String OBJECT_STYLE_UID = ObjectStyleModel.Columns.UID;
    public static final String OBJECT_STYLE_ICON = ObjectStyleModel.Columns.ICON;

    public static final String RESOURCE_TABLE = ResourceModel.TABLE;

    public static final String AUTH_USER_TABLE = AuthenticatedUserModel.TABLE;

    public static final String USER_TABLE = UserModel.TABLE;

    public static final String USER_CREDENTIALS_TABLE = UserCredentialsModel.TABLE;

    public static final String CAT_OPTION_COMBO_TABLE = CategoryOptionComboModel.TABLE;
    public static final String CAT_OPTION_COMBO_UID = CategoryOptionComboModel.Columns.UID;
    public static final String CAT_OPTION_COMBO_CAT_COMBO = CategoryOptionComboModel.Columns.CATEGORY_COMBO;

    public static final String CAT_COMBO_TABLE = CategoryComboModel.TABLE;
    public static final String CAT_COMBO_UID = CategoryComboModel.Columns.UID;
    public static final String CAT_COMBO_IS_DEFAULT = CategoryComboModel.Columns.IS_DEFAULT;

    public static final String CAT_OPTION_TABLE = CategoryOptionModel.TABLE;
    public static final String CAT_OPTION_UID = CategoryOptionModel.Columns.UID;
    public static final String CAT_OPTION_ACCESS_DATA_WRITE = CategoryOptionModel.Columns.ACCESS_DATA_WRITE;

    public static final String PERIOD_TABLE = PeriodModel.TABLE;

    public static final String DATA_ELEMENT_TABLE = DataElementModel.TABLE;
    public static final String DATA_ELEMENT_UID = DataElementModel.Columns.UID;

    public static final String DATA_SET_TABLE = DataSetModel.TABLE;
    public static final String DATA_SET_UID = DataSetModel.Columns.UID;
    public static final String DATA_SET_CODE = DataSetModel.Columns.CODE;
    public static final String DATA_SET_NAME = DataSetModel.Columns.NAME;
    public static final String DATA_SET_DISPLAY_NAME = DataSetModel.Columns.DISPLAY_NAME;
    public static final String DATA_SET_CREATED = DataSetModel.Columns.CREATED;
    public static final String DATA_SET_LAST_UPDATED = DataSetModel.Columns.LAST_UPDATED;
    public static final String DATA_SET_SHORT_NAME = DataSetModel.Columns.SHORT_NAME;
    public static final String DATA_SET_DISPLAY_SHORT_NAME = DataSetModel.Columns.DISPLAY_SHORT_NAME;
    public static final String DATA_SET_DESCRIPTION = DataSetModel.Columns.DESCRIPTION;
    public static final String DATA_SET_DISPLAY_DESCRIPTION = DataSetModel.Columns.DISPLAY_DESCRIPTION;
    public static final String DATA_SET_PERIOD_TYPE = DataSetModel.Columns.PERIOD_TYPE;
    public static final String DATA_SET_CATEGORY_COMBO = DataSetModel.Columns.CATEGORY_COMBO;
    public static final String DATA_SET_MOBILE = DataSetModel.Columns.MOBILE;
    public static final String DATA_SET_VERSION = DataSetModel.Columns.VERSION;
    public static final String DATA_SET_EXPIRY_DAYS = DataSetModel.Columns.EXPIRY_DAYS;
    public static final String DATA_SET_TIMELY_DAYS = DataSetModel.Columns.TIMELY_DAYS;
    public static final String DATA_SET_NOTIFY_COMPLETING_USER = DataSetModel.Columns.NOTIFY_COMPLETING_USER;
    public static final String DATA_SET_OPEN_FUTURE_PERIODS = DataSetModel.Columns.OPEN_FUTURE_PERIODS;
    public static final String DATA_SET_FIELD_COMBINATION_REQUIRED = DataSetModel.Columns.FIELD_COMBINATION_REQUIRED;
    public static final String DATA_SET_VALID_COMPLETE_ONLY = DataSetModel.Columns.VALID_COMPLETE_ONLY;
    public static final String DATA_SET_NO_VALUE_REQUIRES_COMMENT = DataSetModel.Columns.NO_VALUE_REQUIRES_COMMENT;
    public static final String DATA_SET_SKIP_OFFLINE = DataSetModel.Columns.SKIP_OFFLINE;
    public static final String DATA_SET_DATA_ELEMENT_DECORATION = DataSetModel.Columns.DATA_ELEMENT_DECORATION;
    public static final String DATA_SET_RENDER_AS_TABS = DataSetModel.Columns.RENDER_AS_TABS;
    public static final String DATA_SET_RENDER_HORIZONTALLY = DataSetModel.Columns.RENDER_HORIZONTALLY;
    public static final String DATA_SET_ACCESS_DATA_WRITE = DataSetModel.Columns.ACCESS_DATA_WRITE;

    public static final String LEGEND_TABLE = LegendModel.TABLE;
    public static final String LEGEND_COLOR = LegendModel.Columns.COLOR;
    public static final String LEGEND_LEGEND_SET = LegendModel.Columns.LEGEND_SET;
    public static final String LEGEND_START_VALUE = LegendModel.Columns.START_VALUE;
    public static final String LEGEND_END_VALUE = LegendModel.Columns.END_VALUE;

    public static final String PROGRAM_INDICATOR_LEGEND_SET_LINK_TABLE = ProgramIndicatorLegendSetLinkModel.TABLE;
    public static final String PROGRAM_INDICATOR_LEGEND_SET_LINK_LEGEND_SET = ProgramIndicatorLegendSetLinkModel.Columns.LEGEND_SET;
    public static final String PROGRAM_INDICATOR_LEGEND_SET_LINK_PROGRAM_INDICATOR = ProgramIndicatorLegendSetLinkModel.Columns.PROGRAM_INDICATOR;

    public static final String PROGRAM_INDICATOR_TABLE = ProgramIndicatorModel.TABLE;
    public static final String PROGRAM_INDICATOR_PROGRAM = ProgramIndicatorModel.Columns.PROGRAM;
    public static final String PROGRAM_INDICATOR_UID = ProgramIndicatorModel.Columns.UID;

    public static final String NOTE_TABLE = NoteModel.TABLE;

    public static final String DATA_VALUE_TABLE = DataValueModel.TABLE;

    public static final String PROGRAM_RULE_VARIABLE_TABLE = ProgramRuleVariableModel.TABLE;

    public static final String PROGRAM_RULE_TABLE = ProgramRuleModel.TABLE;

    public static final String PROGRAM_RULE_ACTION_TABLE = ProgramRuleActionModel.TABLE;

    public static final String PROGRAM_TE_ATTR_TABLE = ProgramTrackedEntityAttributeModel.TABLE;
    public static final String PROGRAM_TE_ATTR_PROGRAM = ProgramTrackedEntityAttributeModel.Columns.PROGRAM;
    public static final String PROGRAM_TE_ATTR_TE_ATTR = ProgramTrackedEntityAttributeModel.Columns.TRACKED_ENTITY_ATTRIBUTE;
    public static final String PROGRAM_TE_ATTR_SEARCHABLE = ProgramTrackedEntityAttributeModel.Columns.SEARCHABLE;
    public static final String PROGRAM_TE_ATTR_DISPLAY_IN_LIST = ProgramTrackedEntityAttributeModel.Columns.DISPLAY_IN_LIST;
    public static final String PROGRAM_TE_ATTR_SORT_ORDER = ProgramTrackedEntityAttributeModel.Columns.SORT_ORDER;

    public static final String USER_ORG_UNIT_PROGRAM_LINK_TABLE = OrganisationUnitProgramLinkModel.TABLE;

    public static final String USER_ORG_UNIT_LINK_TABLE = UserOrganisationUnitLinkModel.TABLE;
    public static final String USER_ORG_UNIT_LINK_ORG_UNIT = UserOrganisationUnitLinkModel.Columns.ORGANISATION_UNIT;
    public static final String USER_ORG_UNIT_LINK_ORG_UNIT_SCOPE = UserOrganisationUnitLinkModel.Columns.ORGANISATION_UNIT_SCOPE;

    public static final String ORG_UNIT_TABLE = OrganisationUnitModel.TABLE;
    public static final String ORG_UNIT_UID = OrganisationUnitModel.Columns.UID;
    public static final String ORG_UNIT_DISPLAY_NAME = OrganisationUnitModel.Columns.DISPLAY_NAME;
    public static final String ORG_UNIT_OPENING_DATE = OrganisationUnitModel.Columns.OPENING_DATE;
    public static final String ORG_UNIT_CLOSED_DATE = OrganisationUnitModel.Columns.CLOSED_DATE;

    public static final String TE_TYPE_TABLE = TrackedEntityTypeModel.TABLE;
    public static final String TE_TYPE_UID = TrackedEntityTypeModel.Columns.UID;

    public static final String TE_ATTR_VALUE_TABLE = TrackedEntityAttributeValueModel.TABLE;
    public static final String TE_ATTR_VALUE_TEI = TrackedEntityAttributeValueModel.Columns.TRACKED_ENTITY_INSTANCE;
    public static final String TE_ATTR_VALUE_LAST_UPDATED = TrackedEntityAttributeValueModel.Columns.LAST_UPDATED;
    public static final String TE_ATTR_VALUE_VALUE = TrackedEntityAttributeValueModel.Columns.VALUE;
    public static final String TE_ATTR_VALUE_TE_ATTR = TrackedEntityAttributeValueModel.Columns.TRACKED_ENTITY_ATTRIBUTE;

}
