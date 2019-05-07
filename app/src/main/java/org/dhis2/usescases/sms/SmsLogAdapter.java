package org.dhis2.usescases.sms;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.dhis2.R;
import org.hisp.dhis.android.core.sms.domain.interactor.SmsSubmitCase;
import org.hisp.dhis.android.core.sms.domain.repository.SmsRepository;

import java.util.List;

public class SmsLogAdapter extends RecyclerView.Adapter<SmsLogAdapter.ViewHolder> {

    private List<SmsRepository.SmsSendingState> states;
    private Throwable error = null;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sms_log_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        boolean firstItem = position == 0;
        if (error != null) {
            if (position == 0) {
                showError(holder, error);
                return;
            } else {
                position--;
            }
        }
        SmsRepository.SmsSendingState state = states.get(position);
        showState(holder, state, firstItem);
    }

    private void showState(ViewHolder h, SmsRepository.SmsSendingState state, boolean firstItem) {
        Resources res = h.item.getResources();
        switch (state.getState()) {
            case SENDING:
                h.item.setText(res.getString(R.string.sms_sending, state.getSent(), state.getTotal()));
                break;
            case WAITING_SMS_COUNT_ACCEPT:
                h.item.setText(R.string.sms_waiting_amount_confirm);
                break;
            case ALL_SENT:
                h.item.setText(R.string.sms_all_sent);
                break;
        }
        int firstItemColor = ContextCompat.getColor(h.item.getContext(), R.color.sms_sync_last_event);
        int standardColor = ContextCompat.getColor(h.item.getContext(), R.color.text_black_333);
        h.item.setTextColor(firstItem ? firstItemColor : standardColor);
    }

    private void showError(ViewHolder holder, Throwable error) {
        Resources res = holder.item.getResources();
        String text = res.getString(R.string.sms_error);
        if (error instanceof SmsSubmitCase.PreconditionFailed) {
            switch (((SmsSubmitCase.PreconditionFailed) error).getType()) {
                case NO_NETWORK:
                    text = res.getString(R.string.sms_error_no_network);
                    break;
                case NO_CHECK_NETWORK_PERMISSION:
                    text = res.getString(R.string.sms_error_no_check_network_permission);
                    break;
                case NO_RECEIVE_SMS_PERMISSION:
                    text = res.getString(R.string.sms_error_no_receive_sms_permission);
                    break;
                case NO_SEND_SMS_PERMISSION:
                    text = res.getString(R.string.sms_error_no_send_sms_permission);
                    break;
                case NO_GATEWAY_NUMBER_SET:
                    text = res.getString(R.string.sms_error_no_gateway_set);
                    break;
                case NO_USER_LOGGED_IN:
                    text = res.getString(R.string.sms_error_no_user_login);
                    break;
                case NO_METADATA_DOWNLOADED:
                    text = res.getString(R.string.sms_metadata_empty);
                    break;
                case SMS_MODULE_DISABLED:
                    text = res.getString(R.string.sms_error_module_disabled);
                    break;
            }
        } else if (error instanceof SmsRepository.SMSCountException) {
            text = res.getString(R.string.sms_count_error, ((SmsRepository.SMSCountException) error).getCount());
        }
        int errorColor = ContextCompat.getColor(holder.item.getContext(), R.color.sms_sync_last_event);
        holder.item.setText(text);
        holder.item.setTextColor(errorColor);
    }


    @Override
    public int getItemCount() {
        int errorCount = this.error == null ? 0 : 1;
        if (states == null) {
            return errorCount;
        } else {
            return states.size() + errorCount;
        }
    }

    public void setStates(List<SmsRepository.SmsSendingState> states) {
        this.states = states;
        notifyDataSetChanged();
    }

    public void setError(Throwable error) {
        this.error = error;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView item;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            item = itemView.findViewById(R.id.smsLogItem);
        }
    }
}
