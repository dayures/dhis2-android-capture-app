package org.dhis2.usescases.sms;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.sms.domain.interactor.SmsSubmitCase;
import org.hisp.dhis.android.core.sms.domain.repository.SmsRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SmsViewModel extends ViewModel {
    private String enrollmentId;
    private String eventId;
    private String teiId;
    private CompositeDisposable disposables;
    private SmsSubmitCase smsSender;
    private MutableLiveData<Throwable> errors;
    private MutableLiveData<List<SmsRepository.SmsSendingState>> states;
    private ArrayList<SmsRepository.SmsSendingState> statesList;
    private boolean sendInitiated;

    @Inject
    SmsViewModel(D2 d2) {
        disposables = new CompositeDisposable();
        smsSender = d2.smsModule().smsSubmitCase();
        errors = new MutableLiveData<>();
        states = new MutableLiveData<>();
        sendInitiated = false;
        statesList = new ArrayList<>();
    }

    void setEventData(String eventId, String teiId) {
        this.eventId = eventId;
        this.teiId = teiId;
    }

    void setEnrollmentData(String enrollmentId, String teiId) {
        this.enrollmentId = enrollmentId;
        this.teiId = teiId;
    }

    void sendSMS() {
        if (sendInitiated) return;
        sendInitiated = true;
        Observable<SmsRepository.SmsSendingState> sendingTask;
        if (enrollmentId != null && teiId != null) {
            sendingTask = smsSender.submitEnrollment(enrollmentId, teiId);
        } else if (eventId != null && teiId != null) {
            sendingTask = smsSender.submitEvent(eventId, teiId);
        } else {
            errors.postValue(new IllegalArgumentException());
            return;
        }

        disposables.add(sendingTask.subscribeOn(Schedulers.newThread()
        ).observeOn(Schedulers.newThread()
        ).subscribeWith(new DisposableObserver<SmsRepository.SmsSendingState>() {
            @Override
            public void onNext(SmsRepository.SmsSendingState smsSendingState) {
                statesList.add(smsSendingState);
                states.postValue(statesList);
            }

            @Override
            public void onError(Throwable e) {
                errors.postValue(e);
            }

            @Override
            public void onComplete() {
                Timber.d("Sms sending observable completed");
            }
        }));
    }

    @Override
    protected void onCleared() {
        disposables.dispose();
    }

    LiveData<Throwable> errors() {
        return errors;
    }

    LiveData<List<SmsRepository.SmsSendingState>> sendingState() {
        return states;
    }

    void acceptSMSCount(boolean acceptCount) {
        smsSender.acceptSMSCount(acceptCount);
    }
}
