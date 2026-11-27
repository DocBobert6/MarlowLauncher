package app.launcher

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import app.launcher.data.LauncherStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

// Launcher — Robolectric startup gate.
//
// The app must launch into the home screen with no native code and no
// network: the activity shows the home, and the manifest declares the
// config changes a launcher must survive. This is the standing rule
// from Robert's other apps (HowlingDog v1.3.0): a composable that grows
// past 255 registers crashes ART at launch while Robolectric stays
// green, so this gate runs alongside the 255-register dex guard in CI.
//
// NOTE: Robolectric is NOT in the local offline Gradle cache, so this
// test runs on CI only. Locally, assembleDebug + the dex guard are the
// gates. See BUILD_LOG.md.
//
// Decision F2 (home first, 2026-11-26): the five-step first-run
// wizard is deleted. A fresh install lands straight on the home —
// no wizard, no "Skip setup" — and the only first-run prompt is the
// single system default-home prompt (fired once from the home;
// device-gated, not asserted here). The gate asserts the home is
// what a first launch actually shows. The gate still has its
// meaningful assertions (home incl. strip + search pill, recreation).
// The gate builds MainActivity directly with Robolectric.buildActivity
// instead of launching it through ActivityScenario. Three runs proved
// the Scenario path unresolvable from the test side, and the evidence
// is worth keeping because the obvious theories were all wrong:
//  * Run 6: the default MAIN + LAUNCHER intent failed with
//    "Unable to resolve activity". The first theory was the manifest
//    filter — this is a HOME launcher, not a drawer app.
//  * Run 9: the MAIN + HOME + DEFAULT intent failed identically.
//    Reading Robolectric 4.14's source showed why: for an explicit
//    intent, resolveActivityInfo() is purely a
//    getActivityInfo(component) lookup — intent filters play no part.
//  * Run 10: naming the component with the app package explicitly
//    (app.launcher/app.launcher.MainActivity, exactly the manifest
//    entry) failed identically. So the shadow PackageManager's keying
//    is not observable from the test, and a fourth guess would be
//    another wasted run. (The likely cause: this AGP 9 manifest
//    carries no package attribute — the package lives in the
//    namespace — so the test context reports org.robolectric.default
//    and the manifest is parsed under a package the test cannot
//    name. Not verified; it does not matter to the gate.)
//
// The two manifest assertions that used to live here — the required
// config changes and the supplied launcher icon — moved to the
// workflow's Manifest guard step (see android.yml). Run 11 proved
// the test PackageManager returns zeroed stubs for manifest lookups
// under the test package (configChanges 0, icon 0), so those checks
// cannot live in this test; the manifest file itself is the source
// of truth there, and the guard runs on every push like this gate.
//
// Run 13 also settled the launch saga: buildActivity works, and the
// two real gate tests (launchesAndShowsHome, survivesRecreation)
// pass. (Run 13 then failed the old firstLaunchShowsWizard the other
// way — no "Welcome" node at all — because the DataStore file
// persists across test methods; the @Before clearAll above is the
// fix for that, and it is why every test starts from a fresh
// install rather than depending on execution order.)
//
// Runs 20–23: the rebuild's second clearAll hangs. Test 1's whole
// lifecycle works (setup in ~2 s, body passes, destroy fast), but
// the SECOND test method's @Before prints "before clearAll" and
// never reaches "after clearAll" — the runBlocking DataStore edit
// never returns. Run 22's looper-drain (idle() before clearAll)
// did not fix it, so the block is not a pending main-looper
// continuation. The cause is still unproven. What IS proven is
// that one full launch–assert–destroy cycle works, so the gate is
// now a SINGLE test method holding all three original assertions
// (first-launch home, no wizard, survive recreation) in one
// lifecycle — the cross-test DataStore reset is gone, and with it
// the hang. Same assertions, same gate, no weakening. Run 24 went
// green on that shape, and the diagnostic markers + watchdog that
// got it there were removed in the cleanup right after.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityStartupTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var controller: ActivityController<MainActivity>

    @Before
    fun launchHome() {
        // Fresh install: the settings DataStore file persists across
        // test methods in the same JVM. clearAll is the app's own
        // #171 reset, through the DataStore itself, so it clears
        // the in-memory state and the file alike; after it, the
        // next launch is a first launch again. (Run 13 proved the
        // persistence: a Skip tap in an earlier test completed the
        // wizard for the next one. The wizard is gone now, but the
        // fresh-install discipline stays.)
        // Run 22 diagnostic markers: each stage prints a marker
        // (showStandardStreams is on in build.gradle) — the last
        // marker in the console log is exactly where it wedges.
        // Remove these markers once the hang is found and fixed.
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { LauncherStore.clearAll(context) }
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    }

    @After
    fun closeHome() {
        // Run 16: when setup() itself throws (Resources$NotFound-
        // Exception in the rebuild tree), controller was never
        // assigned — an unguarded destroy() throws
        // UninitializedPropertyAccessException and masks the real
        // failure in the report. Only destroy what was built.
        if (::controller.isInitialized) controller.destroy()
    }

    // One test, all three original gate assertions (runs 20–23):
    // a first launch shows the home directly — the dock's fixed
    // search pill and the At a Glance strip — with no wizard step
    // in between, and the home survives a destroy + rebuild.
    // The pill carries the stable content description "Search"
    // (see Dock.kt); the strip carries "At a Glance" (see
    // AtAGlance.kt). Audit fix (2026-11-26): the drawer FAB is
    // gone — the drawer opens by swipe up — so the old "Open app
    // drawer" assertion is replaced by the strip assertion.
    @Test
    fun launchesShowsHomeAndSurvivesRecreation() {
        // First launch: home, no wizard.
        composeRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("At a Glance").assertIsDisplayed()
        assertTrue(
            "no wizard title",
            composeRule.onAllNodesWithText("Welcome").fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            "no wizard skip control",
            composeRule.onAllNodesWithText("Skip setup").fetchSemanticsNodes().isEmpty(),
        )
        // Recreation: destroy + rebuild, home still shows.
        controller.destroy()
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        composeRule.onNodeWithContentDescription("Search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("At a Glance").assertIsDisplayed()
    }
}
