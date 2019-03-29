package org.dhis2.usescases.datasets.datasetInitial;

import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import com.google.android.material.textfield.TextInputEditText;

import org.dhis2.App;
import org.dhis2.R;
import org.dhis2.databinding.ActivityDatasetInitialBinding;
import org.dhis2.databinding.ItemCategoryComboBinding;
import org.dhis2.usescases.general.ActivityGlobalAbstract;
import org.dhis2.utils.Constants;
import org.dhis2.utils.DateUtils;
import org.dhis2.utils.custom_views.OrgUnitDialog;
import org.dhis2.utils.custom_views.PeriodDialog;
import org.hisp.dhis.android.core.category.Category;
import org.hisp.dhis.android.core.category.CategoryCombo;
import org.hisp.dhis.android.core.category.CategoryOption;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnit;
import org.hisp.dhis.android.core.period.PeriodType;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class DataSetInitialActivity extends ActivityGlobalAbstract implements DataSetInitialContract.DataSetDetailView {

    private ActivityDatasetInitialBinding binding;
    View selectedView;
    @Inject
    DataSetInitialContract.DataSetDetailPresenter presenter;

    private HashMap<String, CategoryOption> selectedCatOptions;
    private OrganisationUnit selectedOrgUnit;
    private Date selectedPeriod;
    private String dataSetUid;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataSetUid = getIntent().getStringExtra(Constants.DATA_SET_UID);
        ((App) getApplicationContext()).userComponent().plus(new DataSetInitialModule(dataSetUid)).inject(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_dataset_initial);
        binding.setPresenter(presenter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.init(this);
    }

    @Override
    protected void onPause() {
        presenter.onDettach();
        super.onPause();
    }

    @Override
    public void setAccessDataWrite(Boolean canWrite) {
        // unused
    }

    @Override
    public void setData(DataSetInitialModel dataSetInitialModel) {
        binding.setDataSetModel(dataSetInitialModel);
        binding.catComboContainer.removeAllViews();
        selectedCatOptions = new HashMap<>();
        if (!dataSetInitialModel.categoryCombo().equals(CategoryCombo.DEFAULT_UID))
            for (Category categoryModel : dataSetInitialModel.categories()) {
                selectedCatOptions.put(categoryModel.uid(), null);
                ItemCategoryComboBinding categoryComboBinding = ItemCategoryComboBinding.inflate(getLayoutInflater(), binding.catComboContainer, false);
                categoryComboBinding.inputLayout.setHint(categoryModel.displayName());
                categoryComboBinding.inputEditText.setOnClickListener(view -> {
                    selectedView = view;
                    presenter.onCatOptionClick(categoryModel.uid());
                });
                binding.catComboContainer.addView(categoryComboBinding.getRoot());
            }
        checkActionVisivbility();
    }

    /**
     * When changing orgUnit, date must be cleared
     */
    @Override
    public void showOrgUnitDialog(List<OrganisationUnit> data) {
        OrgUnitDialog orgUnitDialog = OrgUnitDialog.getInstance().setMultiSelection(false);
        orgUnitDialog.setOrgUnits(data);
        orgUnitDialog.setTitle(getString(R.string.org_unit))
                .setPossitiveListener(v -> {
                    if (orgUnitDialog.getSelectedOrgUnit() != null && !orgUnitDialog.getSelectedOrgUnit().isEmpty()) {
                        selectedOrgUnit = orgUnitDialog.getSelectedOrgUnitModel();
                        if(selectedOrgUnit == null)
                            orgUnitDialog.dismiss();
                        binding.dataSetOrgUnitEditText.setText(selectedOrgUnit.displayName());
                        binding.dataSetPeriodEditText.setText("");
                    }
                    checkActionVisivbility();
                    orgUnitDialog.dismiss();
                })
                .setNegativeListener(v -> orgUnitDialog.dismiss());
        if (!orgUnitDialog.isAdded()) {
            orgUnitDialog.show(getSupportFragmentManager(), OrgUnitDialog.class.getSimpleName());
        }
    }

    @Override
    public void showPeriodSelector(PeriodType periodType) {
        new PeriodDialog()
                .setPeriod(periodType)
//                .setMinDate() TODO: Depends on dataSet expiration settings and orgUnit Opening date
//                .setMaxDate() TODO: Depends on dataSet open Future settings. Default: TODAY
                .setMaxDate(DateUtils.getInstance().getCalendar().getTime())
                .setPossitiveListener(selectedDate -> {
                    this.selectedPeriod = selectedDate;
                    binding.dataSetPeriodEditText.setText(DateUtils.getInstance().getPeriodUIString(periodType, selectedDate, Locale.getDefault()));
                    checkActionVisivbility();
                })
                .show(getSupportFragmentManager(), PeriodDialog.class.getSimpleName());
    }

    @Override
    public void showCatComboSelector(String catOptionUid, List<CategoryOption> data) {
        PopupMenu menu = new PopupMenu(this, selectedView, Gravity.BOTTOM);
//        menu.getMenu().add(Menu.NONE, Menu.NONE, 0, viewModel.label()); Don't show label
        for (CategoryOption optionModel : data)
            menu.getMenu().add(Menu.NONE, Menu.NONE, data.indexOf(optionModel), optionModel.displayName());

        menu.setOnDismissListener(menu1 -> selectedView = null);
        menu.setOnMenuItemClickListener(item -> {
            if (selectedCatOptions == null)
                selectedCatOptions = new HashMap<>();
            selectedCatOptions.put(catOptionUid, data.get(item.getOrder()));
            ((TextInputEditText) selectedView).setText(data.get(item.getOrder()).displayName());
            checkActionVisivbility();
            return false;
        });
        menu.show();
    }

    @Override
    public String getDataSetUid() {
        return dataSetUid;
    }

    @Override
    public String getSelectedOrgUnit() {
        return selectedOrgUnit.uid();
    }

    @Override
    public Date getSelectedPeriod() {
        return selectedPeriod;
    }

    @Override
    public String getSelectedCatOptions() {
        StringBuilder catComb = new StringBuilder("");
        for (int i = 0; i < selectedCatOptions.keySet().size(); i++) {
            CategoryOption catOpt = selectedCatOptions.get(selectedCatOptions.keySet().toArray()[i]);
            catComb.append(catOpt.code());
            if (i < selectedCatOptions.values().size() - 1)
                catComb.append(", ");
        }
        return catComb.toString();
    }

    @Override
    public String getPeriodType() {
        return binding.getDataSetModel().periodType().name();
    }

    private void checkActionVisivbility() {
        boolean visible = true;
        if (selectedOrgUnit == null)
            visible = false;
        if (selectedPeriod == null)
            visible = false;
        for (Map.Entry<String, CategoryOption> entry : selectedCatOptions.entrySet()) {
            if (entry.getValue() == null)
                visible = false;
        }

        binding.actionButton.setVisibility(visible ? View.VISIBLE : View.GONE);

    }
}
