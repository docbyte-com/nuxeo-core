/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *      Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.common.function;

import java.util.function.BiFunction;

/**
 * @param <T> the type of the first input to the function
 * @param <U> the type of the second input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception to throw
 * @since 2025.0
 */
@FunctionalInterface
public interface ThrowableBiFunction<T, U, R, E extends Throwable> {

    R apply(T t, U u) throws E;

    /**
     * @return this {@link ThrowableBiFunction} as a {@link BiFunction} throwing the checked exception as an unchecked
     *         one
     */
    default BiFunction<T, U, R> toBiFunction() {
        return asBiFunction(this);
    }

    /**
     * @return the given {@link ThrowableBiFunction} as a {@link BiFunction} throwing the checked exception as an
     *         unchecked one
     */
    static <T, U, R, E extends Throwable> BiFunction<T, U, R> asBiFunction(
            ThrowableBiFunction<T, U, R, E> throwableFunction) {
        return (arg1, arg2) -> {
            try {
                return throwableFunction.apply(arg1, arg2);
            } catch (Throwable t) { // NOSONAR
                return FunctionUtils.sneakyThrow(t); // will never return
            }
        };
    }
}
