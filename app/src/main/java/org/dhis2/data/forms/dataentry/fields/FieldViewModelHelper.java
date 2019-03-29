package org.dhis2.data.forms.dataentry.fields;

import android.database.Cursor;

import com.squareup.sqlbrite2.BriteDatabase;

import org.hisp.dhis.android.core.common.ValueType;

import androidx.annotation.Nullable;
import timber.log.Timber;

import static android.text.TextUtils.isEmpty;

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
    private String formLabel;
    private String description;

    @SuppressWarnings("squid:S00107")
    public FieldViewModelHelper(String uid, String label, ValueType valueType, boolean mandatory, String optionSetUid, String dataValue,
                                String optionCodeName, String section, Boolean allowFutureDates, String formLabel, String description) {
        this.uid = uid;
        this.label = label;
        this.valueType = valueType;
        this.mandatory = mandatory;
        this.optionSetUid = optionSetUid;
        this.dataValue = dataValue;
        this.optionCodeName = optionCodeName;
        this.section = section;
        this.allowFutureDates = allowFutureDates;
        this.formLabel = formLabel;
        this.description = description;
    }

    public static FieldViewModelHelper createFromCursor(Cursor cursor) {
        FieldViewModelHelper fieldViewModelHelper = new FieldViewModelHelper(
                cursor.getString(0),
                cursor.getString(1),
                ValueType.valueOf(cursor.getString(2)),
                cursor.getInt(3) == 1,
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(6),
                cursor.getString(7),
                cursor.getInt(8) == 1,
                cursor.getString(10),
                cursor.getString(11));

        if (!isEmpty(fieldViewModelHelper.getOptionCodeName())) {
            fieldViewModelHelper.setDataValue(fieldViewModelHelper.getOptionCodeName());
        }

        return fieldViewModelHelper;
    }

    public static int getOptionCount(BriteDatabase briteDatabase, @Nullable String optionSetUid) {
        int optionCount = 0;
        try (Cursor countCursor = briteDatabase.query("SELECT COUNT (uid) FROM Option WHERE optionSet = ?",
                optionSetUid == null ? "" : optionSetUid)) {
            if (countCursor != null && countCursor.moveToFirst())
                optionCount = countCursor.getInt(0);

        } catch (Exception e) {
            Timber.e(e);
        }
        return optionCount;
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

    public String getFormLabel() {
        return formLabel;
    }

    public String getDescription() {
        return description;
    }
}
