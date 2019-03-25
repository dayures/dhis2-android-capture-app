package org.dhis2.usescases.programEventDetail;

import com.unnamed.b.atv.model.TreeNode;

import org.dhis2.data.tuples.Pair;
import org.dhis2.usescases.general.AbstractActivityContracts;
import org.dhis2.utils.Period;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.period.DatePeriod;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramModel;

import java.util.Date;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * QUADRAM. Created by Cristian on 13/02/2017.
 */

public class ProgramEventDetailContract {

    public interface View extends AbstractActivityContracts.View {
        void setData(List<ProgramEventViewModel> events);

        void addTree(TreeNode treeNode);

        void openDrawer();

        void showTimeUnitPicker();

        void showRageDatePicker();

        void setProgram(Program programModel);

        void renderError(String message);

        void setCatComboOptions(CategoryCombo catCombo, List<CategoryOptionCombo> catComboList);

        void showHideFilter();

        void apply();

        void setWritePermission(Boolean aBoolean);

        Flowable<Integer> currentPage();

        void orgUnitProgress(boolean showProgress);

        Consumer<Pair<TreeNode, List<TreeNode>>> addNodeToTree();
    }

    public interface Presenter extends AbstractActivityContracts.Presenter {
        void init(View view, Period period);

        void updateDateFilter(List<DatePeriod> datePeriodList);

        void updateOrgUnitFilter(List<String> orgUnitList);

        void onTimeButtonClick();

        void onDateRangeButtonClick();

        void onOrgUnitButtonClick();

        void addEvent();

        void onBackClick();

        void setProgram(Program program);

        void onCatComboSelected(CategoryOptionCombo categoryOptionComboModel);

        void clearCatComboFilters();

        void onEventClick(String eventId, String orgUnit);

        Observable<List<String>> getEventDataValueNew(Event event);

        void showFilter();

        List<OrganisationUnit> getOrgUnits();

        void setFilters(List<Date> selectedDates, Period currentPeriod, String orgUnits);

        void onExpandOrgUnitNode(TreeNode node, String uid);
    }
}
