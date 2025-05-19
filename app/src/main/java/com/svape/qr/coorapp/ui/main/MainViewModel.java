package com.svape.qr.coorapp.ui.main;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.svape.qr.coorapp.model.BackupItem;
import com.svape.qr.coorapp.repository.BackupRepository;
import com.svape.qr.coorapp.util.DataParser;
import com.svape.qr.coorapp.util.DeviceInfoHelper;
import com.svape.qr.coorapp.util.NetworkUtils;
import com.svape.qr.coorapp.util.Resource;
import com.svape.qr.coorapp.util.SessionManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainViewModel extends ViewModel {
    private static final String TAG = "MainViewModel";

    private final BackupRepository backupRepository;
    private final DeviceInfoHelper deviceInfoHelper;
    private final SessionManager sessionManager;
    private final Context context;
    private final MutableLiveData<List<BackupItem>> backupItems = new MutableLiveData<>();
    private final MutableLiveData<Resource<String>> processQrResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> logoutResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> syncResult = new MutableLiveData<>();

    private final CompositeDisposable disposables = new CompositeDisposable();

    private boolean isSyncing = false;

    @Inject
    public MainViewModel(BackupRepository backupRepository, DeviceInfoHelper deviceInfoHelper,
                         SessionManager sessionManager, Context context) {
        this.backupRepository = backupRepository;
        this.deviceInfoHelper = deviceInfoHelper;
        this.sessionManager = sessionManager;
        this.context = context;

        checkUserChange();
    }

    private void checkUserChange() {
        if (sessionManager.isUserChanged()) {
            String currentUser = sessionManager.getUsername();
            String lastUser = sessionManager.getLastUsername();
            Log.d(TAG, "Detectado cambio de usuario: " + lastUser + " -> " + currentUser);

            clearPreviousUserData();
        } else {
            String username = sessionManager.getUsername();
            if (username.isEmpty()) {
                Log.e(TAG, "Error: No hay usuario autenticado");
                return;
            }

            if (NetworkUtils.isNetworkAvailable(context)) {
                Log.d(TAG, "Intentando cargar datos desde Firebase para usuario: " + username);
                loadFromFirebase();
            } else {
                Log.d(TAG, "Sin conexión a internet, cargando datos locales para usuario: " + username);
                loadUserData();
            }
        }
    }

    private void loadUserData() {
        String username = sessionManager.getUsername();
        if (username.isEmpty()) {
            Log.e(TAG, "Error: No hay usuario autenticado");
            return;
        }

        Log.d(TAG, "Cargando datos para usuario: " + username);
        loadBackupItemsForUser(username);
    }

    public void clearPreviousUserData() {
        String currentUser = sessionManager.getUsername();

        if (currentUser.isEmpty()) {
            Log.e(TAG, "Error: No hay usuario autenticado para limpiar datos previos");
            return;
        }

        disposables.add(
                backupRepository.deleteAllExceptUser(currentUser)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d(TAG, "Datos de usuarios anteriores eliminados correctamente");
                                    sessionManager.setLastUsername(currentUser);
                                    loadUserData();
                                },
                                error -> {
                                    Log.e(TAG, "Error al eliminar datos de usuarios anteriores", error);
                                    loadUserData();
                                }
                        )
        );
    }

    public void loadBackupItemsForUser(String username) {
        disposables.add(
                backupRepository.getAllBackupItemsForUser(username)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                items -> {
                                    Log.d(TAG, "Cargados " + items.size() + " elementos para el usuario: " + username);
                                    backupItems.setValue(items);
                                },
                                error -> Log.e(TAG, "Error loading backup items for user: " + username, error)
                        )
        );
    }


    public void loadBackupItems() {
        String username = sessionManager.getUsername();
        if (username.isEmpty()) {
            disposables.add(
                    backupRepository.getAllBackupItems()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    items -> backupItems.setValue(items),
                                    error -> Log.e(TAG, "Error loading backup items", error)
                            )
            );
        } else {
            loadBackupItemsForUser(username);
        }
    }

    public void loadFromFirebase() {
        Context context = deviceInfoHelper.getContext();
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "No hay conexión a internet. Cancelando sincronización.");
            syncResult.setValue(Resource.error("No hay conexión a internet", false));
            isSyncing = false;
            return;
        }

        String username = sessionManager.getUsername();

        if (username.isEmpty()) {
            Log.e(TAG, "Error: Intentando cargar datos sin un usuario autenticado");
            syncResult.setValue(Resource.error("No hay usuario autenticado", false));
            isSyncing = false;
            return;
        }

        disposables.add(
                backupRepository.getBackupFromFirebase(username)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                items -> {
                                    Log.d(TAG, "Datos cargados desde Firebase: " + items.size() + " elementos para usuario: " + username);
                                    checkAndSaveItemsLocally(items);
                                },
                                error -> {
                                    Log.e(TAG, "Error cargando desde Firebase", error);
                                    syncResult.setValue(Resource.error("Error cargando datos: " + error.getMessage(), false));
                                    isSyncing = false;
                                }
                        )
        );
    }

    private void checkAndSaveItemsLocally(List<BackupItem> newItems) {
        if (newItems.isEmpty()) {
            Log.d(TAG, "No hay elementos nuevos para guardar localmente");
            syncUploadToFirebase();
            return;
        }

        disposables.add(
                backupRepository.getAllBackupItems()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                localItems -> {
                                    Set<String> existingTags = new HashSet<>();
                                    for (BackupItem item : localItems) {
                                        existingTags.add(item.getEtiqueta1d());
                                    }

                                    List<BackupItem> itemsToAdd = new ArrayList<>();
                                    for (BackupItem newItem : newItems) {
                                        if (!existingTags.contains(newItem.getEtiqueta1d())) {
                                            itemsToAdd.add(newItem);
                                            Log.d(TAG, "Nuevo elemento para agregar: " + newItem.getEtiqueta1d());
                                        } else {
                                            Log.d(TAG, "Elemento ya existe localmente: " + newItem.getEtiqueta1d());
                                        }
                                    }

                                    saveFilteredItemsLocally(itemsToAdd);
                                },
                                error -> {
                                    Log.e(TAG, "Error obteniendo elementos locales", error);
                                    syncResult.setValue(Resource.error("Error en sincronización: " + error.getMessage(), false));
                                    isSyncing = false;
                                }
                        )
        );
    }

    private void saveFilteredItemsLocally(List<BackupItem> itemsToAdd) {
        if (itemsToAdd.isEmpty()) {
            Log.d(TAG, "No hay elementos nuevos para agregar localmente");
            syncUploadToFirebase();
            return;
        }

        String username = sessionManager.getUsername();
        Log.d(TAG, "Guardando " + itemsToAdd.size() + " elementos nuevos localmente para usuario: " + username);

        Completable completable = Completable.complete();

        for (BackupItem item : itemsToAdd) {
            completable = completable.andThen(backupRepository.saveBackupItem(item, username));
        }

        disposables.add(
                completable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d(TAG, "Elementos guardados localmente con éxito");
                                    loadBackupItems();
                                    syncUploadToFirebase();
                                },
                                error -> {
                                    Log.e(TAG, "Error guardando elementos localmente", error);
                                    syncResult.setValue(Resource.error("Error guardando elementos: " + error.getMessage(), false));
                                    isSyncing = false;
                                }
                        )
        );
    }

    private void syncUploadToFirebase() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "No hay conexión a internet para subida. Cancelando sincronización.");
            syncResult.setValue(Resource.error("No hay conexión a internet", false));
            isSyncing = false;
            return;
        }

        disposables.add(
                syncWithFirebase()
                        .timeout(20, TimeUnit.SECONDS)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    Log.d(TAG, "Sincronización completa");
                                    syncResult.setValue(Resource.success(true));
                                    isSyncing = false;
                                },
                                error -> {
                                    Log.e(TAG, "Error en sincronización", error);
                                    syncResult.setValue(Resource.error("Error en sincronización: " + error.getMessage(), false));
                                    isSyncing = false;
                                }
                        )
        );
    }

    public void processQrData(String qrData) {
        processQrResult.setValue(Resource.loading(null));
        String username = sessionManager.getUsername();

        disposables.add(
                backupRepository.validateQrData(qrData)
                        .flatMap(response -> {
                            if (response.isCorrect()) {
                                BackupItem item = DataParser.parseData(response.getData());
                                return backupRepository.saveBackupItem(item, username)
                                        .andThen(Single.just(item));
                            } else {
                                return Single.error(new IllegalArgumentException("Estructura QR incorrecta"));
                            }
                        })
                        .flatMap(item -> {
                            return backupRepository.getBackupCount()
                                    .map(count -> {
                                        boolean shouldSync = count % 5 == 0 && count > 0;
                                        Log.d(TAG, "Contador de elementos: " + count + ", Sincronizar: " + shouldSync);
                                        return new Object[]{item, shouldSync};
                                    });
                        })
                        .flatMap(result -> {
                            BackupItem item = (BackupItem) result[0];
                            boolean shouldSync = (boolean) result[1];

                            if (shouldSync) {
                                if (!NetworkUtils.isNetworkAvailable(context)) {
                                    Log.d(TAG, "No hay conexión a internet. Se procesará el QR sin sincronizar.");
                                    return Single.just(item.getEtiqueta1d() + " (sin sincronizar)");
                                }

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
                    if (items.isEmpty()) {
                        Log.d(TAG, "No hay elementos para sincronizar");
                        return Completable.complete();
                    }

                    String username = sessionManager.getUsername();

                    if (username.isEmpty()) {
                        Log.e(TAG, "Error: Intentando sincronizar sin un usuario autenticado");
                        return Completable.error(new IllegalStateException("No hay usuario autenticado"));
                    }

                    String currentDate = getCurrentDateAsString();
                    String deviceId = deviceInfoHelper.getDeviceId();
                    return backupRepository.syncWithFirebase(items, username, currentDate, deviceId)
                            .timeout(15, TimeUnit.SECONDS)
                            .doOnComplete(() -> Log.d(TAG, "Sincronización completada exitosamente para usuario: " + username))
                            .doOnError(e -> Log.e(TAG, "Error en sincronización", e));
                });
    }

    public void logout(boolean deleteBackup) {
        logoutResult.setValue(Resource.loading(null));
        String username = sessionManager.getUsername();

        if (deleteBackup && !username.isEmpty() && NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "Eliminando backup del usuario: " + username);
            disposables.add(
                    backupRepository.deleteBackup(username)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> {
                                        Log.d(TAG, "Backup eliminado exitosamente");
                                        sessionManager.clearLoginStatus();
                                        logoutResult.setValue(Resource.success(true));
                                    },
                                    error -> {
                                        Log.e(TAG, "Error al eliminar backup", error);
                                        sessionManager.clearLoginStatus();
                                        logoutResult.setValue(Resource.success(true));
                                    }
                            )
            );
        } else {
            Log.d(TAG, "Cerrando sesión sin eliminar backup");
            sessionManager.clearLoginStatus();
            logoutResult.setValue(Resource.success(true));
        }
    }

    public void logout() {
        logout(false);
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

    public LiveData<Resource<Boolean>> getSyncResult() {
        return syncResult;
    }

    public void syncAllItems() {
        if (isSyncing) {
            Log.d(TAG, "Ya hay una sincronización en progreso");
            return;
        }

        Context context = deviceInfoHelper.getContext();
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "No hay conexión a internet. Cancelando sincronización.");
            syncResult.setValue(Resource.error("No hay conexión a internet", false));
            return;
        }

        isSyncing = true;
        syncResult.setValue(Resource.loading(null));

        loadFromFirebase();
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}