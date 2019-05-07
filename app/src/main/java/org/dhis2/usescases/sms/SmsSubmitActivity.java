package org.dhis2.usescases.sms;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.dhis2.App;
import org.dhis2.R;
import org.dhis2.usescases.general.ViewModelFactory;
import org.hisp.dhis.android.core.sms.domain.repository.SmsRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class SmsSubmitActivity extends AppCompatActivity {
    private static String ARG_TEI = "tei";
    private static String ARG_EVENT = "event";
    private static String ARG_ENROLLMENT = "enrollment";
    private static final int SMS_PERMISSIONS_REQ_ID = 102;
    private SmsLogAdapter adapter;
    private SmsViewModel smsViewModel;
    private View titleBar;
    private TextView state;
    private RotateAnimation rotate = new RotateAnimation(
            0, 360,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
    );
    private boolean submissionFinished = false;

    @Inject
    ViewModelFactory<SmsViewModel> vmFactory;

    public static void setEventData(Bundle args, String eventId, String teiId) {
        args.putString(ARG_EVENT, eventId);
        args.putString(ARG_TEI, teiId);
    }

    public static void setEnrollmentData(Bundle args, String teiId, String enrollmentId) {
        args.putString(ARG_ENROLLMENT, enrollmentId);
        args.putString(ARG_TEI, teiId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        findViewById(R.id.smsLogOverlay).setOnClickListener(v -> finish());
        titleBar = findViewById(R.id.smsLogTitleBar);
        TextView title = findViewById(R.id.smsLogTitle);
        state = findViewById(R.id.smsLogState);
        adapter = new SmsLogAdapter();
        RecyclerView recycler = findViewById(R.id.smsLogRecycler);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        String eventId = getIntent().getStringExtra(ARG_EVENT);
        String enrollmentId = getIntent().getStringExtra(ARG_ENROLLMENT);
        String teiId = getIntent().getStringExtra(ARG_TEI);
        ((App) getApplicationContext()).userComponent().plus(new SmsModule()).inject(this);
        smsViewModel = ViewModelProviders.of(this, vmFactory).get(SmsViewModel.class);
        smsViewModel.sendingState().observe(this, this::stateChanged);
        smsViewModel.errors().observe(this, this::onError);
        if (enrollmentId != null) {
            smsViewModel.setEnrollmentData(enrollmentId, teiId);
            title.setText(R.string.sms_title_enrollment);
        } else if (eventId != null) {
            smsViewModel.setEventData(eventId, teiId);
            title.setText(R.string.sms_title_event);
        }
        state.setText(R.string.sms_bar_state_sending);
        if (checkPermissions()) {
            smsViewModel.sendSMS();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!submissionFinished) {
            rotate.setDuration(1000);
            rotate.setRepeatCount(Animation.INFINITE);
            findViewById(R.id.smsLogIcon).startAnimation(rotate);
        }
    }

    @Override
    protected void onStop() {
        rotate.cancel();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != SMS_PERMISSIONS_REQ_ID) return;
        // Try to send anyway. It will show a right message in case of important permission missing.
        smsViewModel.sendSMS();
    }

    private boolean checkPermissions() {
        // check permissions
        String[] smsPermissions = new String[]{Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS};
        if (!hasPermissions(smsPermissions)) {
            ActivityCompat.requestPermissions(this, smsPermissions, SMS_PERMISSIONS_REQ_ID);
            return false;
        }
        return true;
    }

    private void askForMessagesAmount(int amount) {
        Bundle args = new Bundle();
        args.putInt(MessagesAmountDialog.ARG_AMOUNT, amount);
        MessagesAmountDialog dialog = new MessagesAmountDialog();
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), null);
    }

    private void stateChanged(List<SmsRepository.SmsSendingState> states) {
        adapter.setStates(states);
        if (states.size() == 0) {
            return;
        }
        SmsRepository.SmsSendingState lastState = states.get(states.size() - 1);
        if (lastState.getState() == SmsRepository.State.WAITING_SMS_COUNT_ACCEPT) {
            askForMessagesAmount(lastState.getTotal());
        } else if (lastState.getState() == SmsRepository.State.ALL_SENT) {
            state.setText(R.string.sms_bar_state_sent);
            finishSubmission();
        }
    }

    private void finishSubmission() {
        rotate.cancel();
        submissionFinished = true;

    }

    private void onError(Throwable e) {
        finishSubmission();
        adapter.setError(e);
        Timber.d(e);
        titleBar.setBackgroundColor(ContextCompat.getColor(this, R.color.sms_sync_title_bar_error));
        state.setText(R.string.sms_bar_state_failed);
    }

    public static class MessagesAmountDialog extends DialogFragment {
        static final String ARG_AMOUNT = "amount";

        public MessagesAmountDialog() {
        }

        @NotNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int amount = getArguments().getInt(ARG_AMOUNT);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.sms_amount_question, amount));

            builder.setPositiveButton(android.R.string.yes, (dialog, which) ->
                    ((SmsSubmitActivity) getActivity()).smsViewModel.acceptSMSCount(true)
            );
            builder.setNegativeButton(android.R.string.no, (dialog, which) ->
                    ((SmsSubmitActivity) getActivity()).smsViewModel.acceptSMSCount(false)
            );
            return builder.create();
        }

        @Override
        public void onPause() {
            dismiss();
            super.onPause();
        }
    }
}
