package com.svape.qr.coorapp.repository;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.svape.qr.coorapp.model.BackupItem;
import com.svape.qr.coorapp.model.ApiResponse;
import com.svape.qr.coorapp.repository.local.AppDatabase;
import com.svape.qr.coorapp.repository.local.BackupItemEntity;
import com.svape.qr.coorapp.service.ApiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public Completable saveBackupItem(BackupItem item, String username) {
        BackupItemEntity entity = new BackupItemEntity(
                item.getEtiqueta1d(),
                item.getLatitud(),
                item.getLongitud(),
                item.getObservacion(),
                username
        );
        return database.backupDao().insert(entity);
    }

    public Completable saveBackupItem(BackupItem item) {
        return saveBackupItem(item, "");
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

    public Single<List<BackupItem>> getAllBackupItemsForUser(String username) {
        return database.backupDao().getAllForUser(username)
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

    public Completable syncWithFirebase(List<BackupItem> items, String username, String date) {
        return Completable.create(emitter -> {
            if (items.isEmpty()) {
                Log.d(TAG, "No hay elementos para sincronizar con Firebase");
                emitter.onComplete();
                return;
            }

            firestore.collection("userBackups")
                    .document(username)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        List<BackupItem> allItems = new ArrayList<>(items);

                        if (documentSnapshot.exists() && documentSnapshot.contains("items")) {
                            List<Map<String, Object>> existingItemsMap =
                                    (List<Map<String, Object>>) documentSnapshot.get("items");

                            if (existingItemsMap != null && !existingItemsMap.isEmpty()) {
                                Log.d(TAG, "Encontrados " + existingItemsMap.size() +
                                        " elementos existentes en Firebase");

                                Set<String> existingTags = new HashSet<>();

                                for (BackupItem item : items) {
                                    existingTags.add(item.getEtiqueta1d());
                                }

                                for (Map<String, Object> map : existingItemsMap) {
                                    String etiqueta = (String) map.get("etiqueta1d");

                                    if (etiqueta != null && !existingTags.contains(etiqueta)) {
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

                                        BackupItem existingItem = new BackupItem(etiqueta, latitud, longitud, observacion);
                                        allItems.add(existingItem);
                                        Log.d(TAG, "Agregado elemento existente con etiqueta: " + etiqueta);
                                    }
                                }
                            }
                        }

                       Map<String, Object> data = new HashMap<>();
                        data.put("items", allItems);
                        data.put("username", username);
                        data.put("date", date);
                        data.put("timestamp", System.currentTimeMillis());

                        Log.d(TAG, "Sincronizando total de " + allItems.size() +
                                " elementos a Firebase para usuario: " + username);

                        firestore.collection("userBackups")
                                .document(username)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Datos sincronizados exitosamente a Firebase");
                                    emitter.onComplete();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error sincronizando con Firebase", e);
                                    emitter.onError(e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error verificando documento antes de sincronizar", e);
                        emitter.onError(e);
                    });
        });
    }

    public Single<List<BackupItem>> getBackupFromFirebase(String username) {
        return Single.create(emitter -> {
            Log.d(TAG, "Intentando recuperar datos desde Firebase para usuario: " + username);

            firestore.collection("userBackups")
                    .document(username)
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

/*    public Completable deleteBackup(String username) {
        return Completable.create(emitter -> {
            firestore.collection("userBackups")
                    .document(username)
                    .delete()
                    .addOnSuccessListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(emitter::onError);
        });
    }*/


    public Completable deleteAllExceptUser(String username) {
        return database.backupDao().deleteAllExceptUser(username);
    }

    public Completable deleteAllLocalBackup() {
        return database.backupDao().deleteAll();
    }

}