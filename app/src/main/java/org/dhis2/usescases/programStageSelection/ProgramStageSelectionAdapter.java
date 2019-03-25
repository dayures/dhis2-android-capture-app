package org.dhis2.usescases.programStageSelection;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.dhis2.R;
import org.dhis2.data.tuples.Pair;
import org.dhis2.databinding.ItemProgramStageBinding;
import org.hisp.dhis.android.core.common.ObjectStyle;
import org.hisp.dhis.android.core.program.ProgramStage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by Cristian on 13/02/2018.
 */

public class ProgramStageSelectionAdapter extends RecyclerView.Adapter<ProgramStageSelectionViewHolder> {

    private ProgramStageSelectionContract.Presenter presenter;
    private List<Pair<ProgramStage, ObjectStyle>> programStageModels;

    ProgramStageSelectionAdapter(@NonNull ProgramStageSelectionContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void setProgramStageModels(List<Pair<ProgramStage, ObjectStyle>> programStageModels) {
        this.programStageModels = programStageModels;
    }

    @NotNull
    @Override
    public ProgramStageSelectionViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemProgramStageBinding binding = DataBindingUtil.inflate(inflater, R.layout.item_program_stage, parent, false);
        return new ProgramStageSelectionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NotNull ProgramStageSelectionViewHolder holder, int position) {
        ProgramStage programStageModel = programStageModels.get(position).val0();
        ObjectStyle objectStyleModel = programStageModels.get(position).val1();
        holder.bind(presenter, programStageModel, objectStyleModel);
    }

    @Override
    public int getItemCount() {
        return programStageModels != null ? programStageModels.size() : 0;
    }
}
