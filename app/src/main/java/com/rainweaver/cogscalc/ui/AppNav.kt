package com.rainweaver.cogscalc.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rainweaver.cogscalc.R
import com.rainweaver.cogscalc.ui.screens.*

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    ING("Ingredients", R.drawable.ic_tab_ing),
    RECIPES("Recipes", R.drawable.ic_tab_recipes),
    SETTINGS("Settings", R.drawable.ic_tab_settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(vm: CogsViewModel) {
    var tab by remember { mutableStateOf(Tab.ING) }
    var editorId by remember { mutableStateOf<Long?>(null) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(s.message) { s.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("COGS Calculator", fontWeight = FontWeight.SemiBold)
                    Text("Rainweaver Studio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            })
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t && editorId == null && detailId == null,
                        onClick = { tab = t; editorId = null; detailId = null },
                        icon = { Icon(painterResource(t.icon), contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            val eid = editorId
            val did = detailId
            when {
                eid != null -> RecipeEditorScreen(vm, eid) { editorId = null }
                did != null -> RecipeDetailScreen(vm, did, onBack = { detailId = null }, onEdit = { editorId = it; detailId = null })
                else -> when (tab) {
                    Tab.ING -> IngredientsScreen(vm)
                    Tab.RECIPES -> RecipesScreen(vm, onOpen = { detailId = it }, onEdit = { editorId = it })
                    Tab.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
    }
}
