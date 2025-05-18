package com.svape.qr.coorapp.di.modules;

import android.app.Application;
import android.content.Context;
import androidx.room.Room;
import com.svape.qr.coorapp.repository.local.AppDatabase;
import com.svape.qr.coorapp.util.DeviceInfoHelper;
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
        return Room.databaseBuilder(context, AppDatabase.class, "app_database")
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    DeviceInfoHelper provideDeviceInfoHelper(Context context) {
        return new DeviceInfoHelper(context);
    }
}