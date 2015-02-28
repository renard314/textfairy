package com.renard;

import android.os.Build;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;

public class CustomRobolectricRunner extends RobolectricTestRunner {
    public CustomRobolectricRunner(Class<?> testClass)
            throws InitializationError {
        super(testClass);
    }

    @Override
    protected AndroidManifest getAppManifest(Config config) {
        String path = "app/src/main/AndroidManifest.xml";
        // android studio has a different execution root for tests than pure gradle
        // so we avoid here manual effort to get them running inside android studio
        File file = new File(path);
        if (!file.exists()) {
            path = "AndroidManifest.xml";
        }

        config = overwriteConfig(config, "manifest", path);
        return super.getAppManifest(config);
    }

    protected Config.Implementation overwriteConfig(
            Config config, String key, String value) {
        Properties properties = new Properties();
        properties.setProperty(key, value);
        return new Config.Implementation(config, Config.Implementation.fromProperties(properties));
    }
}