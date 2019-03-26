package org.dhis2.data.forms.dataentry.fields.radiobutton;

import android.view.View;
import android.widget.RadioGroup;

import org.dhis2.R;
import org.dhis2.data.forms.dataentry.fields.FormViewHolder;
import org.dhis2.data.forms.dataentry.fields.RowAction;
import org.dhis2.databinding.FormYesNoBinding;

import io.reactivex.processors.FlowableProcessor;


/**
 * QUADRAM. Created by frodriguez on 18/01/2018.
 */

public class RadioButtonHolder extends FormViewHolder {

    private final FlowableProcessor<RowAction> processor;

    private final RadioGroup radioGroup;
    private final FormYesNoBinding yesNoBinding;
    private final View clearButton;

    @SuppressWarnings("squid:S1450")
    private RadioButtonViewModel viewModel;

    RadioButtonHolder(FormYesNoBinding binding, FlowableProcessor<RowAction> processor) {
        super(binding);
        radioGroup = binding.customYesNo.getRadioGroup();
        clearButton = binding.customYesNo.getClearButton();
        this.yesNoBinding = binding;
        this.processor = processor;
    }


    public void update(RadioButtonViewModel checkBoxViewModel) {


        this.viewModel = checkBoxViewModel;

        radioGroup.setOnCheckedChangeListener(null);
        descriptionText = viewModel.description();
        yesNoBinding.setDescription(descriptionText);
        label = new StringBuilder(checkBoxViewModel.label());
        yesNoBinding.customYesNo.setValueType(checkBoxViewModel.valueType());
        if (checkBoxViewModel.mandatory())
            label.append("*");
        yesNoBinding.setLabel(label.toString());
        yesNoBinding.setValueType(checkBoxViewModel.valueType());
        if (checkBoxViewModel.value() != null && Boolean.valueOf(checkBoxViewModel.value()))
            yesNoBinding.customYesNo.getRadioGroup().check(R.id.yes);
        else if (checkBoxViewModel.value() != null)
            yesNoBinding.customYesNo.getRadioGroup().check(R.id.no);
        else
            yesNoBinding.customYesNo.getRadioGroup().clearCheck();

        if (checkBoxViewModel.warning() != null) {
            yesNoBinding.warningError.setVisibility(View.VISIBLE);
            yesNoBinding.warningError.setText(checkBoxViewModel.warning());
        } else if (checkBoxViewModel.error() != null) {
            yesNoBinding.warningError.setVisibility(View.VISIBLE);
            yesNoBinding.warningError.setText(checkBoxViewModel.error());
        } else {
            yesNoBinding.warningError.setVisibility(View.GONE);
            yesNoBinding.warningError.setText(null);
        }

        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(checkBoxViewModel.editable());
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RowAction rowAction;
            switch (checkedId) {
                case R.id.yes:
                    viewModel = (RadioButtonViewModel) checkBoxViewModel.withValue(String.valueOf(true));
                    rowAction = RowAction.create(checkBoxViewModel.uid(), String.valueOf(true));
                    break;
                case R.id.no:
                    viewModel = (RadioButtonViewModel) checkBoxViewModel.withValue(String.valueOf(false));
                    rowAction = RowAction.create(checkBoxViewModel.uid(), String.valueOf(false));
                    break;
                default:
                    viewModel = (RadioButtonViewModel) checkBoxViewModel.withValue(null);
                    rowAction = RowAction.create(checkBoxViewModel.uid(), null);
                    break;
            }
            processor.onNext(rowAction);
        });

        clearButton.setOnClickListener(view -> {
            if (checkBoxViewModel.editable().booleanValue()) {
                radioGroup.clearCheck();
                processor.onNext(RowAction.create(checkBoxViewModel.uid(), null));
            }
        });


    }

    public void dispose() {
        // unused
    }
}
