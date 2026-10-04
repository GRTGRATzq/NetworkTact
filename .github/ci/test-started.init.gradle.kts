/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Diagnostic init script for the fork-apk workflow: logs every JVM test when it starts, not only when it ends, so a
// test that never finishes is named in the log. Registered after each project is evaluated, so it runs after the
// build-logic convention (PASSED, SKIPPED, FAILED) and adds STARTED to it. Isolated-Projects-safe.
import org.gradle.api.tasks.testing.logging.TestLogEvent

gradle.lifecycle.afterProject {
    tasks.withType(Test::class.java).configureEach {
        testLogging { events(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED) }
    }
}
