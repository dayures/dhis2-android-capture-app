package org.dhis2.data.forms.dataentry.fields.age;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import org.dhis2.R;
import org.dhis2.data.forms.dataentry.fields.Row;
import org.dhis2.data.forms.dataentry.fields.RowAction;
import org.dhis2.databinding.FormAgeCustomBinding;

import io.reactivex.processors.FlowableProcessor;

/**
 * QUADRAM. Created by frodriguez on 20/03/2018.
 */

public class AgeRow implements Row<AgeHolder, AgeViewModel> {

    private final LayoutInflater inflater;
    private final boolean isBgTransparent;
    private boolean isSearchMode = false;
    private final FlowableProcessor<RowAction> processor;

    public AgeRow(LayoutInflater layoutInflater, FlowableProcessor<RowAction> processor, boolean isBgTransparent) {
        this.inflater = layoutInflater;
        this.isBgTransparent = isBgTransparent;
        this.processor = processor;
        this.isSearchMode = true;
    }

    public AgeRow(LayoutInflater layoutInflater, FlowableProcessor<RowAction> processor,
                  boolean isBgTransparent, String renderType) {
        this.inflater = layoutInflater;
        this.isBgTransparent = isBgTransparent;
        this.processor = processor;
    }

    @NonNull
    @Override
    public AgeHolder onCreate(@NonNull ViewGroup parent) {
        FormAgeCustomBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.form_age_custom, parent, false);
        binding.customAgeview.setIsBgTransparent(isBgTransparent);
        return new AgeHolder(binding, processor, isSearchMode);
    }

    @Override
    public void onBind(@NonNull AgeHolder viewHolder, @NonNull AgeViewModel viewModel) {
        viewHolder.update(viewModel);
    }

    @Override
    public void deAttach(@NonNull AgeHolder viewHolder) {
        viewHolder.dispose();
    }
}
