package com.xzfg.app;

import com.xzfg.app.modules.AppModule;
import com.xzfg.app.modules.BasicCryptoModule;

/**
 * Provides the list of modules to be injected.
 */
final class Modules {
    static Object[] list(Application app) {
        return new Object[] {
                new AppModule(app),
                new BasicCryptoModule()
        };
    }

    private Modules() {

    }
}