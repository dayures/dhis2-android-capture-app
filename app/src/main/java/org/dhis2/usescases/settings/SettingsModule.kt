package org.dhis2.usescases.settings

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import dagger.Module
import dagger.Provides
import org.dhis2.data.dagger.PerFragment
import org.dhis2.data.metadata.MetadataRepository
import org.hisp.dhis.android.core.D2

@Module
@PerFragment
class SettingsModule {

    @Provides
    @PerFragment
    internal fun provideFragment(fragment: SettingsFragment): Fragment {
        return fragment
    }

    @Provides
    @PerFragment
    internal fun provideViewModel(fragment: SettingsFragment,
                                  application: Application): ViewModel {
        val viewModel =  ViewModelProviders.of(fragment).get(SettingsViewModel::class.java)
        viewModel.app = application
        return viewModel
    }

}