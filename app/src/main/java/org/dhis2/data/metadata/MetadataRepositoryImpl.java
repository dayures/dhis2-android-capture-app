package org.dhis2.data.metadata;

import android.content.ContentValues;
import android.database.Cursor;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.R;
import org.dhis2.data.tuples.Pair;
import org.dhis2.utils.DateUtils;
import org.hisp.dhis.android.core.category.Category;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryModel;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.category.CategoryOptionComboCategoryOptionLinkTableInfo;
import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionModel;
import org.hisp.dhis.android.core.common.ObjectStyle;
import org.hisp.dhis.android.core.common.ObjectStyleModel;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentModel;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.maintenance.D2Error;
import org.hisp.dhis.android.core.option.Option;
import org.hisp.dhis.android.core.option.OptionGroupOptionLinkTableInfo;
import org.hisp.dhis.android.core.option.OptionModel;
import org.hisp.dhis.android.core.option.OptionSetModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.program.ProgramStageModel;
import org.hisp.dhis.android.core.program.ProgramStageSectionModel;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttributeModel;
import org.hisp.dhis.android.core.resource.Resource;
import org.hisp.dhis.android.core.resource.ResourceModel;
import org.hisp.dhis.android.core.settings.SystemSetting;
import org.hisp.dhis.android.core.settings.SystemSettingModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityType;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityTypeModel;
import org.hisp.dhis.android.core.user.AuthenticatedUserModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;
import static android.text.TextUtils.join;


/**
 * QUADRAM. Created by ppajuelo on 04/12/2017.
 */

public class MetadataRepositoryImpl implements MetadataRepository {

    private static final String SELECT_TEI_ENROLLMENTS = String.format(
            "SELECT * FROM %s WHERE %s.%s =",
            EnrollmentModel.TABLE,
            EnrollmentModel.TABLE, EnrollmentModel.Columns.TRACKED_ENTITY_INSTANCE);

    private static final String ACTIVE_TEI_PROGRAMS = String.format(
            " SELECT %s.* FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = ?",
            ProgramModel.TABLE,
            ProgramModel.TABLE,
            EnrollmentModel.TABLE, EnrollmentModel.TABLE, EnrollmentModel.Columns.PROGRAM, ProgramModel.TABLE, ProgramModel.Columns.UID,
            EnrollmentModel.TABLE, EnrollmentModel.Columns.TRACKED_ENTITY_INSTANCE);

    private static final Set<String> ACTIVE_TEI_PROGRAMS_TABLES = new HashSet<>(Arrays.asList(ProgramModel.TABLE, EnrollmentModel.TABLE));


    private static final String PROGRAM_LIST_ALL_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            ProgramModel.TABLE, ProgramModel.TABLE, ProgramModel.Columns.UID);

    private static final String TRACKED_ENTITY_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            TrackedEntityTypeModel.TABLE, TrackedEntityTypeModel.TABLE, TrackedEntityTypeModel.Columns.UID);

    private static final String TRACKED_ENTITY_INSTANCE_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.Columns.UID);

    private static final String ORG_UNIT_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            OrganisationUnitModel.TABLE, OrganisationUnitModel.TABLE, OrganisationUnitModel.Columns.UID);

    private static final String TEI_ORG_UNIT_QUERY = String.format(
            "SELECT * FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE  %s.%s = ? LIMIT 1",
            OrganisationUnitModel.TABLE,
            TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.Columns.ORGANISATION_UNIT, OrganisationUnitModel.TABLE, OrganisationUnitModel.Columns.UID,
            TrackedEntityInstanceModel.TABLE, TrackedEntityInstanceModel.Columns.UID);

    private static final String ENROLLMENT_ORG_UNIT_QUERY =
            "SELECT OrganisationUnit.* FROM OrganisationUnit " +
                    "WHERE OrganisationUnit.uid IN (" +
                    "SELECT Enrollment.organisationUnit FROM Enrollment " +
                    "JOIN Program ON Program.uid = Enrollment.program WHERE Enrollment.trackedEntityInstance = ? AND Program.uid = ? LIMIT 1)";


    private static final Set<String> TEI_ORG_UNIT_TABLES = new HashSet<>(Arrays.asList(OrganisationUnitModel.TABLE, TrackedEntityInstanceModel.TABLE));

    private static final String PROGRAM_TRACKED_ENTITY_ATTRIBUTES_QUERY = String.format("SELECT * FROM %s WHERE %s.%s = ",
            ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.Columns.PROGRAM);

    private static final String PROGRAM_TRACKED_ENTITY_ATTRIBUTES_NO_PROGRAM_QUERY = String.format(
            "SELECT DISTINCT %s.* FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = '1' GROUP BY %s.%s",
            ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE,
            TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.UID, ProgramTrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.Columns.TRACKED_ENTITY_ATTRIBUTE,
            TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.DISPLAY_IN_LIST_NO_PROGRAM, TrackedEntityAttributeModel.TABLE, TrackedEntityAttributeModel.Columns.UID);
    private static final Set<String> PROGRAM_TRACKED_ENTITY_ATTRIBUTES_NO_PROGRAM_TABLES = new HashSet<>(Arrays.asList(TrackedEntityAttributeModel.TABLE, ProgramTrackedEntityAttributeModel.TABLE));

    private static final String SELECT_PROGRAM_STAGE = String.format("SELECT * FROM %s WHERE %s.%s = ",
            ProgramStageModel.TABLE, ProgramStageModel.TABLE, ProgramStageModel.Columns.UID);

    private static final String SELECT_CATEGORY_OPTION_COMBO = String.format("SELECT * FROM %s WHERE %s.%s = ",
            CategoryOptionComboModel.TABLE, CategoryOptionComboModel.TABLE, CategoryOptionComboModel.Columns.UID);

    private static final String SELECT_CATEGORY_OPTIONS_COMBO = String.format("SELECT %s.* FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s AND %s.%s = ",
            CategoryOptionComboModel.TABLE, CategoryOptionComboModel.TABLE,
            CategoryOptionComboCategoryOptionLinkTableInfo.TABLE_INFO.name(),
            CategoryOptionComboCategoryOptionLinkTableInfo.TABLE_INFO.name(), CategoryOptionComboCategoryOptionLinkTableInfo.Columns.CATEGORY_OPTION_COMBO,
            CategoryOptionComboModel.TABLE, CategoryOptionComboModel.Columns.UID,
            CategoryOptionModel.TABLE, CategoryOptionComboCategoryOptionLinkTableInfo.TABLE_INFO.name(), CategoryOptionComboCategoryOptionLinkTableInfo.Columns.CATEGORY_OPTION,
            CategoryOptionModel.TABLE, CategoryOptionModel.Columns.UID,
            CategoryOptionModel.TABLE, CategoryOptionModel.Columns.ACCESS_DATA_WRITE, CategoryOptionComboModel.TABLE, CategoryOptionComboModel.Columns.CATEGORY_COMBO);

    private static final String SELECT_CATEGORY = "SELECT * FROM Category " +
            "JOIN CategoryCategoryComboLink ON CategoryCategoryComboLink.category = Category.uid " +
            "WHERE CategoryCategoryComboLink.categoryCombo = ?";

    private static final String SELECT_CATEGORY_COMBO = String.format("SELECT * FROM %s WHERE %s.%s = ",
            CategoryComboModel.TABLE, CategoryComboModel.TABLE, CategoryComboModel.Columns.UID);

    private static final String SELECT_DEFAULT_CAT_COMBO = String.format("SELECT %s FROM %s WHERE %s.%s = '1' LIMIT 1",
            CategoryComboModel.Columns.UID, CategoryComboModel.TABLE, CategoryComboModel.TABLE, CategoryComboModel.Columns.IS_DEFAULT);

    private static final String EXPIRY_DATE_PERIOD_QUERY = String.format(
            "SELECT program.* FROM %s " +
                    "JOIN %s ON %s.%s = %s.%s " +
                    "WHERE %s.%s = ? " +
                    "LIMIT 1",
            ProgramModel.TABLE,
            EventModel.TABLE, ProgramModel.TABLE, ProgramModel.Columns.UID, EventModel.TABLE, EventModel.Columns.PROGRAM,
            EventModel.TABLE, EventModel.Columns.UID);

    private final BriteDatabase briteDatabase;

    MetadataRepositoryImpl(@NonNull BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    @Override
    public Observable<TrackedEntityType> getTrackedEntity(String trackedEntityUid) {
        String id = trackedEntityUid == null ? "" : trackedEntityUid;
        return briteDatabase
                .createQuery(TrackedEntityTypeModel.TABLE, TRACKED_ENTITY_QUERY + "'" + id + "' LIMIT 1")
                .mapToOne(TrackedEntityType::create);
    }

    @Override
    public Observable<CategoryCombo> getCategoryComboWithId(String categoryComboId) {
        String id = categoryComboId == null ? "" : categoryComboId;
        return briteDatabase
                .createQuery(CategoryComboModel.TABLE, SELECT_CATEGORY_COMBO + "'" + id + "' LIMIT 1")
                .mapToOne(CategoryCombo::create);
    }

    @Override
    public Observable<String> getDefaultCategoryOptionId() {
        return briteDatabase
                .createQuery(CategoryComboModel.TABLE, SELECT_DEFAULT_CAT_COMBO)
                .mapToOne(cursor -> cursor.getString(0));
    }

    public Observable<TrackedEntityInstance> getTrackedEntityInstance(String teiUid) {
        String id = teiUid == null ? "" : teiUid;
        return briteDatabase
                .createQuery(TrackedEntityInstanceModel.TABLE, TRACKED_ENTITY_INSTANCE_QUERY + "'" + id + "' LIMIT 1")
                .mapToOne(TrackedEntityInstance::create);
    }


    @Override
    public Observable<CategoryOptionCombo> getCategoryOptionComboWithId(String categoryOptionComboId) {
        String id = categoryOptionComboId == null ? "" : categoryOptionComboId;
        return briteDatabase
                .createQuery(CategoryOptionModel.TABLE, SELECT_CATEGORY_OPTION_COMBO + "'" + id + "' LIMIT 1")
                .mapToOne(CategoryOptionCombo::create);
    }


    @Override
    public Observable<List<CategoryOptionCombo>> getCategoryComboOptions(String categoryComboId) {
        String id = categoryComboId == null ? "" : categoryComboId;
        return briteDatabase
                .createQuery(CategoryOptionModel.TABLE, SELECT_CATEGORY_OPTIONS_COMBO + "'" + id + "'")
                .mapToList(CategoryOptionCombo::create);
    }

    @Override
    public Observable<Category> getCategoryFromCategoryCombo(String categoryComboId) {
        return briteDatabase.createQuery(CategoryModel.TABLE, SELECT_CATEGORY, categoryComboId)
                .mapToOne(Category::create);
    }

    @Override
    public void saveCatOption(String eventUid, CategoryOptionCombo selectedOption) {
        ContentValues event = new ContentValues();
        event.put(EventModel.Columns.ATTRIBUTE_OPTION_COMBO, selectedOption.uid());
        briteDatabase.update(EventModel.TABLE, event, EventModel.Columns.UID + " = ?", eventUid == null ? "" : eventUid);
    }

    @Override
    public Observable<OrganisationUnit> getOrganisationUnit(String orgUnitUid) {
        String id = orgUnitUid == null ? "" : orgUnitUid;
        return briteDatabase
                .createQuery(OrganisationUnitModel.TABLE, ORG_UNIT_QUERY + "'" + id + "' LIMIT 1")
                .mapToOne(OrganisationUnit::create);
    }

    @Override
    public Observable<OrganisationUnit> getTeiOrgUnit(String teiUid) {
        return briteDatabase
                .createQuery(TEI_ORG_UNIT_TABLES, TEI_ORG_UNIT_QUERY, teiUid == null ? "" : teiUid)
                .mapToOne(OrganisationUnit::create);
    }

    @Override
    public Observable<OrganisationUnit> getTeiOrgUnit(@NonNull String teiUid, @Nullable String programUid) {
        if (programUid == null)
            return getTeiOrgUnit(teiUid);
        else
            return briteDatabase
                    .createQuery(TEI_ORG_UNIT_TABLES, ENROLLMENT_ORG_UNIT_QUERY, teiUid, programUid)
                    .mapToOne(OrganisationUnit::create);
    }

    @Override
    public Observable<List<ProgramTrackedEntityAttribute>> getProgramTrackedEntityAttributes(String programUid) {
        if (programUid != null)
            return briteDatabase
                    .createQuery(ProgramTrackedEntityAttributeModel.TABLE, PROGRAM_TRACKED_ENTITY_ATTRIBUTES_QUERY + "'" + programUid + "'")
                    .mapToList(ProgramTrackedEntityAttribute::create);
        else
            return briteDatabase
                    .createQuery(PROGRAM_TRACKED_ENTITY_ATTRIBUTES_NO_PROGRAM_TABLES, PROGRAM_TRACKED_ENTITY_ATTRIBUTES_NO_PROGRAM_QUERY)
                    .mapToList(ProgramTrackedEntityAttribute::create);
    }


    @NonNull
    @Override
    public Observable<ProgramStage> programStage(String programStageId) {
        String id = programStageId == null ? "" : programStageId;
        return briteDatabase
                .createQuery(ProgramStageModel.TABLE, SELECT_PROGRAM_STAGE + "'" + id + "' LIMIT 1")
                .mapToOne(ProgramStage::create);
    }


    @Override
    public Observable<List<Enrollment>> getTEIEnrollments(String teiUid) {
        String id = teiUid == null ? "" : teiUid;
        return briteDatabase
                .createQuery(EnrollmentModel.TABLE, SELECT_TEI_ENROLLMENTS + "'" + id + "'")
                .mapToList(Enrollment::create);
    }


    @Override
    public List<Option> optionSet(String optionSetId) {
        List<Option> options = new ArrayList<>();
        String selectOptionSet = "SELECT * FROM " + OptionModel.TABLE + " WHERE Option.optionSet = ?";
        try (Cursor cursor = briteDatabase.query(selectOptionSet, optionSetId == null ? "" : optionSetId)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    options.add(Option.create(cursor));
                    cursor.moveToNext();
                }
            }
        }
        return options;
    }

    @Override
    public Observable<Map<String, ObjectStyle>> getObjectStylesForPrograms(List<Program> enrollmentProgramModels) {
        Map<String, ObjectStyle> objectStyleMap = new HashMap<>();
        for (Program programModel : enrollmentProgramModels) {
            ObjectStyle objectStyle = ObjectStyle.builder().build();
            try (Cursor cursor = briteDatabase.query("SELECT * FROM ObjectStyle WHERE uid = ? LIMIT 1", programModel.uid())) {
                if (cursor.moveToFirst())
                    objectStyle = ObjectStyle.create(cursor);
            } catch (Exception e) {
                Timber.e(e);
            }
            objectStyleMap.put(programModel.uid(), objectStyle);
        }

        return Observable.just(objectStyleMap);
    }

    @Override
    public Flowable<ProgramStage> programStageForEvent(String eventId) {
        return briteDatabase.createQuery(ProgramStageSectionModel.TABLE, "SELECT ProgramStage.* FROM ProgramStage JOIN Event ON Event.programStage = ProgramStage.uid WHERE Event.uid = ? LIMIT 1", eventId)
                .mapToOne(ProgramStage::create).toFlowable(BackpressureStrategy.LATEST);
    }


    @Override
    public Observable<List<Program>> getTeiActivePrograms(String teiUid) {
        return briteDatabase.createQuery(ACTIVE_TEI_PROGRAMS_TABLES, ACTIVE_TEI_PROGRAMS, teiUid == null ? "" : teiUid)
                .mapToList(Program::create);
    }

    @Override
    public Observable<Program> getProgramWithId(String programUid) {
        String id = programUid == null ? "" : programUid;
        return briteDatabase
                .createQuery(ProgramModel.TABLE, PROGRAM_LIST_ALL_QUERY + "'" + id + "' LIMIT 1")
                .mapToOne(Program::create);
    }


    @Override
    public Observable<Pair<String, Integer>> getTheme() {
        return briteDatabase
                .createQuery(SystemSettingModel.TABLE, "SELECT * FROM " + SystemSettingModel.TABLE)
                .mapToList(SystemSetting::create)
                .map(systemSettingModels -> {
                    String flag = "";
                    String style = "";
                    for (SystemSetting settingModel : systemSettingModels)
                        if (SystemSetting.SystemSettingKey.STYLE.equals(settingModel.key()))
                            style = settingModel.value();
                        else
                            flag = settingModel.value();

                    if (style.contains("green"))
                        return Pair.create(flag, R.style.GreenTheme);
                    if (style.contains("india"))
                        return Pair.create(flag, R.style.OrangeTheme);
                    if (style.contains("myanmar"))
                        return Pair.create(flag, R.style.RedTheme);
                    else
                        return Pair.create(flag, R.style.AppTheme);
                });

    }

    @Override
    public Observable<ObjectStyle> getObjectStyle(String uid) {
        return briteDatabase.createQuery(ObjectStyleModel.TABLE, "SELECT * FROM ObjectStyle WHERE uid = ? LIMIT 1", uid == null ? "" : uid)
                .mapToOneOrDefault((ObjectStyle::create), ObjectStyle.builder().build());
    }

    @Override
    public Observable<List<OrganisationUnit>> getOrganisationUnits() {
        return briteDatabase.createQuery(OrganisationUnitModel.TABLE, "SELECT * FROM OrganisationUnit")
                .mapToList(OrganisationUnit::create);
    }


    @Override
    public Observable<Program> getExpiryDateFromEvent(String eventUid) {
        return briteDatabase
                .createQuery(ProgramModel.TABLE, EXPIRY_DATE_PERIOD_QUERY, eventUid == null ? "" : eventUid)
                .mapToOne(Program::create);
    }

    @Override
    public Observable<Boolean> isCompletedEventExpired(String eventUid) {
        return Observable.zip(briteDatabase.createQuery(EventModel.TABLE, "SELECT * FROM Event WHERE uid = ?", eventUid)
                        .mapToOne(Event::create),
                getExpiryDateFromEvent(eventUid),
                ((eventModel, programModel) -> DateUtils.getInstance().isEventExpired(null, eventModel.completedDate(),
                        programModel.completeEventsExpiryDays())));
    }

    @NonNull
    @Override
    public Observable<List<Resource>> syncState(Program program) {
        String syncState = "SELECT * FROM " + ResourceModel.TABLE;
        return briteDatabase
                .createQuery(ResourceModel.TABLE, syncState)
                .mapToList(Resource::create);
    }

    @Override
    public Flowable<Pair<Integer, Integer>> getDownloadedData() {
        String teiCount = "SELECT DISTINCT COUNT (uid) FROM TrackedEntityInstance WHERE TrackedEntityInstance.state != 'RELATIONSHIP'";
        String eventCount = "SELECT DISTINCT COUNT (uid) FROM Event WHERE Event.enrollment IS NULL";

        int currentTei = 0;
        int currentEvent = 0;

        try (Cursor teiCursor = briteDatabase.query(teiCount)) {
            if (teiCursor != null && teiCursor.moveToFirst()) {
                currentTei = teiCursor.getInt(0);
            }
        }

        try (Cursor eventCursor = briteDatabase.query(eventCount)) {
            if (eventCursor != null && eventCursor.moveToFirst()) {
                currentEvent = eventCursor.getInt(0);
            }
        }
        return Flowable.just(Pair.create(currentEvent, currentTei));
    }


    @Override
    public Observable<String> getServerUrl() {
        return briteDatabase.createQuery(AuthenticatedUserModel.TABLE, "SELECT SystemInfo.contextPath FROM SystemInfo LIMIT 1")
                .mapToOne(cursor -> cursor.getString(0));
    }


    @Override
    public List<D2Error> getSyncErrors() {
        List<D2Error> d2Errors = new ArrayList<>();
        try (Cursor cursor = briteDatabase.query("SELECT * FROM D2Error ORDER BY created DESC LIMIT 20")) {
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    d2Errors.add(D2Error.create(cursor));
                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return d2Errors;
    }

    @Override
    public Observable<List<Option>> searchOptions(String text, String idOptionSet, int page, List<String> optionsToHide, List<String> optionsGroupsToHide) {
        String pageQuery = String.format(Locale.US, " LIMIT %d,%d", page * 15, 15);
        String formattedOptionsToHide = "'" + join("','", optionsToHide) + "'";
        String formattedOptionGroupsToHide = "'" + join("','", optionsGroupsToHide) + "'";

        String optionGroupQuery = "SELECT Option.*, OptionGroupOptionLink.optionGroup FROM Option " +
                "LEFT JOIN OptionGroupOptionLink ON OptionGroupOptionLink.option = Option.uid  " +
                "WHERE Option.optionSet = ? " +
                "AND (OptionGroupOptionLink.optionGroup IS NULL OR OptionGroupOptionLink.optionGroup NOT IN (" + formattedOptionGroupsToHide + ")) " +
                "ORDER BY  Option.sortOrder ASC";

        return briteDatabase.createQuery(OptionGroupOptionLinkTableInfo.TABLE_INFO.name(), optionGroupQuery, idOptionSet)
                .mapToList(Option::create)
                .flatMap(list -> {
                    if (list.isEmpty()) {
                        String optionQuery = !isEmpty(text) ?
                                "select Option.* from OptionSet " +
                                        "JOIN Option ON Option.optionSet = OptionSet.uid " +
                                        "where OptionSet.uid = ? and Option.displayName like '%" + text + "%' " +
                                        "AND Option.uid NOT IN (" + formattedOptionsToHide + ") " + pageQuery :
                                "select Option.* from OptionSet " +
                                        "JOIN Option ON Option.optionSet = OptionSet.uid " +
                                        "where OptionSet.uid = ? " +
                                        "AND Option.uid NOT IN (" + formattedOptionsToHide + ") " + pageQuery;

                        return briteDatabase.createQuery(OptionSetModel.TABLE, optionQuery, idOptionSet)
                                .mapToList(Option::create);
                    } else {
                        Iterator<Option> iterator = list.iterator();
                        while (iterator.hasNext()) {
                            Option option = iterator.next();
                            if (optionsToHide.contains(option.uid()))
                                iterator.remove();
                        }
                        return Observable.just(list);
                    }
                });
    }
}