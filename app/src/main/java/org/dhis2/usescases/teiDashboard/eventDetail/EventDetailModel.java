package org.dhis2.usescases.teiDashboard.eventDetail;

import org.dhis2.data.tuples.Pair;
import org.dhis2.utils.DateUtils;
import org.hisp.dhis.android.core.category.CategoryOptionCombo;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramStage;

import java.util.Date;
import java.util.List;

import androidx.databinding.BaseObservable;

/**
 * QUADRAM. Created by Cristian on 08/02/2018.
 */

public class EventDetailModel extends BaseObservable {

    private final ProgramStage programStage;
    private final List<CategoryOptionCombo> optionComboList;
    private final Program programModel;
    private final String catComboName;
    private final boolean isEnrollmentActive;
    private Event eventModel;
    private final OrganisationUnit orgUnit;

    EventDetailModel(Event eventModel,
                     ProgramStage programStage,
                     OrganisationUnit orgUnit,
                     Pair<String, List<CategoryOptionCombo>> optionComboList,
                     Program programModel,
                     boolean isEnrollmentActive) {
        this.eventModel = eventModel;
        this.programStage = programStage;
        this.orgUnit = orgUnit;
        this.catComboName = optionComboList.val0();
        this.optionComboList = optionComboList.val1();
        this.programModel = programModel;
        this.isEnrollmentActive = isEnrollmentActive;

    }

    Event getEventModel() {
        return eventModel;
    }

    public ProgramStage getProgramStage() {
        return programStage;
    }

    public String getCatComboName() {
        return catComboName;
    }

    public Date orgUnitOpeningDate() {
        return orgUnit.openingDate();
    }

    public Date orgUnitClosingDate() {
        return orgUnit.closedDate();
    }

    public String getOrgUnitName() {
        return orgUnit.displayName();
    }

    public List<CategoryOptionCombo> getOptionComboList() {
        return optionComboList;
    }

    public boolean hasExpired() {
        return eventModel.completedDate() != null && DateUtils.getInstance().hasExpired(eventModel, programModel.expiryDays(), programModel.completeEventsExpiryDays(), programModel.expiryPeriodType());
    }

    public String getEventCatComboOptionName() {
        String eventCatComboOptionName = null;

        for (CategoryOptionCombo option : optionComboList) {
            if (option.uid().equals(eventModel.attributeOptionCombo()))
                eventCatComboOptionName = option.name();
        }

        return eventCatComboOptionName;
    }

    public boolean isEnrollmentActive() {
        return isEnrollmentActive;
    }

    public Program getProgram() {
        return programModel;
    }
}
