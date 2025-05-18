package com.svape.qr.coorapp.di.modules;

import com.google.firebase.firestore.FirebaseFirestore;
import com.svape.qr.coorapp.repository.BackupRepository;
import com.svape.qr.coorapp.repository.UserRepository;
import com.svape.qr.coorapp.repository.local.AppDatabase;
import com.svape.qr.coorapp.service.ApiService;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    @Provides
    @Singleton
    FirebaseFirestore provideFirebaseFirestore() {
        return FirebaseFirestore.getInstance();
    }

    @Provides
    @Singleton
    UserRepository provideUserRepository(FirebaseFirestore firebaseFirestore) {
        return new UserRepository(firebaseFirestore);
    }

    @Provides
    @Singleton
    BackupRepository provideBackupRepository(
            AppDatabase database,
            FirebaseFirestore firebaseFirestore,
            ApiService apiService) {
        return new BackupRepository(database, firebaseFirestore, apiService);
    }
}