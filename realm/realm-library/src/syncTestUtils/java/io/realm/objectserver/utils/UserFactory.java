/*
 * Copyright 2016 Realm Inc.
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

package io.realm.objectserver.utils;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmApp;
import io.realm.RealmCredentials;
import io.realm.RealmUser;
import io.realm.RealmConfiguration;
import io.realm.TestHelper;
import io.realm.log.RealmLog;


// Helper class to retrieve users with same IDs even in multi-processes.
// Must be in `io.realm.objectserver` to work around package protected methods.
// This require Realm.init() to be called before using this class.
public class UserFactory {
    public static final String PASSWORD = "myPassw0rd";
    // Since the integration tests need to use the same user for different processes, we create a new user name when the
    // test starts and store it in a Realm. Then it can be retrieved for every process.
    private String userName;
    private static UserFactory instance;
    private static RealmConfiguration configuration;
    private static RealmApp app;

    // Run initializer here to make it possible to ensure that Realm.init has been called.
    // It is unpredictable when the static initializer is running
    private static synchronized void initFactory(boolean forceReset) {
        if (configuration == null || forceReset) {
            RealmConfiguration.Builder builder = new RealmConfiguration.Builder().name("user-factory.realm");
            configuration = builder.build();
            app = new RealmApp(Constants.APP_ID);
        }
    }

    private UserFactory(String userName) {
        this.userName = userName;
    }

    public RealmUser loginWithDefaultUser() {
        RealmCredentials credentials = RealmCredentials.emailPassword(userName, PASSWORD);
        return app.login(credentials);
    }

    public static RealmUser createUniqueUser() {
        String uniqueName = UUID.randomUUID().toString();
        return createUser(uniqueName);
    }

    private static RealmUser createUser(String username) {
        return null; // FIXME
//        RealmCredentials credentials = RealmCredentials.emailPassword(username, PASSWORD, true);
//        return app.login(credentials);
    }

    public RealmUser createDefaultUser() {
        return null; // FIXME
//        RealmCredentials credentials = RealmCredentials.emailPassword(userName, PASSWORD, true);
//        return app.login(credentials);
    }

    public static RealmUser createAdminUser() {
        return null; //FIXME
//        // `admin` required as user identifier to be granted admin rights.
//        // ROS 2.0 comes with a default admin user named "realm-admin" with password "".
//        RealmCredentials credentials = RealmCredentials.emailPassword("realm-admin", "", false);
//        return app.login(credentials);
    }

    // Since we don't have a reliable way to reset the sync server and client, just use a new user factory for every
    // test case.
    public static void resetInstance() {
        initFactory(true);
        Realm realm = Realm.getInstance(configuration);
        UserFactoryStore store = realm.where(UserFactoryStore.class).findFirst();
        realm.beginTransaction();
        if (store == null) {
            store = realm.createObject(UserFactoryStore.class);
        }
        store.setUserName(UUID.randomUUID().toString());
        realm.commitTransaction();
        realm.close();
        instance = null;
    }

    // The @Before method will be called before the looper tests finished. We need to find a better place to call this.
    public static void clearInstance()  {
        Realm realm = Realm.getInstance(configuration);
        realm.beginTransaction();
        realm.delete(UserFactoryStore.class);
        realm.commitTransaction();
        realm.close();
    }

    public static synchronized UserFactory getInstance() {
        if (instance == null)  {
            initFactory(false);
            Realm realm = Realm.getInstance(configuration);
            UserFactoryStore store = realm.where(UserFactoryStore.class).findFirst();
            if (store == null || store.getUserName() == null) {
                throw new IllegalStateException("Current user has not been set. Call resetInstance() first.");
            }

            instance = new UserFactory(store.getUserName());
            realm.close();
        }
        RealmLog.debug("UserFactory.getInstance, the default user is " + instance.userName + " .");
        return instance;
    }

    /**
     * Blocking call that logs out all users
     */
    public static void logoutAllUsers() {
        final CountDownLatch allUsersLoggedOut = new CountDownLatch(1);
        final HandlerThread ht = new HandlerThread("LoggingOutUsersThread");
        ht.start();
        Handler handler = new Handler(ht.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
//                Map<String, RealmUser> users = RealmApp.allUsers();
//                for (RealmUser user : users.values()) {
//                    RealmApp.logout(user);
//                }
                TestHelper.waitForNetworkThreadExecutorToFinish();
                allUsersLoggedOut.countDown();
            }
        });
        TestHelper.awaitOrFail(allUsersLoggedOut);
        ht.quit();
        try {
            ht.join(TimeUnit.SECONDS.toMillis(TestHelper.SHORT_WAIT_SECS));
        } catch (InterruptedException e) {
            throw new AssertionError("LoggingOutUsersThread failed to finish in time");
        }
    }
}
