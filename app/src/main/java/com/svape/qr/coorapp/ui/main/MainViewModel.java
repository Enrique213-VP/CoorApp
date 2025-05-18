package com.svape.qr.coorapp.ui.main;

import android.util.Base64;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.svape.qr.coorapp.model.BackupItem;
import com.svape.qr.coorapp.repository.BackupRepository;
import com.svape.qr.coorapp.util.DataParser;
import com.svape.qr.coorapp.util.DeviceInfoHelper;
import com.svape.qr.coorapp.util.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainViewModel extends ViewModel {
    private static final String TAG = "MainViewModel";

    private final BackupRepository backupRepository;
    private final DeviceInfoHelper deviceInfoHelper;

    private final MutableLiveData<List<BackupItem>> backupItems = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> processQrResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> logoutResult = new MutableLiveData<>();

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public MainViewModel(BackupRepository backupRepository, DeviceInfoHelper deviceInfoHelper) {
        this.backupRepository = backupRepository;
        this.deviceInfoHelper = deviceInfoHelper;

        loadBackupItems();
    }

    public void loadBackupItems() {
        disposables.add(
                backupRepository.getAllBackupItems()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                items -> backupItems.setValue(items),
                                error -> Log.e(TAG, "Error loading backup items", error)
                        )
        );
    }

    public void processQrData(String qrData) {
        processQrResult.setValue(Resource.loading(null));

        disposables.add(
                backupRepository.validateQrData(qrData)
                        .flatMap(response -> {
                            if (response.isCorrect()) {
                                BackupItem item = DataParser.parseData(response.getData());
                                return backupRepository.saveBackupItem(item)
                                        .andThen(Single.just(item));
                            } else {
                                return Single.error(new IllegalArgumentException("Estructura QR incorrecta"));
                            }
                        })
                        .flatMap(item -> {
                            return backupRepository.getBackupCount()
                                    .map(count -> {
                                         boolean shouldSync = count % 1 == 0;

                                        Log.d(TAG, "Contador de elementos: " + count + ", Sincronizar: " + shouldSync);
                                        return new Object[]{item, shouldSync};
                                    });
                        })
                        .flatMap(result -> {
                            BackupItem item = (BackupItem) result[0];
                            boolean shouldSync = (boolean) result[1];

                            if (shouldSync) {
                                Log.d(TAG, "Iniciando sincronización con Firebase para elemento: " + item.getEtiqueta1d());
                                return syncWithFirebase()
                                        .andThen(Single.just(item.getEtiqueta1d()));
                            } else {
                                return Single.just(item.getEtiqueta1d());
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                etiqueta -> {
                                    processQrResult.setValue(Resource.success("Procesado con éxito: " + etiqueta));
                                    loadBackupItems();
                                },
                                error -> {
                                    Log.e(TAG, "Error al procesar QR", error);
                                    processQrResult.setValue(Resource.error(
                                            "Error al procesar: " + error.getMessage(), null));
                                }
                        )
        );
    }

    private io.reactivex.rxjava3.core.Completable syncWithFirebase() {
        return backupRepository.getAllBackupItems()
                .flatMapCompletable(items -> {
                    String deviceId = deviceInfoHelper.getDeviceId();
                    String currentDate = getCurrentDateAsString();
                    return backupRepository.syncWithFirebase(items, deviceId, currentDate);
                });
    }

    public void logout() {
        logoutResult.setValue(Resource.loading(null));

        String deviceId = deviceInfoHelper.getDeviceId();

        disposables.add(
                backupRepository.deleteBackup(deviceId)
                        .andThen(backupRepository.deleteAllLocalBackup())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> logoutResult.setValue(Resource.success(true)),
                                error -> logoutResult.setValue(Resource.error("Error al cerrar sesión: " + error.getMessage(), false))
                        )
        );
    }

    public void processManualInput(String input) {
        if (isValidInputFormat(input)) {
            try {
                String formattedInput = DataParser.formatInput(input);
                String base64Input = Base64.encodeToString(formattedInput.getBytes(), Base64.DEFAULT);

                processQrData(base64Input);
            } catch (IllegalArgumentException e) {
                processQrResult.setValue(Resource.error("Formato incorrecto: " + e.getMessage(), null));
            }
        } else {
            processQrResult.setValue(Resource.error("Formato de entrada inválido", null));
        }
    }

    private boolean isValidInputFormat(String input) {
        String regex = "^.+(-[^-]*){3}$";
        return input.matches(regex);
    }

    private String getCurrentDateAsString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public LiveData<List<BackupItem>> getBackupItems() {
        return backupItems;
    }

    public LiveData<Resource<String>> getProcessQrResult() {
        return processQrResult;
    }

    public LiveData<Resource<Boolean>> getLogoutResult() {
        return logoutResult;
    }

    public void syncAllItems() {
        disposables.add(
                syncWithFirebase()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d(TAG, "Sincronización manual completa"),
                                error -> Log.e(TAG, "Error en sincronización manual", error)
                        )
        );
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}