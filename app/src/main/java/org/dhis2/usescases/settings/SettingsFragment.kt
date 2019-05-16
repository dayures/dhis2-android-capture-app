package org.dhis2.usescases.settings

import android.app.AlertDialog
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
import kotlinx.android.synthetic.main.date_view.*
import org.dhis2.App
import org.dhis2.R
import org.dhis2.data.user.UserComponent
import org.dhis2.extensions.viewModel
import org.dhis2.usescases.general.FragmentGlobalAbstract
import org.dhis2.databinding.FragmentSettingsBinding
import org.dhis2.usescases.login.LoginActivity
import org.dhis2.usescases.reservedValue.ReservedValueActivity
import org.dhis2.utils.OnDialogClickListener
import org.dhis2.utils.SyncUtils
import org.hisp.dhis.android.core.D2
import org.jetbrains.anko.noButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.yesButton
import javax.inject.Inject

class SettingsFragment: FragmentGlobalAbstract() {


    lateinit var binding: FragmentSettingsBinding
    lateinit var viewModel: SettingsViewModel


    override fun onAttach(context: Context) {
        super.onAttach(context)
    }


    val syncReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action != null && intent.action == "action_sync") {
                if (!(SyncUtils.isSyncRunning() && abstractActivity.progressBar.visibility == View.VISIBLE)) {
                    binding.recycler.adapter?.notifyDataSetChanged()
                    //presenter.checkData();
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
        viewModel = viewModel {
            this.d2 =  (context!!.applicationContext as App).serverComponent()!!.userManager().d2
            goToIntent = ::goToIntentAction
            requiereConfirm = ::requireConfirmAction
            goToLogin = ::goToLoginAction
        }
        return binding.root
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
}