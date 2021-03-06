package com.android.server.mtm.iaware.brjob.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.rms.iaware.AwareLog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobStatus;
import com.android.server.mtm.iaware.brjob.scheduler.AwareStateChangedListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SIMStatusController extends AwareStateController {
    private static final String CONDITION_NAME = "SIMStatus";
    private static final String TAG = "SIMStatusController";
    private static SIMStatusController mSingleton;
    private static final Object sCreationLock = new Object();
    private BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (SIMStatusController.this.isViceCardSimStateBroadcast(intent)) {
                    if (SIMStatusController.this.DEBUG) {
                        AwareLog.i(SIMStatusController.TAG, "iaware_brjob br is vice card sim, not change state");
                    }
                    return;
                }
                if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                    String state = SIMStatusController.this.getSimStateFromIntent(intent);
                    if (SIMStatusController.this.DEBUG) {
                        String str = SIMStatusController.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("iaware_brjob onReceiver sim state:");
                        stringBuilder.append(state);
                        AwareLog.i(str, stringBuilder.toString());
                    }
                    if (!SIMStatusController.this.mSimStatus.equals(state)) {
                        synchronized (SIMStatusController.this.mLock) {
                            boolean changeToSatisfied = false;
                            SIMStatusController.this.mSimStatus = state;
                            List<AwareJobStatus> changedJobs = new ArrayList();
                            Iterator it = SIMStatusController.this.mTrackedJobs.iterator();
                            while (it.hasNext()) {
                                AwareJobStatus job = (AwareJobStatus) it.next();
                                String value = job.getActionFilterValue("SIMStatus");
                                if (SIMStatusController.this.matchSimStatus(value) && !job.isSatisfied("SIMStatus")) {
                                    job.setSatisfied("SIMStatus", true);
                                    changeToSatisfied = true;
                                    changedJobs.add(job);
                                } else if (!SIMStatusController.this.matchSimStatus(value) && job.isSatisfied("SIMStatus")) {
                                    job.setSatisfied("SIMStatus", false);
                                }
                            }
                            if (changeToSatisfied) {
                                if (SIMStatusController.this.DEBUG) {
                                    AwareLog.i(SIMStatusController.TAG, "iaware_brjob onControllerStateChanged");
                                }
                                SIMStatusController.this.mStateChangedListener.onControllerStateChanged(changedJobs);
                            }
                        }
                    }
                }
            }
        }
    };
    private String mSimStatus = "UNKNOWN";
    private TelephonyManager mTelephonyManager;
    @GuardedBy("mLock")
    private final ArrayList<AwareJobStatus> mTrackedJobs = new ArrayList();

    public static SIMStatusController get(AwareJobSchedulerService jms) {
        SIMStatusController sIMStatusController;
        synchronized (sCreationLock) {
            if (mSingleton == null) {
                mSingleton = new SIMStatusController(jms, jms.getContext(), jms.getLock());
            }
            sIMStatusController = mSingleton;
        }
        return sIMStatusController;
    }

    private SIMStatusController(AwareStateChangedListener stateChangedListener, Context context, Object lock) {
        super(stateChangedListener, context, lock);
        if (this.mContext != null) {
            this.mContext.registerReceiverAsUser(this.mSimStateReceiver, UserHandle.SYSTEM, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"), null, null);
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
    }

    public void maybeStartTrackingJobLocked(AwareJobStatus job) {
        if (job != null && job.hasConstraint("SIMStatus")) {
            this.mSimStatus = getSimState();
            String filter = job.getActionFilterValue("SIMStatus");
            if (TextUtils.isEmpty(filter)) {
                AwareLog.w(TAG, "iaware_brjob sim status value is null");
                job.setSatisfied("SIMStatus", false);
                return;
            }
            if (this.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brjob start tracking SimState, filter:");
                stringBuilder.append(filter);
                stringBuilder.append("current: ");
                stringBuilder.append(this.mSimStatus);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (job.getIntent() == null || !isViceCardSimStateBroadcast(job.getIntent())) {
                job.setSatisfied("SIMStatus", matchSimStatus(filter));
                addJobLocked(this.mTrackedJobs, job);
            } else {
                job.setSatisfied("SIMStatus", true);
            }
        }
    }

    private boolean matchSimStatus(String filterValue) {
        String[] values = filterValue.split("[:]");
        for (Object equals : values) {
            if (this.mSimStatus.equals(equals)) {
                return true;
            }
        }
        return false;
    }

    public void maybeStopTrackingJobLocked(AwareJobStatus jobStatus) {
        if (jobStatus != null && jobStatus.hasConstraint("SIMStatus")) {
            if (this.DEBUG) {
                AwareLog.i(TAG, "iaware_brjob stop tracking begin");
            }
            this.mTrackedJobs.remove(jobStatus);
        }
    }

    public void dump(PrintWriter pw) {
        if (pw != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    SIMStatusController tracked job num: ");
            stringBuilder.append(this.mTrackedJobs.size());
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("        now sim state is ");
            stringBuilder.append(this.mSimStatus);
            pw.println(stringBuilder.toString());
        }
    }

    private String getSimState() {
        if (this.mTelephonyManager == null) {
            if (this.mContext == null) {
                return "UNKNOWN";
            }
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        String simstate = "UNKNOWN";
        switch (this.mTelephonyManager.getSimState()) {
            case 1:
                simstate = AwareJobSchedulerConstants.SIM_STATUS_ABSENT;
                break;
            case 2:
            case 3:
            case 4:
                simstate = AwareJobSchedulerConstants.SIM_STATUS_LOCKED;
                break;
            case 5:
                simstate = AwareJobSchedulerConstants.SIM_STATUS_READY;
                break;
        }
        return simstate;
    }

    private String getSimStateFromIntent(Intent intent) {
        String curSimState = intent.getStringExtra("ss");
        if (curSimState == null) {
            return "UNKNOWN";
        }
        String str;
        if (this.DEBUG) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("iaware_brjob curSimState : ");
            stringBuilder.append(curSimState);
            AwareLog.i(str, stringBuilder.toString());
        }
        str = "UNKNOWN";
        if (AwareJobSchedulerConstants.SIM_STATUS_READY.equals(curSimState)) {
            str = AwareJobSchedulerConstants.SIM_STATUS_READY;
        } else if (AwareJobSchedulerConstants.SIM_STATUS_LOCKED.equals(curSimState) || "INTERNAL_LOCKED".equals(curSimState)) {
            str = AwareJobSchedulerConstants.SIM_STATUS_LOCKED;
        } else if (AwareJobSchedulerConstants.SIM_STATUS_ABSENT.equals(curSimState)) {
            str = AwareJobSchedulerConstants.SIM_STATUS_ABSENT;
        } else {
            str = "UNKNOWN";
        }
        return str;
    }

    private boolean isViceCardSimStateBroadcast(Intent intent) {
        if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
            int subKey = intent.getIntExtra("subscription", -1);
            int defaultSubId = SubscriptionManager.getDefaultSubId();
            if (this.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("iaware_brjob is Vice Card sim state Broadcast, subkey : ");
                stringBuilder.append(subKey);
                stringBuilder.append(", defaultSubId : ");
                stringBuilder.append(defaultSubId);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (subKey == -1 || subKey != defaultSubId) {
                return true;
            }
        }
        return false;
    }
}
