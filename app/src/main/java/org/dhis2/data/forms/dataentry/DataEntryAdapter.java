package org.dhis2.data.forms.dataentry;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.dhis2.data.forms.dataentry.fields.FieldViewModel;
import org.dhis2.data.forms.dataentry.fields.Row;
import org.dhis2.data.forms.dataentry.fields.RowAction;
import org.dhis2.data.forms.dataentry.fields.age.AgeRow;
import org.dhis2.data.forms.dataentry.fields.age.AgeViewModel;
import org.dhis2.data.forms.dataentry.fields.coordinate.CoordinateRow;
import org.dhis2.data.forms.dataentry.fields.coordinate.CoordinateViewModel;
import org.dhis2.data.forms.dataentry.fields.datetime.DateTimeRow;
import org.dhis2.data.forms.dataentry.fields.datetime.DateTimeViewModel;
import org.dhis2.data.forms.dataentry.fields.edittext.EditTextModel;
import org.dhis2.data.forms.dataentry.fields.edittext.EditTextRow;
import org.dhis2.data.forms.dataentry.fields.file.FileRow;
import org.dhis2.data.forms.dataentry.fields.file.FileViewModel;
import org.dhis2.data.forms.dataentry.fields.image.ImageRow;
import org.dhis2.data.forms.dataentry.fields.image.ImageViewModel;
import org.dhis2.data.forms.dataentry.fields.orgUnit.OrgUnitRow;
import org.dhis2.data.forms.dataentry.fields.orgUnit.OrgUnitViewModel;
import org.dhis2.data.forms.dataentry.fields.radiobutton.RadioButtonRow;
import org.dhis2.data.forms.dataentry.fields.radiobutton.RadioButtonViewModel;
import org.dhis2.data.forms.dataentry.fields.spinner.SpinnerRow;
import org.dhis2.data.forms.dataentry.fields.spinner.SpinnerViewModel;
import org.dhis2.data.forms.dataentry.fields.unsupported.UnsupportedRow;
import org.dhis2.data.forms.dataentry.fields.unsupported.UnsupportedViewModel;
import org.dhis2.data.tuples.Trio;
import org.dhis2.usescases.searchTrackEntity.adapters.FormAdapter;
import org.hisp.dhis.android.core.common.ValueType;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import io.reactivex.Observable;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import timber.log.Timber;

public final class DataEntryAdapter extends Adapter {

    @NonNull
    private final List<FieldViewModel> viewModels;

    @NonNull
    private final FlowableProcessor<RowAction> processor;

    @NonNull
    private final FlowableProcessor<Integer> currentPosition;

    @NonNull
    private final ObservableField<String> imageSelector;

    @NonNull
    private final List<Row> rows;

    private final FlowableProcessor<Trio<String, String, Integer>> processorOptionSet;

    public DataEntryAdapter(@NonNull LayoutInflater layoutInflater,
                            @NonNull FragmentManager fragmentManager,
                            @NonNull DataEntryArguments dataEntryArguments,
                            @NonNull Observable<List<OrganisationUnit>> orgUnits,
                            ObservableBoolean isEditable) { //TODO: Add isEditable to all fields and test if can be changed on the fly
        setHasStableIds(true);
        rows = new ArrayList<>();
        viewModels = new ArrayList<>();
        processor = PublishProcessor.create();
        imageSelector = new ObservableField<>("");
        currentPosition = PublishProcessor.create();
        this.processorOptionSet = PublishProcessor.create();

        addRows(layoutInflater, fragmentManager, dataEntryArguments, orgUnits, isEditable);
    }

    private void addRows(@NonNull LayoutInflater layoutInflater,
                         @NonNull FragmentManager fragmentManager,
                         @NonNull DataEntryArguments dataEntryArguments,
                         @NonNull Observable<List<OrganisationUnit>> orgUnits,
                         ObservableBoolean isEditable) {

        rows.add(FormAdapter.EDITTEXT, new EditTextRow(layoutInflater, processor, true, dataEntryArguments.renderType(), isEditable));
        rows.add(FormAdapter.BUTTON, new FileRow(layoutInflater, processor, true));
        rows.add(FormAdapter.CHECKBOX, new RadioButtonRow(layoutInflater, processor, true));
        rows.add(FormAdapter.SPINNER, new SpinnerRow(layoutInflater, processor, processorOptionSet, true, dataEntryArguments.renderType()));
        rows.add(FormAdapter.COORDINATES, new CoordinateRow(layoutInflater, processor, true));
        rows.add(FormAdapter.TIME, new DateTimeRow(layoutInflater, processor, currentPosition, FormAdapter.TIME, true));
        rows.add(FormAdapter.DATE, new DateTimeRow(layoutInflater, processor, currentPosition, FormAdapter.DATE, true));
        rows.add(FormAdapter.DATETIME, new DateTimeRow(layoutInflater, processor, currentPosition, FormAdapter.DATETIME, true));
        rows.add(FormAdapter.AGEVIEW, new AgeRow(layoutInflater, processor, true));
        rows.add(FormAdapter.YES_NO, new RadioButtonRow(layoutInflater, processor, true));
        rows.add(FormAdapter.ORG_UNIT, new OrgUnitRow(fragmentManager, layoutInflater, processor, true, orgUnits, dataEntryArguments.renderType()));
        rows.add(FormAdapter.IMAGE, new ImageRow(layoutInflater, processor, dataEntryArguments.renderType()));
        rows.add(FormAdapter.UNSUPPORTED, new UnsupportedRow(layoutInflater));
    }

    public DataEntryAdapter(@NonNull LayoutInflater layoutInflater,
                            @NonNull FragmentManager fragmentManager,
                            @NonNull DataEntryArguments dataEntryArguments,
                            @NonNull Observable<List<OrganisationUnit>> orgUnits,
                            ObservableBoolean isEditable,
                            @NonNull FlowableProcessor<RowAction> processor,
                            @NonNull FlowableProcessor<Trio<String, String, Integer>> processorOptSet) { //TODO: Add isEditable to all fields and test if can be changed on the fly
        setHasStableIds(true);
        rows = new ArrayList<>();
        viewModels = new ArrayList<>();
        this.processor = processor;
        imageSelector = new ObservableField<>("");
        currentPosition = PublishProcessor.create();
        this.processorOptionSet = processorOptSet;

        addRows(layoutInflater, fragmentManager, dataEntryArguments, orgUnits, isEditable);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == FormAdapter.IMAGE)
            return ((ImageRow) rows.get(FormAdapter.IMAGE)).onCreate(parent, getItemCount(), imageSelector);
        else
            return rows.get(viewType).onCreate(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        rows.get(holder.getItemViewType()).onBind(holder,
                viewModels.get(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return viewModels.size();
    }

    private int getDateTimeItemViewType(DateTimeViewModel viewModel) {
        if (viewModel.valueType() == ValueType.DATE)
            return FormAdapter.DATE;
        if (viewModel.valueType() == ValueType.TIME)
            return FormAdapter.TIME;
        else
            return FormAdapter.DATETIME;
    }

    @Override
    public int getItemViewType(int position) {

        FieldViewModel viewModel = viewModels.get(position);
        if (viewModel instanceof EditTextModel) {
            return FormAdapter.EDITTEXT;
        } else if (viewModel instanceof RadioButtonViewModel) {
            return FormAdapter.CHECKBOX;
        } else if (viewModel instanceof SpinnerViewModel) {
            return FormAdapter.SPINNER;
        } else if (viewModel instanceof CoordinateViewModel) {
            return FormAdapter.COORDINATES;
        } else if (viewModel instanceof DateTimeViewModel) {
            return getDateTimeItemViewType((DateTimeViewModel) viewModel);
        } else if (viewModel instanceof AgeViewModel) {
            return FormAdapter.AGEVIEW;
        } else if (viewModel instanceof FileViewModel) {
            return FormAdapter.BUTTON;
        } else if (viewModel instanceof OrgUnitViewModel) {
            return FormAdapter.ORG_UNIT;
        } else if (viewModel instanceof ImageViewModel) {
            return FormAdapter.IMAGE;
        } else if (viewModel instanceof UnsupportedViewModel) {
            return FormAdapter.UNSUPPORTED;
        } else {
            throw new IllegalStateException("Unsupported view model type: "
                    + viewModel.getClass());
        }
    }

    @Override
    public long getItemId(int position) {
        return viewModels.get(position).uid().hashCode();
    }

    @NonNull
    public FlowableProcessor<RowAction> asFlowable() {
        return processor;
    }

    public FlowableProcessor<Trio<String, String, Integer>> asFlowableOption() {
        return processorOptionSet;
    }

    public void swap(@NonNull List<FieldViewModel> updates) {
        long currentTime = System.currentTimeMillis();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new DataEntryDiffCallback(viewModels, updates));

        viewModels.clear();
        viewModels.addAll(updates);

        diffResult.dispatchUpdatesTo(this);
        Timber.d("ADAPTER SWAP TOOK %s ms", System.currentTimeMillis() - currentTime);
    }

    public boolean mandatoryOk() {
        boolean isOk = true;
        for (FieldViewModel fieldViewModel : viewModels) {
            if (fieldViewModel.mandatory() && (fieldViewModel.value() == null || fieldViewModel.value().isEmpty()))
                isOk = false;
        }

        return isOk;
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        rows.get(holder.getItemViewType()).deAttach(holder);
    }

    public boolean hasError() {
        boolean hasError = false;
        for (FieldViewModel fieldViewModel : viewModels) {
            if (fieldViewModel.error() != null)
                hasError = true;
        }

        return hasError;
    }
}
