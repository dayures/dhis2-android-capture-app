package org.dhis2.usescases.sms;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.dhis2.App;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.sms.domain.interactor.SmsSubmitCase;
import org.hisp.dhis.android.core.sms.domain.repository.SmsRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class SmsSendingService extends Service {
    private final IBinder binder = new LocalBinder();
    @Inject
    D2 d2;
    private Type type;
    private String enrollmentId;
    private String eventId;
    private String teiId;
    private CompositeDisposable disposables;
    private SmsSubmitCase smsSender = null;
    private MutableLiveData<List<SendingStatus>> states = new MutableLiveData<>();
    private ArrayList<SendingStatus> statesList = new ArrayList<>();
    private boolean bound = false;
    private boolean submissionRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((App) getApplicationContext()).userComponent().plus(new SmsModule()).inject(this);
        disposables = new CompositeDisposable();
    }

    @Override
    public IBinder onBind(Intent intent) {
        bound = true;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        bound = false;
        stopEventually();
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        bound = true;
    }

    @Override
    public void onDestroy() {
        disposables.dispose();
        super.onDestroy();
    }

    private void stopEventually() {
        if (!bound && !submissionRunning) {
            stopSelf();
        }
    }

    boolean setTrackerEventData(String eventId, String teiId) {
        if ((type != Type.TRACKER_EVENT && type != null) ||
                (this.eventId != null && !this.eventId.equals(eventId)) ||
                (this.teiId != null && !this.teiId.equals(teiId))
        ) return false;

        this.eventId = eventId;
        this.teiId = teiId;
        this.enrollmentId = null;
        type = Type.TRACKER_EVENT;
        return true;
    }

    boolean setSimpleEventData(String eventId) {
        if ((type != Type.SIMPLE_EVENT && type != null) ||
                (this.eventId != null && !this.eventId.equals(eventId))
        ) return false;

        this.eventId = eventId;
        this.teiId = null;
        this.enrollmentId = null;
        type = Type.SIMPLE_EVENT;
        return true;
    }

    boolean setEnrollmentData(String enrollmentId, String teiId) {
        if ((type != Type.ENROLLMENT && type != null) ||
                (this.enrollmentId != null && !this.enrollmentId.equals(enrollmentId)) ||
                (this.teiId != null && !this.teiId.equals(teiId))
        ) return false;

        this.eventId = null;
        this.teiId = teiId;
        this.enrollmentId = enrollmentId;
        type = Type.ENROLLMENT;
        return true;
    }

    void sendSMS() {
        if (smsSender != null) {
            // started sending before, just republish state
            return;
        }
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
        submissionRunning = true;
        disposables.add(smsSender.send().subscribeOn(Schedulers.newThread()
        ).observeOn(Schedulers.newThread()
        ).subscribeWith(new DisposableObserver<SmsRepository.SmsSendingState>() {
            @Override
            public void onNext(SmsRepository.SmsSendingState state) {
                if (!isLastStateTheSame(state.getSent(), state.getTotal())) {
                    reportState(State.SENDING, state.getSent(), state.getTotal());
                }
            }

            @Override
            public void onError(Throwable e) {
                submissionRunning = false;
                reportError(e);
                stopEventually();
            }

            @Override
            public void onComplete() {
                submissionRunning = false;
                reportState(State.COMPLETED, 0, 0);
                stopEventually();
            }
        }));
    }

    private boolean isLastStateTheSame(int sent, int total) {
        if (statesList == null || statesList.size() == 0) return false;
        SendingStatus last = statesList.get(statesList.size() - 1);
        return last.state == State.SENDING && last.sent == sent && last.total == total;
    }

    public static class SendingStatus implements Serializable {
        public final State state;
        public final int sent;
        public final int total;
        public final Throwable error;

        SendingStatus(State state, Throwable error, int sent, int total) {
            this.state = state;
            this.sent = sent;
            this.total = total;
            this.error = error;
        }
    }

    public enum State implements Serializable {
        STARTED, CONVERTED, ITEM_NOT_READY, WAITING_COUNT_CONFIRMATION,
        COUNT_NOT_ACCEPTED, SENDING, COMPLETED, ERROR
    }

    public enum Type {
        ENROLLMENT, TRACKER_EVENT, SIMPLE_EVENT
    }

    class LocalBinder extends Binder {
        SmsSendingService getService() {
            return SmsSendingService.this;
        }
    }
}
