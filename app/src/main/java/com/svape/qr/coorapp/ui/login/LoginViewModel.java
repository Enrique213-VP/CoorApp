package com.svape.qr.coorapp.ui.login;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.svape.qr.coorapp.repository.UserRepository;
import com.svape.qr.coorapp.util.Resource;
import com.svape.qr.coorapp.util.SessionManager;
import javax.inject.Inject;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginViewModel extends ViewModel {
    private static final String TAG = "LoginViewModel";

    private final UserRepository userRepository;
    private final SessionManager sessionManager;
    private final MutableLiveData<Resource<Boolean>> loginResult = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public LoginViewModel(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
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
                                        String lastUser = sessionManager.getLastUsername();
                                        boolean isUserChanged = !username.equals(lastUser) && !lastUser.isEmpty();
                                        Log.d(TAG, "Usuario actual: " + username + ", último: " + lastUser);

                                        sessionManager.setUsername(username);
                                        sessionManager.setLoggedIn(true);

                                        loginResult.setValue(Resource.success(true));
                                    } else {
                                        loginResult.setValue(Resource.error("Usuario no existe o credenciales incorrectas", false));
                                    }
                                },
                                error -> {
                                    Log.e(TAG, "Error en autenticación", error);
                                    loginResult.setValue(Resource.error("Error en la autenticación: " + error.getMessage(), false));
                                }
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