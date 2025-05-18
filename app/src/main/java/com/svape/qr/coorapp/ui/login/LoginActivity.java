package com.svape.qr.coorapp.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.svape.qr.coorapp.App;
import com.svape.qr.coorapp.R;
import com.svape.qr.coorapp.databinding.ActivityLoginBinding;
import com.svape.qr.coorapp.di.ViewModelFactory;
import com.svape.qr.coorapp.ui.main.MainActivity;
import com.svape.qr.coorapp.ui.register.RegisterActivity;
import com.svape.qr.coorapp.util.Resource;
import javax.inject.Inject;

public class LoginActivity extends AppCompatActivity {
    @Inject
    ViewModelFactory viewModelFactory;

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //DI
        ((App) getApplication()).getAppComponent().inject(this);
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, viewModelFactory).get(LoginViewModel.class);

        setupLoginButton();
        observeLoginResult();
        setupRegisterLink();
    }

    private void setupLoginButton() {
        binding.loginButton.setOnClickListener(v -> {
            String username = binding.usernameEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            if (validateInputs(username, password)) {
                viewModel.login(username, password);
            }
        });
    }

    private void setupRegisterLink() {
        binding.registerLinkTextView.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean validateInputs(String username, String password) {
        boolean isValid = true;

        if (username.isEmpty()) {
            binding.usernameLayout.setError(getString(R.string.error_username_required));
            isValid = false;
        } else {
            binding.usernameLayout.setError(null);
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setError(getString(R.string.error_password_required));
            isValid = false;
        } else {
            binding.passwordLayout.setError(null);
        }

        return isValid;
    }

    private void observeLoginResult() {
        viewModel.getLoginResult().observe(this, result -> {
            if (result.status == Resource.Status.LOADING) {
                showLoading(true);
            } else {
                showLoading(false);

                if (result.status == Resource.Status.SUCCESS && result.data != null && result.data) {
                    navigateToMain();
                } else if (result.status == Resource.Status.ERROR) {
                    showError(result.message);
                }
            }
        });
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!isLoading);
    }

    private void showError(String message) {
        if (message != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(binding.getRoot(), R.string.error_login_generic, Snackbar.LENGTH_LONG).show();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}