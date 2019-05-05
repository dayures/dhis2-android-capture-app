package org.dhis2.usescases.sms;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;

import org.dhis2.App;
import org.dhis2.R;
import org.dhis2.usescases.general.ViewModelFactory;
import org.hisp.dhis.android.core.sms.domain.interactor.SmsSubmitCase;
import org.hisp.dhis.android.core.sms.domain.repository.SmsRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

public class SmsSubmitActivity extends AppCompatActivity {
    private static String ARG_TEI = "tei";
    private static String ARG_EVENT = "event";
    private static String ARG_ENROLLMENT = "enrollment";
    private static final int SMS_PERMISSIONS_REQ_ID = 102;
    private LinearLayout layout;
    private SmsViewModel smsViewModel;
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
        findViewById(R.id.smsOverlay).setOnClickListener(v -> finish());
        layout = findViewById(R.id.smsLogLayout);
        String eventId = getIntent().getStringExtra(ARG_EVENT);
        String enrollmentId = getIntent().getStringExtra(ARG_ENROLLMENT);
        String teiId = getIntent().getStringExtra(ARG_TEI);
        ((App) getApplicationContext()).userComponent().plus(new SmsModule()).inject(this);
        smsViewModel = ViewModelProviders.of(this, vmFactory).get(SmsViewModel.class);
        smsViewModel.sendingState().observe(this, this::stateChanged);
        smsViewModel.errors().observe(this, this::onError);
        if (enrollmentId != null) {
            smsViewModel.setEnrollmentData(enrollmentId, teiId);
        } else if (eventId != null) {
            smsViewModel.setEventData(eventId, teiId);
        }
        if (checkPermissions()) {
            smsViewModel.sendSMS();
        }
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

    private void addText(int text) {
        addText(getString(text));
    }

    private void addText(String text) {
        TextView textView = new TextView(SmsSubmitActivity.this);
        textView.setText(text);
        layout.addView(textView, 0);
    }

    private void askForMessagesAmount(int amount) {
        Bundle args = new Bundle();
        args.putInt(MessagesAmountDialog.ARG_AMOUNT, amount);
        MessagesAmountDialog dialog = new MessagesAmountDialog();
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), null);
    }

    private void stateChanged(List<SmsRepository.SmsSendingState> states) {
        layout.removeAllViews();
        for (int i = 0; i < states.size(); i++) {
            SmsRepository.SmsSendingState state = states.get(i);
            switch (state.getState()) {
                case SENDING:
                    addText(getString(R.string.sms_sending, state.getSent(), state.getTotal()));
                    break;
                case WAITING_SMS_COUNT_ACCEPT:
                    addText(R.string.sms_waiting_amount_confirm);
                    if (i == states.size() - 1) {
                        askForMessagesAmount(state.getTotal());
                    }
                    break;
                case ALL_SENT:
                    addText(R.string.sms_all_sent);
                    break;
            }
        }
    }

    private void onError(Throwable e) {
        e.printStackTrace();

        if (e instanceof SmsSubmitCase.PreconditionFailed) {
            switch (((SmsSubmitCase.PreconditionFailed) e).getType()) {
                case NO_NETWORK:
                    addText(R.string.sms_error_no_network);
                    break;
                case NO_CHECK_NETWORK_PERMISSION:
                    addText(R.string.sms_error_no_check_network_permission);
                    break;
                case NO_RECEIVE_SMS_PERMISSION:
                    addText(R.string.sms_error_no_receive_sms_permission);
                    break;
                case NO_SEND_SMS_PERMISSION:
                    addText(R.string.sms_error_no_send_sms_permission);
                    break;
                case NO_GATEWAY_NUMBER_SET:
                    addText(R.string.sms_error_no_gateway_set);
                    break;
                case NO_USER_LOGGED_IN:
                    addText(R.string.sms_error_no_user_login);
                    break;
                case NO_METADATA_DOWNLOADED:
                    addText(R.string.sms_metadata_empty);
                    break;
                case SMS_MODULE_DISABLED:
                    addText(R.string.sms_error_module_disabled);
                    break;
            }
        } else if (e instanceof SmsRepository.SMSCountException) {
            addText(getString(R.string.sms_count_error, ((SmsRepository.SMSCountException) e).getCount()));
        } else {
            addText(R.string.sms_error);
        }
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
