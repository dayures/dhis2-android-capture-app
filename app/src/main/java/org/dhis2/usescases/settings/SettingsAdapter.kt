package org.dhis2.usescases.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import org.dhis2.R
import org.dhis2.databinding.*

class SettingsAdapter(val viewModel: SettingsViewModel): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val list: List<ConfigData> = listOf(ConfigData.SYNC_DATA, ConfigData.SYNC_CONFIGURATION, ConfigData.SYNC_PARAMETERS,
            ConfigData.RESERVED_VALUES, ConfigData.OPEN_SYNC_ERROR, ConfigData.DELETE_LOCAL, ConfigData.RESET_APP)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when(viewType) {
            DELETE_LOCAL -> {
                val binding: ItemSettingsDeleteLocalBinding =
                        DataBindingUtil.inflate(inflater, R.layout.item_settings_delete_local, parent, false)
                return DeleteLocalHolder(binding)
            }
            OPEN_SYNC_ERROR -> {
                val binding: ItemSettingsSyncErrorBinding =
                        DataBindingUtil.inflate(inflater, R.layout.item_settings_sync_error, parent, false)
                return SyncErrorHolder(binding)
            }
            RESERVED_VALUES -> {
                val binding: ItemSettingsReservedValuesBinding =
                        DataBindingUtil.inflate(inflater, R.layout.item_settings_reserved_values, parent, false)
                return ReservedValuesHolder(binding)
            }
            SYNC_CONFIGURATION -> {
                val binding: ItemSettingsSyncParametersBinding =
                        DataBindingUtil.inflate(inflater, R.layout.item_settings_sync_parameters, parent, false)
                return SyncParametersHolders(binding)
            }
            RESET_APP -> {
                val binding: ItemSettingsResetAppBinding =
                        DataBindingUtil.inflate(inflater, R.layout.item_settings_reset_app, parent, false)
                return ResetAppHolder(binding)
            }
            SYNC_DATA -> {
                val binding: ItemSettingsSyncDataBinding =
                        DataBindingUtil.inflate(inflater, R.layout.item_settings_sync_data, parent, false)
                return SyncDataHolder(binding)
            }
            SYNC_PARAMETERS -> {
                val binding: ItemSettingsSyncConfigurationBinding =
                        DataBindingUtil.inflate(inflater, R.layout.item_settings_sync_configuration, parent, false)
                return SyncConfigHolder(binding)
            }
            else -> {
                val binding: ItemSettingsSyncParametersBinding =
                        DataBindingUtil.inflate(inflater, R.layout.item_settings_sync_parameters, parent, false)
                return SyncParametersHolders(binding)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val config = list[position]
        when(holder) {
            is DeleteLocalHolder -> holder.bind(config)
            is SyncErrorHolder -> holder.bind(config)
            is ReservedValuesHolder -> holder.bind(config)
            is SyncConfigHolder -> holder.bind(config)
            is ResetAppHolder -> holder.bind(config)
            is SyncDataHolder -> holder.bind(config)
            is SyncParametersHolders -> holder.bind(config)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(list[position]) {
            ConfigData.DELETE_LOCAL -> DELETE_LOCAL
            ConfigData.OPEN_SYNC_ERROR -> OPEN_SYNC_ERROR
            ConfigData.RESERVED_VALUES -> RESERVED_VALUES
            ConfigData.SYNC_CONFIGURATION -> SYNC_CONFIGURATION
            ConfigData.RESET_APP -> RESET_APP
            ConfigData.SYNC_DATA -> SYNC_DATA
            ConfigData.SYNC_PARAMETERS -> SYNC_PARAMETERS
        }
    }


    inner class DeleteLocalHolder(val binding: ItemSettingsDeleteLocalBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(config: ConfigData) {
            binding.let {
                it.config = config
                it.viewModel = viewModel
            }
        }
    }

    inner class SyncErrorHolder(val binding: ItemSettingsSyncErrorBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(config: ConfigData) {
            binding.let {
                it.config = config
                it.viewModel = viewModel
            }
        }
    }

    inner class ReservedValuesHolder(val binding: ItemSettingsReservedValuesBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(config: ConfigData) {
            binding.let {
                it.config = config
                it.viewModel = viewModel
            }
        }
    }

    inner class SyncConfigHolder(val binding: ItemSettingsSyncConfigurationBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(config: ConfigData) {
            binding.let {
                it.config = config
                it.viewModel = viewModel
            }
        }
    }

    inner class ResetAppHolder(val binding: ItemSettingsResetAppBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(config: ConfigData) {
            binding.let {
                it.config = config
                it.viewModel = viewModel
            }
        }
    }

    inner class SyncDataHolder(val binding: ItemSettingsSyncDataBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(config: ConfigData) {
            binding.let {
                it.config = config
                it.viewModel = viewModel
            }
        }
    }

    inner class SyncParametersHolders(val binding: ItemSettingsSyncParametersBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(config: ConfigData) {
            binding.let {
                it.config = config
                it.viewModel = viewModel
            }
        }
    }

}


const val DELETE_LOCAL = 0
const val OPEN_SYNC_ERROR = 1
const val RESERVED_VALUES = 2
const val SYNC_CONFIGURATION = 3
const val RESET_APP = 4
const val SYNC_DATA = 5
const val SYNC_PARAMETERS = 6

