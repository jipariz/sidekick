package dev.parez.sidekick.database

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.parez.sidekick.database.di.DatabaseInspectorKoinContext
import dev.parez.sidekick.database.di.databaseInspectorViewModelModule
import dev.parez.sidekick.database.ui.DatabaseInspectorContent
import dev.parez.sidekick.plugin.LocalSidekickBackNavigator
import dev.parez.sidekick.plugin.SidekickLifecycleAware
import dev.parez.sidekick.plugin.SidekickPlugin
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel

/**
 * Browses the consumer's own SQLite database from inside the app.
 *
 * The panel is empty until a database is bound — call `DatabaseInspector.attach(database,
 * fileName)` from `:plugins:database-inspector:room` during app startup. Deliberately not
 * auto-discovered: an overlay that goes looking for database files is guessing, and would find the
 * monitors' own databases alongside the consumer's.
 *
 * Unlike the monitors this plugin carries no [SidekickBadged] implementation — a schema has no
 * notion of "unread".
 */
public class DatabaseInspectorPlugin : SidekickPlugin, SidekickLifecycleAware {

    init {
        DatabaseInspectorKoinContext.loadViewModelModule(databaseInspectorViewModelModule)
    }

    override val id: String = "sidekick.database-inspector"
    override val title: String = "Database"
    override val icon: ImageVector = Icons.Default.Storage

    /** Re-reads on open so the panel never shows a snapshot from an earlier session. */
    override fun onAttach() {
        DatabaseInspectorKoinContext.getDefaultStore().refresh()
    }

    @Composable
    override fun Content() {
        val navigateBack = LocalSidekickBackNavigator.current
        KoinIsolatedContext(context = DatabaseInspectorKoinContext.koinApp) {
            val viewModel: DatabaseInspectorViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val selectedTable by viewModel.selectedTable.collectAsStateWithLifecycle()
            val sql by viewModel.sql.collectAsStateWithLifecycle()
            val queryResult by viewModel.queryResult.collectAsStateWithLifecycle()

            DatabaseInspectorContent(
                state = state,
                selectedTable = selectedTable,
                sql = sql,
                queryResult = queryResult,
                onSelectTable = viewModel::select,
                onSqlChange = viewModel::setSql,
                onRunQuery = viewModel::runQuery,
                onRefresh = viewModel::refresh,
                onUpdateCell = viewModel::updateCell,
                onBack = navigateBack,
            )
        }
    }
}
