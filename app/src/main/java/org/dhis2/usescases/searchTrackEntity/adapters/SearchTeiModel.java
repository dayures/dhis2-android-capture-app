package org.dhis2.usescases.searchTrackEntity.adapters;

import org.dhis2.data.tuples.Trio;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;

import java.util.ArrayList;
import java.util.List;

public class SearchTeiModel {


    private TrackedEntityInstance tei; //7
    private boolean hasOverdue; //6
    private boolean isOnline;//8

    private List<TrackedEntityAttributeValue> attributeValues; //3,4
    private List<Enrollment> enrollments;
    private List<Trio<String, String, String>> enrollmentsInfo;//2


    public SearchTeiModel(TrackedEntityInstance tei, List<TrackedEntityAttributeValue> attributeValues) {
        this.tei = tei;
        this.enrollments = new ArrayList<>();
        this.enrollmentsInfo = new ArrayList<>();

        this.attributeValues = new ArrayList<>();
        this.attributeValues.addAll(attributeValues);
        this.isOnline = true;
    }

    public TrackedEntityInstance getTei() {
        return tei;
    }

    public List<Enrollment> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }

    public void addEnrollment(Enrollment enrollmentModel) {
        this.enrollments.add(enrollmentModel);
    }

    public void addEnrollmentInfo(Trio<String, String, String> enrollmentInfo) {
        enrollmentsInfo.add(enrollmentInfo);
    }

    public boolean isHasOverdue() {
        return hasOverdue;
    }

    public void setHasOverdue(boolean hasOverdue) {
        this.hasOverdue = hasOverdue;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
        this.attributeValues.clear();
    }

    public List<TrackedEntityAttributeValue> getAttributeValues() {
        return attributeValues;
    }

    public void addAttributeValues(TrackedEntityAttributeValue attributeValues) {
        this.attributeValues.add(attributeValues);
    }

    public void resetEnrollments() {
        this.enrollments.clear();
        this.enrollmentsInfo.clear();
    }

    public List<Trio<String, String, String>> getEnrollmentInfo() {
        return enrollmentsInfo;
    }

    public void toLocalTei(TrackedEntityInstance localTei) {
        this.tei = localTei;
    }
}
