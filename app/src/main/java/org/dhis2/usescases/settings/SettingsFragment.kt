package org.dhis2.usescases.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import org.dhis2.App
import org.dhis2.R
import org.dhis2.data.user.UserComponent
import org.dhis2.extensions.viewModel
import org.dhis2.usescases.general.FragmentGlobalAbstract
import org.dhis2.databinding.FragmentSettingsBinding
import org.hisp.dhis.android.core.D2
import javax.inject.Inject

class SettingsFragment: FragmentGlobalAbstract() {


    lateinit var binding: FragmentSettingsBinding
    lateinit var viewModel: SettingsViewModel


    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)
        viewModel = viewModel {
            this.d2 =  (context!!.applicationContext as App).serverComponent()!!.userManager().d2

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = SettingsAdapter(viewModel)
    }
}