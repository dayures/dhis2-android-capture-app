package org.dhis2.data.forms.dataentry;


import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import io.reactivex.Observable;

interface DataEntryPresenter {
    @UiThread
    void onAttach(@NonNull DataEntryView view);

    @UiThread
    void onDetach();

    @NonNull
    Observable<List<OrganisationUnit>> getOrgUnits();


}
