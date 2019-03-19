package org.dhis2.usescases.splash

import androidx.annotation.UiThread
import org.dhis2.usescases.general.AbstractActivityContracts

class SplashContracts {

    interface View : AbstractActivityContracts.View {

        fun renderFlag(resource:Int)
    }

    interface Presenter {
        fun destroy()

        fun init(view: View)

        @UiThread
        fun isUserLoggedIn()

        @UiThread
        fun navigateToLoginView()

        @UiThread
        fun navigateToHomeView()
    }
}