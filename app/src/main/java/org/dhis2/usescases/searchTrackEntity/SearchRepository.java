package org.dhis2.usescases.searchTrackEntity;

import org.dhis2.usescases.searchTrackEntity.adapters.SearchTeiModel;
import org.hisp.dhis.android.core.option.Option;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * QUADRAM. Created by ppajuelo on 02/11/2017.
 */

public interface SearchRepository {

    @NonNull
    Observable<List<TrackedEntityAttribute>> programAttributes(String programId);

    Observable<List<TrackedEntityAttribute>> programAttributes();

    Observable<List<Option>> optionSet(String optionSetId);

    Observable<List<Program>> programsWithRegistration(String programTypeId);

    Observable<List<TrackedEntityInstance>> trackedEntityInstances(@NonNull String teType,
                                                                   @Nullable Program selectedProgram,
                                                                   @Nullable HashMap<String, String> queryData, Integer page);

    Observable<List<TrackedEntityInstance>> trackedEntityInstancesToUpdate(@NonNull String teType,
                                                                           @Nullable Program selectedProgram,
                                                                           @Nullable HashMap<String, String> queryData, int listSize);

    @NonNull
    Observable<String> saveToEnroll(@NonNull String teiType, @NonNull String orgUnitUID, @NonNull String programUid, @Nullable String teiUid, HashMap<String, String> queryDatam, Date enrollmentDate);

    Observable<List<OrganisationUnit>> getOrgUnits(@Nullable String selectedProgramUid);

    Flowable<List<SearchTeiModel>> transformIntoModel(List<SearchTeiModel> teiList, @Nullable Program selectedProgram);

    String getProgramColor(@NonNull String programUid);

    Observable<List<TrackedEntityAttribute>> trackedEntityTypeAttributes();
}
