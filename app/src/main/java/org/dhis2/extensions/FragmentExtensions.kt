package org.dhis2.extensions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import org.dhis2.usescases.general.FragmentGlobalAbstract

inline fun <reified T : ViewModel> Fragment.viewModel(body: T.() -> Unit): T {
    val vm = ViewModelProviders.of(this)[T::class.java]
    vm.body()
    return vm
}

inline fun <reified T : ViewModel> FragmentActivity.viewModel(body: T.() -> Unit): T {
    val vm = ViewModelProviders.of(this)[T::class.java]
    vm.body()
    return vm
}

inline fun <reified T : ViewModel> FragmentGlobalAbstract.viewModel(body: T.() -> Unit): T {
    val vm = ViewModelProviders.of(this)[T::class.java]
    vm.body()
    return vm
}