package org.dhis2.usescases.programEventDetail;

import android.os.Bundle;

import com.unnamed.b.atv.model.TreeNode;

import org.dhis2.data.metadata.MetadataRepository;
import org.dhis2.data.tuples.Pair;
import org.dhis2.usescases.eventsWithoutRegistration.eventCapture.EventCaptureActivity;
import org.dhis2.usescases.eventsWithoutRegistration.eventInitial.EventInitialActivity;
import org.dhis2.utils.Constants;
import org.dhis2.utils.OrgUnitUtils;
import org.dhis2.utils.Period;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.period.DatePeriod;
import org.hisp.dhis.android.core.program.Program;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.dhis2.utils.Constants.ORG_UNIT;
import static org.dhis2.utils.Constants.PROGRAM_UID;


/**
 * QUADRAM. Created by Cristian on 13/02/2018.
 */

public class ProgramEventDetailPresenterImpl implements ProgramEventDetailContract.ProgramEventDetailPresenter {

    private final ProgramEventDetailRepository eventRepository;
    private final MetadataRepository metaRepository;
    private ProgramEventDetailContract.ProgramEventDetailView view;
    protected Program program;
    protected String programId;
    private CompositeDisposable compositeDisposable;
    private CategoryOptionCombo categoryOptionComboModel;
    private List<OrganisationUnit> orgUnits = new ArrayList<>();
    private FlowableProcessor<Pair<TreeNode, String>> parentOrgUnit;
    private FlowableProcessor<Pair<List<DatePeriod>, List<String>>> programQueries;
    private List<DatePeriod> currentDateFilter;
    private List<String> currentOrgUnitFilter;

    //Search fields
    private CategoryCombo mCatCombo;
    private List<Date> dates;
    private String orgUnitQuery;
    private Period currentPeriod;

    ProgramEventDetailPresenterImpl(
            @NonNull String programUid, @NonNull ProgramEventDetailRepository programEventDetailRepository,
            @NonNull MetadataRepository metadataRepository) {
        this.eventRepository = programEventDetailRepository;
        this.metaRepository = metadataRepository;
        this.programId = programUid;
    }

    @Override
    public void init(ProgramEventDetailContract.ProgramEventDetailView mview, Period period) {
        view = mview;
        compositeDisposable = new CompositeDisposable();
        this.currentPeriod = period;
        this.currentOrgUnitFilter = new ArrayList<>();
        this.currentDateFilter = new ArrayList<>();
        programQueries = PublishProcessor.create();
        parentOrgUnit = PublishProcessor.create();

        compositeDisposable.add(metaRepository.getProgramWithId(programId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        programModel -> {
                            view.setProgram(programModel);
                            view.setWritePermission(programModel.access().data().write());
                            getCatCombo(programModel);
                        },
                        Timber::d)
        );

        compositeDisposable.add(
                parentOrgUnit
                        .flatMap(orgUnit -> eventRepository.orgUnits(orgUnit.val1()).toFlowable(BackpressureStrategy.LATEST)
                                .map(orgUnits1 -> OrgUnitUtils.createNode(view.getContext(), orgUnits, true))
                                .map(nodeList -> Pair.create(orgUnit.val0(), nodeList)))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                view.addNodeToTree(),
                                Timber::e
                        ));

        compositeDisposable.add(
                view.currentPage()
                        .startWith(0)
                        .flatMap(page -> eventRepository.filteredProgramEvents(programId, dates, currentPeriod, categoryOptionComboModel, orgUnitQuery, page).distinctUntilChanged())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                view::setData,
                                Timber::e));

    }

    private void getCatCombo(Program programModel) {
        compositeDisposable.add(metaRepository.getCategoryComboWithId(programModel.categoryCombo().uid())
                .filter(categoryComboModel -> categoryComboModel != null && !categoryComboModel.isDefault() && !categoryComboModel.uid().equals(CategoryComboModel.DEFAULT_UID))
                .flatMap(catCombo -> {
                    this.mCatCombo = catCombo;
                    return eventRepository.catCombo(programModel.categoryCombo().uid());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(catComboOptions -> view.setCatComboOptions(mCatCombo, catComboOptions), Timber::d)
        );
    }

    @Override
    public void updateDateFilter(List<DatePeriod> datePeriodList) {
        this.currentDateFilter = datePeriodList;
        programQueries.onNext(Pair.create(currentDateFilter, currentOrgUnitFilter));
    }

    @Override
    public void updateOrgUnitFilter(List<String> orgUnitList) {
        this.currentOrgUnitFilter = orgUnitList;
        programQueries.onNext(Pair.create(currentDateFilter, currentOrgUnitFilter));
    }

    @Override
    public void onTimeButtonClick() {
        view.showTimeUnitPicker();
    }

    @Override
    public void onDateRangeButtonClick() {
        view.showRageDatePicker();
    }

    @Override
    public void onOrgUnitButtonClick() {
        view.openDrawer();
        if (orgUnits.isEmpty()) {
            view.orgUnitProgress(true);
            compositeDisposable.add(
                    eventRepository.orgUnits()
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    data -> {
                                        this.orgUnits = data;
                                        view.orgUnitProgress(false);
                                        view.addTree(OrgUnitUtils.renderTree(view.getContext(), orgUnits, true));
                                    },
                                    throwable -> view.renderError(throwable.getMessage())));
        }
    }

    @Override
    public void setProgram(Program program) {
        this.program = program;
    }

    @Override
    public List<OrganisationUnit> getOrgUnits() {
        return this.orgUnits;
    }

    @Override
    public void setFilters(List<Date> selectedDates, Period currentPeriod, String orgUnits) {
        this.dates = selectedDates;
        this.currentPeriod = currentPeriod;
        this.orgUnitQuery = orgUnits;
    }

    @Override
    public void onExpandOrgUnitNode(TreeNode treeNode, String parentUid) {
        parentOrgUnit.onNext(Pair.create(treeNode, parentUid));

    }

    @Override
    public void onCatComboSelected(CategoryOptionCombo categoryOptionComboModel) {
        this.categoryOptionComboModel = categoryOptionComboModel;

    }

    @Override
    public void clearCatComboFilters() {
        this.categoryOptionComboModel = null;

    }

    @Override
    public void onEventClick(String eventId, String orgUnit) {
        Bundle bundle = new Bundle();
        bundle.putString(PROGRAM_UID, programId);
        bundle.putString(Constants.EVENT_UID, eventId);
        bundle.putString(ORG_UNIT, orgUnit);
        view.startActivity(EventCaptureActivity.class,
                EventCaptureActivity.getActivityBundle(eventId, programId),
                false, false, null
        );
    }

    @Override
    public Observable<List<String>> getEventDataValueNew(Event event) {
        return eventRepository.eventDataValuesNew(event);
    }

    public void addEvent() {
        Bundle bundle = new Bundle();
        bundle.putString(PROGRAM_UID, programId);
        view.startActivity(EventInitialActivity.class, bundle, false, false, null);
    }

    @Override
    public void onBackClick() {
        view.back();
    }

    @Override
    public void onDettach() {
        compositeDisposable.clear();
    }

    @Override
    public void displayMessage(String message) {
        view.displayMessage(message);
    }

    @Override
    public void showFilter() {
        view.showHideFilter();
    }
}