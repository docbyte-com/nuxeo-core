/*
 * (C) Copyright 2016-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search.client.opensearch1.aggregate;

import java.time.ZonedDateTime;

/**
 * Helper to add duration to a date with the same format as ES Date histogram interval
 *
 * @since 8.4
 */
public final class DateHelper {

    public static final String EPOCH_MILLIS_FORMAT = "epoch_millis";

    private DateHelper() {

    }

    /**
     * Returns a new ZonedDateTime plus the specified duration.
     *
     * @param origin the initial ZonedDateTime
     * @param duration can be expressed with a noun: hour, day, month, quarter, year or expression: 2d, 3h, 5w, 2M, 3y
     *            or a number of ms: 1234
     * @throws IllegalArgumentException if the duration cannot be parsed
     * @return a new ZonedDateTime
     */
    public static ZonedDateTime plusDuration(ZonedDateTime origin, String duration) {
        if (duration.matches("[a-zA-Z]+")) {
            return plusDurationAsNoun(origin, duration);
        }
        if (duration.matches("[0-9]+")) {
            return origin.plusNanos(Integer.valueOf(duration) * 1000);
        }
        return plusDurationAsExpression(origin, duration);
    }

    private static ZonedDateTime plusDurationAsExpression(ZonedDateTime origin, String duration) {
        int k = getFactor(duration);
        return switch (duration.substring(duration.length() - 1)) {
            case "s" -> origin.plusSeconds(k);
            case "m" -> origin.plusMinutes(k);
            case "h" -> origin.plusHours(k);
            case "d" -> origin.plusDays(k);
            case "w" -> origin.plusWeeks(k);
            case "M" -> origin.plusMonths(k);
            case "y" -> origin.plusYears(k);
            default -> invalid(duration);
        };
    }

    private static int getFactor(String duration) {
        try {
            return Integer.valueOf(duration.substring(0, duration.length() - 1));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            invalid(duration);
        }
        return 1;
    }

    private static ZonedDateTime plusDurationAsNoun(ZonedDateTime origin, String duration) {
        return switch (duration.toLowerCase()) {
            case "second" -> origin.plusSeconds(1);
            case "minute" -> origin.plusMinutes(1);
            case "hour" -> origin.plusHours(1);
            case "day" -> origin.plusDays(1);
            case "week" -> origin.plusWeeks(1);
            case "month" -> origin.plusMonths(1);
            case "quarter" -> origin.plusMonths(3);
            case "year" -> origin.plusYears(1);
            default -> invalid(duration);
        };
    }

    private static ZonedDateTime invalid(String msg) {
        throw new IllegalArgumentException("Invalid duration: " + msg);
    }

}
