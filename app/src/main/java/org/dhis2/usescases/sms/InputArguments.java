package org.dhis2.usescases.sms;

public class InputArguments {

    private final String eventId;
    private final String enrollmentId;
    private final String teiId;

    public InputArguments(String eventId, String enrollmentId, String teiId) {
        this.eventId = eventId;
        this.enrollmentId = enrollmentId;
        this.teiId = teiId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getTeiId() {
        return teiId;
    }

    public Type getSubmissionType() {
        if (enrollmentId != null && teiId != null && enrollmentId.length() > 0 && teiId.length() > 0) {
            return Type.ENROLLMENT;
        } else if (eventId != null && teiId != null && eventId.length() > 0 && teiId.length() > 0) {
            return Type.TRACKER_EVENT;
        } else if (eventId != null && eventId.length() > 0) {
            return Type.SIMPLE_EVENT;
        }
        return Type.WRONG_PARAMS;
    }

    public boolean isSameSubmission(InputArguments second) {
        if (second == null || getSubmissionType() != second.getSubmissionType()) {
            return false;
        }
        switch (getSubmissionType()) {
            case ENROLLMENT:
                return enrollmentId.equals(second.enrollmentId) && teiId.equals(second.teiId);
            case TRACKER_EVENT:
                return eventId.equals(second.eventId) && teiId.equals(second.teiId);
            case SIMPLE_EVENT:
                return eventId.equals(second.eventId);
            case WRONG_PARAMS:
                return true;
        }
        return false;
    }

    public enum Type {
        ENROLLMENT, TRACKER_EVENT, SIMPLE_EVENT, WRONG_PARAMS
    }
}
