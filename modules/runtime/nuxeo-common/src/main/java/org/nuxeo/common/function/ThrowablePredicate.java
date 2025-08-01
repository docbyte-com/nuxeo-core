/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.function;

import java.util.function.Predicate;

/**
 * @param <T> the type of the input to the predicate
 * @param <E> the type of exception to throw
 * @since 2025.0
 */
@FunctionalInterface
public interface ThrowablePredicate<T, E extends Throwable> {

    boolean test(T t) throws E;

    /**
     * @return this {@link ThrowablePredicate} as a {@link Predicate} throwing the checked exception as an unchecked one
     */
    default Predicate<T> toPredicate() {
        return asPredicate(this);
    }

    /**
     * @return the given {@link ThrowablePredicate} as a {@link Predicate} throwing the checked exception as an
     *         unchecked one
     */
    static <T, R, E extends Throwable> Predicate<T> asPredicate(ThrowablePredicate<T, E> throwablePredicate) {
        return arg -> {
            try {
                return throwablePredicate.test(arg);
            } catch (Throwable t) { // NOSONAR
                return FunctionUtils.sneakyThrow(t); // will never return
            }
        };
    }

}
