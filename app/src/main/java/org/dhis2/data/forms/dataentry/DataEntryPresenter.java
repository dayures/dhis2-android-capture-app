package org.dhis2.data.forms.dataentry;


import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;

import java.util.List;

import io.reactivex.Observable;

interface DataEntryPresenter {
    @UiThread
    void onAttach(@NonNull DataEntryView view);

    @UiThread
    void onDetach();

    @NonNull
    Observable<List<OrganisationUnitModel>> getOrgUnits();


}
