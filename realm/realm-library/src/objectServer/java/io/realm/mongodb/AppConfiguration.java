/*
 * Copyright 2020 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.realm.mongodb;

import android.content.Context;

import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.IterableCodecProvider;
import org.bson.codecs.MapCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.realm.Realm;
import io.realm.annotations.Beta;
import io.realm.internal.Util;
import io.realm.internal.network.interceptor.LoggingInterceptor;
import io.realm.log.RealmLog;
import io.realm.mongodb.sync.SyncSession;

/**
 * A AppConfiguration is used to setup a MongoDB Realm application.
 * <p>
 * Instances of a AppConfiguration can only created by using the
 * {@link AppConfiguration.Builder} and calling its
 * {@link AppConfiguration.Builder#build()} method.
 * <p>
 * Configuring a App is only required if the default settings are not enough. Otherwise calling
 * {@code new App("app-id")} is sufficient.
 */
@Beta
public class AppConfiguration {

    /**
     * The default url for MongoDB Realm applications.
     *
     * @see Builder#baseUrl(String)
     */
    public final static String DEFAULT_BASE_URL = "https://realm.mongodb.com";

    /**
     * The default request timeout for network requests towards MongoDB Realm in seconds.
     *
     * @see Builder#requestTimeout(long, TimeUnit)
     */
    public final static long DEFAULT_REQUEST_TIMEOUT = 60;

    /**
     * The default header name used to carry authorization data when making network requests
     * towards MongoDB Realm.
     */
    public static final String DEFAULT_AUTHORIZATION_HEADER_NAME = "Authorization";

    /**
     * Default BSON codec registry for encoding/decoding arguments and results to/from MongoDB Realm backend.
     * <p>
     * This will encode/decode most primitive types, list and map types and BsonValues.
     *
     * @see AppConfiguration#getDefaultCodecRegistry()
     * @see AppConfiguration.Builder#codecRegistry(CodecRegistry)
     * @see ValueCodecProvider
     * @see BsonValueCodecProvider
     * @see IterableCodecProvider
     * @see MapCodecProvider
     * @see DocumentCodecProvider
     */
    public static final CodecRegistry DEFAULT_BSON_CODEC_REGISTRY = CodecRegistries.fromRegistries(
            CodecRegistries.fromProviders(
                    // For primitive support
                    new ValueCodecProvider(),
                    // For BSONValue support
                    new BsonValueCodecProvider(),
                    new DocumentCodecProvider(),
                    // For list support
                    new IterableCodecProvider(),
                    new MapCodecProvider()
            )
    );

    private final String appId;
    private final String appName;
    private final String appVersion;
    private final URL baseUrl;
    private final SyncSession.ErrorHandler defaultErrorHandler;
    @Nullable
    private final byte[] encryptionKey;
    private final long requestTimeoutMs;
    private final String authorizationHeaderName;
    private final Map<String, String> customHeaders;
    private final File syncRootDir; // Root directory for storing Sync related files
    private final CodecRegistry codecRegistry;
    private final LoggingInterceptor loggingInterceptor;

    private AppConfiguration(String appId,
                             String appName,
                             String appVersion,
                             URL baseUrl,
                             SyncSession.ErrorHandler defaultErrorHandler,
                             @Nullable byte[] encryptionKey,
                             long requestTimeoutMs,
                             String authorizationHeaderName,
                             Map<String, String> customHeaders,
                             File syncRootdir,
                             CodecRegistry codecRegistry,
                             LoggingInterceptor loggingInterceptor) {
        this.appId = appId;
        this.appName = appName;
        this.appVersion = appVersion;
        this.baseUrl = baseUrl;
        this.defaultErrorHandler = defaultErrorHandler;
        this.encryptionKey = (encryptionKey == null) ? null : Arrays.copyOf(encryptionKey, encryptionKey.length);
        this.requestTimeoutMs = requestTimeoutMs;
        this.authorizationHeaderName = (!Util.isEmptyString(authorizationHeaderName)) ? authorizationHeaderName : "Authorization";
        this.customHeaders = Collections.unmodifiableMap(customHeaders);
        this.syncRootDir = syncRootdir;
        this.codecRegistry = codecRegistry;
        this.loggingInterceptor = loggingInterceptor;
    }

    /**
     * Returns the unique app id that identities the Realm application.
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Returns the name used to describe the Realm application. This is only used as debug
     * information.
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Returns the version of this Realm application. This is only used as debug information.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Returns the base url for this Realm application.
     */
    public URL getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the encryption key, if any, that is used to encrypt Realm users meta data on this
     * device. If no key is returned, the data is not encrypted.
     */
    @Nullable
    public byte[] getEncryptionKey() {
        return encryptionKey == null ? null : Arrays.copyOf(encryptionKey, encryptionKey.length);
    }

    /**
     * Returns the default timeout for network requests against the Realm application in
     * milliseconds.
     */
    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    /**
     * Returns the name of the header used to carry authentication data when making network
     * requests towards MongoDB Realm.
     */
    public String getAuthorizationHeaderName() {
        return authorizationHeaderName;
    }

    /**
     * Returns any custom configured headers that will be sent alongside other headers when
     * making network requests towards MongoDB Realm.
     */
    public Map<String, String> getCustomRequestHeaders() {
        return customHeaders;
    }

    /**
     * Returns the default error handler used by synced Realms if there are problems with their
     * {@link SyncSession}.
     */
    public SyncSession.ErrorHandler getDefaultErrorHandler() {
        return defaultErrorHandler;
    }

    /**
     * Returns the root folder containing all files and Realms used when synchronizing data
     * between the device and MongoDB Realm.
     */
    public File getSyncRootDirectory() {
        return syncRootDir;
    }

    /**
     * Returns the default codec registry used to encode and decode BSON arguments and results when
     * calling remote Realm {@link io.realm.mongodb.functions.Functions} and accessing a remote
     * {@link io.realm.mongodb.mongo.MongoDatabase}.
     *
     * @return The default codec registry for the App.
     * @see #DEFAULT_BSON_CODEC_REGISTRY
     * @see Builder#getDefaultCodecRegistry()
     */
    public CodecRegistry getDefaultCodecRegistry() {
        return codecRegistry;
    }

    /**
     * Returns the {@link LoggingInterceptor} used in the app, which keeps sensitive information
     * from being displayed in the logcat.
     *
     * @return the logging interceptor.
     */
    public LoggingInterceptor getLoggingInterceptor() {
        return loggingInterceptor;
    }

    /**
     * Builder used to construct instances of a {@link AppConfiguration} in a fluent manner.
     */
    public static class Builder {

        private String appId;
        private String appName;
        private String appVersion;
        private URL baseUrl = createUrl(DEFAULT_BASE_URL);
        private SyncSession.ErrorHandler defaultErrorHandler = new SyncSession.ErrorHandler() {
            @Override
            public void onError(SyncSession session, AppException error) {
                if (error.getErrorCode() == ErrorCode.CLIENT_RESET) {
                    RealmLog.error("Client Reset required for: " + session.getConfiguration().getServerUrl());
                    return;
                }

                String errorMsg = String.format(Locale.US, "Session Error[%s]: %s",
                        session.getConfiguration().getServerUrl(),
                        error.toString());
                switch (error.getErrorCode().getCategory()) {
                    case FATAL:
                        RealmLog.error(errorMsg);
                        break;
                    case RECOVERABLE:
                        RealmLog.info(errorMsg);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported error category: " + error.getErrorCode().getCategory());
                }
            }
        };
        private byte[] encryptionKey;
        private long requestTimeoutMs = TimeUnit.MILLISECONDS.convert(DEFAULT_REQUEST_TIMEOUT, TimeUnit.SECONDS);
        private String authorizationHeaderName;
        private Map<String, String> customHeaders = new HashMap<>();
        private File syncRootDir;
        private CodecRegistry codecRegistry = DEFAULT_BSON_CODEC_REGISTRY;
        private LoggingInterceptor loggingInterceptor = LoggingInterceptor.interceptor(RegexObfuscatorPatternFactory.LOGIN_FEATURE);

        /**
         * Creates an instance of the Builder for the AppConfiguration.
         *
         * @param appId the application id of the MongoDB Realm Application.
         */
        public Builder(String appId) {
            Util.checkEmpty(appId, "appId");
            this.appId = appId;
            Context context = Realm.getApplicationContext();
            if (context == null) {
                throw new IllegalStateException("Call `Realm.init(Context)` before calling this method.");
            }
            File rootDir = new File(context.getFilesDir(), "mongodb-realm");
            if (!rootDir.exists() && !rootDir.mkdir()) {
                throw new IllegalStateException("Could not create Sync root dir: " + rootDir.getAbsolutePath());
            }
            syncRootDir = rootDir;
        }

        /**
         * Sets the encryption key used to encrypt user meta data only. Individual Realms needs to
         * use {@link io.realm.mongodb.sync.SyncConfiguration.Builder#encryptionKey(byte[])} to make them encrypted.
         *
         * @param key a 64 byte encryption key.
         * @throws IllegalArgumentException if the key is not 64 bytes long.
         */
        public Builder encryptionKey(byte[] key) {
            Util.checkNull(key, "key");
            if (key.length != Realm.ENCRYPTION_KEY_LENGTH) {
                throw new IllegalArgumentException(String.format(Locale.US,
                        "The provided key must be %s bytes. Yours was: %s",
                        Realm.ENCRYPTION_KEY_LENGTH, key.length));
            }
            this.encryptionKey = Arrays.copyOf(key, key.length);
            return this;
        }

        /**
         * Sets the base url for the MongoDB Realm Application. The default value is
         * {@link #DEFAULT_BASE_URL}.
         *
         * @param baseUrl the base url for the MongoDB Realm application.
         */
        public Builder baseUrl(String baseUrl) {
            Util.checkNull(baseUrl, "baseUrl");
            this.baseUrl = createUrl(baseUrl);
            return this;
        }

        /**
         * Sets the apps name. This is only used as part of debug headers sent when making
         * network requests at the MongoDB Realm application.
         *
         * @param appName app name used to identify the application.
         */
        public Builder appName(String appName) {
            Util.checkEmpty(appName, "appName");
            this.appName = appName;
            return this;
        }

        /**
         * Sets the apps version. This is only used as part of debug headers sent when making
         * network requests at the MongoDB Realm application.
         *
         * @param appVersion app version used to identify the application.
         */
        public Builder appVersion(String appVersion) {
            Util.checkEmpty(appVersion, "appVersion");
            this.appVersion = appVersion;
            return this;
        }

        /**
         * Sets the default timeout used by network requests against the MongoDB Realm application.
         * Requests will terminate with a failure if they exceed this limit. The default value is
         * {@link AppConfiguration#DEFAULT_REQUEST_TIMEOUT} seconds.
         *
         * @param time the timeout value for network requests.
         * @param unit the unit of time used to define the timeout.
         */
        public Builder requestTimeout(long time, TimeUnit unit) {
            if (time < 1) {
                throw new IllegalStateException("A timeout above 0 is required: " + time);
            }
            Util.checkNull(unit, "unit");
            this.requestTimeoutMs = TimeUnit.MICROSECONDS.convert(time, unit);
            return this;
        }

        /**
         * Sets the name of the HTTP header used to send authorization data in when making requests to
         * MongoDB Realm. The MongoDB server or firewall must have been configured to expect a
         * custom authorization header.
         * <p>
         * The default authorization header is named {@link #DEFAULT_AUTHORIZATION_HEADER_NAME}.
         *
         * @param headerName name of the header.
         * @throws IllegalArgumentException if a null or empty header is provided.
         * @see <a href="https://docs.realm.io/platform/guides/learn-realm-sync-and-integrate-with-a-proxy#adding-a-custom-proxy">Adding a custom proxy</a>
         */
        public Builder authorizationHeaderName(String headerName) {
            Util.checkEmpty(headerName, "headerName");
            this.authorizationHeaderName = headerName;
            return this;
        }

        /**
         * Adds an extra HTTP header to append to every request to a Realm Object Server.
         *
         * @param headerName  the name of the header.
         * @param headerValue the value of header.
         * @throws IllegalArgumentException if a non-empty {@code headerName} is provided or a null {@code headerValue}.
         */
        public Builder addCustomRequestHeader(String headerName, String headerValue) {
            Util.checkEmpty(headerName, "headerName");
            Util.checkNull(headerValue, "headerValue");
            customHeaders.put(headerName, headerValue);
            return this;
        }

        /**
         * Adds extra HTTP headers to append to every request to a Realm Object Server.
         *
         * @param headers map of (headerName, headerValue) pairs.
         * @throws IllegalArgumentException If any of the headers provided are illegal.
         */
        public Builder addCustomRequestHeaders(@Nullable Map<String, String> headers) {
            if (headers != null) {
                customHeaders.putAll(headers);
            }
            return this;
        }

        /**
         * Sets the default error handler used by Synced Realms when reporting errors with their
         * session.
         * <p>
         * This default can be overridden by calling
         * {@link io.realm.mongodb.sync.SyncConfiguration.Builder#errorHandler(SyncSession.ErrorHandler)} when creating
         * the {@link io.realm.mongodb.sync.SyncConfiguration}.
         *
         * @param errorHandler the default error handler.
         */
        public Builder defaultSyncErrorHandler(SyncSession.ErrorHandler errorHandler) {
            Util.checkNull(errorHandler, "errorHandler");
            defaultErrorHandler = errorHandler;
            return this;
        }

        /**
         * Configures the root folder containing all files and Realms used when synchronizing data
         * between the device and MongoDB Realm.
         * <p>
         * The default root dir is {@code Context.getFilesDir()/mongodb-realm}.
         *
         * @param rootDir where to store sync related files.
         */
        public Builder syncRootDirectory(File rootDir) {
            Util.checkNull(rootDir, "rootDir");
            if (rootDir.isFile()) {
                throw new IllegalArgumentException("'rootDir' is a file, not a directory: " +
                        rootDir.getAbsolutePath() + ".");
            }
            if (!rootDir.exists() && !rootDir.mkdirs()) {
                throw new IllegalArgumentException("Could not create the specified directory: " +
                        rootDir.getAbsolutePath() + ".");
            }
            if (!rootDir.canWrite()) {
                throw new IllegalArgumentException("Realm directory is not writable: " +
                        rootDir.getAbsolutePath() + ".");
            }
            syncRootDir = rootDir;
            return this;
        }

        private URL createUrl(String baseUrl) {
            try {
                return new URL(baseUrl);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(baseUrl);
            }
        }

        /**
         * Set the default codec registry used to encode and decode BSON arguments and results when
         * calling remote Realm {@link io.realm.mongodb.functions.Functions} and accessing a remote
         * {@link io.realm.mongodb.mongo.MongoDatabase}.
         * <p>
         * Will default to {@link #DEFAULT_BSON_CODEC_REGISTRY} if not specified.
         *
         * @param codecRegistry The default codec registry for the App.
         * @see #DEFAULT_BSON_CODEC_REGISTRY
         * @see Builder#getDefaultCodecRegistry()
         */
        public Builder codecRegistry(CodecRegistry codecRegistry) {
            Util.checkNull(codecRegistry, "codecRegistry");
            this.codecRegistry = codecRegistry;
            return this;
        }

        /**
         * Sets the {@link LoggingInterceptor} used to keep sensitive data from being displayed in
         * the logcat.
         *
         * @param loggingInterceptor The default login interceptor for the app.
         */
        public Builder loggingInterceptor(LoggingInterceptor loggingInterceptor) {
            Util.checkNull(loggingInterceptor, "loggingInterceptor");
            this.loggingInterceptor = loggingInterceptor;
            return this;
        }

        /**
         * Creates the AppConfiguration.
         *
         * @return the AppConfiguration that can be used to create a {@link App}.
         */
        public AppConfiguration build() {
            return new AppConfiguration(appId,
                    appName,
                    appVersion,
                    baseUrl,
                    defaultErrorHandler,
                    encryptionKey,
                    requestTimeoutMs,
                    authorizationHeaderName,
                    customHeaders,
                    syncRootDir,
                    codecRegistry,
                    loggingInterceptor);
        }
    }
}
