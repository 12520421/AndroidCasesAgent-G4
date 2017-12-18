package com.xzfg.app.modules;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import com.xzfg.app.Application;
import com.xzfg.app.BuildConfig;
import com.xzfg.app.R;
import com.xzfg.app.util.CustomTrustManager;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import timber.log.Timber;

@Module(
        complete = false,
        library = true
)
public class NetworkModule {


    private static final char[] password = "7152989333117865".toCharArray();

    // have dagger create and load the keystore if SSL pinning is enabled.
    @Provides
    @Singleton
    public KeyStore getKeyStore(Application application) {
        KeyStore keyStore = null;
        if (BuildConfig.SSL_PINNING) {
            InputStream resourceStream = null;
            try {
                resourceStream = application.getResources().openRawResource(R.raw.keystore);
                keyStore = KeyStore.getInstance("BKS");
                keyStore.load(resourceStream, password);

            }
            catch (Exception e) {
                throw new RuntimeException("SSL Pinning Requested, but couldn't load keystore",e);
            }
            finally {
                // ensure that the resource stream is closed.
                try {
                    if (resourceStream != null) {
                        resourceStream.close();
                    }
                }
                catch (Exception e) {
                    Timber.w(e,"Couldn't close resource stream.");
                }
            }
        }
        return keyStore;
    }

    // have dagger create the trust manager if ssl pinning is enabled.
    @Provides
    @Singleton
    public X509TrustManager getTrustManager(KeyStore keyStore) {
        if (BuildConfig.SSL_PINNING) {
            try {
                return new CustomTrustManager(keyStore);
            }
            catch (Exception e) {
                throw new RuntimeException("SSL Pinning Requested, but couldn't configure trust manager");
            }
        }
        return null;
    }

    // have dagger provide us with an SSLSocketFactory.
    @Provides
    @Singleton
    public SSLSocketFactory getSSLSocketFactory(KeyStore keyStore, X509TrustManager trustManager) {
        if (!BuildConfig.SSL_PINNING) {
            return (SSLSocketFactory)SSLSocketFactory.getDefault();
        }

        try {
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);

            // create and initialize the ssl context
            final SSLContext pinningSSLContext = SSLContext.getInstance("TLS");
            pinningSSLContext.init(kmf.getKeyManagers(), new TrustManager[]{trustManager}, null);

            return pinningSSLContext.getSocketFactory();
        }
        catch (Exception e) {
            throw new RuntimeException("SSL Pinning Requested, but couldn't setup SSL Context");
        }

    }


    @Provides
    @Singleton
    public OkHttpClient getHttpClient(SSLSocketFactory socketFactory, X509TrustManager trustManager) {

        // don't use keepAlive, since we're not doing real http anyway.
        System.setProperty("http.keepAlive", "false");

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(60, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);

        // pin SSL certificates
        if (BuildConfig.SSL_PINNING) {
            //noinspection ConstantConditions
            builder.hostnameVerifier(null);
            /*
                hostnameVerifier is supposed to be @NotNull - but the old HttpClientBuilder had
                null as the hardcoded constant?!? This implementation does no host name
                verification, and should probably be used instead of null.

                    new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    }
            */

            builder.sslSocketFactory(socketFactory, trustManager);
        }

        if (BuildConfig.DEBUG) {
            builder.addNetworkInterceptor(new StethoInterceptor());

            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    Timber.tag("OkHttp").d(message);
                }
            });
            httpLoggingInterceptor.setLevel(Level.HEADERS);
            builder.addInterceptor(httpLoggingInterceptor);

        }

        return builder.build();
    }

    @Provides
    @Singleton
    Picasso providePicasso(Application application, OkHttpClient okHttpClient) {
        Picasso.Builder builder = new Picasso.Builder(application);

        if (BuildConfig.DEBUG) {
            builder.indicatorsEnabled(true);
        }

        builder.downloader(new OkHttp3Downloader(okHttpClient));

        return builder.build();
    }

}
