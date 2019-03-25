package org.dhis2.usescases.teiDashboard;

import org.hisp.dhis.android.core.common.ObjectStyle;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.hisp.dhis.android.core.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;

import java.util.List;
import java.util.Map;

import androidx.databinding.BaseObservable;

/**
 * QUADRAM. Created by ppajuelo on 04/12/2017.
 */
public class DashboardProgramModel extends BaseObservable {

    private TrackedEntityInstance tei;
    private List<ProgramTrackedEntityAttribute> trackedEntityAttributesModel;
    private List<TrackedEntityAttributeValue> trackedEntityAttributeValues;
    private List<Event> eventModels;
    private Enrollment currentEnrollment;
    private List<ProgramStage> programStages;
    private List<Program> enrollmentProgramModels;
    private OrganisationUnit orgnUnit;
    private List<Enrollment> teiEnrollments;
    private Map<String, ObjectStyle> programObjectStyles;

    @SuppressWarnings("squid:S00107")
    public DashboardProgramModel(
            TrackedEntityInstance tei,
            Enrollment currentEnrollment,
            List<ProgramStage> programStages,
            List<Event> events,
            List<ProgramTrackedEntityAttribute> trackedEntityAttributeModels,
            List<TrackedEntityAttributeValue> trackedEntityAttributeValues,
            OrganisationUnit orgnUnit,
            List<Program> enrollmentProgramModels) {

        this.currentEnrollment = currentEnrollment;
        this.programStages = programStages;
        this.orgnUnit = orgnUnit;
        this.enrollmentProgramModels = enrollmentProgramModels;
        this.tei = tei;
        this.eventModels = events;
        this.trackedEntityAttributesModel = trackedEntityAttributeModels;
        this.trackedEntityAttributeValues = trackedEntityAttributeValues;
    }

    public DashboardProgramModel(TrackedEntityInstance tei,
                                 List<ProgramTrackedEntityAttribute> trackedEntityAttributeModels,
                                 List<TrackedEntityAttributeValue> trackedEntityAttributeValues,
                                 OrganisationUnit orgnUnit,
                                 List<Program> enrollmentProgramModels,
                                 List<Enrollment> teiEnrollments) {
        this.tei = tei;
        this.trackedEntityAttributesModel = trackedEntityAttributeModels;
        this.trackedEntityAttributeValues = trackedEntityAttributeValues;
        this.orgnUnit = orgnUnit;
        this.enrollmentProgramModels = enrollmentProgramModels;
        this.teiEnrollments = teiEnrollments;
    }

    public TrackedEntityInstance getTei() {
        return tei;
    }

    public Enrollment getCurrentEnrollment() {
        return currentEnrollment;
    }

    public List<ProgramStage> getProgramStages() {
        return programStages;
    }

    public OrganisationUnit getOrgUnit() {
        return orgnUnit;
    }


    public String getAttributeBySortOrder(int sortOrder) {
        TrackedEntityAttributeValue attributeValue = null;
        sortOrder--;
        if (sortOrder < trackedEntityAttributesModel.size())
            for (TrackedEntityAttributeValue attribute : trackedEntityAttributeValues)
                if (attribute.trackedEntityAttribute().equals(trackedEntityAttributesModel.get(sortOrder).trackedEntityAttribute().uid()))
                    attributeValue = attribute;


        return attributeValue != null ? attributeValue.value() : "";
    }

    public List<Program> getEnrollmentProgramModels() {
        return enrollmentProgramModels;
    }

    public List<Event> getEvents() {
        return eventModels;
    }

    public Program getCurrentProgram() {
        Program selectedProgram = null;
        if (currentEnrollment != null)
            for (Program programModel : enrollmentProgramModels)
                if (programModel.uid().equals(currentEnrollment.program()))
                    selectedProgram = programModel;
        return selectedProgram;
    }

    public List<TrackedEntityAttributeValue> getTrackedEntityAttributeValues() {
        return trackedEntityAttributeValues;
    }

    public Enrollment getEnrollmentForProgram(String uid) {
        for (Enrollment enrollment : teiEnrollments)
            if (enrollment.program().equals(uid))
                return enrollment;
        return null;
    }

    public String getTrackedEntityAttributeValueBySortOrder(int sortOrder) {
        if (sortOrder <= trackedEntityAttributeValues.size()) {
            return trackedEntityAttributeValues.get(sortOrder - 1).value();
        }
        return "";
    }

    public void setProgramsObjectStyles(Map<String, ObjectStyle> stringObjectStyleMap) {
        this.programObjectStyles = stringObjectStyleMap;
    }

    public ObjectStyle getObjectStyleForProgram(String programUid) {
        if (programObjectStyles.containsKey(programUid))
            return programObjectStyles.get(programUid);
        else return null;
    }
}