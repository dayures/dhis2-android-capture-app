package org.dhis2.usescases.settings

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.work.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.dhis2.R
import org.dhis2.data.metadata.MetadataRepository
import org.dhis2.data.service.SyncDataWorker
import org.dhis2.data.service.SyncMetadataWorker
import org.dhis2.data.tuples.Pair
import org.dhis2.utils.Constants
import org.dhis2.utils.Constants.*
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.constant.Constant
import org.hisp.dhis.android.core.maintenance.D2Error
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

const val TYPE_DELETE_DATA = 0
const val TYPE_RESET_APP = 1

class SettingsViewModel(val app: Application): AndroidViewModel(app) {

    lateinit var d2: D2
    private val prefs: SharedPreferences = app.getSharedPreferences(Constants.SHARE_PREFS, MODE_PRIVATE)
    val syncDataFrequency = ObservableInt(prefs.getInt("timeData", Constants.TIME_DAILY))
    val syncDataFrequencyString = ObservableField<String>(getStringTagByMinutes(syncDataFrequency.get()))
    val syncConfigFrequency = ObservableInt(prefs.getInt("timeMeta", Constants.TIME_DAILY))
    val syncConfigFrequencyString = ObservableField<String>(getStringTagByMinutes(syncConfigFrequency.get()))
    var config = ObservableField<ConfigData>()
    private val compositeDisposable = CompositeDisposable()
    val checkData: FlowableProcessor<Boolean> = PublishProcessor.create()
    lateinit var metadataRepository: MetadataRepository
    lateinit var requiereConfirm: (type: Int, callback: () -> Unit) -> Unit
    lateinit var goToIntent: (config: ConfigData) -> Unit
    lateinit var showLocalDataDeleted: (error: Boolean) -> Unit
    lateinit var goToLogin: () -> Unit

    init {
        compositeDisposable.add(
                checkData
                        .startWith(true)
                        .flatMap { _ ->
                            metadataRepository.downloadedData
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            setSyncData(it)
                        }, {
                            Timber.e(it)
                        })
        )
    }

    private fun setSyncData(it: Pair<Int, Int>?) {

    }

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
        requiereConfirm(TYPE_RESET_APP) {
            wipeDB()
        }
    }

    fun deleteData() {
       requiereConfirm(TYPE_DELETE_DATA) {
           deleteLocal()
       }
    }


    fun goTo(config: ConfigData) {
        goToIntent(config)
    }

    fun setDataFrequency(frequency: Int) {
        syncDataFrequency.set(frequency)
        syncDataFrequencyString.set(getStringTagByMinutes(syncDataFrequency.get()))
        prefs.edit().putInt(Constants.TIME_DATA, frequency).apply()
        if (frequency != TIME_MANUAL) {
            syncData(frequency, Constants.DATA)
        } else {
            cancelPendingWork(Constants.DATA)
        }
    }

    fun setConfigFrequency(frequency: Int) {
        syncConfigFrequency.set(frequency)
        syncConfigFrequencyString.set(getStringTagByMinutes(syncConfigFrequency.get()))
        prefs.edit().putInt(Constants.TIME_META, frequency).apply()
        if (frequency != TIME_MANUAL) {
            syncMeta(frequency, Constants.META)
        } else {
            cancelPendingWork(Constants.META)
        }
    }


    private fun cancelPendingWork(tag: String) {
        WorkManager.getInstance().cancelAllWorkByTag(tag)
    }


    private fun syncData(seconds: Int, scheduleTag: String) {
        WorkManager.getInstance().cancelAllWorkByTag(scheduleTag);
        val syncDataBuilder = PeriodicWorkRequest.Builder(SyncDataWorker::class.java, seconds.toLong(), TimeUnit.SECONDS)
        syncDataBuilder.addTag(scheduleTag);
        syncDataBuilder.setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build());
        val request = syncDataBuilder.build();
        WorkManager.getInstance().enqueueUniquePeriodicWork(scheduleTag, ExistingPeriodicWorkPolicy.REPLACE, request);
    }

    private fun syncMeta(seconds: Int, scheduleTag: String) {
        WorkManager.getInstance().cancelAllWorkByTag(scheduleTag)
        val syncDataBuilder = PeriodicWorkRequest.Builder(SyncMetadataWorker::class.java, seconds.toLong(), TimeUnit.SECONDS);
        syncDataBuilder.addTag(scheduleTag);
        syncDataBuilder.setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build());
        val request = syncDataBuilder.build();
        WorkManager.getInstance().enqueueUniquePeriodicWork(scheduleTag, ExistingPeriodicWorkPolicy.REPLACE, request);

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

    private fun deleteLocal() {
        var error = false;
        try {
            d2.wipeModule().wipeData();
        } catch (e: D2Error) {
            Timber.e(e);
            error = true;
        }

        showLocalDataDeleted(error);
    }

    private fun wipeDB() {
        try {
            WorkManager.getInstance().cancelAllWork()
            WorkManager.getInstance().pruneWork()
            d2.wipeModule().wipeEverything()
            // clearing cache data
            deleteDir(app.cacheDir)
            prefs.edit().clear().apply()
            goToLogin()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (aChildren: String in children) {
                val success = deleteDir(File(dir, aChildren))
                if (!success) {
                    return false
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile) {
            return dir.delete();
        } else {
            return false;
        }
    }

    fun resetSyncParameters() {
        val editor = prefs.edit()
        editor.putInt(Constants.EVENT_MAX, Constants.EVENT_MAX_DEFAULT)
        editor.putInt(Constants.TEI_MAX, Constants.TEI_MAX_DEFAULT)
        editor.putBoolean(Constants.LIMIT_BY_ORG_UNIT, false)
        editor.apply()
        checkData.onNext(true)
    }
}