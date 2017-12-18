package com.xzfg.app.modules;

import com.xzfg.app.security.Crypto;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Provides the DESede (3DES) encryption engine.
 */
@Module(
        library=true,
        overrides=true,
        complete=false
)
public class BasicCryptoModule {
    @Provides
    @Singleton
    public Crypto getCrypto() {
        return new Crypto("DESede/CTR/NoPadding",8,192);
    }
}
