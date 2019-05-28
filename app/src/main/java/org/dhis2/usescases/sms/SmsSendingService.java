package org.dhis2.usescases.sms;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.dhis2.App;
import org.dhis2.R;
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
    private final static String SMS_NOTIFICATION_CHANNEL_ID = "sms_notification";
    private final static int SMS_NOTIFICATION_ID = 345434369;
    @Inject
    D2 d2;
    private InputArguments inputArguments = null;
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

    /**
     * @param args new input arguments
     * @return false when failed to set these arguments because this service is submitting different
     * set of data. View shouldn't connect to it, because it' submitting something else.
     */
    boolean setInputArguments(InputArguments args) {
        if (args == null) {
            return false;
        }
        if (inputArguments == null || args.isSameSubmission(inputArguments)) {
            inputArguments = args;
            return true;
        }
        return false;
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
        switch (inputArguments.getSubmissionType()) {
            case ENROLLMENT:
                return smsSender.convertEnrollment(inputArguments.getEnrollmentId(), inputArguments.getTeiId());
            case TRACKER_EVENT:
                return smsSender.convertTrackerEvent(inputArguments.getEventId(), inputArguments.getTeiId());
            case SIMPLE_EVENT:
                return smsSender.convertSimpleEvent(inputArguments.getEventId());
            case WRONG_PARAMS:
                reportState(State.ITEM_NOT_READY, 0, 0);
        }
        return null;
    }

    private void reportState(State state, int sent, int total) {
        SendingStatus currentStatus = new SendingStatus(state, null, sent, total);
        statesList.add(currentStatus);
        states.postValue(statesList);

        if (submissionRunning) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(SMS_NOTIFICATION_ID, makeNotification(currentStatus));
        }
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

    private void startBackgroundSubmissionNotification() {
        submissionRunning = true;
        startForeground(SMS_NOTIFICATION_ID, makeNotification(statesList.get(statesList.size() - 1)));
    }

    private void stopBackgroundSubmissionNotification() {
        submissionRunning = false;
        stopForeground(true);
        if (!bound) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(SMS_NOTIFICATION_ID, makeNotification(statesList.get(statesList.size() - 1)));
        }
    }

    private Notification makeNotification(SendingStatus currentStatus) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SMS_NOTIFICATION_CHANNEL_ID);
        String text = StatusText.getTextForStatus(getResources(), currentStatus);
        String title = StatusText.getTextSubmissionType(getResources(), inputArguments);
        builder.setContentText(text);
        builder.setContentTitle(title);
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(title);
        bigTextStyle.bigText(text);
        builder.setStyle(bigTextStyle);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setPriority(Notification.PRIORITY_MAX);
        return builder.build();
    }

    private void executeSending() {
        startBackgroundSubmissionNotification();
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
                reportError(e);
                stopBackgroundSubmissionNotification();
                stopEventually();
            }

            @Override
            public void onComplete() {
                reportState(State.COMPLETED, 0, 0);
                stopBackgroundSubmissionNotification();
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

    class LocalBinder extends Binder {
        SmsSendingService getService() {
            return SmsSendingService.this;
        }
    }
}
