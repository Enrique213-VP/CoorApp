package com.svape.qr.coorapp.di;

import android.app.Application;
import com.svape.qr.coorapp.di.modules.AppModule;
import com.svape.qr.coorapp.di.modules.NetworkModule;
import com.svape.qr.coorapp.di.modules.RepositoryModule;
import com.svape.qr.coorapp.di.modules.ViewModelModule;
import com.svape.qr.coorapp.ui.login.LoginActivity;
import com.svape.qr.coorapp.ui.main.MainActivity;
import com.svape.qr.coorapp.ui.map.MapActivity;
import com.svape.qr.coorapp.ui.register.RegisterActivity;
import com.svape.qr.coorapp.ui.splash.SplashActivity;
import javax.inject.Singleton;
import dagger.BindsInstance;
import dagger.Component;

@Singleton
@Component(modules = {
        AppModule.class,
        NetworkModule.class,
        RepositoryModule.class,
        ViewModelModule.class
})
public interface AppComponent {
    void inject(LoginActivity loginActivity);
    void inject(MainActivity mainActivity);
    void inject(RegisterActivity registerActivity);
    void inject(MapActivity mapActivity);
    void inject(SplashActivity splashActivity);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);

        AppComponent build();
    }
}