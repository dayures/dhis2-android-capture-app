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

import java.util.List;

public class SmsLogAdapter extends RecyclerView.Adapter<SmsLogAdapter.ViewHolder> {

    private List<SmsViewModel.SendingStatus> states;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sms_log_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        boolean firstItem = position == 0;
        position = states.size() - position - 1;
        Resources res = h.item.getResources();
        SmsViewModel.SendingStatus state = states.get(position);
        switch (state.state) {
            case STARTED:
                h.item.setText(R.string.sms_state_started);
                break;
            case CONVERTED:
                h.item.setText(R.string.sms_state_converted);
                break;
            case WAITING_COUNT_CONFIRMATION:
                h.item.setText(res.getString(R.string.sms_waiting_amount_confirm, state.total));
                break;
            case COUNT_NOT_ACCEPTED:
                h.item.setText(R.string.sms_count_error);
                break;
            case SENDING:
                h.item.setText(res.getString(R.string.sms_sending, state.sent, state.total));
                break;
            case COMPLETED:
                h.item.setText(R.string.sms_all_sent);
                break;
            case ERROR:
                h.item.setText(getErrorText(res, state.error));
                return;
            case ITEM_NOT_READY:
                h.item.setText(R.string.sms_error_item_not_ready);
                return;
        }
        int firstItemColor = ContextCompat.getColor(h.item.getContext(), R.color.sms_sync_last_event);
        int standardColor = ContextCompat.getColor(h.item.getContext(), R.color.text_black_333);
        h.item.setTextColor(firstItem ? firstItemColor : standardColor);
    }

    private String getErrorText(Resources res, Throwable error) {
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
        }
        return text;
    }

    @Override
    public int getItemCount() {
        if (states == null) {
            return 0;
        }
        return states.size();
    }

    public void setStates(List<SmsViewModel.SendingStatus> states) {
        this.states = states;
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
