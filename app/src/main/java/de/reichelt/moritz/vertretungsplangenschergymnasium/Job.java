package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

class Job {

    private static final int JOB_ID = 14062001;
    private static JobScheduler mScheduler;


    /**
     * Schedules a job with constraints that are received from SharedPreferences
     *
     * @param context     Application context
     * @param packageName String with the package name
     */
    public static void schedule(Context context, String packageName) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int interval = Integer.parseInt(sharedPreferences.getString("pref_sync_interval", "60"));
        int network_type = Integer.parseInt(sharedPreferences.getString("pref_network_type", "1"));

        mScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        ComponentName serviceName = new ComponentName(packageName,
                NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceName)
                .setPersisted(true)
                .setRequiredNetworkType(network_type)
                .setPeriodic(interval * 60000)
                .setBackoffCriteria(60000, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        JobInfo jobInfo = builder.build();

        assert mScheduler != null;
        mScheduler.schedule(jobInfo);
    }


    /**
     * Cancels all jobs. This is normally not recommended as it might cancel jobs that are still
     * needed running. In our case it is safe as we always have just one job scheduled at a time.
     */
    public static void cancel() {
        if (mScheduler != null) {
            mScheduler.cancelAll();
            mScheduler = null;
        }
    }
}
