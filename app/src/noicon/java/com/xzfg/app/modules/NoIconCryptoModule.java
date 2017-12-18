package com.xzfg.app.modules;

import com.xzfg.app.security.Crypto;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Provides a non-encrypting encryption engine.
 */
@Module(
        library=true,
        overrides=true,
        complete=false
)
public class NoIconCryptoModule {
    @Provides
    @Singleton
    @SuppressWarnings("unused")
    public Crypto getCrypto() {
        return new Crypto();
    }
}
