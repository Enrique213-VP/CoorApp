package com.svape.qr.coorapp.ui.register;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.svape.qr.coorapp.repository.UserRepository;
import com.svape.qr.coorapp.util.Resource;
import javax.inject.Inject;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RegisterViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Resource<Boolean>> registrationResult = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public RegisterViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void registerUser(String username, String password) {
        registrationResult.setValue(Resource.loading(null));

        disposables.add(
                userRepository.checkIfUsernameExists(username)
                        .flatMap(exists -> {
                            if (exists) {
                                return io.reactivex.rxjava3.core.Single.error(
                                        new Exception("Username already exists"));
                            } else {
                                return userRepository.registerUser(username, password);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                success -> registrationResult.setValue(Resource.success(true)),
                                error -> registrationResult.setValue(Resource.error(error.getMessage(), false))
                        )
        );
    }

    public LiveData<Resource<Boolean>> getRegistrationResult() {
        return registrationResult;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}