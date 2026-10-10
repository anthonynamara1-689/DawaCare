package com.mycompany.dawacare.jobs;

import com.mycompany.dawacare.dao.DoseDao;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Marks PENDING doses as MISSED once they are more than GRACE_MINUTES overdue. */
public final class MissedDoseJob {
    private static final int GRACE_MINUTES = 120;
    private static final int RUN_EVERY_MINUTES = 5;

    private static ScheduledExecutorService executor;

    private MissedDoseJob() {}

    /** Runs immediately (catches doses missed while the app was closed), then every few minutes. */
    public static synchronized void start() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "missed-dose-job");
            t.setDaemon(true);   // don't keep the app alive after the windows close
            return t;
        });
        executor.scheduleAtFixedRate(MissedDoseJob::runOnce, 0, RUN_EVERY_MINUTES, TimeUnit.MINUTES);
    }

    private static void runOnce() {
        try {
            DoseDao dao = new DoseDao();

            int marked = dao.markOverdueAsMissed(GRACE_MINUTES);
            if (marked > 0) {
                System.out.println("MissedDoseJob: marked " + marked + " dose(s) as MISSED");
            }

            int[] done = dao.completeFinishedPrescriptions();
            if (done[0] > 0 || done[1] > 0) {
                System.out.println("MissedDoseJob: completed " + done[0] + " item(s) and "
                        + done[1] + " prescription(s)");
            }
        } catch (Exception e) {
            // Must catch everything: an uncaught exception would silently stop all future runs
            System.err.println("MissedDoseJob failed: " + e.getMessage());
        }
    }
}