package com.android.internal.telephony.imsphone;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Registrant;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telecom.Connection.RttTextStream;
import android.telecom.Connection.VideoProvider;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ims.ImsCallProfile;
import android.text.TextUtils;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.ims.internal.ImsVideoCallProviderWrapper;
import com.android.ims.internal.ImsVideoCallProviderWrapper.ImsVideoProviderWrapperCallback;
import com.android.internal.telephony.Call.State;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Connection.PostDialState;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.UUSInfo;
import java.util.Objects;

public class ImsPhoneConnection extends Connection implements ImsVideoProviderWrapperCallback {
    private static final boolean DBG = true;
    private static final int EVENT_DTMF_DELAY_DONE = 5;
    private static final int EVENT_DTMF_DONE = 1;
    private static final int EVENT_NEXT_POST_DIAL = 3;
    private static final int EVENT_PAUSE_DONE = 2;
    private static final int EVENT_WAKE_LOCK_TIMEOUT = 4;
    private static final String LOG_TAG = "ImsPhoneConnection";
    private static final int PAUSE_DELAY_MILLIS = 3000;
    private static final int WAKE_LOCK_TIMEOUT_MILLIS = 60000;
    private static final boolean isImsAsNormal = HuaweiTelephonyConfigs.isHisiPlatform();
    private static final boolean mIsDocomo = SystemProperties.get("ro.product.custom", "NULL").contains("docomo");
    private long mConferenceConnectTime = 0;
    private long mDisconnectTime;
    private boolean mDisconnected;
    private int mDtmfToneDelay = 0;
    private Bundle mExtras = new Bundle();
    private Handler mHandler;
    private Messenger mHandlerMessenger;
    private ImsCall mImsCall;
    private ImsVideoCallProviderWrapper mImsVideoCallProviderWrapper;
    private boolean mIsEmergency = false;
    private boolean mIsMergeInProcess = false;
    private boolean mIsRttEnabledForCall = false;
    private boolean mIsVideoEnabled = true;
    private boolean mIsWifiStateFromExtras = false;
    private ImsPhoneCallTracker mOwner;
    private ImsPhoneCall mParent;
    private WakeLock mPartialWakeLock;
    private int mPreciseDisconnectCause = 0;
    private ImsRttTextHandler mRttTextHandler;
    private RttTextStream mRttTextStream;
    private boolean mShouldIgnoreVideoStateChanges = false;
    private UUSInfo mUusInfo;

    class MyHandler extends Handler {
        MyHandler(Looper l) {
            super(l);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ImsPhoneConnection.this.mHandler.sendMessageDelayed(ImsPhoneConnection.this.mHandler.obtainMessage(5), (long) ImsPhoneConnection.this.mDtmfToneDelay);
                    return;
                case 2:
                case 3:
                case 5:
                    ImsPhoneConnection.this.processNextPostDialChar();
                    return;
                case 4:
                    ImsPhoneConnection.this.releaseWakeLock();
                    return;
                default:
                    return;
            }
        }
    }

    public ImsPhoneConnection(Phone phone, ImsCall imsCall, ImsPhoneCallTracker ct, ImsPhoneCall parent, boolean isUnknown) {
        super(5);
        createWakeLock(phone.getContext());
        acquireWakeLock();
        this.mOwner = ct;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mHandlerMessenger = new Messenger(this.mHandler);
        this.mImsCall = imsCall;
        if (imsCall == null || imsCall.getCallProfile() == null) {
            this.mNumberPresentation = 3;
            this.mCnapNamePresentation = 3;
            this.mRedirectNumberPresentation = 1;
        } else {
            this.mAddress = imsCall.getCallProfile().getCallExtra("oi");
            this.mCnapName = imsCall.getCallProfile().getCallExtra("cna");
            this.mNumberPresentation = ImsCallProfile.OIRToPresentation(imsCall.getCallProfile().getCallExtraInt("oir"));
            this.mCnapNamePresentation = ImsCallProfile.OIRToPresentation(imsCall.getCallProfile().getCallExtraInt("cnap"));
            updateMediaCapabilities(imsCall);
            if (mIsDocomo) {
                this.mRedirectAddress = imsCall.getCallProfile().getCallExtra("redirect_number");
                this.mRedirectNumberPresentation = ImsCallProfile.OIRToPresentation(imsCall.getCallProfile().getCallExtraInt("redirect_number_presentation"));
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("new ImsPhoneConnection  mRedirectNumberPresentation ");
                stringBuilder.append(this.mRedirectNumberPresentation);
                Rlog.i(str, stringBuilder.toString());
            }
        }
        this.mIsIncoming = isUnknown ^ 1;
        this.mCreateTime = System.currentTimeMillis();
        this.mUusInfo = null;
        updateWifiState();
        updateExtras(imsCall);
        this.mParent = parent;
        this.mParent.attach(this, this.mIsIncoming ? State.INCOMING : State.DIALING);
        fetchDtmfToneDelay(phone);
        if (phone.getContext().getResources().getBoolean(17957066)) {
            setAudioModeIsVoip(true);
        }
    }

    public ImsPhoneConnection(Phone phone, String dialString, ImsPhoneCallTracker ct, ImsPhoneCall parent, boolean isEmergency) {
        super(5);
        createWakeLock(phone.getContext());
        acquireWakeLock();
        this.mOwner = ct;
        this.mHandler = new MyHandler(this.mOwner.getLooper());
        this.mDialString = dialString;
        this.mAddress = PhoneNumberUtils.extractNetworkPortionAlt(dialString);
        this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(dialString);
        this.mIsIncoming = false;
        this.mCnapName = null;
        this.mCnapNamePresentation = 1;
        this.mNumberPresentation = 1;
        this.mCreateTime = System.currentTimeMillis();
        this.mParent = parent;
        parent.attachFake(this, State.DIALING);
        this.mIsEmergency = isEmergency;
        fetchDtmfToneDelay(phone);
        if (phone.getContext().getResources().getBoolean(17957066)) {
            setAudioModeIsVoip(true);
        }
    }

    public void dispose() {
    }

    static boolean equalsHandlesNulls(Object a, Object b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }

    static boolean equalsBaseDialString(String a, String b) {
        if (a == null) {
            if (b != null) {
                return false;
            }
        } else if (b == null || !a.startsWith(b)) {
            return false;
        }
        return true;
    }

    private int applyLocalCallCapabilities(ImsCallProfile localProfile, int capabilities) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("applyLocalCallCapabilities - localProfile = ");
        stringBuilder.append(localProfile);
        Rlog.i(str, stringBuilder.toString());
        capabilities = Connection.removeCapability(capabilities, 4);
        if (this.mIsVideoEnabled) {
            switch (localProfile.mCallType) {
                case 3:
                case 4:
                    capabilities = Connection.addCapability(capabilities, 4);
                    break;
            }
            return capabilities;
        }
        Rlog.i(LOG_TAG, "applyLocalCallCapabilities - disabling video (overidden)");
        return capabilities;
    }

    private static int applyRemoteCallCapabilities(ImsCallProfile remoteProfile, int capabilities) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("applyRemoteCallCapabilities - remoteProfile = ");
        stringBuilder.append(remoteProfile);
        Rlog.w(str, stringBuilder.toString());
        capabilities = Connection.removeCapability(capabilities, 8);
        switch (remoteProfile.mCallType) {
            case 3:
            case 4:
                return Connection.addCapability(capabilities, 8);
            default:
                return capabilities;
        }
    }

    public String getOrigDialString() {
        return this.mDialString;
    }

    public ImsPhoneCall getCall() {
        return this.mParent;
    }

    public long getDisconnectTime() {
        return this.mDisconnectTime;
    }

    public long getHoldingStartTime() {
        return this.mHoldingStartTime;
    }

    public long getHoldDurationMillis() {
        if (getState() != State.HOLDING) {
            return 0;
        }
        return SystemClock.elapsedRealtime() - this.mHoldingStartTime;
    }

    public void setDisconnectCause(int cause) {
        this.mCause = cause;
    }

    public String getVendorDisconnectCause() {
        return null;
    }

    public ImsPhoneCallTracker getOwner() {
        return this.mOwner;
    }

    public State getState() {
        if (this.mDisconnected) {
            return State.DISCONNECTED;
        }
        return super.getState();
    }

    public void deflect(String number) throws CallStateException {
        if (this.mParent.getState().isRinging()) {
            try {
                if (this.mImsCall != null) {
                    this.mImsCall.deflect(number);
                    return;
                }
                throw new CallStateException("no valid ims call to deflect");
            } catch (ImsException e) {
                throw new CallStateException("cannot deflect call");
            }
        }
        throw new CallStateException("phone not ringing");
    }

    public void hangup() throws CallStateException {
        if (this.mDisconnected) {
            throw new CallStateException("disconnected");
        }
        this.mOwner.hangup(this);
    }

    public void separate() throws CallStateException {
        throw new CallStateException("not supported");
    }

    public void proceedAfterWaitChar() {
        if (this.mPostDialState != PostDialState.WAIT) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImsPhoneConnection.proceedAfterWaitChar(): Expected getPostDialState() to be WAIT but was ");
            stringBuilder.append(this.mPostDialState);
            Rlog.w(str, stringBuilder.toString());
            return;
        }
        setPostDialState(PostDialState.STARTED);
        processNextPostDialChar();
    }

    public void proceedAfterWildChar(String str) {
        if (this.mPostDialState != PostDialState.WILD) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImsPhoneConnection.proceedAfterWaitChar(): Expected getPostDialState() to be WILD but was ");
            stringBuilder.append(this.mPostDialState);
            Rlog.w(str2, stringBuilder.toString());
            return;
        }
        setPostDialState(PostDialState.STARTED);
        StringBuilder buf = new StringBuilder(str);
        buf.append(this.mPostDialString.substring(this.mNextPostDialChar));
        this.mPostDialString = buf.toString();
        this.mNextPostDialChar = 0;
        String str3 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("proceedAfterWildChar: new postDialString is ");
        stringBuilder2.append(this.mPostDialString);
        Rlog.d(str3, stringBuilder2.toString());
        processNextPostDialChar();
    }

    public void cancelPostDial() {
        setPostDialState(PostDialState.CANCELLED);
    }

    void onHangupLocal() {
        this.mCause = 3;
    }

    public boolean onDisconnect(int cause) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onDisconnect: cause=");
        stringBuilder.append(cause);
        Rlog.d(str, stringBuilder.toString());
        this.mCause = cause;
        return onDisconnect();
    }

    int getImsIndex() throws CallStateException {
        String sIndex = this.mImsCall.getCallSession().getCallId();
        if (sIndex == null || sIndex.equals("")) {
            throw new CallStateException("ISM index not yet assigned");
        }
        int iIndex = Integer.parseInt(sIndex);
        if (iIndex >= 0) {
            return iIndex;
        }
        throw new CallStateException("ISM index not yet assigned");
    }

    public boolean onDisconnect() {
        boolean changed = false;
        if (!this.mDisconnected) {
            this.mDisconnectTime = System.currentTimeMillis();
            this.mDuration = SystemClock.elapsedRealtime() - this.mConnectTimeReal;
            this.mDisconnected = true;
            this.mOwner.mPhone.notifyDisconnect(this);
            notifyDisconnect(this.mCause);
            this.mOwner.updateCallLog(this, this.mOwner.mPhone);
            if (this.mParent != null) {
                changed = this.mParent.connectionDisconnected(this);
            } else {
                Rlog.d(LOG_TAG, "onDisconnect: no parent");
            }
            boolean changed2 = changed;
            synchronized (this) {
                if (this.mImsCall != null) {
                    this.mImsCall.close();
                }
                this.mImsCall = null;
            }
            changed = changed2;
        }
        releaseWakeLock();
        return changed;
    }

    void onConnectedInOrOut() {
        this.mConnectTime = System.currentTimeMillis();
        this.mConnectTimeReal = SystemClock.elapsedRealtime();
        this.mDuration = 0;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onConnectedInOrOut: connectTime=");
        stringBuilder.append(this.mConnectTime);
        Rlog.d(str, stringBuilder.toString());
        if (!this.mIsIncoming) {
            processNextPostDialChar();
        }
        releaseWakeLock();
    }

    void onStartedHolding() {
        this.mHoldingStartTime = SystemClock.elapsedRealtime();
    }

    private boolean processPostDialChar(char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            Message dtmfComplete = this.mHandler.obtainMessage(1);
            dtmfComplete.replyTo = this.mHandlerMessenger;
            this.mOwner.sendDtmf(c, dtmfComplete);
        } else if (c == ',') {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 3000);
        } else if (c == ';') {
            setPostDialState(PostDialState.WAIT);
        } else if (c != 'N') {
            return false;
        } else {
            setPostDialState(PostDialState.WILD);
        }
        return true;
    }

    protected void finalize() {
        releaseWakeLock();
    }

    private void processNextPostDialChar() {
        if (this.mPostDialState != PostDialState.CANCELLED) {
            char c;
            if (this.mPostDialString == null || this.mPostDialString.length() <= this.mNextPostDialChar) {
                setPostDialState(PostDialState.COMPLETE);
                c = 0;
            } else {
                setPostDialState(PostDialState.STARTED);
                String str = this.mPostDialString;
                int i = this.mNextPostDialChar;
                this.mNextPostDialChar = i + 1;
                c = str.charAt(i);
                if (!processPostDialChar(c)) {
                    this.mHandler.obtainMessage(3).sendToTarget();
                    String str2 = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("processNextPostDialChar: c=");
                    stringBuilder.append(c);
                    stringBuilder.append(" isn't valid!");
                    Rlog.e(str2, stringBuilder.toString());
                    return;
                }
            }
            notifyPostDialListenersNextChar(c);
            Registrant postDialHandler = this.mOwner.mPhone.getPostDialHandler();
            if (postDialHandler != null) {
                Message messageForRegistrant = postDialHandler.messageForRegistrant();
                Message notifyMessage = messageForRegistrant;
                if (messageForRegistrant != null) {
                    PostDialState state = this.mPostDialState;
                    AsyncResult ar = AsyncResult.forMessage(notifyMessage);
                    ar.result = this;
                    ar.userObj = state;
                    notifyMessage.arg1 = c;
                    notifyMessage.sendToTarget();
                }
            }
        }
    }

    private void setPostDialState(PostDialState s) {
        if (this.mPostDialState != PostDialState.STARTED && s == PostDialState.STARTED) {
            acquireWakeLock();
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4), 60000);
        } else if (this.mPostDialState == PostDialState.STARTED && s != PostDialState.STARTED) {
            this.mHandler.removeMessages(4);
            releaseWakeLock();
        }
        this.mPostDialState = s;
        notifyPostDialListeners();
    }

    private void createWakeLock(Context context) {
        this.mPartialWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, LOG_TAG);
    }

    private void acquireWakeLock() {
        Rlog.d(LOG_TAG, "acquireWakeLock");
        this.mPartialWakeLock.acquire();
    }

    void releaseWakeLock() {
        if (this.mPartialWakeLock != null) {
            synchronized (this.mPartialWakeLock) {
                if (this.mPartialWakeLock.isHeld()) {
                    Rlog.d(LOG_TAG, "releaseWakeLock");
                    this.mPartialWakeLock.release();
                }
            }
        }
    }

    private void fetchDtmfToneDelay(Phone phone) {
        PersistableBundle b = ((CarrierConfigManager) phone.getContext().getSystemService("carrier_config")).getConfigForSubId(phone.getSubId());
        if (b != null) {
            this.mDtmfToneDelay = b.getInt("ims_dtmf_tone_delay_int");
        }
    }

    public int getNumberPresentation() {
        return this.mNumberPresentation;
    }

    public UUSInfo getUUSInfo() {
        return this.mUusInfo;
    }

    public Connection getOrigConnection() {
        return null;
    }

    public synchronized boolean isMultiparty() {
        boolean z;
        z = this.mImsCall != null && this.mImsCall.isMultiparty();
        return z;
    }

    public synchronized boolean isConferenceHost() {
        boolean z;
        z = this.mImsCall != null && this.mImsCall.isConferenceHost();
        return z;
    }

    public boolean isMemberOfPeerConference() {
        return isConferenceHost() ^ 1;
    }

    public synchronized ImsCall getImsCall() {
        return this.mImsCall;
    }

    public synchronized void setImsCall(ImsCall imsCall) {
        this.mImsCall = imsCall;
    }

    public void changeParent(ImsPhoneCall parent) {
        this.mParent = parent;
    }

    public boolean update(ImsCall imsCall, State state) {
        boolean z = false;
        if (state == State.ACTIVE) {
            if (imsCall.isPendingHold()) {
                Rlog.w(LOG_TAG, "update : state is ACTIVE, but call is pending hold, skipping");
                return false;
            }
            if (this.mParent.getState().isRinging() || this.mParent.getState().isDialing()) {
                onConnectedInOrOut();
            }
            if (this.mParent.getState().isRinging() || this.mParent == this.mOwner.mBackgroundCall || this.mParent == this.mOwner.mRingingCall) {
                this.mParent.detach(this);
                this.mParent = this.mOwner.mForegroundCall;
                this.mParent.attach(this);
            }
        } else if (state == State.HOLDING) {
            onStartedHolding();
            if (isImsAsNormal && (this.mParent == this.mOwner.mForegroundCall || this.mParent == this.mOwner.mRingingCall)) {
                this.mParent.detach(this);
                this.mParent = this.mOwner.mBackgroundCall;
                this.mParent.attach(this);
            }
        }
        boolean updateParent = this.mParent.update(this, imsCall, state);
        boolean updateWifiState = updateWifiState();
        boolean updateAddressDisplay = updateAddressDisplay(imsCall);
        boolean updateMediaCapabilities = updateMediaCapabilities(imsCall);
        boolean updateExtras = updateExtras(imsCall);
        if (updateParent || updateWifiState || updateAddressDisplay || updateMediaCapabilities || updateExtras) {
            z = true;
        }
        return z;
    }

    public int getPreciseDisconnectCause() {
        return this.mPreciseDisconnectCause;
    }

    public void setPreciseDisconnectCause(int cause) {
        this.mPreciseDisconnectCause = cause;
    }

    public void onDisconnectConferenceParticipant(Uri endpoint) {
        ImsCall imsCall = getImsCall();
        if (imsCall != null) {
            try {
                imsCall.removeParticipants(new String[]{endpoint.toString()});
            } catch (ImsException e) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onDisconnectConferenceParticipant: no session in place. Failed to disconnect endpoint = ");
                stringBuilder.append(endpoint);
                Rlog.e(str, stringBuilder.toString());
            }
        }
    }

    public void setConferenceConnectTime(long conferenceConnectTime) {
        this.mConferenceConnectTime = conferenceConnectTime;
    }

    public long getConferenceConnectTime() {
        return this.mConferenceConnectTime;
    }

    public boolean updateAddressDisplay(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }
        boolean changed = false;
        ImsCallProfile callProfile = imsCall.getCallProfile();
        if (callProfile != null && (isIncoming() || HuaweiTelephonyConfigs.isQcomPlatform())) {
            String address = callProfile.getCallExtra("oi");
            String name = callProfile.getCallExtra("cna");
            int nump = ImsCallProfile.OIRToPresentation(callProfile.getCallExtraInt("oir"));
            int namep = ImsCallProfile.OIRToPresentation(callProfile.getCallExtraInt("cnap"));
            String redirect_address = imsCall.getCallProfile().getCallExtra("redirect_number");
            int redirect_nump = ImsCallProfile.OIRToPresentation(callProfile.getCallExtraInt("redirect_number_presentation"));
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateAddressDisplay: callId = ");
            stringBuilder.append(getTelecomCallId());
            stringBuilder.append(" address = ");
            stringBuilder.append(Rlog.pii(LOG_TAG, address));
            stringBuilder.append(" name = ");
            stringBuilder.append(Rlog.pii(LOG_TAG, name));
            stringBuilder.append(" nump = ");
            stringBuilder.append(nump);
            stringBuilder.append(" namep = ");
            stringBuilder.append(namep);
            Rlog.d(str, stringBuilder.toString());
            if (equalsHandlesNulls(this.mAddress, address)) {
                this.mAddress = address;
                changed = true;
            }
            if (!equalsHandlesNulls(this.mRedirectAddress, redirect_address) && mIsDocomo) {
                this.mRedirectAddress = address;
                changed = true;
            }
            if (!this.mIsMergeInProcess) {
                if (HuaweiTelephonyConfigs.isQcomPlatform()) {
                    if (!equalsHandlesNulls(this.mAddress, address)) {
                        this.mAddress = address;
                        changed = true;
                    }
                } else if (!equalsBaseDialString(this.mAddress, address)) {
                    this.mAddress = address;
                    changed = true;
                }
                if (TextUtils.isEmpty(name)) {
                    if (!TextUtils.isEmpty(this.mCnapName)) {
                        this.mCnapName = "";
                        changed = true;
                    }
                } else if (!name.equals(this.mCnapName)) {
                    this.mCnapName = name;
                    changed = true;
                }
                if (this.mNumberPresentation != nump) {
                    this.mNumberPresentation = nump;
                    changed = true;
                }
                if (mIsDocomo && this.mRedirectNumberPresentation != redirect_nump) {
                    this.mRedirectNumberPresentation = redirect_nump;
                    changed = true;
                }
                if (this.mCnapNamePresentation != namep) {
                    this.mCnapNamePresentation = namep;
                    changed = true;
                }
            }
        }
        return changed;
    }

    public boolean updateMediaCapabilities(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }
        boolean changed = false;
        try {
            StringBuilder stringBuilder;
            ImsCallProfile negotiatedCallProfile = imsCall.getCallProfile();
            if (negotiatedCallProfile != null) {
                int oldVideoState = getVideoState();
                int newVideoState = ImsCallProfile.getVideoStateFromImsCallProfile(negotiatedCallProfile);
                if (oldVideoState != newVideoState) {
                    if (VideoProfile.isPaused(oldVideoState) && !VideoProfile.isPaused(newVideoState)) {
                        this.mShouldIgnoreVideoStateChanges = false;
                    }
                    if (this.mShouldIgnoreVideoStateChanges) {
                        Rlog.d(LOG_TAG, "updateMediaCapabilities - ignoring video state change due to paused state.");
                    } else {
                        updateVideoState(newVideoState);
                        changed = true;
                    }
                    if (!VideoProfile.isPaused(oldVideoState) && VideoProfile.isPaused(newVideoState)) {
                        this.mShouldIgnoreVideoStateChanges = true;
                    }
                }
                if (negotiatedCallProfile.mMediaProfile != null) {
                    this.mIsRttEnabledForCall = negotiatedCallProfile.mMediaProfile.isRttCall();
                    String str;
                    if (this.mIsRttEnabledForCall && this.mRttTextHandler == null) {
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateMediaCapabilities -- turning RTT on, profile=");
                        stringBuilder.append(negotiatedCallProfile);
                        Rlog.d(str, stringBuilder.toString());
                        startRttTextProcessing();
                        onRttInitiated();
                        changed = true;
                    } else if (!(this.mIsRttEnabledForCall || this.mRttTextHandler == null)) {
                        str = LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("updateMediaCapabilities -- turning RTT off, profile=");
                        stringBuilder.append(negotiatedCallProfile);
                        Rlog.d(str, stringBuilder.toString());
                        this.mRttTextHandler.tearDown();
                        this.mRttTextHandler = null;
                        onRttTerminated();
                        changed = true;
                    }
                }
            }
            int capabilities = getConnectionCapabilities();
            if (this.mOwner.isCarrierDowngradeOfVtCallSupported()) {
                capabilities = Connection.addCapability(capabilities, 3);
            } else {
                capabilities = Connection.removeCapability(capabilities, 3);
            }
            ImsCallProfile localCallProfile = imsCall.getLocalCallProfile();
            String str2 = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("update localCallProfile=");
            stringBuilder.append(localCallProfile);
            Rlog.v(str2, stringBuilder.toString());
            if (localCallProfile != null) {
                capabilities = applyLocalCallCapabilities(localCallProfile, capabilities);
            }
            ImsCallProfile remoteCallProfile = imsCall.getRemoteCallProfile();
            String str3 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("update remoteCallProfile=");
            stringBuilder2.append(remoteCallProfile);
            Rlog.v(str3, stringBuilder2.toString());
            if (remoteCallProfile != null) {
                capabilities = applyRemoteCallCapabilities(remoteCallProfile, capabilities);
            }
            if (getConnectionCapabilities() != capabilities) {
                setConnectionCapabilities(capabilities);
                changed = true;
            }
            int newAudioQuality = getAudioQualityFromCallProfile(localCallProfile, remoteCallProfile);
            if (getAudioQuality() != newAudioQuality) {
                setAudioQuality(newAudioQuality);
                changed = true;
            }
        } catch (ImsException e) {
        }
        return changed;
    }

    private void updateVideoState(int newVideoState) {
        if (this.mImsVideoCallProviderWrapper != null) {
            this.mImsVideoCallProviderWrapper.onVideoStateChanged(newVideoState);
        }
        setVideoState(newVideoState);
    }

    public void sendRttModifyRequest(RttTextStream textStream) {
        getImsCall().sendRttModifyRequest();
        setCurrentRttTextStream(textStream);
    }

    public void sendRttModifyResponse(RttTextStream textStream) {
        boolean accept = textStream != null;
        getImsCall().sendRttModifyResponse(accept);
        if (accept) {
            setCurrentRttTextStream(textStream);
        } else {
            Rlog.e(LOG_TAG, "sendRttModifyResponse: foreground call has no connections");
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code skipped:
            r2.mRttTextHandler.sendToInCall(r3);
     */
    /* JADX WARNING: Missing block: B:12:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onRttMessageReceived(String message) {
        synchronized (this) {
            if (this.mRttTextHandler == null) {
                Rlog.w(LOG_TAG, "onRttMessageReceived: RTT text handler not available. Attempting to create one.");
                if (this.mRttTextStream == null) {
                    Rlog.e(LOG_TAG, "onRttMessageReceived: Unable to process incoming message. No textstream available");
                    return;
                }
                createRttTextHandler();
            }
        }
    }

    public void setCurrentRttTextStream(RttTextStream rttTextStream) {
        synchronized (this) {
            this.mRttTextStream = rttTextStream;
            if (this.mRttTextHandler == null && this.mIsRttEnabledForCall) {
                Rlog.i(LOG_TAG, "setCurrentRttTextStream: Creating a text handler");
                createRttTextHandler();
            }
        }
    }

    public boolean hasRttTextStream() {
        return this.mRttTextStream != null;
    }

    public boolean isRttEnabledForCall() {
        return this.mIsRttEnabledForCall;
    }

    public void startRttTextProcessing() {
        synchronized (this) {
            if (this.mRttTextStream == null) {
                Rlog.w(LOG_TAG, "startRttTextProcessing: no RTT text stream. Ignoring.");
            } else if (this.mRttTextHandler != null) {
                Rlog.w(LOG_TAG, "startRttTextProcessing: RTT text handler already exists");
            } else {
                createRttTextHandler();
            }
        }
    }

    private void createRttTextHandler() {
        this.mRttTextHandler = new ImsRttTextHandler(Looper.getMainLooper(), new -$$Lambda$ImsPhoneConnection$gXYXXIQcibrbO2gQqP7d18avaBI(this));
        this.mRttTextHandler.initialize(this.mRttTextStream);
    }

    public boolean updateWifiState() {
        if (this.mIsWifiStateFromExtras) {
            return false;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateWifiState: ");
        stringBuilder.append(this.mOwner.isVowifiEnabled());
        Rlog.d(str, stringBuilder.toString());
        if (isWifi() == this.mOwner.isVowifiEnabled()) {
            return false;
        }
        setWifi(this.mOwner.isVowifiEnabled());
        return true;
    }

    private void updateWifiStateFromExtras(Bundle extras) {
        if (extras.containsKey("CallRadioTech") || extras.containsKey("callRadioTech")) {
            ImsCall call = getImsCall();
            boolean isWifi = false;
            if (call != null) {
                isWifi = call.isWifiCall();
            }
            if (isWifi() != isWifi) {
                setWifi(isWifi);
            }
        }
    }

    boolean updateExtras(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }
        ImsCallProfile callProfile = imsCall.getCallProfile();
        Bundle extras = callProfile != null ? callProfile.mCallExtras : null;
        if (extras == null) {
            Rlog.d(LOG_TAG, "Call profile extras are null.");
        }
        boolean changed = areBundlesEqual(extras, this.mExtras) ^ 1;
        if (changed) {
            updateWifiStateFromExtras(extras);
            this.mExtras.clear();
            this.mExtras.putAll(extras);
            setConnectionExtras(this.mExtras);
        }
        return changed;
    }

    private static boolean areBundlesEqual(Bundle extras, Bundle newExtras) {
        boolean z = true;
        if (extras == null || newExtras == null) {
            if (extras != newExtras) {
                z = false;
            }
            return z;
        } else if (extras.size() != newExtras.size()) {
            return false;
        } else {
            for (String key : extras.keySet()) {
                if (key != null && !Objects.equals(extras.get(key), newExtras.get(key))) {
                    return false;
                }
            }
            return true;
        }
    }

    private int getAudioQualityFromCallProfile(ImsCallProfile localCallProfile, ImsCallProfile remoteCallProfile) {
        int i = 1;
        if (localCallProfile == null || remoteCallProfile == null || localCallProfile.mMediaProfile == null) {
            return 1;
        }
        boolean z = false;
        boolean isEvsCodecHighDef = localCallProfile.mMediaProfile.mAudioQuality == 18 || localCallProfile.mMediaProfile.mAudioQuality == 19 || localCallProfile.mMediaProfile.mAudioQuality == 20;
        if ((localCallProfile.mMediaProfile.mAudioQuality == 2 || localCallProfile.mMediaProfile.mAudioQuality == 6 || isEvsCodecHighDef) && remoteCallProfile.mRestrictCause == 0) {
            z = true;
        }
        if (z) {
            i = 2;
        }
        return i;
    }

    public String toString() {
        State state = getState();
        StringBuilder sb = new StringBuilder();
        sb.append("[ImsPhoneConnection objId: ");
        sb.append(System.identityHashCode(this));
        sb.append(" state: ");
        if (state != null) {
            sb.append(state.toString());
        } else {
            sb.append("null");
        }
        sb.append(" mParent objId: ");
        synchronized (this) {
            if (this.mParent == null) {
                sb.append("null");
            } else {
                sb.append(System.identityHashCode(this.mParent));
            }
        }
        sb.append(" telecomCallID: ");
        sb.append(getTelecomCallId());
        sb.append(" ImsCall: ");
        synchronized (this) {
            if (this.mImsCall == null) {
                sb.append("null");
            } else {
                sb.append(this.mImsCall);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public void setVideoProvider(VideoProvider videoProvider) {
        super.setVideoProvider(videoProvider);
        if (videoProvider instanceof ImsVideoCallProviderWrapper) {
            this.mImsVideoCallProviderWrapper = (ImsVideoCallProviderWrapper) videoProvider;
        }
    }

    protected boolean isEmergency() {
        return this.mIsEmergency;
    }

    public void onReceiveSessionModifyResponse(int status, VideoProfile requestProfile, VideoProfile responseProfile) {
        if (status == 1 && this.mShouldIgnoreVideoStateChanges) {
            int currentVideoState = getVideoState();
            int newVideoState = responseProfile.getVideoState();
            int changedBits = (currentVideoState ^ newVideoState) & 3;
            if (changedBits != 0) {
                currentVideoState = (currentVideoState & (~(changedBits & currentVideoState))) | (changedBits & newVideoState);
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceiveSessionModifyResponse : received ");
                stringBuilder.append(VideoProfile.videoStateToString(requestProfile.getVideoState()));
                stringBuilder.append(" / ");
                stringBuilder.append(VideoProfile.videoStateToString(responseProfile.getVideoState()));
                stringBuilder.append(" while paused ; sending new videoState = ");
                stringBuilder.append(VideoProfile.videoStateToString(currentVideoState));
                Rlog.d(str, stringBuilder.toString());
                setVideoState(currentVideoState);
            }
        }
    }

    public void pauseVideo(int source) {
        if (this.mImsVideoCallProviderWrapper != null) {
            this.mImsVideoCallProviderWrapper.pauseVideo(getVideoState(), source);
        }
    }

    public void resumeVideo(int source) {
        if (this.mImsVideoCallProviderWrapper != null) {
            this.mImsVideoCallProviderWrapper.resumeVideo(getVideoState(), source);
        }
    }

    public boolean wasVideoPausedFromSource(int source) {
        if (this.mImsVideoCallProviderWrapper == null) {
            return false;
        }
        return this.mImsVideoCallProviderWrapper.wasVideoPausedFromSource(source);
    }

    public void handleMergeStart() {
        this.mIsMergeInProcess = true;
        onConnectionEvent("android.telecom.event.MERGE_START", null);
    }

    public void handleMergeComplete() {
        this.mIsMergeInProcess = false;
        onConnectionEvent("android.telecom.event.MERGE_COMPLETE", null);
    }

    public void changeToPausedState() {
        int newVideoState = getVideoState() | 4;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ImsPhoneConnection: changeToPausedState - setting paused bit; newVideoState=");
        stringBuilder.append(VideoProfile.videoStateToString(newVideoState));
        Rlog.i(str, stringBuilder.toString());
        updateVideoState(newVideoState);
        this.mShouldIgnoreVideoStateChanges = true;
    }

    public void changeToUnPausedState() {
        int newVideoState = getVideoState() & -5;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ImsPhoneConnection: changeToUnPausedState - unsetting paused bit; newVideoState=");
        stringBuilder.append(VideoProfile.videoStateToString(newVideoState));
        Rlog.i(str, stringBuilder.toString());
        updateVideoState(newVideoState);
        this.mShouldIgnoreVideoStateChanges = false;
    }

    public void handleDataEnabledChange(boolean isDataEnabled) {
        this.mIsVideoEnabled = isDataEnabled;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleDataEnabledChange: isDataEnabled=");
        stringBuilder.append(isDataEnabled);
        stringBuilder.append("; updating local video availability.");
        Rlog.i(str, stringBuilder.toString());
        updateMediaCapabilities(getImsCall());
        if (this.mImsVideoCallProviderWrapper != null) {
            this.mImsVideoCallProviderWrapper.setIsVideoEnabled(hasCapabilities(4));
        }
    }
}
