package com.xzfg.app.modules;

import com.xzfg.app.security.Crypto;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Provides the AES-128 encryption engine.
 */
@Module(
        library=true,
        overrides=true,
        complete=false
)
public class ProCryptoModule {

    public ProCryptoModule() {}

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    public Crypto getCrypto() {
        return new Crypto("AES/CTR/NoPadding",16,128);
    }
}
