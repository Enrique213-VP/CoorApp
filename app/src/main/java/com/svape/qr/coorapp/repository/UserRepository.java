package com.svape.qr.coorapp.repository;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;

public class UserRepository {
    private final FirebaseFirestore firestore;

    public UserRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Single<Boolean> validateUser(String username, String password) {
        return Single.create(emitter -> {
            firestore.collection("Usuarios")
                    .whereEqualTo("username", username)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        boolean isValid = !queryDocumentSnapshots.isEmpty();
                        emitter.onSuccess(isValid);
                    })
                    .addOnFailureListener(e -> {
                        emitter.onError(e);
                    });
        });
    }

    public Single<Boolean> checkIfUsernameExists(String username) {
        return Single.create(emitter -> {
            firestore.collection("Usuarios")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        boolean exists = !queryDocumentSnapshots.isEmpty();
                        emitter.onSuccess(exists);
                    })
                    .addOnFailureListener(e -> {
                        emitter.onError(e);
                    });
        });
    }

    public Single<Boolean> registerUser(String username, String password) {
        return Single.create(emitter -> {
            Map<String, Object> user = new HashMap<>();
            user.put("username", username);
            user.put("password", password);

            firestore.collection("Usuarios")
                    .add(user)
                    .addOnSuccessListener(documentReference -> {
                        emitter.onSuccess(true);
                    })
                    .addOnFailureListener(e -> {
                        emitter.onError(e);
                    });
        });
    }
}