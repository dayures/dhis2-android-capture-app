package org.dhis2.usescases.teiDashboard.adapters;

import org.dhis2.BR;
import org.dhis2.Bindings.Bindings;
import org.dhis2.databinding.ItemDashboardProgramBinding;
import org.dhis2.usescases.teiDashboard.DashboardProgramModel;
import org.dhis2.usescases.teiDashboard.TeiDashboardContracts;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.program.Program;

import androidx.recyclerview.widget.RecyclerView;

/**
 * QUADRAM. Created by ppajuelo on 29/11/2017.
 */

class DashboardProgramViewHolder extends RecyclerView.ViewHolder {
    private ItemDashboardProgramBinding binding;

    DashboardProgramViewHolder(ItemDashboardProgramBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(TeiDashboardContracts.TeiDashboardPresenter presenter, DashboardProgramModel dashboardProgramModel, int position) {
        Program programModel = dashboardProgramModel.getEnrollmentProgramModels().get(position);
        Enrollment enrollment = dashboardProgramModel.getEnrollmentForProgram(programModel.uid());
        binding.setVariable(BR.presenter, presenter);
        binding.setVariable(BR.program, programModel);

        Bindings.setObjectStyle(binding.programImage, binding.programImage, dashboardProgramModel.getObjectStyleForProgram(programModel.uid()));

        if (enrollment != null)
            binding.setVariable(BR.enrollment, enrollment);
        binding.executePendingBindings();

        itemView.setOnClickListener(v -> presenter.setProgram(dashboardProgramModel.getEnrollmentProgramModels().get(position)));
    }
}
