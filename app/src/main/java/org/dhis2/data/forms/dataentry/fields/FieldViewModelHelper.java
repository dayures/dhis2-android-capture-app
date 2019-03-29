package org.dhis2.data.forms.dataentry.fields;

import android.database.Cursor;

import org.hisp.dhis.android.core.common.ValueType;

public class FieldViewModelHelper {

    private String uid;
    private String label;
    private ValueType valueType;
    private boolean mandatory;
    private String optionSetUid;
    private String dataValue;
    private String optionCodeName;
    private String section;
    private Boolean allowFutureDates;

    @SuppressWarnings("squid:S00107")
    public FieldViewModelHelper(String uid, String label, ValueType valueType, boolean mandatory, String optionSetUid, String dataValue,
                                String optionCodeName, String section, Boolean allowFutureDates) {
        this.uid = uid;
        this.label = label;
        this.valueType = valueType;
        this.mandatory = mandatory;
        this.optionSetUid = optionSetUid;
        this.dataValue = dataValue;
        this.optionCodeName = optionCodeName;
        this.section = section;
        this.allowFutureDates = allowFutureDates;
    }

    public static FieldViewModelHelper createFromCursor(Cursor cursor) {
        return new FieldViewModelHelper(
                cursor.getString(0),
                cursor.getString(1),
                ValueType.valueOf(cursor.getString(2)),
                cursor.getInt(3) == 1,
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(6),
                cursor.getString(7),
                cursor.getInt(8) == 1);
    }

    public String getUid() {
        return uid;
    }

    public String getLabel() {
        return label;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getOptionSetUid() {
        return optionSetUid;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public String getOptionCodeName() {
        return optionCodeName;
    }

    public String getSection() {
        return section;
    }

    public Boolean getAllowFutureDates() {
        return allowFutureDates;
    }
}
