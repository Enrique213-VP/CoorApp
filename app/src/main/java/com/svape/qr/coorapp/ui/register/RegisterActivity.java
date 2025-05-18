package com.svape.qr.coorapp.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.snackbar.Snackbar;
import com.svape.qr.coorapp.App;
import com.svape.qr.coorapp.R;
import com.svape.qr.coorapp.databinding.ActivityRegisterBinding;
import com.svape.qr.coorapp.di.ViewModelFactory;
import com.svape.qr.coorapp.ui.login.LoginActivity;
import com.svape.qr.coorapp.util.Resource;
import javax.inject.Inject;

public class RegisterActivity extends AppCompatActivity {
    @Inject
    ViewModelFactory viewModelFactory;

    private ActivityRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((App) getApplication()).getAppComponent().inject(this);
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, viewModelFactory).get(RegisterViewModel.class);

        setupRegisterButton();
        setupLoginLink();
        observeRegistrationResult();
    }

    private void setupRegisterButton() {
        binding.registerButton.setOnClickListener(v -> {
            String username = binding.usernameEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

            if (validateInputs(username, password, confirmPassword)) {
                viewModel.registerUser(username, password);
            }
        });
    }

    private void setupLoginLink() {
        binding.loginLinkTextView.setOnClickListener(v -> {
            navigateToLogin();
        });
    }

    private boolean validateInputs(String username, String password, String confirmPassword) {
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

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError(getString(R.string.error_confirm_password));
            isValid = false;
        } else {
            binding.confirmPasswordLayout.setError(null);
        }

        return isValid;
    }

    private void observeRegistrationResult() {
        viewModel.getRegistrationResult().observe(this, result -> {
            if (result.status == Resource.Status.LOADING) {
                showLoading(true);
            } else {
                showLoading(false);

                if (result.status == Resource.Status.SUCCESS) {
                    Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                } else if (result.status == Resource.Status.ERROR) {
                    showError(result.message);
                }
            }
        });
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.registerButton.setEnabled(!isLoading);
    }

    private void showError(String message) {
        if (message != null) {
            if (message.contains("exists")) {
                Snackbar.make(binding.getRoot(), R.string.error_username_exists, Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        } else {
            Snackbar.make(binding.getRoot(), R.string.error_registration, Snackbar.LENGTH_LONG).show();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}