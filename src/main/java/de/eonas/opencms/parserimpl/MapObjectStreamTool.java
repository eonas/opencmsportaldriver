package de.eonas.opencms.parserimpl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class MapObjectStreamTool<T> {
    @Nullable
    static String[] readArrayOfString(@NotNull StringReader stream) throws IOException {
        Integer size = readNumberOrNull(stream);
        if (size == null) return null;
        String[] ret = new String[size];
        for (int i = 0; i < size; i++) {
            ret[i] = readString(stream);
        }
        return ret;
    }

    @Nullable
    private static Integer readNumberOrNull(@NotNull StringReader stream) throws IOException {
        int format = stream.read();
        if (format == 'n') return null;
        int length = (format - '0') + 1;
        String string = getString(stream, length);
        return Integer.parseInt(string);
    }

    private static void writeNumberOrNull(@NotNull StringWriter stream, @Nullable Integer number) {
        if (number == null) {
            stream.write('n');
            return;
        }
        String decimalString = String.format("%d", number);
        stream.write('0' + (decimalString.length() - 1));
        stream.write(decimalString);
    }

    static void writeArrayOfString(@NotNull StringWriter stream, @Nullable String[] arrayOfValues) throws IOException {
        Integer size = null;
        if (arrayOfValues != null) {
            size = arrayOfValues.length;
        }
        writeNumberOrNull(stream, size);
        if (arrayOfValues != null) {
            for (String arrayOfValue : arrayOfValues) {
                writeString(stream, arrayOfValue);
            }
        }
    }


    static public void writeString(@NotNull StringWriter out, @Nullable String data) throws IOException {
        Integer len = null;
        if (data != null) {
            len = data.length();
        }
        writeNumberOrNull(out, len);
        if (data != null) {
            out.write(data);
        }
    }

    @Nullable
    static public String readString(@NotNull StringReader in) throws IOException {
        Integer len = readNumberOrNull(in);
        if (len == null) return null;
        return getString(in, len);
    }

    @NotNull
    private static String getString(@NotNull StringReader in, Integer len) throws IOException {
        char[] ret = new char[len];
        int readLen = 0;
        while (readLen < len) {
            int lenReadThisTime = in.read(ret, readLen, len - readLen);
            if (lenReadThisTime == -1) throw new IOException("End-Of-Stream while reading number");
            readLen += lenReadThisTime;
        }
        return new String(ret);
    }

    public void writeMap(@NotNull StringWriter stream, @Nullable Map<String, T> map) throws IOException {
        if (map == null) {
            writeNumberOrNull(stream, null);
            return;
        }
        final Set<Map.Entry<String, T>> entries = map.entrySet();
        writeNumberOrNull(stream, entries.size());
        for (Map.Entry<String, T> e : entries) {
            writeString(stream, e.getKey());
            T o = e.getValue();
            writeObject(stream, o);
        }
    }

    @NotNull
    public Map<String, T> readMap(@NotNull StringReader stream) throws IOException, ClassNotFoundException {
        Integer size = readNumberOrNull(stream);
        Map<String, T> map = new HashMap<String, T>();
        if (size == null) return map;
        for (int i = 0; i < size; i++) {
            String key = readString(stream);
            T p = readObject(stream);
            map.put(key, p);
        }
        return map;
    }

    @Nullable
    protected abstract T readObject(StringReader stream) throws IOException, ClassNotFoundException;

    protected abstract void writeObject(StringWriter out, T o) throws IOException;
}
