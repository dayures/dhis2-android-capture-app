package org.dhis2.utils;

import org.dhis2.databinding.OrgUnitMenuSelectorItemBinding;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * QUADRAM. Created by ppajuelo on 28/11/2018.
 */
class OrgUnitSelectorHolder extends RecyclerView.ViewHolder {

    private final OrgUnitMenuSelectorItemBinding binding;

    public OrgUnitSelectorHolder(@NonNull OrgUnitMenuSelectorItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bind(OrganisationUnit organisationUnitModel) {
        binding.setOrgUnit(organisationUnitModel);
    }
}
