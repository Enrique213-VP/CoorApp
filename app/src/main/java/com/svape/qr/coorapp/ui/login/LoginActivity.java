package com.svape.qr.coorapp.ui.login;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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
import com.svape.qr.coorapp.util.NetworkUtils;
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
        setupLinkedInButton();
    }

    private void showNoConnectionWarning(String username, String password) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning_title_no_connection)
                .setMessage(R.string.warning_no_internet_connection)
                .setIcon(R.drawable.ic_info)
                .setCancelable(false)
                .setPositiveButton(R.string.continue_anyway, (dialog, which) -> {
                    viewModel.login(username, password);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupLoginButton() {
        binding.loginButton.setOnClickListener(v -> {
            String username = binding.usernameEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            if (validateInputs(username, password)) {
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    showNoConnectionWarning(username, password);
                } else {
                    viewModel.login(username, password);
                }
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
            Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(getResources().getColor(R.color.error_color));
            snackbar.show();
        } else {
            Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.error_login_generic, Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(getResources().getColor(R.color.error_color));
            snackbar.show();
        }
    }

    private void setupLinkedInButton() {
        binding.linkedinButton.setOnClickListener(v -> {
            openLinkedIn();
        });
    }

    private void openLinkedIn() {
        String linkedInProfileUrl = "https://www.linkedin.com/in/svap/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkedInProfileUrl));

        intent.setPackage("com.linkedin.android");

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {}
            intent.setPackage(null);
            startActivity(intent);
        }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}