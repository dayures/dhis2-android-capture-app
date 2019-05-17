package org.dhis2.usescases.settings

import dagger.Subcomponent
import org.dhis2.data.dagger.PerFragment

@PerFragment
@Subcomponent(modules = [SettingsModule::class])
interface SettingsComponent {
    fun inject(fragment: SettingsFragment)
}
