package org.dhis2.usescases.settings

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import org.dhis2.App
import org.dhis2.R
import org.dhis2.data.base.BaseFragment
import org.dhis2.databinding.FragmentSettingsBinding
import org.dhis2.extensions.viewModel
import org.dhis2.usescases.login.LoginActivity
import org.dhis2.usescases.reservedValue.ReservedValueActivity
import org.dhis2.usescases.syncManager.ErrorDialog
import org.dhis2.utils.SyncUtils
import org.hisp.dhis.android.core.maintenance.D2Error
import javax.inject.Inject

class SettingsFragment: BaseFragment() {


    lateinit var binding: FragmentSettingsBinding

    @Inject
    lateinit var viewModel: SettingsViewModel


    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context.applicationContext as App).userComponent()
                ?.plus(SettingsModule())?.inject(this)
    }


    val syncReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action != null && intent.action == "action_sync") {
                if (!(SyncUtils.isSyncRunning() && abstractActivity.progressBar.visibility == View.VISIBLE)) {
                    binding.recycler.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(abstractActivity.applicationContext).registerReceiver(syncReceiver,  IntentFilter("action_sync"))

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        viewModel.goToIntent = ::goToIntentAction
        viewModel.requiereConfirm = ::requireConfirmAction
        viewModel.goToLogin = ::goToLoginAction
        viewModel.showErroDialog = ::showErrorDialogAction
        viewModel.showLocalDataDeleted = ::showLocalDataDeletedAction
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
                .setPositiveButton(getString(R.string.wipe_data_ok)) { dialog, witch ->
                    callback()
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.wipe_data_no)) { dialog, witch ->
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

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(abstractActivity.applicationContext).unregisterReceiver(syncReceiver)
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
}