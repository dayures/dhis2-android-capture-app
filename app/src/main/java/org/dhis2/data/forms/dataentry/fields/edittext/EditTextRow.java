package org.dhis2.data.forms.dataentry.fields.edittext;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ObservableBoolean;

import org.dhis2.R;
import org.dhis2.data.forms.dataentry.fields.Row;
import org.dhis2.data.forms.dataentry.fields.RowAction;
import org.dhis2.databinding.FormEditTextCustomBinding;

import io.reactivex.processors.FlowableProcessor;

/**
 * QUADRAM. Created by frodriguez on 1/24/2018.
 */

public class EditTextRow implements Row<EditTextCustomHolder, EditTextModel> {

    @NonNull
    private final LayoutInflater inflater;
    @NonNull
    private final FlowableProcessor<RowAction> processor;
    private final boolean isBgTransparent;
    private final String renderType;
    private final ObservableBoolean isEditable;
    private boolean isSearchMode = false;

    //Search form constructor
    public EditTextRow(@NonNull LayoutInflater layoutInflater, @NonNull FlowableProcessor<RowAction> processor, boolean isBgTransparent) {
        this.inflater = layoutInflater;
        this.processor = processor;
        this.isBgTransparent = isBgTransparent;
        this.renderType = null;
        this.isEditable = new ObservableBoolean(true);
        this.isSearchMode = true;
    }

    //Data entryconstructor
    public EditTextRow(@NonNull LayoutInflater layoutInflater, @NonNull FlowableProcessor<RowAction> processor,
                       @NonNull FlowableProcessor<Integer> currentPosition,
                       boolean isBgTransparent, String renderType, ObservableBoolean isEditable) {
        this.inflater = layoutInflater;
        this.processor = processor;
        this.isBgTransparent = isBgTransparent;
        this.renderType = renderType;
        this.isEditable = isEditable;
    }

    @NonNull
    @Override
    public EditTextCustomHolder onCreate(@NonNull ViewGroup viewGroup) {
        FormEditTextCustomBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.form_edit_text_custom, viewGroup, false);
        binding.customEdittext.setIsBgTransparent(isBgTransparent);
        binding.customEdittext.setRenderType(renderType);
        return new EditTextCustomHolder(binding, processor, isSearchMode);
    }

    @Override
    public void onBind(@NonNull EditTextCustomHolder viewHolder, @NonNull EditTextModel viewModel) {
        viewHolder.update(viewModel);
    }

    @Override
    public void deAttach(@NonNull EditTextCustomHolder viewHolder) {
        viewHolder.dispose();
    }

}
