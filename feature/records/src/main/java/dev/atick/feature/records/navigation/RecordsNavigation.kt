package dev.atick.feature.records.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import dev.atick.feature.records.ui.patient.PatientRecordsScreen
import dev.atick.feature.records.ui.patient.RecordDetailScreen

const val RECORDS_GRAPH = "records_graph"

fun NavController.navigateToRecords() = navigate(Records)
fun NavController.navigateToRecordDetail(id: String) = navigate(RecordDetail(id))

fun NavGraphBuilder.recordsNavGraph(
    onOpenRecord: (String) -> Unit,
    onBack: () -> Unit
) {
    navigation<RecordsRoot>(startDestination = Records, route = RECORDS_GRAPH) {

        composable<Records> {
            PatientRecordsScreen(onOpenRecord = onOpenRecord)
        }

        composable<RecordDetail> { entry ->
            val args = entry.toRoute<RecordDetail>()
            RecordDetailScreen(recordId = args.recordId, onBack = onBack)
        }
    }
}
