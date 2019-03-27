package org.dhis2.data.user;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.utils.SqlConstants;
import org.hisp.dhis.android.core.user.User;
import org.hisp.dhis.android.core.user.UserCredentials;

import androidx.annotation.NonNull;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import static org.dhis2.utils.SqlConstants.LIMIT_1;
import static org.dhis2.utils.SqlConstants.SELECT_ALL_FROM;

public class UserRepositoryImpl implements UserRepository {
    private static final String SELECT_USER = SELECT_ALL_FROM +
            SqlConstants.USER_TABLE + LIMIT_1;
    private static final String SELECT_USER_CREDENTIALS = SELECT_ALL_FROM +
            SqlConstants.USER_CREDENTIALS_TABLE + LIMIT_1;

    private final BriteDatabase briteDatabase;

    UserRepositoryImpl(@NonNull BriteDatabase briteDatabase) {
        this.briteDatabase = briteDatabase;
    }

    @NonNull
    @Override
    public Flowable<UserCredentials> credentials() {
        return briteDatabase
                .createQuery(SqlConstants.USER_CREDENTIALS_TABLE, SELECT_USER_CREDENTIALS)
                .mapToOne(UserCredentials::create)
                .take(1).toFlowable(BackpressureStrategy.BUFFER);
    }

    @NonNull
    @Override
    public Flowable<User> me() {
        return briteDatabase
                .createQuery(SqlConstants.USER_TABLE, SELECT_USER)
                .mapToOne(User::create).toFlowable(BackpressureStrategy.BUFFER);
    }
}
