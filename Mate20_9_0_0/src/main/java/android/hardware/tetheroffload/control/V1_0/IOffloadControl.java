package android.hardware.tetheroffload.control.V1_0;

import android.hidl.base.V1_0.DebugInfo;
import android.hidl.base.V1_0.IBase;
import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwBinder.DeathRecipient;
import android.os.IHwInterface;
import android.os.RemoteException;
import com.android.server.usb.descriptors.UsbASFormat;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public interface IOffloadControl extends IBase {
    public static final String kInterfaceName = "android.hardware.tetheroffload.control@1.0::IOffloadControl";

    @FunctionalInterface
    public interface addDownstreamCallback {
        void onValues(boolean z, String str);
    }

    @FunctionalInterface
    public interface getForwardedStatsCallback {
        void onValues(long j, long j2);
    }

    @FunctionalInterface
    public interface initOffloadCallback {
        void onValues(boolean z, String str);
    }

    @FunctionalInterface
    public interface removeDownstreamCallback {
        void onValues(boolean z, String str);
    }

    @FunctionalInterface
    public interface setDataLimitCallback {
        void onValues(boolean z, String str);
    }

    @FunctionalInterface
    public interface setLocalPrefixesCallback {
        void onValues(boolean z, String str);
    }

    @FunctionalInterface
    public interface setUpstreamParametersCallback {
        void onValues(boolean z, String str);
    }

    @FunctionalInterface
    public interface stopOffloadCallback {
        void onValues(boolean z, String str);
    }

    public static final class Proxy implements IOffloadControl {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(interfaceDescriptor());
                stringBuilder.append("@Proxy");
                return stringBuilder.toString();
            } catch (RemoteException e) {
                return "[class or subclass of android.hardware.tetheroffload.control@1.0::IOffloadControl]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        public void initOffload(ITetheringOffloadCallback cb, initOffloadCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOffloadControl.kInterfaceName);
            _hidl_request.writeStrongBinder(cb == null ? null : cb.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readBool(), _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void stopOffload(stopOffloadCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOffloadControl.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readBool(), _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void setLocalPrefixes(ArrayList<String> prefixes, setLocalPrefixesCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOffloadControl.kInterfaceName);
            _hidl_request.writeStringVector((ArrayList) prefixes);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readBool(), _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void getForwardedStats(String upstream, getForwardedStatsCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOffloadControl.kInterfaceName);
            _hidl_request.writeString(upstream);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readInt64(), _hidl_reply.readInt64());
            } finally {
                _hidl_reply.release();
            }
        }

        public void setDataLimit(String upstream, long limit, setDataLimitCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOffloadControl.kInterfaceName);
            _hidl_request.writeString(upstream);
            _hidl_request.writeInt64(limit);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readBool(), _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void setUpstreamParameters(String iface, String v4Addr, String v4Gw, ArrayList<String> v6Gws, setUpstreamParametersCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOffloadControl.kInterfaceName);
            _hidl_request.writeString(iface);
            _hidl_request.writeString(v4Addr);
            _hidl_request.writeString(v4Gw);
            _hidl_request.writeStringVector((ArrayList) v6Gws);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readBool(), _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void addDownstream(String iface, String prefix, addDownstreamCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOffloadControl.kInterfaceName);
            _hidl_request.writeString(iface);
            _hidl_request.writeString(prefix);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readBool(), _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public void removeDownstream(String iface, String prefix, removeDownstreamCallback _hidl_cb) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IOffloadControl.kInterfaceName);
            _hidl_request.writeString(iface);
            _hidl_request.writeString(prefix);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                _hidl_cb.onValues(_hidl_reply.readBool(), _hidl_reply.readString());
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                int _hidl_index_0 = 0;
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16);
                int _hidl_vec_size = _hidl_blob.getInt32(8);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer((long) (_hidl_vec_size * 32), _hidl_blob.handle(), 0, true);
                _hidl_out_hashchain.clear();
                while (true) {
                    int _hidl_index_02 = _hidl_index_0;
                    if (_hidl_index_02 >= _hidl_vec_size) {
                        break;
                    }
                    byte[] _hidl_vec_element = new byte[32];
                    childBlob.copyToInt8Array((long) (_hidl_index_02 * 32), _hidl_vec_element, 32);
                    _hidl_out_hashchain.add(_hidl_vec_element);
                    _hidl_index_0 = _hidl_index_02 + 1;
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean linkToDeath(DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        public boolean unlinkToDeath(DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    public static abstract class Stub extends HwBinder implements IOffloadControl {
        public IHwBinder asBinder() {
            return this;
        }

        public final ArrayList<String> interfaceChain() {
            return new ArrayList(Arrays.asList(new String[]{IOffloadControl.kInterfaceName, IBase.kInterfaceName}));
        }

        public final String interfaceDescriptor() {
            return IOffloadControl.kInterfaceName;
        }

        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList(Arrays.asList(new byte[][]{new byte[]{(byte) 68, (byte) 123, (byte) 0, UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION, (byte) 107, (byte) -55, (byte) 90, (byte) 122, (byte) -81, (byte) -20, (byte) 29, (byte) 102, UsbDescriptor.DESCRIPTORTYPE_BOS, (byte) 111, (byte) 62, (byte) -97, (byte) 118, (byte) -84, (byte) -117, (byte) -64, (byte) 53, (byte) 49, (byte) -109, (byte) 67, (byte) 94, (byte) 85, (byte) 121, (byte) -85, UsbASFormat.EXT_FORMAT_TYPE_III, (byte) 61, (byte) -90, (byte) 25}, new byte[]{(byte) -67, (byte) -38, (byte) -74, (byte) 24, (byte) 77, (byte) 122, (byte) 52, (byte) 109, (byte) -90, (byte) -96, (byte) 125, (byte) -64, UsbASFormat.EXT_FORMAT_TYPE_II, (byte) -116, (byte) -15, (byte) -102, (byte) 105, (byte) 111, (byte) 76, (byte) -86, (byte) 54, (byte) 17, (byte) -59, (byte) 31, (byte) 46, (byte) 20, (byte) 86, (byte) 90, (byte) 20, (byte) -76, UsbDescriptor.DESCRIPTORTYPE_BOS, (byte) -39}}));
        }

        public final void setHALInstrumentation() {
        }

        public final boolean linkToDeath(DeathRecipient recipient, long cookie) {
            return true;
        }

        public final void ping() {
        }

        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0;
            info.arch = 0;
            return info;
        }

        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        public final boolean unlinkToDeath(DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IOffloadControl.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(interfaceDescriptor());
            stringBuilder.append("@Stub");
            return stringBuilder.toString();
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, final HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            boolean _hidl_is_oneway = false;
            boolean _hidl_is_oneway2 = true;
            String iface;
            switch (_hidl_code) {
                case 1:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IOffloadControl.kInterfaceName);
                    initOffload(ITetheringOffloadCallback.asInterface(_hidl_request.readStrongBinder()), new initOffloadCallback() {
                        public void onValues(boolean success, String errMsg) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeBool(success);
                            _hidl_reply.writeString(errMsg);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 2:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IOffloadControl.kInterfaceName);
                    stopOffload(new stopOffloadCallback() {
                        public void onValues(boolean success, String errMsg) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeBool(success);
                            _hidl_reply.writeString(errMsg);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 3:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IOffloadControl.kInterfaceName);
                    setLocalPrefixes(_hidl_request.readStringVector(), new setLocalPrefixesCallback() {
                        public void onValues(boolean success, String errMsg) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeBool(success);
                            _hidl_reply.writeString(errMsg);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 4:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IOffloadControl.kInterfaceName);
                    getForwardedStats(_hidl_request.readString(), new getForwardedStatsCallback() {
                        public void onValues(long rxBytes, long txBytes) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeInt64(rxBytes);
                            _hidl_reply.writeInt64(txBytes);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 5:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IOffloadControl.kInterfaceName);
                    setDataLimit(_hidl_request.readString(), _hidl_request.readInt64(), new setDataLimitCallback() {
                        public void onValues(boolean success, String errMsg) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeBool(success);
                            _hidl_reply.writeString(errMsg);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 6:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IOffloadControl.kInterfaceName);
                    iface = _hidl_request.readString();
                    String v4Addr = _hidl_request.readString();
                    String v4Gw = _hidl_request.readString();
                    setUpstreamParameters(iface, v4Addr, v4Gw, _hidl_request.readStringVector(), new setUpstreamParametersCallback() {
                        public void onValues(boolean success, String errMsg) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeBool(success);
                            _hidl_reply.writeString(errMsg);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 7:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IOffloadControl.kInterfaceName);
                    addDownstream(_hidl_request.readString(), _hidl_request.readString(), new addDownstreamCallback() {
                        public void onValues(boolean success, String errMsg) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeBool(success);
                            _hidl_reply.writeString(errMsg);
                            _hidl_reply.send();
                        }
                    });
                    return;
                case 8:
                    if ((_hidl_flags & 1) != 0) {
                        _hidl_is_oneway = true;
                    }
                    if (_hidl_is_oneway) {
                        _hidl_reply.writeStatus(Integer.MIN_VALUE);
                        _hidl_reply.send();
                        return;
                    }
                    _hidl_request.enforceInterface(IOffloadControl.kInterfaceName);
                    removeDownstream(_hidl_request.readString(), _hidl_request.readString(), new removeDownstreamCallback() {
                        public void onValues(boolean success, String errMsg) {
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeBool(success);
                            _hidl_reply.writeString(errMsg);
                            _hidl_reply.send();
                        }
                    });
                    return;
                default:
                    switch (_hidl_code) {
                        case 256067662:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ArrayList _hidl_out_descriptors = interfaceChain();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeStringVector(_hidl_out_descriptors);
                            _hidl_reply.send();
                            return;
                        case 256131655:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 256136003:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            iface = interfaceDescriptor();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.writeString(iface);
                            _hidl_reply.send();
                            return;
                        case 256398152:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                            _hidl_reply.writeStatus(0);
                            HwBlob _hidl_blob = new HwBlob(16);
                            int _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob.putInt32(8, _hidl_vec_size);
                            _hidl_blob.putBool(12, false);
                            HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                            int _hidl_index_0;
                            while (_hidl_index_0 < _hidl_vec_size) {
                                childBlob.putInt8Array((long) (_hidl_index_0 * 32), (byte[]) _hidl_out_hashchain.get(_hidl_index_0));
                                _hidl_index_0++;
                            }
                            _hidl_blob.putBlob(0, childBlob);
                            _hidl_reply.writeBuffer(_hidl_blob);
                            _hidl_reply.send();
                            return;
                        case 256462420:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            setHALInstrumentation();
                            return;
                        case 256660548:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        case 256921159:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            ping();
                            _hidl_reply.writeStatus(0);
                            _hidl_reply.send();
                            return;
                        case 257049926:
                            if ((_hidl_flags & 1) == 0) {
                                _hidl_is_oneway2 = false;
                            }
                            if (_hidl_is_oneway2) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            DebugInfo _hidl_out_info = getDebugInfo();
                            _hidl_reply.writeStatus(0);
                            _hidl_out_info.writeToParcel(_hidl_reply);
                            _hidl_reply.send();
                            return;
                        case 257120595:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (!_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            _hidl_request.enforceInterface(IBase.kInterfaceName);
                            notifySyspropsChanged();
                            return;
                        case 257250372:
                            if ((_hidl_flags & 1) != 0) {
                                _hidl_is_oneway = true;
                            }
                            if (_hidl_is_oneway) {
                                _hidl_reply.writeStatus(Integer.MIN_VALUE);
                                _hidl_reply.send();
                                return;
                            }
                            return;
                        default:
                            return;
                    }
            }
        }
    }

    void addDownstream(String str, String str2, addDownstreamCallback adddownstreamcallback) throws RemoteException;

    IHwBinder asBinder();

    DebugInfo getDebugInfo() throws RemoteException;

    void getForwardedStats(String str, getForwardedStatsCallback getforwardedstatscallback) throws RemoteException;

    ArrayList<byte[]> getHashChain() throws RemoteException;

    void initOffload(ITetheringOffloadCallback iTetheringOffloadCallback, initOffloadCallback initoffloadcallback) throws RemoteException;

    ArrayList<String> interfaceChain() throws RemoteException;

    String interfaceDescriptor() throws RemoteException;

    boolean linkToDeath(DeathRecipient deathRecipient, long j) throws RemoteException;

    void notifySyspropsChanged() throws RemoteException;

    void ping() throws RemoteException;

    void removeDownstream(String str, String str2, removeDownstreamCallback removedownstreamcallback) throws RemoteException;

    void setDataLimit(String str, long j, setDataLimitCallback setdatalimitcallback) throws RemoteException;

    void setHALInstrumentation() throws RemoteException;

    void setLocalPrefixes(ArrayList<String> arrayList, setLocalPrefixesCallback setlocalprefixescallback) throws RemoteException;

    void setUpstreamParameters(String str, String str2, String str3, ArrayList<String> arrayList, setUpstreamParametersCallback setupstreamparameterscallback) throws RemoteException;

    void stopOffload(stopOffloadCallback stopoffloadcallback) throws RemoteException;

    boolean unlinkToDeath(DeathRecipient deathRecipient) throws RemoteException;

    static IOffloadControl asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IOffloadControl)) {
            return (IOffloadControl) iface;
        }
        IOffloadControl proxy = new Proxy(binder);
        try {
            Iterator it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                if (((String) it.next()).equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static IOffloadControl castFrom(IHwInterface iface) {
        return iface == null ? null : asInterface(iface.asBinder());
    }

    static IOffloadControl getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IOffloadControl getService(boolean retry) throws RemoteException {
        return getService(HealthServiceWrapper.INSTANCE_VENDOR, retry);
    }

    static IOffloadControl getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IOffloadControl getService() throws RemoteException {
        return getService(HealthServiceWrapper.INSTANCE_VENDOR);
    }
}
