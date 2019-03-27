package org.dhis2.usescases.datasets.datasetInitial;

import android.database.Cursor;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.category.Category;
import org.hisp.dhis.android.core.category.CategoryOption;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.period.PeriodType;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Observable;

public class DataSetInitialRepositoryImpl implements DataSetInitialRepository {

    private static final String GET_DATA_SET_INFO = "SELECT " +
            "DataSet.displayName, " +
            "DataSet.description, " +
            "DataSet.categoryCombo, " +
            "DataSet.periodType, " +
            "CategoryCombo.displayName " +
            "FROM DataSet JOIN CategoryCombo ON CategoryCombo.uid = DataSet.categoryCombo " +
            "WHERE DataSet.uid = ? LIMIT 1";

    private static final String GET_ORG_UNITS = "SELECT OrganisationUnit.* FROM OrganisationUnit " +
            "JOIN DataSetOrganisationUnitLink ON DataSetOrganisationUnitLink.organisationUnit = OrganisationUnit.uid " +
            "WHERE DataSetOrganisationUnitLink.dataSet = ?";

    private static final String GET_CATEGORIES = "SELECT Category.* FROM Category " +
            "JOIN CategoryCategoryComboLink ON CategoryCategoryComboLink.category = Category.uid " +
            "WHERE CategoryCategoryComboLink.categoryCombo = ?";
    private static final String GET_CATEGORY_OPTION = "SELECT CategoryOption.* FROM CategoryOption " +
            "JOIN CategoryCategoryOptionLink ON CategoryCategoryOptionLink.categoryOption = CategoryOption.uid " +
            "WHERE CategoryCategoryOptionLink.category = ? ORDER BY CategoryOption.displayName ASC";


    private final BriteDatabase briteDatabase;
    private final String dataSetUid;

    public DataSetInitialRepositoryImpl(com.squareup.sqlbrite2.BriteDatabase briteDatabase, String dataSetUid) {
        this.briteDatabase = briteDatabase;
        this.dataSetUid = dataSetUid;
    }

    @NonNull
    @Override
    public Observable<DataSetInitialModel> dataSet() {
        return briteDatabase.createQuery(SqlConstants.DATA_SET_TABLE, GET_DATA_SET_INFO, dataSetUid)
                .mapToOne(cursor -> {

                    String displayName = cursor.getString(0);
                    String description = cursor.getString(1);
                    String categoryComboUid = cursor.getString(2);
                    PeriodType periodType = PeriodType.valueOf(cursor.getString(3));
                    String categoryComboName = cursor.getString(4);

                    List<Category> categoryModels = getCategoryModels(categoryComboUid);

                    return DataSetInitialModel.create(
                            displayName,
                            description,
                            categoryComboUid,
                            categoryComboName,
                            periodType,
                            categoryModels
                    );
                });
    }

    private List<Category> getCategoryModels(String categoryComboUid) {
        List<Category> categoryModelList = new ArrayList<>();
        try (Cursor cursor = briteDatabase.query(GET_CATEGORIES, categoryComboUid)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    categoryModelList.add(Category.create(cursor));
                    cursor.moveToNext();
                }
            }
        }

        return categoryModelList;
    }

    @NonNull
    @Override
    public Observable<List<OrganisationUnit>> orgUnits() {
        return briteDatabase.createQuery(SqlConstants.ORG_UNIT_TABLE, GET_ORG_UNITS, dataSetUid)
                .mapToList(OrganisationUnit::create);
    }

    @NonNull
    @Override
    public Observable<List<CategoryOption>> catCombo(String categoryUid) {
        return briteDatabase.createQuery(SqlConstants.CAT_OPTION_TABLE, GET_CATEGORY_OPTION, categoryUid)
                .mapToList(CategoryOption::create);
    }
}
