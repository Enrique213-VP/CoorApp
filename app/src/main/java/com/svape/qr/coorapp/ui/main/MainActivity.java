package com.svape.qr.coorapp.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.ScanMode;
import com.google.android.material.snackbar.Snackbar;
import com.svape.qr.coorapp.App;
import com.svape.qr.coorapp.R;
import com.svape.qr.coorapp.databinding.ActivityMainBinding;
import com.svape.qr.coorapp.di.ViewModelFactory;
import com.svape.qr.coorapp.model.BackupItem;
import com.svape.qr.coorapp.ui.login.LoginActivity;
import com.svape.qr.coorapp.ui.map.MapActivity;
import com.svape.qr.coorapp.util.Resource;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity implements BackupAdapter.OnMapClickListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 100;

    @Inject
    ViewModelFactory viewModelFactory;

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private BackupAdapter adapter;
    private CodeScanner codeScanner;
    private RotateAnimation rotateAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((App) getApplication()).getAppComponent().inject(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, viewModelFactory).get(MainViewModel.class);

        setupRecyclerView();
        setupCamera();
        setupClickListeners();
        setupRotateAnimation();
        observeViewModel();
    }

    private void setupRotateAnimation() {
        rotateAnimation = new RotateAnimation(0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(1000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
    }

    private void setupRecyclerView() {
        adapter = new BackupAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupCamera() {
        codeScanner = new CodeScanner(this, binding.scannerView);
        codeScanner.setScanMode(ScanMode.SINGLE);
        codeScanner.setDecodeCallback(result -> runOnUiThread(() -> {
            hideCameraView();
            viewModel.processQrData(result.getText());
        }));
    }

    private void setupClickListeners() {
        binding.logoutButton.setOnClickListener(v -> viewModel.logout());

        binding.syncButton.setOnClickListener(v -> {
            Log.d(TAG, "Botón de sincronización presionado");
            setSyncButtonEnabled(false);
            showLoading(true);
            animateSyncIcon(true);
            Snackbar.make(binding.getRoot(), R.string.sync_started, Snackbar.LENGTH_SHORT).show();
            viewModel.syncAllItems();
        });

        binding.cameraButton.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                showCameraView();
            } else {
                requestCameraPermission();
            }
        });

        binding.closeCameraButton.setOnClickListener(v -> hideCameraView());

        binding.inputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String input = binding.inputEditText.getText().toString().trim();
                if (!input.isEmpty()) {
                    viewModel.processManualInput(input);
                    binding.inputEditText.setText("");
                }
                return true;
            }
            return false;
        });
    }

    private void observeViewModel() {
        viewModel.getBackupItems().observe(this, items -> {
            Log.d(TAG, "BackupItems actualizados, cantidad: " + items.size());
            adapter.setItems(items);

            if (items.isEmpty()) {
                Log.d(TAG, "No hay elementos para mostrar");
            }
        });

        viewModel.getProcessQrResult().observe(this, result -> {
            switch (result.status) {
                case LOADING:
                    showLoading(true);
                    break;
                case SUCCESS:
                    showLoading(false);
                    if (result.data != null) {
                        Snackbar.make(binding.getRoot(), result.data, Snackbar.LENGTH_SHORT).show();
                    }
                    break;
                case ERROR:
                    showLoading(false);
                    if (result.message != null) {
                        Snackbar.make(binding.getRoot(), result.message, Snackbar.LENGTH_LONG).show();
                    }
                    break;
            }
        });

        viewModel.getLogoutResult().observe(this, result -> {
            switch (result.status) {
                case LOADING:
                    showLoading(true);
                    break;
                case SUCCESS:
                    showLoading(false);
                    navigateToLogin();
                    break;
                case ERROR:
                    showLoading(false);
                    if (result.message != null) {
                        Snackbar.make(binding.getRoot(), result.message, Snackbar.LENGTH_LONG).show();
                    }
                    break;
            }
        });

        viewModel.getSyncResult().observe(this, result -> {
            Log.d(TAG, "Estado de sincronización actualizado: " + result.status);

            switch (result.status) {
                case LOADING:
                    showLoading(true);
                    setSyncButtonEnabled(false);
                    animateSyncIcon(true);
                    break;
                case SUCCESS:
                    showLoading(false);
                    setSyncButtonEnabled(true);
                    animateSyncIcon(false);
                    Snackbar.make(binding.getRoot(), R.string.sync_completed, Snackbar.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    showLoading(false);
                    setSyncButtonEnabled(true);
                    animateSyncIcon(false);
                    if (result.message != null) {
                        Snackbar.make(binding.getRoot(), result.message, Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(binding.getRoot(), R.string.sync_error, Snackbar.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    private void setSyncButtonEnabled(boolean enabled) {
        binding.syncButton.setEnabled(enabled);
        binding.syncButton.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void animateSyncIcon(boolean animate) {
        if (animate) {
            binding.syncButton.startAnimation(rotateAnimation);
        } else {
            binding.syncButton.clearAnimation();
        }
    }

    private void showCameraView() {
        binding.cameraContainer.setVisibility(View.VISIBLE);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone((ConstraintLayout) binding.getRoot());
        constraintSet.connect(R.id.inputContainer, ConstraintSet.TOP,
                R.id.cameraContainer, ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo((ConstraintLayout) binding.getRoot());

        codeScanner.startPreview();
    }

    private void hideCameraView() {
        binding.cameraContainer.setVisibility(View.GONE);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone((ConstraintLayout) binding.getRoot());
        constraintSet.connect(R.id.inputContainer, ConstraintSet.TOP,
                R.id.toolbar, ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo((ConstraintLayout) binding.getRoot());

        if (codeScanner != null) {
            codeScanner.stopPreview();
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CAMERA
        );
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCameraView();
            } else {
                Snackbar.make(binding.getRoot(), R.string.camera_permission_required, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadBackupItems();

        if (binding.cameraContainer.getVisibility() == View.VISIBLE && hasCameraPermission()) {
            codeScanner.startPreview();
        }
    }

    @Override
    protected void onPause() {
        if (codeScanner != null) {
            codeScanner.releaseResources();
        }
        super.onPause();
    }

    @Override
    public void onMapClick(BackupItem item) {
        Log.d(TAG, "Click en mapa para item - Etiqueta: " + item.getEtiqueta1d() +
                ", Latitud: " + item.getLatitud() +
                ", Longitud: " + item.getLongitud());

        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(MapActivity.EXTRA_LATITUDE, item.getLatitud());
        intent.putExtra(MapActivity.EXTRA_LONGITUDE, item.getLongitud());
        intent.putExtra(MapActivity.EXTRA_ETIQUETA, item.getEtiqueta1d());
        startActivity(intent);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}