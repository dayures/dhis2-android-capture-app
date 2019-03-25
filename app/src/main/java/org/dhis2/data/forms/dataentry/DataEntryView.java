package org.dhis2.data.forms.dataentry;

import org.dhis2.data.forms.dataentry.fields.FieldViewModel;
import org.dhis2.data.forms.dataentry.fields.RowAction;
import org.dhis2.data.tuples.Trio;
import org.hisp.dhis.android.core.option.Option;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;

interface DataEntryView {

    @NonNull
    Flowable<RowAction> rowActions();

    @NonNull
    Consumer<List<FieldViewModel>> showFields();

    void removeSection(String sectionUid);

    void messageOnComplete(String message, boolean canComplete);

    Flowable<Trio<String, String, Integer>> optionSetActions();

    void setListOptions(List<Option> options);

    void showMessage(int messageId);
}
