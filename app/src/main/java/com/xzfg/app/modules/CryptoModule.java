package com.xzfg.app.modules;

import com.xzfg.app.security.Crypto;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        complete = false,
        library = true
)
public class CryptoModule {
    @Provides
    @Singleton
    public Crypto getCrypto() {
        return new Crypto();
    }
}
