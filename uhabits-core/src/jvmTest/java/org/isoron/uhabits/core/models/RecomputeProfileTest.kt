/*
 * Copyright (C) 2026 Loop Habit Tracker contributors
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.models

import org.junit.Test
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.math.ceil
import kotlin.system.measureNanoTime

class RecomputeProfileTest {
    @Volatile
    private var observed: Any? = null

    @Test
    fun profile() {
        check(System.getProperty("uhabits.profile") == "true") {
            "Run :uhabits-core:profileRecomputation, not the regular unit test task."
        }
        val warmups = positiveSetting("warmups", 5)
        val samples = positiveSetting("samples", 20)
        val calls = positiveSetting("calls", 3)
        val output = File(System.getProperty("uhabits.profile.output"))
        check(output.mkdirs() || output.isDirectory)
        File(output, "environment.txt").writeText(
            """
            java=${System.getProperty("java.runtime.version")}
            vm=${System.getProperty("java.vm.name")}
            os=${System.getProperty("os.name")} ${System.getProperty("os.version")}
            arch=${System.getProperty("os.arch")}
            processors=${Runtime.getRuntime().availableProcessors()}
            maxHeapBytes=${Runtime.getRuntime().maxMemory()}
            jvmArgs=${ManagementFactory.getRuntimeMXBean().inputArguments}
            warmupBatches=$warmups
            measuredBatches=$samples
            callsPerBatch=$calls
            seed=deterministic arithmetic; end=2025-01-01; daysPerYear=365
            """.trimIndent() + "\n"
        )
        File(output, "results.csv").printWriter().use { csv ->
            csv.println(
                "years,days,schedule,kind,skips,operation,fixture_ns,warmup_batches,samples,calls_per_batch," +
                    "min_ns_per_call,p50_ns_per_call,p95_ns_per_call,mean_ns_per_call"
            )
            for (scenario in RecomputeScenario.all()) {
                lateinit var fixture: RecomputeFixture
                val fixtureTime = measureNanoTime { fixture = RecomputeFixture(scenario) }
                val operations = linkedMapOf<String, () -> Unit>(
                    "getKnown" to { observed = fixture.original.getKnown() },
                    "entries" to fixture::recomputeEntries,
                    "scores" to fixture::recomputeScores,
                    "streaks" to fixture::recomputeStreaks,
                    "full" to fixture::recomputeAll
                )
                for ((name, operation) in operations) {
                    repeat(warmups) {
                        repeat(calls) { operation() }
                        consume(fixture)
                    }
                    val timings = LongArray(samples) {
                        val elapsed = measureNanoTime { repeat(calls) { operation() } }
                        consume(fixture)
                        elapsed / calls
                    }.sorted()
                    val p50 = timings[ceil(samples * 0.50).toInt() - 1]
                    val p95 = timings[ceil(samples * 0.95).toInt() - 1]
                    csv.println(
                        "${scenario.years},${scenario.days},${scenario.schedule},${scenario.kind}," +
                            "${scenario.skips},$name,$fixtureTime,$warmups,$samples,$calls," +
                            "${timings.first()},$p50,$p95,${timings.average()}"
                    )
                    csv.flush()
                }
            }
        }
        println("Recomputation profile: ${output.absolutePath}")
    }

    private fun consume(fixture: RecomputeFixture) {
        observed = listOf(
            fixture.computed.get(fixture.scenario.to),
            fixture.scores[fixture.scenario.to],
            fixture.streaks.getBest(10)
        )
    }

    private fun positiveSetting(name: String, default: Int): Int {
        val value = System.getProperty("uhabits.profile.$name", default.toString()).toInt()
        require(value > 0) { "$name must be positive" }
        return value
    }
}
