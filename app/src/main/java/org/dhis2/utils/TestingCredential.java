package org.dhis2.utils;

import androidx.annotation.NonNull;

/**
 * QUADRAM. Created by ppajuelo on 21/03/2019.
 */
public class TestingCredential {

    @NonNull
    private String serverUrl;
    @NonNull
    private String userName;
    @NonNull
    private String userPass;

    public TestingCredential(@NonNull String serverUrl, @NonNull String userName, @NonNull String userPass) {
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.userPass = userPass;
    }

    @NonNull
    public String getServerUrl() {
        return serverUrl;
    }

    @NonNull
    public String getUserName() {
        return userName;
    }

    @NonNull
    public String getUserPass() {
        return userPass;
    }
}
