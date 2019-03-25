package org.dhis2.data.forms.dataentry;

import org.dhis2.data.forms.dataentry.fields.FieldViewModel;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Observable;

public interface DataEntryRepository {

    @NonNull
    Observable<List<FieldViewModel>> list();

    List<FieldViewModel> fieldList();

    Observable<List<OrganisationUnit>> getOrgUnits();

    void assign(String field, String content);
}
