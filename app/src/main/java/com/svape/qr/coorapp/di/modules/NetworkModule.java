package com.svape.qr.coorapp.di.modules;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.svape.qr.coorapp.service.ApiService;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;

@Module
public class NetworkModule {

    @Provides
    @Singleton
    RequestQueue provideRequestQueue(Context context) {
        return Volley.newRequestQueue(context);
    }

    @Provides
    @Singleton
    ApiService provideApiService(RequestQueue requestQueue) {
        return new ApiService(requestQueue);
    }
}