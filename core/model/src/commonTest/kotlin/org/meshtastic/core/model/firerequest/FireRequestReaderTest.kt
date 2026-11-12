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
package org.meshtastic.core.model.firerequest

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FireRequestReaderTest {

    /** Written, split into parts, each part parsed, then read back. */
    private fun roundTrip(request: FireRequest): ReadRequest {
        val parts = FireRequestSplitter.split(request.number, FireRequestText.write(request))
        val lines = parts.map { assertNotNull(FireRequestPart.parse(it), it) }.sortedBy { it.k }.flatMap { it.lines }
        return FireRequestReader.read(lines)
    }

    private fun assertRoundTrip(request: FireRequest) {
        val read = roundTrip(request)
        assertEquals(request, read.request)
        assertEquals((1..11).toSet(), read.boxes)
        assertEquals(emptyMap(), read.unread)
    }

    @Test
    fun specification_example_reads_back() {
        assertRoundTrip(FireRequestExamples.specification)
    }

    @Test
    fun empty_request_reads_back() {
        assertRoundTrip(FireRequest(number = "A1"))
    }

    @Test
    fun a_box_cut_across_two_parts_reads_back() {
        // Box 2 filled to its limit is too long for one part: it is cut at a « · » and joined again on reading.
        val request =
            FireRequest(
                origin = FireRequestExamples.longest.origin,
                number = "ABCDE12345",
                fireType = FireRequestExamples.longest.fireType,
                command = FireRequestExamples.longest.command,
            )
        assertTrue(FireRequestSplitter.split(request.number, FireRequestText.write(request)).size <= 9)
        assertRoundTrip(request)
    }

    @Test
    fun every_box_alone_reads_back() {
        val full = FireRequestExamples.longest
        val requests =
            listOf(
                FireRequest(number = "N1", fireType = full.fireType),
                FireRequest(number = "N1", effect = full.effect),
                FireRequest(number = "N1", observation = Observation(shotsSecond = "6", intervalFirst = "10")),
                FireRequest(number = "N1", charge = Charge(launcher = "4", modeFirst = "4", delayM = "20")),
                FireRequest(number = "N1", charge = Charge(trajectorySecond = "3", modeSecond = "3")),
                FireRequest(number = "N1", targets = full.targets),
                FireRequest(number = "N1", targets = Targets(lightArmour = true, other = "Pont détruit")),
                FireRequest(number = "N1", location = full.location),
                FireRequest(number = "N1", location = Location(tle = "3")),
                FireRequest(number = "N1", shape = full.shape),
                FireRequest(number = "N1", shape = Shape(kind = ShapeKind.CONVERGENT, side = Side.LG)),
                FireRequest(number = "N1", adjustment = full.adjustment),
                FireRequest(number = "N1", adjustment = Adjustment(observerY = "88800")),
                FireRequest(number = "N1", friends = Friends("1200", "N", "3")),
                FireRequest(number = "N1", command = full.command),
                FireRequest(number = "N1", command = Command(limitedStart = "", limitedEnd = "14H45")),
                FireRequest(number = "N1", effect = Effect(mixFirst = "", mixSecond = "2", illumination = "")),
                FireRequest(number = "N1", fireType = FireType(setUp = "", renew = "Ancien tir")),
                FireRequest(origin = "Poste 2", recipient = "CDS TIR", requestGdh = "112150Z NOV 26", number = "N1"),
            )
        requests.forEach { assertRoundTrip(it) }
    }

    @Test
    fun random_requests_read_back() {
        val random = Random(seed = 2026)
        repeat(200) { assertRoundTrip(randomRequest(random)) }
    }

    @Test
    fun missing_part_leaves_its_boxes_absent() {
        val parts = FireRequestExamples.specificationParts.map { assertNotNull(FireRequestPart.parse(it)) }
        val read = FireRequestReader.read(parts[0].lines + parts[2].lines)
        assertEquals(setOf(1, 2, 3, 4, 9, 10, 11), read.boxes)
        assertEquals("AB1234", read.request.number)
        assertTrue(read.request.command.whenReady)
    }

    @Test
    fun what_does_not_read_is_kept_as_it_came() {
        val read =
            FireRequestReader.read(listOf("1 AB1", "5 Lanceurs 2 · Obus fumigène", "7 Z9 quelque part", "texte libre"))
        assertEquals("2", read.request.charge.launcher)
        assertEquals(listOf("Obus fumigène"), read.unread[5])
        assertEquals(listOf("Z9 quelque part"), read.unread[7])
        assertEquals(listOf("texte libre"), read.unread[BoxLine.HEADER])
    }

    @Test
    fun part_tag_is_read_only_at_the_start() {
        val part = assertNotNull(FireRequestPart.parse("[DT AB1234 2/3]\n5 Lanceurs 2\n6 non rempli"))
        assertEquals(FireRequestPart("AB1234", 2, 3, listOf("5 Lanceurs 2", "6 non rempli")), part)
        for (
        text in
        listOf(
            "Voir [DT AB1234 1/3]\n1 AB1234",
            "[DT AB1234 0/3]\n1 AB1234",
            "[DT AB1234 4/3]\n1 AB1234",
            "[DT AB1234 1/10]\n1 AB1234",
            "[DT ab1234 1/3]\n1 AB1234",
            "[DT ABCDEFGHIJK 1/3]\n1 X",
            "[DT AB1234 1/3] 1 AB1234",
            "[DT  1/3]\n1 X",
            "[URG] [DT AB1234 1/3]\n1 X",
        )
        ) {
            assertNull(FireRequestPart.parse(text), text)
        }
    }

    @Test
    fun reply_reads_and_writes() {
        val reply = FireRequestReply("AB1234", ReplyKind.ALLOCATION, "112252 loc NOV 26")
        assertEquals("[DT AB1234] GDH attribution 112252 loc NOV 26", reply.text())
        assertEquals(reply, FireRequestReply.parse(reply.text()))
        assertEquals(
            FireRequestReply("AB1234", ReplyKind.END, "112310Z NOV 26"),
            FireRequestReply.parse("[DT AB1234] GDH fin 112310Z NOV 26"),
        )
        assertNull(FireRequestReply.parse("[DT AB1234] GDH demande 112252 loc NOV 26"))
        assertNull(FireRequestReply.parse("[DT AB1234] GDH fin"))
        assertNull(FireRequestReply.parse("[DT AB1234 1/1] GDH fin 112310Z NOV 26"))
        assertNull(FireRequestPart.parse("[DT AB1234] GDH fin 112310Z NOV 26"))
    }

    private fun randomRequest(random: Random): FireRequest {
        fun digits() = if (random.nextBoolean()) "" else random.nextInt(0, 100_000).toString()
        fun code(max: Int) = if (random.nextBoolean()) "" else random.nextInt(1, max + 1).toString()
        fun word() = if (random.nextBoolean()) "" else listOf("P1", "Pont", "Ferme", "OA", "R2").random(random)
        fun <T> maybe(value: T): T? = value.takeIf { random.nextBoolean() }
        return FireRequest(
            origin = word(),
            recipient = word(),
            number = "R" + random.nextInt(1, 999),
            fireType =
            FireType(
                lines = ImpactLine.entries.filter { random.nextBoolean() }.toSet(),
                firstRound = random.nextBoolean(),
                setUp = maybe(code(9)),
                prepare = random.nextBoolean(),
                renew = maybe(word()),
            ),
            effect = Effect(stop = random.nextBoolean(), illumination = maybe(code(2)), mixFirst = maybe(code(5))),
            observation = Observation(digits(), digits(), digits(), digits(), digits(), digits()),
            charge = Charge(code(4), code(3), code(3), code(4), code(4), digits(), digits(), digits()),
            targets =
            Targets(
                tanks = true,
                tanksMotion = maybe(Motion.entries.random(random)),
                pickup = random.nextBoolean(),
                personnel = true,
                personnelCover = maybe(Cover.SHELTERED),
                building = random.nextBoolean(),
                other = word(),
            ),
            location =
            Location(
                b = PolarLine(digits(), digits(), digits()),
                e = RectangularLine(digits(), digits(), digits()),
                reference =
                ReferenceLine(first = word().ifEmpty { "R1" }, nearFar = Shift.FURTHER, nearFarM = "100"),
                h = LruLine("31", "U", "DQ", "48251", "11932", digits()),
                tle = code(4),
            ),
            shape = Shape(kind = maybe(ShapeKind.entries.random(random)), front = digits(), centre = word()),
            adjustment = Adjustment(maybe(AdjustMethod.GRID), digits(), digits(), digits(), digits()),
            friends = Friends(digits(), digits(), code(3)),
            command = Command(whenReady = random.nextBoolean(), fireTime = maybe("14H25"), atCommandOf = maybe("OA")),
        )
    }
}
