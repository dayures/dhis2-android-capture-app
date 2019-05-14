package org.dhis2.usescases.settings

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import org.dhis2.R
import org.dhis2.utils.Constants
import org.dhis2.utils.Constants.*
import org.hisp.dhis.android.core.D2

class SettingsViewModel(val app: Application): AndroidViewModel(app) {

    lateinit var d2: D2
    private val prefs: SharedPreferences = app.getSharedPreferences(Constants.SHARE_PREFS, MODE_PRIVATE)
    val syncDataFrequency = ObservableInt(prefs.getInt("timeData", Constants.TIME_DAILY))
    val syncDataFrequencyString = ObservableField<String>(getStringTagByMinutes(syncDataFrequency.get()))
    val syncConfigFrequency = ObservableInt(prefs.getInt("timeMeta", Constants.TIME_DAILY))
    val syncConfigFrequencyString = ObservableField<String>(getStringTagByMinutes(syncConfigFrequency.get()))
    var config = ObservableField<ConfigData>()

    fun getPreferences(key: String): String {
        return prefs.getString(key, "-") ?: "-"
    }

    fun show(confi: ConfigData) {
        if (confi == config.get())
            config.set(null)
        else
            config.set(confi)
    }

    fun resetApp() {
        Toast.makeText(app, "reset pressed", Toast.LENGTH_SHORT).show()
    }

    fun deleteData() {
        Toast.makeText(app, "delete pressed", Toast.LENGTH_SHORT).show()
    }

    fun goTo(config: ConfigData) {
        Toast.makeText(app, config.toString(), Toast.LENGTH_SHORT).show()
    }

    fun setDataFrequency(frequency: Int) {
        syncDataFrequency.set(frequency)
        syncDataFrequencyString.set(getStringTagByMinutes(syncDataFrequency.get()))
        prefs.edit().putInt("timeData", frequency).apply()
    }

    fun setConfigFrequency(frequency: Int) {
        syncConfigFrequency.set(frequency)
        syncConfigFrequencyString.set(getStringTagByMinutes(syncConfigFrequency.get()))
        prefs.edit().putInt("timeMeta", frequency).apply()
    }


    private fun getStringTagByMinutes(minutes: Int): String {
        return app.resources.getString(when(minutes) {
            TIME_MANUAL -> R.string.Manual
            TIME_15M -> R.string.TIME_15
            TIME_HOURLY -> R.string.EVERY_HOUR
            TIME_DAILY -> R.string.DAILY
            TIME_WEEKLY -> R.string.WEEKLY
            else ->  R.string.DAILY
        })
    }
}