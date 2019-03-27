package org.dhis2.usescases.datasets.dataSetTable;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.utils.DateUtils;
import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.common.Access;
import org.hisp.dhis.android.core.common.ObjectWithUid;
import org.hisp.dhis.android.core.dataelement.DataElement;
import org.hisp.dhis.android.core.dataset.DataSet;
import org.hisp.dhis.android.core.datavalue.DataValue;
import org.hisp.dhis.android.core.period.Period;
import org.hisp.dhis.android.core.period.PeriodType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;

public class DataSetTableRepositoryImpl implements DataSetTableRepository {

    private static final String DATA_ELEMENTS = "SELECT " +
            "DataElement.*," +
            "DataSetSection.sectionName," +
            "DataSetSection.sectionOrder " +
            "FROM DataElement " +
            "LEFT JOIN (" +
            "   SELECT " +
            "       Section.sortOrder AS sectionOrder," +
            "       Section.displayName AS sectionName," +
            "       Section.uid AS sectionId," +
            "       SectionDataElementLink.dataElement AS sectionDataElement " +
            "   FROM Section " +
            "   JOIN SectionDataElementLink ON SectionDataElementLink.section = Section.uid " +
            ") AS DataSetSection ON DataSetSection.sectionDataElement = DataElement.uid " +
            "JOIN DataSetDataElementLink ON DataSetDataElementLink.dataElement = DataElement.uid " +
            "WHERE DataSetDataElementLink.dataSet = ? " +
            "ORDER BY DataSetSection.sectionOrder";

    private static final String PERIOD_CODE = "SELECT Period.* FROM Period WHERE Period.periodType = ? AND Period.startDate = ? LIMIT 1";
    private static final String DATA_VALUES = "SELECT * FROM DataValue " +
            "WHERE DataValue.organisationUnit = ? " +
            "AND DataValue.categoryOptionCombo = ? " +
            "AND DataValue.period = ?";
    private static final String DATA_SET = "SELECT DataSet.* FROM DataSet WHERE DataSet.uid = ?";

    private final BriteDatabase briteDatabase;
    private final String dataSetUid;

    public DataSetTableRepositoryImpl(BriteDatabase briteDatabase, String dataSetUid) {
        this.briteDatabase = briteDatabase;
        this.dataSetUid = dataSetUid;
    }

    @Override
    public Flowable<DataSet> getDataSet() {
        return briteDatabase.createQuery(SqlConstants.DATA_SET_TABLE, DATA_SET, dataSetUid)
                .mapToOne(cursor -> DataSet.builder()
                        .uid(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_UID)))
                        .code(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_CODE)))
                        .name(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_NAME)))
                        .displayName(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_DISPLAY_NAME)))
                        .created(DateUtils.databaseDateFormat().parse(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_CREATED))))
                        .lastUpdated(DateUtils.databaseDateFormat().parse(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_LAST_UPDATED))))
                        .shortName(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_SHORT_NAME)))
                        .displayShortName(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_DISPLAY_SHORT_NAME)))
                        .description(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_DESCRIPTION)))
                        .displayDescription(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_DISPLAY_DESCRIPTION)))
                        .periodType(PeriodType.valueOf(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_PERIOD_TYPE))))
                        .categoryCombo(ObjectWithUid.create(cursor.getString(cursor.getColumnIndex(SqlConstants.DATA_SET_CATEGORY_COMBO))))
                        .mobile(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_MOBILE)) == 1)
                        .version(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_VERSION)))
                        .expiryDays(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_EXPIRY_DAYS)))
                        .timelyDays(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_TIMELY_DAYS)))
                        .notifyCompletingUser(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_NOTIFY_COMPLETING_USER)) == 1)
                        .openFuturePeriods(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_OPEN_FUTURE_PERIODS)))
                        .fieldCombinationRequired(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_FIELD_COMBINATION_REQUIRED)) == 1)
                        .validCompleteOnly(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_VALID_COMPLETE_ONLY)) == 1)
                        .noValueRequiresComment(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_NO_VALUE_REQUIRES_COMMENT)) == 1)
                        .skipOffline(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_SKIP_OFFLINE)) == 1)
                        .dataElementDecoration(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_DATA_ELEMENT_DECORATION)) == 1)
                        .renderAsTabs(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_RENDER_AS_TABS)) == 1)
                        .renderHorizontally(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_RENDER_HORIZONTALLY)) == 1)
                        .access(Access.createForDataWrite(cursor.getInt(cursor.getColumnIndex(SqlConstants.DATA_SET_ACCESS_DATA_WRITE)) == 1))
                        .build()).toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public Flowable<Map<String, List<DataElement>>> getDataElements() {
        Map<String, List<DataElement>> map = new HashMap<>();
        return briteDatabase.createQuery(SqlConstants.DATA_ELEMENT_TABLE, DATA_ELEMENTS, dataSetUid)
                .mapToList(cursor -> {
                    DataElement dataElementModel = DataElement.create(cursor);
                    String section = cursor.getString(cursor.getColumnIndex("sectionName"));
                    if (section == null)
                        section = "NO_SECTION";
                    if (map.get(section) == null) {
                        map.put(section, new ArrayList<>());
                    }
                    map.get(section).add(dataElementModel);

                    return dataElementModel;
                })
                .flatMap(dataElementModels -> Observable.just(map)).toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public Flowable<Map<String, List<CategoryOptionCombo>>> getCatOptions() {
        String query = "SELECT CategoryOptionCombo.* FROM CategoryOptionCombo " +
                "JOIN DataElement ON DataElement.categoryCombo = CategoryOptionCombo.categoryCombo " +
                "JOIN DataSetDataElementLink ON DataSetDataElementLink.dataElement = DataElement.uid " +
                "WHERE DataSetDataElementLink.dataSet = ? " +
                "GROUP BY CategoryOptionCombo.uid";
        Map<String, List<CategoryOptionCombo>> map = new HashMap<>();

        return briteDatabase.createQuery(SqlConstants.CAT_OPTION_COMBO_TABLE, query, dataSetUid)
                .mapToList(cursor -> {
                    CategoryOptionCombo catOptionCombo = CategoryOptionCombo.create(cursor);
                    if (map.get(catOptionCombo.categoryCombo()) == null) {
                        map.put(catOptionCombo.categoryCombo().uid(), new ArrayList<>());
                    }
                    map.get(catOptionCombo.categoryCombo().uid()).add(catOptionCombo);
                    return catOptionCombo;
                }).flatMap(categoryOptionComboModels -> Observable.just(map)).toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public Flowable<List<DataValue>> getDataValues(String orgUnitUid, String periodType, String initPeriodType, String catOptionComb) {
        return briteDatabase.createQuery(SqlConstants.PERIOD_TABLE, PERIOD_CODE, periodType, initPeriodType)
                .mapToOne(Period::create)
                .flatMap(periodModel -> briteDatabase.createQuery(SqlConstants.DATA_VALUE_TABLE, DATA_VALUES, periodModel.periodId())
                        .mapToList(cursor -> DataValue.builder()
                                .build())).toFlowable(BackpressureStrategy.LATEST);
    }
}
