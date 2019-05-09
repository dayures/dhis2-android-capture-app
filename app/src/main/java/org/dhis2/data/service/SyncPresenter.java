package org.dhis2.data.service;

import android.content.Context;

public interface SyncPresenter {
    void syncAndDownloadEvents(Context context) throws SyncError;

    void syncAndDownloadTeis(Context context) throws Exception;

    void syncMetadata(Context context) throws SyncError;

    void syncReservedValues();

    public class SyncError extends Exception {

    }
}
