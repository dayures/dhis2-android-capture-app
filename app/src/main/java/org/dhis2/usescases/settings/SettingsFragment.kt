package org.dhis2.usescases.settings

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.State.RUNNING
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.google.android.material.snackbar.Snackbar
import me.toptas.fancyshowcase.DismissListener
import me.toptas.fancyshowcase.FancyShowCaseView
import me.toptas.fancyshowcase.FocusShape
import org.dhis2.App
import org.dhis2.BuildConfig
import org.dhis2.R
import org.dhis2.data.base.BaseFragment
import org.dhis2.databinding.FragmentSettingsBinding
import org.dhis2.extensions.getDhisPreferences
import org.dhis2.extensions.setDhisPreferences
import org.dhis2.extensions.viewModel
import org.dhis2.usescases.login.LoginActivity
import org.dhis2.usescases.reservedValue.ReservedValueActivity
import org.dhis2.usescases.syncManager.ErrorDialog
import org.dhis2.utils.Constants
import org.dhis2.utils.HelpManager
import org.hisp.dhis.android.core.maintenance.D2Error

class SettingsFragment: BaseFragment() {


    lateinit var binding: FragmentSettingsBinding
    lateinit var viewModel: SettingsViewModel


    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as App).userComponent()
                ?.plus(SettingsModule())?.inject(this)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        viewModel = viewModel(viewModelFactory) {
            d2 = (context!!.applicationContext as App).serverComponent.userManager().d2
            goToIntent = ::goToIntentAction
            requiereConfirm = ::requireConfirmAction
            goToLogin = ::goToLoginAction
            showErroDialog = ::showErrorDialogAction
            showLocalDataDeleted = ::showLocalDataDeletedAction
            cacheDir = context!!.cacheDir
        }
        val observer = Observer<List<WorkStatus>> {
            if (it.isNotEmpty()) {
                when(it[0].state) {
                    RUNNING -> {
                        viewModel.isEnabledButton.set(false)
                    }
                    else -> {
                        viewModel.isEnabledButton.set(true)
                    }
                }
            } else {
                viewModel.isEnabledButton.set(false)
            }
        }
        WorkManager.getInstance().getStatusesByTagLiveData(Constants.DATA).observe(this, observer)
        WorkManager.getInstance().getStatusesByTagLiveData(Constants.META).observe(this, observer)
        return binding.root
    }

    private fun showErrorDialogAction(list: @ParameterName(name = "data") List<D2Error>) {
        ErrorDialog().setData(list).show(childFragmentManager.beginTransaction(), ErrorDialog.TAG)
    }

    private fun goToLoginAction() {
        startActivity(LoginActivity::class.java, null, true, true, null);
    }

    private fun requireConfirmAction(type: Int, callback: () -> Unit) {
        var alertMessage = 0
        var alertTitle = 0
        when (type) {
            TYPE_DELETE_DATA -> {
                alertMessage = R.string.delete_local_data_message
                alertTitle = R.string.delete_local_data
            }
            TYPE_RESET_APP -> {
                alertMessage = R.string.wipe_data_meesage
                alertTitle = R.string.wipe_data
            }
        }
        androidx.appcompat.app.AlertDialog.Builder(binding.root.context, R.style.MaterialDialog)
                .setTitle(getString(alertTitle))
                .setMessage(getString(alertMessage))
                .setPositiveButton(getString(R.string.wipe_data_ok)) { dialog, _ ->
                    callback()
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.wipe_data_no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
    }

    private fun goToIntentAction(configData: @ParameterName(name = "config") ConfigData) {
        if (configData == ConfigData.RESERVED_VALUES){
            startActivity(ReservedValueActivity::class.java, null, false, false, null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = SettingsAdapter(viewModel)
    }

    override fun onStop() {
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(123456)
        super.onStop()
    }

    private fun showLocalDataDeletedAction(error: Boolean) {
        if(!error) {
            viewModel.eventCurrentdata.set(0.toString())
            viewModel.teiCurrentData.set(0.toString())
        }
        val message = if(error) {
            R.string.delete_local_data_error
        } else {
            R.string.delete_local_data_done
        }
        val deleteDataSnack = Snackbar.make(binding.root,
        message,
        Snackbar.LENGTH_SHORT)
        deleteDataSnack.show()
    }

    override fun showTutorial(shacked: Boolean) {
        if (isAdded && abstractActivity != null && context != null) {
            Handler().postDelayed({
                val recycler = binding.recycler
                val tuto1 = FancyShowCaseView.Builder(abstractActivity)
                        .focusOn(recycler.findViewById(R.id.settings_sync_data))
                        .title(getString(R.string.tuto_settings_1))
                        .closeOnTouch(true)
                        .focusShape(FocusShape.ROUNDED_RECTANGLE)
                        .build()

                val tuto2 = FancyShowCaseView.Builder(abstractActivity)
                        .focusOn(recycler.findViewById(R.id.settings_sync_configuration))
                        .title(getString(R.string.tuto_settings_2))
                        .focusShape(FocusShape.ROUNDED_RECTANGLE)
                        .closeOnTouch(true)
                        .build()

                val tuto3 = FancyShowCaseView.Builder(abstractActivity)
                        .focusOn(recycler.findViewById(R.id.settings_sync_parameters))
                        .title(getString(R.string.tuto_settings_3))
                        .focusShape(FocusShape.ROUNDED_RECTANGLE)
                        .titleGravity(Gravity.TOP)
                        .closeOnTouch(true)
                        .build()

                val tuto4 = FancyShowCaseView.Builder(abstractActivity)
                        .focusOn(recycler.findViewById(R.id.settings_reserved_values))
                        .title(getString(R.string.tuto_settings_reserved))
                        .focusShape(FocusShape.ROUNDED_RECTANGLE)
                        .closeOnTouch(true)
                        .titleGravity(Gravity.TOP)
                        .build()

                val tuto5 = FancyShowCaseView.Builder(abstractActivity)
                        .focusOn(recycler.findViewById(R.id.settings_sync_error))
                        .title(getString(R.string.tuto_settings_errors))
                        .focusShape(FocusShape.ROUNDED_RECTANGLE)
                        .closeOnTouch(true)
                        .titleGravity(Gravity.TOP)
                        .build()

                val tuto6 = FancyShowCaseView.Builder(abstractActivity)
                        .focusOn(recycler.findViewById(R.id.settings_delete_local))
                        .title(getString(R.string.tuto_settings_reset))
                        .focusShape(FocusShape.ROUNDED_RECTANGLE)
                        .closeOnTouch(true)
                        .build()

                val tuto7 = FancyShowCaseView.Builder(abstractActivity)
                        .focusOn(recycler.findViewById(R.id.settings_reset_app))
                        .title(getString(R.string.tuto_settings_4))
                        .closeOnTouch(true)
                        .focusShape(FocusShape.ROUNDED_RECTANGLE)
                        .build()


                val steps = arrayListOf<FancyShowCaseView>()
                steps.add(tuto1);
                steps.add(tuto2);
                steps.add(tuto3);
                steps.add(tuto4);
                steps.add(tuto5);
                steps.add(tuto6);
                steps.add(tuto7);
                HelpManager.getInstance().setScreenHelp(javaClass.name, steps);
                if (!abstractActivity.getDhisPreferences("TUTO_SETTINGS_SHOWN", false) && !BuildConfig.DEBUG) {
                    HelpManager.getInstance().showHelp()
                    abstractActivity.setDhisPreferences("TUTO_SETTINGS_SHOWN", true)
                }

            }, 500)
        }
    }

    override fun onResume() {
        super.onResume()
        showTutorial(!context!!.getDhisPreferences("TUTO_SETTINGS_SHOWN", false) && !BuildConfig.DEBUG)
    }
}