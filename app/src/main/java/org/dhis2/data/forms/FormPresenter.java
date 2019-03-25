package org.dhis2.data.forms;

import org.hisp.dhis.android.core.category.CategoryOptionCombo;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

interface FormPresenter {

    @UiThread
    void onAttach(@NonNull FormView view);

    @UiThread
    void onDetach();

    void checkSections();

    void checkMandatoryFields();

    void deleteCascade();

    void saveCategoryOption(CategoryOptionCombo selectedOption);
}