package dev.nick.android.injection;

import android.content.Context;
import android.support.annotation.NonNull;

public abstract class Injector {

    public static void init(@NonNull Context applicationContext) {
        InjectorService.init(applicationContext);
    }

    public static void inject(@NonNull Object target) {
        InjectorService.shared().inject(target);
    }
}
