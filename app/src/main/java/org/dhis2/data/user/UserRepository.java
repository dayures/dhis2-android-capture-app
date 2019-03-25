package org.dhis2.data.user;

import org.hisp.dhis.android.core.user.User;
import org.hisp.dhis.android.core.user.UserCredentials;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;

public interface UserRepository {

    @NonNull
    Flowable<UserCredentials> credentials();

    @NonNull
    Flowable<User> me();

}
