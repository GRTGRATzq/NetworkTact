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

/**
 * The words of the "Demande de tir" sheet (68e RAA, EOC, February 2024), written as on paper. They are a fixed French
 * convention, the same on every phone and in every language, like `[CR] FAIT OBSERVÉ`: the message is written with them
 * and nothing else. Nothing here may be added to or reworded without the project owner's decision
 * (`docs/SPEC-DEMANDE-DE-TIR.md`).
 */
object FireRequestSheet {
    /** Tag of every part, `[DT AB1234 1/3]`, and badge of the request. */
    const val TAG = "DT"

    /** An empty box, written `4 non rempli`. */
    const val NOT_FILLED = "non rempli"

    /** Separator between the elements of a box. */
    const val SEPARATOR = " · "

    // Header.
    const val ORIGIN = "Origine"
    const val RECIPIENT = "Destinataire"
    const val REQUEST_GDH = "GDH demande"
    const val ALLOCATION_GDH = "GDH attribution"
    const val END_GDH = "GDH fin"

    /** Titles of boxes 1 to 11, in order. */
    val BOX_TITLES =
        listOf(
            "N° de tir",
            "Type de tir",
            "Type d'effet",
            "Observation",
            "Type de charge",
            "Objectifs",
            "Localisation de l'impact",
            "Forme de l'impact",
            "Mise en place",
            "Ami(s) le(s) plus proche(s)",
            "À mon commandement",
        )

    const val BOX_COUNT = 11

    // Units.
    const val METRES = "m"
    const val MINUTES = "min"
    const val SECONDS = "s"
    const val MILLISECONDS = "ms"

    // Box 2.
    const val FIRST_ROUND = "Emblée"
    const val SET_UP = "Mettre en place"
    const val OFFSET_SET_UP = "Mise en place décentrée"
    const val PREPARE = "Préparer"
    const val RENEW = "Renouveler"
    const val ENGAGEMENT = "Accrochage"

    // Box 3.
    const val NEUTRALISATION = "Neutralisation"
    const val DESTRUCTION = "Destruction"
    const val STOP = "Arrêt"
    const val WARNING = "Semonce"
    const val BLINDING = "Aveuglement"
    const val ILLUMINATION = "Éclairement"
    const val MIX = "Panaché"
    const val AND = "et"
    val ILLUMINATION_CODES = mapOf("1" to "Visible", "2" to "IR")
    val MIX_CODES = mapOf("1" to "Explo", "2" to "Fumigène", "3" to "ACED", "4" to "ECL", "5" to "Semonce")
    val MIX_ILLUMINATION_CODES = mapOf("A" to "VISIB", "B" to "IR")

    // Box 4.
    const val SHOTS = "Nb de cps"
    const val DURATION = "Durée"
    const val INTERVAL = "Intervalle entre les coups"

    // Box 5.
    const val LAUNCHERS = "Lanceurs"
    const val TRAJECTORY = "Trajectoire"
    const val MODE = "Mode"
    const val SHORT_DELAY = "Court retard"
    const val AIRBURST = "Fusant"
    const val DELAY = "Retard"
    val LAUNCHER_CODES = mapOf("1" to "Mo 81 mm", "2" to "Mo 120", "3" to "155 mm", "4" to "LRU")
    val TRAJECTORY_CODES = mapOf("1" to "Plongeante", "2" to "Verticale", "3" to "Nominale")
    val MODE_CODES =
        mapOf("1" to "Percutant instantané", "2" to "Percutant court retard", "3" to "Fusant", "4" to "Retard")

    // Box 6.
    const val TANKS = "Chars"
    const val LIGHT_ARMOUR = "Blindés légers"
    const val PICKUP = "Pick-up"
    const val PERSONNEL = "Personnel"
    const val ARTILLERY = "Artillerie"
    const val BUILDING = "Bâtiment"

    // Box 7.
    const val POLAR = "POLAIRES"
    const val RECTANGULAR = "RECTANG."
    const val LRU = "L R U"
    const val BEARING_LETTER = "G"
    const val DISTANCE_LETTER = "D"
    const val SITE_LETTER = "S"
    const val X = "X"
    const val Y = "Y"
    const val Z = "Z"
    const val REFERENCE = "Rep"
    const val TLE = "TLE"
    val TLE_CODES = listOf("1", "2", "3", "4")

    // Box 8.
    const val CENTRAL_POINT = "Point central"
    const val BEARING = "Gisement"
    const val FRONT = "Front"
    const val DEPTH = "Profondeur"
    const val CENTRE = "Centre"
    const val RADIUS = "Rayon"

    // Box 9.
    const val OBSERVATION_BEARING = "Gisement d'observ."
    const val OBSERVER = "Observateur"

    // Box 10.
    const val DISTANCE = "Distance"
    const val PROTECTION = "Niveau de protection"
    val PROTECTION_CODES = mapOf("1" to "à découvert", "2" to "abrité", "3" to "sous blindage")
    val CARDINAL_POINTS = listOf("N", "O", "E", "S")

    // Box 11.
    const val AT_MY_COMMAND = "À mon commandement"
    const val WHEN_READY = "Aussitôt prêt"
    const val WHEN_READY_NLT = "Aussitôt prêt limité NLT"
    const val SYNCHRO = "Synchro"
    const val TIME_ON_TARGET = "Heure sur objectif"
    const val TIME_ON_TARGET_LIMITED = "HSO limité"
    const val START = "début"
    const val END = "fin"
    const val FIRE_TIME = "Heure de tir"
    const val AT_COMMAND_OF = "Au commandement de"
}
