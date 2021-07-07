/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.net.Severity;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * {@link Level} resolver.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config         = field , [ severity ]
 * field          = "field" -> ( "name" | "severity" )
 * severity       = severity-field
 * severity-field = "field" -> ( "keyword" | "code" )
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve the level name:
 *
 * <pre>
 * {
 *   "$resolver": "level",
 *   "field": "name"
 * }
 * </pre>
 *
 * Resolve the severity keyword:
 *
 * <pre>
 * {
 *   "$resolver": "level",
 *   "field": "severity",
 *   "severity": {
 *     "field": "keyword"
 *   }
 * }
 *
 * Resolve the severity code:
 *
 * <pre>
 * {
 *   "$resolver": "level",
 *   "field": "severity",
 *   "severity": {
 *     "field": "code"
 *   }
 * }
 * </pre>
 */
public final class LevelResolver implements EventResolver {

    private static final String[] SEVERITY_CODE_RESOLUTION_BY_STANDARD_LEVEL_ORDINAL;

    static {
        final int levelCount = Level.values().length;
        final String[] severityCodeResolutionByStandardLevelOrdinal =
                new String[levelCount + 1];
        for (final Level level : Level.values()) {
            final int standardLevelOrdinal = level.getStandardLevel().ordinal();
            final int severityCode = Severity.getSeverity(level).getCode();
            severityCodeResolutionByStandardLevelOrdinal[standardLevelOrdinal] =
                    String.valueOf(severityCode);
        }
        SEVERITY_CODE_RESOLUTION_BY_STANDARD_LEVEL_ORDINAL =
                severityCodeResolutionByStandardLevelOrdinal;
    }

    private static final EventResolver SEVERITY_CODE_RESOLVER =
            (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
                final int standardLevelOrdinal =
                        logEvent.getLevel().getStandardLevel().ordinal();
                final String severityCodeResolution =
                        SEVERITY_CODE_RESOLUTION_BY_STANDARD_LEVEL_ORDINAL[
                                standardLevelOrdinal];
                jsonWriter.writeRawString(severityCodeResolution);
            };

    private final EventResolver internalResolver;

    LevelResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        this.internalResolver = createResolver(context, config);
    }

    private static EventResolver createResolver(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        final String fieldName = config.getString("field");
        if ("name".equals(fieldName)) {
            return createNameResolver();
        } else if ("severity".equals(fieldName)) {
            final String severityFieldName =
                    config.getString(new String[]{"severity", "field"});
            if ("keyword".equals(severityFieldName)) {
                return createSeverityKeywordResolver();
            } else if ("code".equals(severityFieldName)) {
                return SEVERITY_CODE_RESOLVER;
            }
            throw new IllegalArgumentException(
                    "unknown severity field: " + config);
        }
        throw new IllegalArgumentException("unknown field: " + config);
    }

    private static EventResolver createNameResolver() {
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
            final String resolution = logEvent.getLevel().name();
            jsonWriter.writeRawString(resolution);
        };
    }

    private static EventResolver createSeverityKeywordResolver() {
        return (final LogEvent logEvent, final JsonWriter jsonWriter) -> {
            final String resolution = Severity.getSeverity(logEvent.getLevel()).name();
            jsonWriter.writeRawString(resolution);
        };
    }

    static String getName() {
        return "level";
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        internalResolver.resolve(logEvent, jsonWriter);
    }

}
