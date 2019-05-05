package org.dhis2.usescases.sms;

import org.dhis2.data.dagger.PerActivity;
import org.dhis2.usescases.general.ViewModelFactory;

import dagger.Subcomponent;

@PerActivity
@Subcomponent(modules = SmsModule.class)
public interface SmsComponent {

    ViewModelFactory<SmsViewModel> smsViewModelFactory();

    void inject(SmsSubmitActivity activity);
}
