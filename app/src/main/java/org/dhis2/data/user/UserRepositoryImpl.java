package org.dhis2.data.user;

import com.squareup.sqlbrite2.BriteDatabase;

import org.hisp.dhis.android.core.user.User;
import org.hisp.dhis.android.core.user.UserCredentials;
import org.hisp.dhis.android.core.user.UserCredentialsModel;
import org.hisp.dhis.android.core.user.UserModel;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

public class UserRepositoryImpl implements UserRepository {
    private static final String SELECT_USER = "SELECT * FROM " +
            UserModel.TABLE + " LIMIT 1";
    private static final String SELECT_USER_CREDENTIALS = "SELECT * FROM " +
            UserCredentialsModel.TABLE + " LIMIT 1";

    private final BriteDatabase briteDatabase;

    UserRepositoryImpl(@NonNull BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    @NonNull
    @Override
    public Flowable<UserCredentials> credentials() {
        return briteDatabase
                .createQuery(UserCredentialsModel.TABLE, SELECT_USER_CREDENTIALS)
                .mapToOne(UserCredentials::create)
                .take(1).toFlowable(BackpressureStrategy.BUFFER);
    }

    @NonNull
    @Override
    public Flowable<User> me() {
        return briteDatabase
                .createQuery(UserModel.TABLE, SELECT_USER)
                .mapToOne(User::create).toFlowable(BackpressureStrategy.BUFFER);
    }
}
