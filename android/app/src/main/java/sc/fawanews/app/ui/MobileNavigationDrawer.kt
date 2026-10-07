package sc.fawanews.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sc.fawanews.app.R
import sc.fawanews.app.ui.components.FawaIcon
import sc.fawanews.app.ui.components.FawaIconSizes
import sc.fawanews.app.ui.theme.PlexColors

@Composable
fun MobileNavigationDrawer(
    selectedTab: HomeTab,
    liveCategories: List<String>,
    selectedCategory: String?,
    selectedScoreLeague: String?,
    scoreLeagues: List<String>,
    onTabSelect: (HomeTab) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onScoreLeagueSelect: (String?) -> Unit,
    onNavigate: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = PlexColors.sidebar,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 20.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.clutch_wordmark),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 8.dp)
                    .height(FawaIconSizes.brandWordmark),
                contentScale = ContentScale.Fit,
            )
            Text(
                stringResource(R.string.menu_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))

            DrawerTab(
                label = stringResource(R.string.menu_live),
                selected = selectedTab == HomeTab.LIVE,
                icon = { FawaIcon(Icons.Default.Sports, null) },
                onClick = {
                    onTabSelect(HomeTab.LIVE)
                    onNavigate()
                },
            )
            DrawerTab(
                label = stringResource(R.string.menu_scores),
                selected = selectedTab == HomeTab.SCORES,
                icon = { FawaIcon(Icons.Default.Scoreboard, null) },
                onClick = {
                    onTabSelect(HomeTab.SCORES)
                    onNavigate()
                },
            )
            DrawerTab(
                label = stringResource(R.string.menu_news),
                selected = selectedTab == HomeTab.NEWS,
                icon = { FawaIcon(Icons.Default.Newspaper, null) },
                onClick = {
                    onTabSelect(HomeTab.NEWS)
                    onNavigate()
                },
            )

            if (selectedTab == HomeTab.SCORES) {
                HorizontalDivider(Modifier.padding(vertical = 16.dp, horizontal = 28.dp))
                Text(
                    stringResource(R.string.menu_leagues),
                    style = MaterialTheme.typography.labelLarge,
                    color = PlexColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                )
                DrawerTab(
                    label = "All today",
                    selected = selectedScoreLeague == null,
                    onClick = {
                        onScoreLeagueSelect(null)
                        onNavigate()
                    },
                )
                scoreLeagues.forEach { league ->
                    DrawerTab(
                        label = league,
                        selected = selectedScoreLeague == league,
                        onClick = {
                            onScoreLeagueSelect(league)
                            onNavigate()
                        },
                    )
                }
            }
            if (selectedTab != HomeTab.SCORES && liveCategories.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 16.dp, horizontal = 28.dp))
                Text(
                    stringResource(R.string.menu_sports),
                    style = MaterialTheme.typography.labelLarge,
                    color = PlexColors.textSecondary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                )
                DrawerTab(
                    label = "All sports",
                    selected = selectedCategory == null,
                    onClick = {
                        onCategorySelect(null)
                        onNavigate()
                    },
                )
                liveCategories.forEach { category ->
                    DrawerTab(
                        label = category,
                        selected = selectedCategory == category,
                        onClick = {
                            onCategorySelect(category)
                            onNavigate()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    NavigationDrawerItem(
        label = { Text(label, maxLines = 2) },
        selected = selected,
        onClick = onClick,
        icon = icon,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = PlexColors.accent.copy(alpha = 0.22f),
            selectedTextColor = PlexColors.accentBright,
            selectedIconColor = PlexColors.accentBright,
            unselectedTextColor = PlexColors.textPrimary,
            unselectedIconColor = PlexColors.textSecondary,
        ),
    )
}
