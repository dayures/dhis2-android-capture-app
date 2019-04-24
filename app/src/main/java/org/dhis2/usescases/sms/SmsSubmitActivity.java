package org.dhis2.usescases.sms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.dhis2.App;
import org.dhis2.R;
import org.dhis2.usescases.general.ActivityGlobalAbstract;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.sms.domain.interactor.SmsSubmitCase;
import org.hisp.dhis.android.core.sms.domain.repository.SmsRepository;
import org.hisp.dhis.android.core.sms.domain.repository.WebApiRepository;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;

public class SmsSubmitActivity extends ActivityGlobalAbstract {
    private static final int SMS_PERMISSIONS_REQ_ID = 102;
    private CompositeDisposable disposables;
    private String eventId;
    private String teiId;
    @Inject
    D2 d2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);
        ((App) getApplicationContext()).userComponent().inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        disposables = new CompositeDisposable();
    }

    @Override
    protected void onStop() {
        disposables.dispose();
        super.onStop();
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private WebApiRepository.GetMetadataIdsConfig getMetadataConfig() {
        WebApiRepository.GetMetadataIdsConfig config = new WebApiRepository.GetMetadataIdsConfig();
        config.categoryOptionCombos = true;
        config.dataElements = true;
        config.organisationUnits = true;
        config.programs = true;
        config.trackedEntityAttributes = true;
        config.trackedEntityTypes = true;
        config.users = true;
        return config;
    }

    public void sendSMS() {
        String[] smsPermissions = new String[]{Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS};
        if (!hasPermissions(smsPermissions)) {
            getPermissions(SMS_PERMISSIONS_REQ_ID, smsPermissions);
            return;
        }
        SmsSubmitCase smsSender = d2.smsModule().smsSubmitCase();
        Disposable d = d2.smsModule(
        ).initCase().initSMSModule("+23279741472", getMetadataConfig()
        ).andThen(Single.fromCallable(() ->
                d2.trackedEntityModule().trackedEntityDataValues.byEvent().eq(eventId).get())
        ).map(values ->
                d2.eventModule().events.byUid().eq(eventId).one().get().toBuilder()
                        .trackedEntityInstance(teiId)
                        .trackedEntityDataValues(values)
                        .build()
        ).flatMapObservable(
                smsSender::submit
        ).subscribeWith(new DisposableObserver<SmsRepository.SmsSendingState>() {
            @Override
            public void onNext(SmsRepository.SmsSendingState state) {
                if (state.getState() == SmsRepository.State.WAITING_SMS_COUNT_ACCEPT) {
                    smsSender.acceptSMSCount(true);
                }
                Log.d("", "");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Log.d("", "");
            }

            @Override
            public void onComplete() {
                Log.d("", "");
            }
        });
    }
}
