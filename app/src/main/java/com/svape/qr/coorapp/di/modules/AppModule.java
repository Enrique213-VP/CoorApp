package com.svape.qr.coorapp.di.modules;

import android.app.Application;
import android.content.Context;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.svape.qr.coorapp.repository.local.AppDatabase;
import com.svape.qr.coorapp.util.DeviceInfoHelper;
import com.svape.qr.coorapp.util.SessionManager;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

    @Provides
    @Singleton
    Context provideContext(Application application) {
        return application;
    }

    @Provides
    @Singleton
    AppDatabase provideDatabase(Context context) {
        Migration MIGRATION_1_2 = new Migration(1, 2) {
            @Override
            public void migrate(SupportSQLiteDatabase database) {
                database.execSQL("ALTER TABLE backup_local ADD COLUMN username TEXT");

                database.execSQL("UPDATE backup_local SET username = 'legacy_user' WHERE username IS NULL");
            }
        };

        return Room.databaseBuilder(context, AppDatabase.class, "app_database")
                .addMigrations(MIGRATION_1_2)
                .build();
    }

    @Provides
    @Singleton
    DeviceInfoHelper provideDeviceInfoHelper(Context context) {
        return new DeviceInfoHelper(context);
    }

    @Provides
    @Singleton
    SessionManager provideSessionManager(Context context) {
        return new SessionManager(context);
    }
}