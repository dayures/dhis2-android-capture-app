package org.dhis2.usescases.programEventDetail;

import org.dhis2.utils.Period;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.period.DatePeriod;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * Created by Cristian E. on 02/11/2017.
 */

public interface ProgramEventDetailRepository {

    @NonNull
    Flowable<List<ProgramEventViewModel>> filteredProgramEvents(String programUid, List<Date> dates, Period period,
                                                                CategoryOptionCombo categoryOptionComboModel, String orgUnitQuery, int page);

    @NonNull
    Flowable<List<ProgramEventViewModel>> filteredProgramEvents(List<DatePeriod> dateFilter, List<String> orgUnitFilter, int page);

    @NonNull
    Observable<List<OrganisationUnit>> orgUnits();

    @NonNull
    Observable<List<OrganisationUnit>> orgUnits(String parentUid);

    @NonNull
    Observable<List<CategoryOptionCombo>> catCombo(String programUid);

    Observable<List<String>> eventDataValuesNew(Event eventModel);

    Observable<Boolean> writePermission(String programId);
}
