package com.xzfg.app.modules;

import com.xzfg.app.security.Crypto;

import java.security.Security;

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
public class ProEnhancedCryptoModule {

    public ProEnhancedCryptoModule() {}

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    public Crypto getCrypto() {
        if (Security.getProvider("SC") == null) {
            Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        }
        return new Crypto("AES/CTR/NoPadding",16,128);
    }
}
