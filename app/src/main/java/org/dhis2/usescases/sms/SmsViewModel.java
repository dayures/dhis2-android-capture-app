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

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class SmsViewModel extends ViewModel {
    private final D2 d2;
    private Type type;
    private String enrollmentId;
    private String eventId;
    private String teiId;
    private CompositeDisposable disposables;
    private SmsSubmitCase smsSender = null;
    private MutableLiveData<List<SendingStatus>> states;
    private ArrayList<SendingStatus> statesList;

    @Inject
    SmsViewModel(D2 d2) {
        this.d2 = d2;
        disposables = new CompositeDisposable();
        states = new MutableLiveData<>();
        statesList = new ArrayList<>();
    }

    void setTrackerEventData(String eventId, String teiId) {
        this.eventId = eventId;
        this.teiId = teiId;
        this.enrollmentId = null;
        type = Type.TRACKER_EVENT;
    }

    void setSimpleEventData(String eventId) {
        this.eventId = eventId;
        this.teiId = null;
        this.enrollmentId = null;
        type = Type.SIMPLE_EVENT;
    }

    void setEnrollmentData(String enrollmentId, String teiId) {
        this.eventId = null;
        this.teiId = teiId;
        this.enrollmentId = enrollmentId;
        type = Type.ENROLLMENT;
    }

    void sendSMS() {
        if (smsSender != null) return; // maybe activity rotated caused double call
        smsSender = d2.smsModule().smsSubmitCase();
        reportState(State.STARTED, 0, 0);
        Single<Integer> convertTask = chooseConvertTask();
        if (convertTask == null) return;

        disposables.add(convertTask.subscribeOn(Schedulers.newThread()
        ).observeOn(Schedulers.newThread()
        ).subscribeWith(new DisposableSingleObserver<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                reportState(State.CONVERTED, 0, count);
                reportState(State.WAITING_COUNT_CONFIRMATION, 0, count);
            }

            @Override
            public void onError(Throwable e) {
                reportError(e);
            }
        }));
    }

    private Single<Integer> chooseConvertTask() {
        switch (type) {
            case ENROLLMENT:
                if (enrollmentId != null && teiId != null)
                    return smsSender.convertEnrollment(enrollmentId, teiId);
                break;
            case TRACKER_EVENT:
                if (eventId != null && teiId != null) {
                    return smsSender.convertTrackerEvent(eventId, teiId);
                }
                break;
            case SIMPLE_EVENT:
                if (eventId != null) {
                    return smsSender.convertSimpleEvent(eventId);
                }
                break;
        }
        reportState(State.ITEM_NOT_READY, 0, 0);
        return null;
    }

    private void reportState(State state, int sent, int total) {
        statesList.add(new SendingStatus(state, null, sent, total));
        states.postValue(statesList);
    }

    private void reportError(Throwable throwable) {
        statesList.add(new SendingStatus(State.ERROR, throwable, 0, 0));
        states.postValue(statesList);
    }

    @Override
    protected void onCleared() {
        disposables.dispose();
    }

    LiveData<List<SendingStatus>> sendingState() {
        return states;
    }

    void acceptSMSCount(boolean acceptCount) {
        if (acceptCount) {
            executeSending();
        } else {
            reportState(State.COUNT_NOT_ACCEPTED, 0, 0);
        }
    }

    private void executeSending() {
        disposables.add(smsSender.send().subscribeOn(Schedulers.newThread()
        ).observeOn(Schedulers.newThread()
        ).subscribeWith(new DisposableObserver<SmsRepository.SmsSendingState>() {
            @Override
            public void onNext(SmsRepository.SmsSendingState state) {
                if (!isLastStateSame(state.getSent(), state.getTotal())) {
                    reportState(State.SENDING, state.getSent(), state.getTotal());
                }
            }

            @Override
            public void onError(Throwable e) {
                reportError(e);
            }

            @Override
            public void onComplete() {
                reportState(State.COMPLETED, 0, 0);
            }
        }));
    }

    private boolean isLastStateSame(int sent, int total) {
        if (statesList == null || statesList.size() == 0) return false;
        SendingStatus last = statesList.get(statesList.size() - 1);
        return last.state == State.SENDING && last.sent == sent && last.total == total;
    }

    public static class SendingStatus {
        public final State state;
        public final int sent;
        public final int total;
        public final Throwable error;

        public SendingStatus(State state, Throwable error, int sent, int total) {
            this.state = state;
            this.sent = sent;
            this.total = total;
            this.error = error;
        }
    }

    public enum State {
        STARTED, CONVERTED, ITEM_NOT_READY, WAITING_COUNT_CONFIRMATION,
        COUNT_NOT_ACCEPTED, SENDING, COMPLETED, ERROR
    }

    public enum Type {
        ENROLLMENT, TRACKER_EVENT, SIMPLE_EVENT
    }
}
