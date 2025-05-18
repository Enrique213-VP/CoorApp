package com.svape.qr.coorapp.repository;

import com.google.firebase.firestore.FirebaseFirestore;
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
            Map<String, Object> data = new HashMap<>();
            data.put("items", items);
            data.put("deviceId", deviceId);
            data.put("date", date);

            firestore.collection("backup")
                    .document(deviceId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> emitter.onComplete())
                    .addOnFailureListener(emitter::onError);
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