package org.dhis2.usescases.error;

import android.os.Bundle;
import android.view.View;

import org.dhis2.R;
import org.dhis2.databinding.ActivityDhisErrorBinding;
import org.dhis2.usescases.general.ActivityGlobalAbstract;
import org.dhis2.usescases.jira.JiraFragment;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;

/**
 * QUADRAM. Created by ppajuelo on 17/04/2019.
 */
public class DhisCustomErrorActivity extends ActivityGlobalAbstract {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dhis_error);
        ActivityDhisErrorBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_dhis_error);

        CaocConfig config = CustomActivityOnCrash.getConfigFromIntent(getIntent());
        if (config == null) {
            finish();
            return;
        }

        if (config.getErrorDrawable() != null)
            binding.errorImage.setImageResource(config.getErrorDrawable());

        if (config.isShowRestartButton() && config.getRestartActivityClass() != null)
            binding.restartAppButton.setOnClickListener(view ->
                    CustomActivityOnCrash.restartApplication(DhisCustomErrorActivity.this, config));
        else
            binding.restartAppButton.setVisibility(View.GONE);

        binding.sendReportButton.setOnClickListener(view -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.jiraFragment, new JiraFragment()).commit();

//            CustomActivityOnCrash.restartApplication(DhisCustomErrorActivity.this, config);
        });

        binding.errorDetail.setText(CustomActivityOnCrash.getStackTraceFromIntent(getIntent()));

    }
}
