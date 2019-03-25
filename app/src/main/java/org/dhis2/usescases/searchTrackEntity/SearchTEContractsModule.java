package org.dhis2.usescases.searchTrackEntity;

import org.dhis2.data.forms.dataentry.fields.RowAction;
import org.dhis2.data.tuples.Pair;
import org.dhis2.data.tuples.Trio;
import org.dhis2.usescases.general.AbstractActivityContracts;
import org.dhis2.usescases.searchTrackEntity.adapters.SearchTeiModel;
import org.hisp.dhis.android.core.option.Option;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityType;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.Nullable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * QUADRAM. Created by ppajuelo on 02/11/2017.
 */

public class SearchTEContractsModule {

    public interface View extends AbstractActivityContracts.View {
        void setForm(List<TrackedEntityAttribute> trackedEntityAttributeModels, @Nullable Program program, HashMap<String, String> queryData);

        Consumer<Pair<List<SearchTeiModel>, String>> swapTeiListData();

        void setPrograms(List<Program> programModels);

        void clearList(String uid);

        Flowable<RowAction> rowActionss();

        Flowable<Trio<String, String, Integer>> optionSetActions();

        Flowable<Integer> onlinePage();

        Flowable<Integer> offlinePage();

        void clearData();

        void setTutorial();

        void setProgramColor(String data);

        String fromRelationshipTEI();

        void setListOptions(List<Option> options);
    }

    public interface Presenter {

        void init(View view, String trackedEntityType, String initialProgram);

        void onDestroy();

        void setProgram(Program programSelected);

        void onBackClick();

        void onClearClick();

        void onFabClick(android.view.View view);

        void onEnrollClick(android.view.View view);

        void enroll(String programUid, String uid);

        void onTEIClick(String teiUid, boolean isOnline);

        void getTrakedEntities();

        TrackedEntityType getTrackedEntityName();

        Program getProgramModel();

        void addRelationship(String teiUid, String relationshipTypeUid, boolean online);

        void addRelationship(String teiUid, boolean online);

        void downloadTei(String teiUid);

        void downloadTeiForRelationship(String teiUid, String relationshipTypeUid);

        Observable<List<OrganisationUnit>> getOrgUnits();

        String getProgramColor(String uid);
    }
}
