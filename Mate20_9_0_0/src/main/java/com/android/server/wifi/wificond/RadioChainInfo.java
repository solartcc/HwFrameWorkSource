package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;

public class RadioChainInfo implements Parcelable {
    public static final Creator<RadioChainInfo> CREATOR = new Creator<RadioChainInfo>() {
        public RadioChainInfo createFromParcel(Parcel in) {
            RadioChainInfo result = new RadioChainInfo();
            result.chainId = in.readInt();
            result.level = in.readInt();
            return result;
        }

        public RadioChainInfo[] newArray(int size) {
            return new RadioChainInfo[size];
        }
    };
    private static final String TAG = "RadioChainInfo";
    public int chainId;
    public int level;

    public RadioChainInfo(int chainId, int level) {
        this.chainId = chainId;
        this.level = level;
    }

    public boolean equals(Object rhs) {
        boolean z = true;
        if (this == rhs) {
            return true;
        }
        if (!(rhs instanceof RadioChainInfo)) {
            return false;
        }
        RadioChainInfo chainInfo = (RadioChainInfo) rhs;
        if (chainInfo == null) {
            return false;
        }
        if (!(this.chainId == chainInfo.chainId && this.level == chainInfo.level)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.chainId), Integer.valueOf(this.level)});
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.chainId);
        out.writeInt(this.level);
    }
}
