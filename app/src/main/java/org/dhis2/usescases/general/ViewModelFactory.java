package org.dhis2.usescases.general;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;
import javax.inject.Provider;

public class ViewModelFactory<VM extends ViewModel> implements ViewModelProvider.Factory {
    private final Provider<VM> provider;

    @Inject
    public ViewModelFactory(Provider<VM> provider) {
        this.provider = provider;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) provider.get();
    }
}