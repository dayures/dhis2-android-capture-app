package org.dhis2.usescases.teiDashboard.teiProgramList;

import android.app.DatePickerDialog;
import android.content.DialogInterface;

import org.dhis2.R;
import org.dhis2.usescases.main.program.ProgramViewModel;
import org.dhis2.utils.custom_views.OrgUnitDialog;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * QUADRAM. Created by Cristian on 06/03/2018.
 */

public class TeiProgramListInteractorImpl implements TeiProgramListContract.TeiProgramListInteractor {

    private TeiProgramListContract.TeiProgramListView view;
    private String trackedEntityId;
    private CompositeDisposable compositeDisposable;
    private final TeiProgramListRepository teiProgramListRepository;

    @SuppressWarnings("squid:S1450")
    private Date selectedEnrollmentDate;

    TeiProgramListInteractorImpl(TeiProgramListRepository teiProgramListRepository) {
        this.teiProgramListRepository = teiProgramListRepository;
    }

    @Override
    public void init(TeiProgramListContract.TeiProgramListView view, String trackedEntityId) {
        this.view = view;
        this.trackedEntityId = trackedEntityId;
        compositeDisposable = new CompositeDisposable();

        getActiveEnrollments();
        getOtherEnrollments();
        getPrograms();
    }

    private void filterOrgUnits(List<OrganisationUnit> allOrgUnits, OrgUnitDialog orgUnitDialog, String programUid, String uid){
        ArrayList<OrganisationUnit> orgUnits = new ArrayList<>();
        for (OrganisationUnit orgUnit : allOrgUnits) {
            boolean afterOpening = false;
            boolean beforeClosing = false;
            if (orgUnit.openingDate() == null || !selectedEnrollmentDate.before(orgUnit.openingDate()))
                afterOpening = true;
            if (orgUnit.closedDate() == null || !selectedEnrollmentDate.after(orgUnit.closedDate()))
                beforeClosing = true;
            if (afterOpening && beforeClosing)
                orgUnits.add(orgUnit);
        }
        if (orgUnits.size() > 1) {
            orgUnitDialog.setOrgUnits(orgUnits);
            if (!orgUnitDialog.isAdded())
                orgUnitDialog.show(view.getAbstracContext().getSupportFragmentManager(), "OrgUnitEnrollment");
        } else
            enrollInOrgUnit(orgUnits.get(0).uid(), programUid, uid, selectedEnrollmentDate);
    }

    @Override
    public void enroll(String programUid, String uid) {
        selectedEnrollmentDate = Calendar.getInstance().getTime();

        OrgUnitDialog orgUnitDialog = OrgUnitDialog.getInstance().setMultiSelection(false);
        orgUnitDialog.setTitle("Enrollment Org Unit")
                .setPossitiveListener(v -> {
                    if (orgUnitDialog.getSelectedOrgUnit() != null && !orgUnitDialog.getSelectedOrgUnit().isEmpty())
                        enrollInOrgUnit(orgUnitDialog.getSelectedOrgUnit(), programUid, uid, selectedEnrollmentDate);
                    orgUnitDialog.dismiss();
                })
                .setNegativeListener(v -> orgUnitDialog.dismiss());

        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dateDialog = new DatePickerDialog(view.getContext(), (
                (datePicker, year1, month1, day1) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(Calendar.YEAR, year1);
                    selectedCalendar.set(Calendar.MONTH, month1);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, day1);
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
                    selectedCalendar.set(Calendar.MINUTE, 0);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);
                    selectedEnrollmentDate = selectedCalendar.getTime();

                    compositeDisposable.add(getOrgUnits(programUid)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    allOrgUnits -> filterOrgUnits(allOrgUnits, orgUnitDialog, programUid, uid),
                                    Timber::d
                            )
                    );


                }),
                year,
                month,
                day);

        Program selectedProgram = getProgramFromUid(programUid);
        if (selectedProgram != null && !selectedProgram.selectEnrollmentDatesInFuture()) {
            dateDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        }
        if (selectedProgram != null) {
            dateDialog.setTitle(selectedProgram.enrollmentDateLabel());
        }
        dateDialog.setButton(DialogInterface.BUTTON_NEGATIVE, view.getContext().getString(R.string.date_dialog_clear), (dialog, which) -> dialog.dismiss());
        dateDialog.show();

    }

    private Program getProgramFromUid(String programUid) {
        return teiProgramListRepository.getProgram(programUid);
    }

    private void enrollInOrgUnit(String orgUnitUid, String programUid, String teiUid, Date enrollmentDate) {
        compositeDisposable.add(
                teiProgramListRepository.saveToEnroll(orgUnitUid, programUid, teiUid, enrollmentDate)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(enrollmentUid -> view.goToEnrollmentScreen(enrollmentUid, programUid),
                                Timber::d)
        );
    }

    public Observable<List<OrganisationUnit>> getOrgUnits(String programUid) {
        return teiProgramListRepository.getOrgUnits(programUid);
    }

    private void getActiveEnrollments() {
        compositeDisposable.add(teiProgramListRepository.activeEnrollments(trackedEntityId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        view::setActiveEnrollments,
                        Timber::d)
        );
    }

    private void getOtherEnrollments() {
        compositeDisposable.add(teiProgramListRepository.otherEnrollments(trackedEntityId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        view::setOtherEnrollments,
                        Timber::d)
        );
    }

    private void getPrograms() {
        compositeDisposable.add(teiProgramListRepository.allPrograms(trackedEntityId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::getAlreadyEnrolledPrograms,
                        Timber::d)
        );
    }

    private void getAlreadyEnrolledPrograms(List<ProgramViewModel> programs) {
        compositeDisposable.add(teiProgramListRepository.alreadyEnrolledPrograms(trackedEntityId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        alreadyEnrolledPrograms -> deleteRepeatedPrograms(programs, alreadyEnrolledPrograms),
                        Timber::d)
        );
    }

    private void deleteRepeatedPrograms(List<ProgramViewModel> allPrograms, List<Program> alreadyEnrolledPrograms) {
        ArrayList<ProgramViewModel> programListToPrint = new ArrayList<>();
        for (ProgramViewModel programModel1 : allPrograms) {
            boolean isAlreadyEnrolled = false;
            boolean onlyEnrollOnce = false;
            for (Program programModel2 : alreadyEnrolledPrograms) {
                if (programModel1.id().equals(programModel2.uid())) {
                    isAlreadyEnrolled = true;
                    onlyEnrollOnce = programModel2.onlyEnrollOnce();
                }
            }
            if (!isAlreadyEnrolled || !onlyEnrollOnce) {
                programListToPrint.add(programModel1);
            }
        }
        view.setPrograms(programListToPrint);
    }

    @Override
    public String getProgramColor(@NotNull String uid) {
        return teiProgramListRepository.getProgramColor(uid);
    }

    @Override
    public void onDettach() {
        compositeDisposable.clear();
    }
}
