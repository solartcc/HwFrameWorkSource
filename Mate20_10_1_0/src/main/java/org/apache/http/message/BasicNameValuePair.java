package org.apache.http.message;

import org.apache.http.NameValuePair;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.LangUtils;

@Deprecated
public class BasicNameValuePair implements NameValuePair, Cloneable {
    private final String name;
    private final String value;

    public BasicNameValuePair(String name2, String value2) {
        if (name2 != null) {
            this.name = name2;
            this.value = value2;
            return;
        }
        throw new IllegalArgumentException("Name may not be null");
    }

    @Override // org.apache.http.NameValuePair
    public String getName() {
        return this.name;
    }

    @Override // org.apache.http.NameValuePair
    public String getValue() {
        return this.value;
    }

    public String toString() {
        int len = this.name.length();
        String str = this.value;
        if (str != null) {
            len += str.length() + 1;
        }
        CharArrayBuffer buffer = new CharArrayBuffer(len);
        buffer.append(this.name);
        if (this.value != null) {
            buffer.append("=");
            buffer.append(this.value);
        }
        return buffer.toString();
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!(object instanceof NameValuePair)) {
            return false;
        }
        BasicNameValuePair that = (BasicNameValuePair) object;
        if (!this.name.equals(that.name) || !LangUtils.equals(this.value, that.value)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return LangUtils.hashCode(LangUtils.hashCode(17, this.name), this.value);
    }

    @Override // java.lang.Object
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
