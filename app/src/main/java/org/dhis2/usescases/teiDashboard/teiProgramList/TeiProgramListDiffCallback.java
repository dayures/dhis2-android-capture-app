package org.dhis2.usescases.teiDashboard.teiProgramList;

import org.hisp.dhis.android.core.enrollment.Enrollment;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

/**
 * Created by ppajuelo on 29/01/2018.
 */

class TeiProgramListDiffCallback extends DiffUtil.Callback {

    @NonNull
    private List<Enrollment> oldList;
    @NonNull
    private List<Enrollment> newList;

    TeiProgramListDiffCallback(@NonNull List<Enrollment> oldList, @NonNull List<Enrollment> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).uid()
                .equals(newList.get(newItemPosition).uid());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition)
                .equals(newList.get(newItemPosition));
    }
}
