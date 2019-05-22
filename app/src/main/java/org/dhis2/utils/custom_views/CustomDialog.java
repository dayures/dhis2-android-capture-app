package org.dhis2.utils.custom_views;

import android.app.AlertDialog;
import android.content.Context;
import androidx.databinding.DataBindingUtil;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import org.dhis2.R;
import org.dhis2.databinding.CustomDialogBinding;
import org.dhis2.utils.DialogClickListener;

/**
 * QUADRAM. Created by frodriguez on 5/4/2018.
 */

public class CustomDialog extends AlertDialog {

    private Context context;
    private AlertDialog dialog;
    private String title;
    private String message;
    private String positiveText;
    private String negativeText;
    private int requestCode;
    private DialogClickListener listener;

    public CustomDialog(@NonNull Context context,
                        @NonNull String title,
                        @NonNull String message,
                        @NonNull String positiveText,
                        @Nullable String negativeText,
                        int requestCode,
                        @Nullable DialogClickListener listener) {
        super(context);
        this.context = context;
        this.title = title;
        this.message = message;
        this.positiveText = positiveText;
        this.negativeText = negativeText;
        this.requestCode = requestCode;
        this.listener = listener;
    }


    @Override
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DhisMaterialDialog);
        builder.setTitle(title);
        builder.setMessage(message);
        if (!TextUtils.isEmpty(negativeText))
            builder.setNegativeButton(negativeText, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (listener != null)
                        listener.onNegative();
                }
            });
        if (!TextUtils.isEmpty(positiveText))
            builder.setPositiveButton(positiveText, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (listener != null)
                        listener.onPositive();
                }
            });


        dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.show();
    }

    @Override
    public void dismiss() {
        if (dialog != null)
            dialog.dismiss();
    }

    public int getRequestCode() {
        return requestCode;
    }


}
