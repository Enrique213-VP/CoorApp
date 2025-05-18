package com.svape.qr.coorapp.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.svape.qr.coorapp.repository.UserRepository;
import com.svape.qr.coorapp.util.Resource;
import javax.inject.Inject;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Resource<Boolean>> loginResult = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public LoginViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void login(String username, String password) {
        loginResult.setValue(Resource.loading(null));

        disposables.add(
                userRepository.validateUser(username, password)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                isValid -> {
                                    if (isValid) {
                                        loginResult.setValue(Resource.success(true));
                                    } else {
                                        loginResult.setValue(Resource.error("Usuario no existe o credenciales incorrectas", false));
                                    }
                                },
                                error -> loginResult.setValue(Resource.error("Error en la autenticación: " + error.getMessage(), false))
                        )
        );
    }

    public LiveData<Resource<Boolean>> getLoginResult() {
        return loginResult;
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}