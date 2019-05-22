package org.dhis2.utils;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

/**
 * QUADRAM. Created by ppajuelo on 09/10/2018.
 */

public interface OnDialogClickListener {
    void onPossitiveClick(DialogInterface alertDialog);
    void onNegativeClick(DialogInterface alertDialog);
}
