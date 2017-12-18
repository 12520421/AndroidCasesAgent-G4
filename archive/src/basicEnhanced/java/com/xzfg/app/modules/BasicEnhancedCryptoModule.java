package com.xzfg.app.modules;

import com.xzfg.app.security.Crypto;

import java.security.Security;

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
public class BasicEnhancedCryptoModule {
    @Provides
    @Singleton
    public Crypto getCrypto() {
        if (Security.getProvider("SC") == null) {
            Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        }
        return new Crypto("DESede/CTR/NoPadding",8,192);
    }
}
