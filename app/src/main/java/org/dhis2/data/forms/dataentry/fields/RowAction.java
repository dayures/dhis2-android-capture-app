package org.dhis2.data.forms.dataentry.fields;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RowAction {

    @NonNull
    public abstract String id();

    @Nullable
    public abstract String value();

    @NonNull
    public abstract Boolean requiresExactMatch();

    @Nullable
    public abstract String optionCode();
    @Nullable
    public abstract String optionName();
    @NonNull
    public static RowAction create(@NonNull String id, @Nullable String value) {
        return new AutoValue_RowAction(id, value, false, null,null);
    }

    @NonNull
    public static RowAction create(@NonNull String id, @Nullable String value, @NonNull Boolean requieresExactMatch) {
        return new AutoValue_RowAction(id, value, requieresExactMatch, null,null);
    }

    @NonNull
    public static RowAction create(@NonNull String id, @Nullable String value, @NonNull Boolean requieresExactMatch, @NonNull String code, @NonNull String name) {
        return new AutoValue_RowAction(id, value, requieresExactMatch, code,name);
    }
}
