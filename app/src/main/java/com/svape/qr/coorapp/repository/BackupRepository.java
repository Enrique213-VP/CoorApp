package com.svape.qr.coorapp.repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.svape.qr.coorapp.model.BackupItem;
import com.svape.qr.coorapp.model.ApiResponse;
import com.svape.qr.coorapp.repository.local.AppDatabase;
import com.svape.qr.coorapp.repository.local.BackupItemEntity;
import com.svape.qr.coorapp.service.ApiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public class BackupRepository {
    private static final String TAG = "BackupRepository";
    private final AppDatabase database;
    private final FirebaseFirestore firestore;
    private final ApiService apiService;

    public BackupRepository(AppDatabase database, FirebaseFirestore firestore, ApiService apiService) {
        this.database = database;
        this.firestore = firestore;
        this.apiService = apiService;
    }

    public Single<ApiResponse> validateQrData(String base64Data) {
        return apiService.validateData(base64Data);
    }

    public Completable saveBackupItem(BackupItem item) {
        BackupItemEntity entity = new BackupItemEntity(
                item.getEtiqueta1d(),
                item.getLatitud(),
                item.getLongitud(),
                item.getObservacion()
        );
        return database.backupDao().insert(entity);
    }

    public Single<List<BackupItem>> getAllBackupItems() {
        return database.backupDao().getAll()
                .map(entities -> {
                    List<BackupItem> items = new ArrayList<>();
                    for (BackupItemEntity entity : entities) {
                        items.add(new BackupItem(
                                entity.getEtiqueta1d(),
                                entity.getLatitud(),
                                entity.getLongitud(),
                                entity.getObservacion()
                        ));
                    }
                    return items;
                });
    }

    public Single<Integer> getBackupCount() {
        return database.backupDao().getCount();
    }

    public Completable syncWithFirebase(List<BackupItem> items, String deviceId, String date) {
        return Completable.create(emitter -> {
            if (items.isEmpty()) {
                Log.d(TAG, "No hay elementos para sincronizar con Firebase");
                emitter.onComplete();
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            data.put("deviceId", deviceId);
            data.put("date", date);
            data.put("timestamp", System.currentTimeMillis());

            Log.d(TAG, "Sincronizando " + items.size() + " elementos a Firebase");

            firestore.collection("backup")
                    .document(deviceId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Datos sincronizados exitosamente a Firebase");
                        emitter.onComplete();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error sincronizando con Firebase", e);
                        emitter.onError(e);
                    });
        });
    }

    public Single<List<BackupItem>> getBackupFromFirebase(String deviceId) {
        return Single.create(emitter -> {
            Log.d(TAG, "Intentando recuperar datos desde Firebase para dispositivo: " + deviceId);

            firestore.collection("backup")
                    .document(deviceId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("items")) {
                            try {
                                List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) documentSnapshot.get("items");
                                List<BackupItem> items = new ArrayList<>();

                                if (itemsMap != null) {
                                    Log.d(TAG, "Recuperados " + itemsMap.size() + " elementos de Firebase");

                                    for (Map<String, Object> map : itemsMap) {
                                        String etiqueta = (String) map.get("etiqueta1d");
                                        double latitud = 0;
                                        double longitud = 0;
                                        String observacion = (String) map.get("observacion");

                                        if (map.get("latitud") instanceof Double) {
                                            latitud = (Double) map.get("latitud");
                                        } else if (map.get("latitud") instanceof Long) {
                                            latitud = ((Long) map.get("latitud")).doubleValue();
                                        } else if (map.get("latitud") instanceof Number) {
                                            latitud = ((Number) map.get("latitud")).doubleValue();
                                        }

                                        if (map.get("longitud") instanceof Double) {
                                            longitud = (Double) map.get("longitud");
                                        } else if (map.get("longitud") instanceof Long) {
                                            longitud = ((Long) map.get("longitud")).doubleValue();
                                        } else if (map.get("longitud") instanceof Number) {
                                            longitud = ((Number) map.get("longitud")).doubleValue();
                                        }

                                        if (etiqueta != null) {
                                            BackupItem item = new BackupItem(etiqueta, latitud, longitud, observacion);
                                            items.add(item);
                                            Log.d(TAG, "Item recuperado: " + etiqueta);
                                        }
                                    }

                                    emitter.onSuccess(items);
                                } else {
                                    Log.d(TAG, "No se encontraron items en Firebase");
                                    emitter.onSuccess(new ArrayList<>());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parseando datos de Firebase", e);
                                emitter.onError(e);
                            }
                        } else {
                            Log.d(TAG, "No se encontró documento en Firebase o no contiene items");
                            emitter.onSuccess(new ArrayList<>());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error recuperando datos de Firebase", e);
                        emitter.onError(e);
                    });
        });
    }

    public Completable deleteBackup(String deviceId) {
        return Completable.create(emitter -> {
            firestore.collection("backup")
                    .document(deviceId)
                    .delete()
                    .addOnSuccessListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(emitter::onError);
        });
    }

    public Completable deleteAllLocalBackup() {
        return database.backupDao().deleteAll();
    }
}