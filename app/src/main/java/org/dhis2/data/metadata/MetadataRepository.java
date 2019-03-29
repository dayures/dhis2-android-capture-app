package org.dhis2.data.metadata;

import org.dhis2.data.tuples.Pair;
import org.hisp.dhis.android.core.category.Category;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.common.ObjectStyle;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.maintenance.D2Error;
import org.hisp.dhis.android.core.option.Option;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.core.resource.Resource;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityType;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Flowable;
import io.reactivex.Observable;


/**
 * QUADRAM. Created by ppajuelo on 04/12/2017.
 */

public interface MetadataRepository {

    /*PROGRAMS*/
    Observable<List<Program>> getTeiActivePrograms(String teiUid);

    Observable<Program> getProgramWithId(String programUid);

    /*TRACKED ENTITY*/

    Observable<TrackedEntityType> getTrackedEntity(String trackedEntityUid);

    Observable<TrackedEntityInstance> getTrackedEntityInstance(String teiUid);

    /*CATEGORY OPTION*/

    Observable<String> getDefaultCategoryOptionId();


    /*CATEGORY OPTION COMBO*/

    Observable<CategoryOptionCombo> getCategoryOptionComboWithId(String categoryOptionComboId);

    Observable<List<CategoryOptionCombo>> getCategoryComboOptions(String categoryComboId);

    Observable<CategoryCombo> catComboForProgram(String programUid);

    Observable<Category> getCategoryFromCategoryCombo(String categoryComboId);

    void saveCatOption(String eventUid, String catOptionComboUid);

    /*CATEGORY COMBO*/

    Observable<CategoryCombo> getCategoryComboWithId(String categoryComboId);

    /*ORG UNIT*/

    Observable<OrganisationUnit> getOrganisationUnit(String orgUnitUid);

    Observable<OrganisationUnit> getTeiOrgUnit(String teiUid);

    Observable<OrganisationUnit> getTeiOrgUnit(@NonNull String teiUid, @Nullable String programUid);

    /*PROGRAM TRACKED ENTITY ATTRIBUTE*/

    Observable<List<ProgramTrackedEntityAttribute>> getProgramTrackedEntityAttributes(String programUid);


    //ProgramStage

    @NonNull
    Observable<ProgramStage> programStage(String programStageId);

    /*ENROLLMENTS*/
    Observable<List<Enrollment>> getTEIEnrollments(String teiUid);


    /*EVENTS*/

    Observable<Program> getExpiryDateFromEvent(String eventUid);

    Observable<Boolean> isCompletedEventExpired(String eventUid);


    /*OPTION SET*/
    List<Option> optionSet(String optionSetId);

    /*RESOURCE*/

    /*SETINGS*/
    Observable<Pair<String, Integer>> getTheme();

    Observable<ObjectStyle> getObjectStyle(String uid);

    Observable<List<OrganisationUnit>> getOrganisationUnits();


    @NonNull
    Observable<List<Resource>> syncState(Program program);

    Flowable<Pair<Integer, Integer>> getDownloadedData();

    Observable<String> getServerUrl();

    List<D2Error> getSyncErrors();

    Observable<List<Option>> searchOptions(String text, String idOptionSet, int page, List<String> optionsToHide, List<String> optionsGroupsToHide);

    Observable<Map<String, ObjectStyle>> getObjectStylesForPrograms(List<Program> enrollmentProgramModels);

    Flowable<ProgramStage> programStageForEvent(String eventId);
}