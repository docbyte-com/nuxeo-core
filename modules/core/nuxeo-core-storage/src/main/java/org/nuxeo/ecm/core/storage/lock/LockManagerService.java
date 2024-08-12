/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.lock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.lock.LockManager;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service holding the registered lock managers.
 * <p>
 * Actual instantiation is done by storage backends.
 *
 * @since 6.0
 */
public class LockManagerService extends DefaultComponent {

    private static final String XP_LOCKMANAGER = "lockmanager";

    protected Map<String, LockManager> lockManagers = new ConcurrentHashMap<>();

    /**
     * Returns the lock manager registered with the given name.
     * <p>
     * Lazily constructs it if needed.
     *
     * @param name the lock manager name
     * @return the lock manager, or {@code null} if none is registered
     * @since 6.0
     */
    public synchronized LockManager getLockManager(String name) {
        LockManager lockManager = lockManagers.get(name);
        if (lockManager == null) {
            LockManagerDescriptor descriptor = getDescriptor(XP_LOCKMANAGER, name);
            if (descriptor == null) {
                return null;
            }
            try {
                Constructor<? extends LockManager> ctor = descriptor.klass.getConstructor(String.class);
                lockManager = ctor.newInstance(name);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new NuxeoException(e);
            }
            registerLockManager(name, lockManager);
        }
        return lockManager;
    }

    public void registerLockManager(String name, LockManager lockManager) {
        lockManagers.put(name, lockManager);
    }

    /**
     * @since 7.4
     */
    public void unregisterLockManager(String name) {
        lockManagers.remove(name);
    }

}
