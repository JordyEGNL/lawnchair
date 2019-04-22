package com.android.quickstep;

import static androidx.test.InstrumentationRegistry.getInstrumentation;

import static com.android.launcher3.LauncherState.OVERVIEW;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.app.usage.UsageStatsManager;
import android.content.Intent;

import com.android.launcher3.Launcher;
import com.android.quickstep.views.DigitalWellBeingToast;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

import org.junit.Test;

import java.time.Duration;

public class DigitalWellBeingToastTest extends AbstractQuickStepTest {
    private static final String CALCULATOR_PACKAGE =
            resolveSystemApp(Intent.CATEGORY_APP_CALCULATOR);

    @Test
    public void testToast() throws Exception {
        final UsageStatsManager usageStatsManager =
                mTargetContext.getSystemService(UsageStatsManager.class);
        final int observerId = 0;

        try {
            final String[] packages = new String[]{CALCULATOR_PACKAGE};

            // Set time limit for app.
            runWithShellPermission(() ->
                    usageStatsManager.registerAppUsageLimitObserver(observerId, packages,
                            Duration.ofSeconds(600), Duration.ofSeconds(300),
                            PendingIntent.getActivity(mTargetContext, -1, new Intent(), 0)));

            mLauncher.pressHome();
            final DigitalWellBeingToast toast = getToast();

            assertTrue("Toast is not visible", toast.isShown());
            assertEquals("Toast text: ", "5 minutes left today", toast.getTextView().getText());

            // Unset time limit for app.
            runWithShellPermission(
                    () -> usageStatsManager.unregisterAppUsageLimitObserver(observerId));

            mLauncher.pressHome();
            assertFalse("Toast is visible", getToast().isShown());
        } finally {
            runWithShellPermission(
                    () -> usageStatsManager.unregisterAppUsageLimitObserver(observerId));
        }
    }

    private DigitalWellBeingToast getToast() {
        executeOnLauncher(launcher -> launcher.getStateManager().goToState(OVERVIEW));
        waitForState("Launcher internal state didn't switch to Overview", OVERVIEW);
        waitForLauncherCondition("No latest task", launcher -> getLatestTask(launcher) != null);

        return getFromLauncher(launcher -> {
            final TaskView task = getLatestTask(launcher);
            assertTrue("Latest task is not Calculator",
                    CALCULATOR_PACKAGE.equals(task.getTask().getTopComponent().getPackageName()));
            return task.getDigitalWellBeingToast();
        });
    }

    private TaskView getLatestTask(Launcher launcher) {
        return launcher.<RecentsView>getOverviewPanel().getTaskViewAt(0);
    }

    private void runWithShellPermission(Runnable action) {
        getInstrumentation().getUiAutomation().adoptShellPermissionIdentity();
        try {
            action.run();
        } finally {
            getInstrumentation().getUiAutomation().dropShellPermissionIdentity();
        }

    }
}
