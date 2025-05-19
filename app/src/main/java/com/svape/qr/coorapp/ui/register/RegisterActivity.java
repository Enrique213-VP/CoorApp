package com.svape.qr.coorapp.ui.register;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
        setupKeyboardNavigation();
    }

    private void setupKeyboardNavigation() {
        binding.usernameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.passwordEditText.requestFocus();
                return true;
            }
            return false;
        });

        binding.passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.confirmPasswordEditText.requestFocus();
                return true;
            }
            return false;
        });

        binding.confirmPasswordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                attemptRegistration();
                return true;
            }
            return false;
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void attemptRegistration() {
        String username = binding.usernameEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

        if (validateInputs(username, password, confirmPassword)) {
            viewModel.registerUser(username, password);
        }
    }

    private void setupRegisterButton() {
        binding.registerButton.setOnClickListener(v -> {
            hideKeyboard();
            attemptRegistration();
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
        } else if (!isValidEmail(username)) {
            binding.usernameLayout.setError(getString(R.string.error_invalid_email));
            isValid = false;
        } else {
            binding.usernameLayout.setError(null);
        }

        if (password.isEmpty()) {
            binding.passwordLayout.setError(getString(R.string.error_password_required));
            isValid = false;
        } else if (password.length() < 6) {
            binding.passwordLayout.setError(getString(R.string.error_password_too_short));
            isValid = false;
        } else if (hasConsecutiveDigits(password)) {
            binding.passwordLayout.setError(getString(R.string.error_password_consecutive));
            isValid = false;
        } else {
            binding.passwordLayout.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordLayout.setError(getString(R.string.error_confirm_password_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError(getString(R.string.error_confirm_password));
            isValid = false;
        } else {
            binding.confirmPasswordLayout.setError(null);
        }

        return isValid;
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    private boolean hasConsecutiveDigits(String password) {
        for (int i = 0; i < password.length() - 1; i++) {
            char current = password.charAt(i);
            char next = password.charAt(i + 1);

            if (Character.isDigit(current) && Character.isDigit(next)) {
                int currentDigit = Character.getNumericValue(current);
                int nextDigit = Character.getNumericValue(next);

                if (nextDigit == currentDigit + 1) {
                    return true;
                }
            }
        }
        return false;
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
            Snackbar snackbar;
            if (message.contains("exists")) {
                snackbar = Snackbar.make(binding.getRoot(), R.string.error_username_exists, Snackbar.LENGTH_LONG);
            } else {
                snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
            }
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(getResources().getColor(R.color.error_color));
            snackbar.show();
        } else {
            Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.error_registration, Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(getResources().getColor(R.color.error_color));
            snackbar.show();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}