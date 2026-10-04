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
package org.meshtastic.core.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class TabsResetOnSwitchTest {

    private val nodesStack = NavBackStack<NavKey>().apply { addAll(listOf(NodesRoute.Nodes, NodesRoute.CommandPost)) }
    private val contactsStack =
        NavBackStack<NavKey>().apply { addAll(listOf(ContactsRoute.Contacts, ContactsRoute.Messages("0^all"))) }

    private val multiBackstack =
        MultiBackstack(NodesRoute.Nodes, mutableStateOf(NodesRoute.Nodes)).apply {
            backStacks = mapOf(NodesRoute.Nodes to nodesStack, ContactsRoute.Contacts to contactsStack)
        }

    private val tabs =
        TabsResetOnSwitch(
            multiBackstack = multiBackstack,
            shownFor = false,
            startPathsWhenOn = mapOf(NodesRoute.Nodes to listOf(NodesRoute.Nodes, NodesRoute.CommandPost)),
        )

    @Test
    fun switchingOnPutsEveryTabOnItsStartPath() {
        tabs.sync(true)

        assertEquals(listOf(NodesRoute.Nodes, NodesRoute.CommandPost), nodesStack.toList())
        assertEquals(listOf<NavKey>(ContactsRoute.Contacts), contactsStack.toList())
    }

    @Test
    fun switchingOffPutsEveryTabBackOnItsRoot() {
        tabs.sync(true)
        contactsStack.add(ContactsRoute.Messages("1^all"))

        tabs.sync(false)

        assertEquals(listOf<NavKey>(NodesRoute.Nodes), nodesStack.toList())
        assertEquals(listOf<NavKey>(ContactsRoute.Contacts), contactsStack.toList())
    }

    @Test
    fun aLinkOpenedRightAfterTheSwitchKeepsItsNavigation() {
        tabs.sync(true)
        val link = listOf(ContactsRoute.Contacts, ContactsRoute.Messages("8!0000beef"))

        // Opening the link switches demo mode off: the link handler resets first, then the screen reports it again.
        tabs.sync(false)
        multiBackstack.handleDeepLink(link)
        tabs.sync(false)

        assertEquals(ContactsRoute.Contacts, multiBackstack.currentTabRoute)
        assertEquals(link, contactsStack.toList())
    }

    @Test
    fun nothingIsResetWhileTheSwitchStaysAsItWas() {
        contactsStack.add(ContactsRoute.Messages("1^all"))

        tabs.sync(false)

        assertEquals(3, contactsStack.size)
    }
}
