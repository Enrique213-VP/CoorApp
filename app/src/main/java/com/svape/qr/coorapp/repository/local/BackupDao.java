package com.svape.qr.coorapp.repository.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface BackupDao {
    @Insert
    Completable insert(BackupItemEntity item);

    @Query("SELECT * FROM backup_local")
    Single<List<BackupItemEntity>> getAll();

    @Query("SELECT * FROM backup_local WHERE username = :username")
    Single<List<BackupItemEntity>> getAllForUser(String username);

    @Query("SELECT COUNT(*) FROM backup_local")
    Single<Integer> getCount();

    @Query("DELETE FROM backup_local")
    Completable deleteAll();

    @Query("DELETE FROM backup_local WHERE username != :username")
    Completable deleteAllExceptUser(String username);
}