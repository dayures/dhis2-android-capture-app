package org.dhis2.usescases.teiDashboard.adapters;

import org.dhis2.BR;
import org.dhis2.databinding.ItemEventBinding;
import org.dhis2.usescases.teiDashboard.TeiDashboardContracts;
import org.dhis2.utils.DateUtils;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.program.Program;
import org.hisp.dhis.android.core.program.ProgramStage;

import java.util.Locale;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by ppajuelo on 29/11/2017.
 */

class EventViewHolder extends RecyclerView.ViewHolder {
    private ItemEventBinding binding;

    EventViewHolder(ItemEventBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(TeiDashboardContracts.Presenter presenter, Event eventModel, ProgramStage programStage,
                     Enrollment enrollment, Program program) {
        binding.setVariable(BR.event, eventModel);
        binding.setVariable(BR.stage, programStage);
        binding.setVariable(BR.enrollment, enrollment);
        binding.setVariable(BR.program, program);
        binding.executePendingBindings();

        String date = DateUtils.getInstance().getPeriodUIString(programStage.periodType(), eventModel.eventDate() != null ? eventModel.eventDate() : eventModel.dueDate(), Locale.getDefault());
        binding.eventDate.setText(date);

        itemView.setOnClickListener(view -> {
            if (eventModel.status() == EventStatus.SCHEDULE) {
                presenter.onScheduleSelected(eventModel.uid(), binding.sharedView);
            } else
                presenter.onEventSelected(eventModel.uid(), binding.sharedView);
        });
    }
}
