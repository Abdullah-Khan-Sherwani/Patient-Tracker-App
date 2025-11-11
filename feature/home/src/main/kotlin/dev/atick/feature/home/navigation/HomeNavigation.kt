/*
 * Copyright 2023 Atick Faisal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.atick.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import dev.atick.core.ui.utils.SnackbarAction
import dev.atick.feature.home.ui.home.HomeScreen
import dev.atick.feature.home.ui.item.ItemScreen
import dev.atick.feature.home.ui.schedule.ScheduleAppointmentRoute
import kotlinx.serialization.Serializable

/** Home navigation (type-safe destinations). */
@Serializable
data object Home

/** Home navigation graph root. */
@Serializable
data object HomeNavGraph

/** Item navigation (with optional id). */
@Serializable
data class Item(val itemId: String?)

/** Schedule Appointment destination (type-safe). */
@Serializable
data object ScheduleAppointment

/** Action keys passed via HomeScreen(onJetpackClick: (String) -> Unit). */
object HomeActions {
    // When HomeScreen calls onJetpackClick with this key, we navigate to schedule.
    const val ScheduleKey = "__schedule__"
}

/** Navigate to Home graph. */
fun NavController.navigateToHomeNavGraph(navOptions: NavOptions? = null) {
    navigate(HomeNavGraph, navOptions)
}

/** Navigate to Item screen. */
fun NavController.navigateToItemScreen(itemId: String?) {
    navigate(Item(itemId)) { launchSingleTop = true }
}

/** Item screen destination. */
fun NavGraphBuilder.itemScreen(
    onBackClick: () -> Unit,
    onShowSnackbar: suspend (String, SnackbarAction, Throwable?) -> Boolean,
) {
    composable<Item> {
        ItemScreen(
            onBackClick = onBackClick,
            onShowSnackbar = onShowSnackbar,
        )
    }
}

/** Schedule Appointment destination (type-safe). */
fun NavGraphBuilder.scheduleAppointmentScreen(
    onBackClick: () -> Unit,
) {
    composable<ScheduleAppointment> {
        ScheduleAppointmentRoute(
            onBack = onBackClick,
            onSubmit = {
                // TODO: hook to VM/repo later if needed
                onBackClick()
            }
        )
    }
}

/**
 * Home navigation graph.
 *
 * Wires the destinations and handles the Schedule action inside the HomeScreen callback.
 */
fun NavGraphBuilder.homeNavGraph(
    navController: NavController,
    nestedNavGraphs: NavGraphBuilder.() -> Unit = {},
    onJetpackClickPassthrough: (String) -> Unit = {}, // if parent needs the original callback
) {
    navigation<HomeNavGraph>(startDestination = Home) {

        // Home
        composable<Home> {
            HomeScreen(
                onJetpackClick = { key ->
                    if (key == HomeActions.ScheduleKey) {
                        navController.navigate(ScheduleAppointment)
                    } else {
                        onJetpackClickPassthrough(key)
                    }
                },
                onShowSnackbar = { _, _, _ -> false }
            )
        }

        // Item
        itemScreen(
            onBackClick = { navController.popBackStack() },
            onShowSnackbar = { _, _, _ -> false }
        )

        // Schedule screen
        scheduleAppointmentScreen(
            onBackClick = { navController.popBackStack() }
        )

        // Any nested graphs the caller wants to add
        nestedNavGraphs()
    }
}
