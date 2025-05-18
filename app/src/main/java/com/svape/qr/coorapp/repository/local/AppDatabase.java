package com.svape.qr.coorapp.repository.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {BackupItemEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BackupDao backupDao();
}