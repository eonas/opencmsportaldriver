package de.eonas.opencms.parserimpl;

import org.apache.pluto.driver.url.PortalURLParameter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class MapObjectStreamTool<T> {
    public void writeMap ( @NotNull ObjectOutputStream stream, @NotNull Map<String, T> map) throws IOException {
        final Set<Map.Entry<String, T>> entries = map.entrySet();
        stream.writeInt(entries.size());
        for ( Map.Entry<String,T> e: entries) {
            stream.writeUTF(e.getKey());
            T o = e.getValue();
            writeObject(stream, o);
        }
    }

    @NotNull
    public Map<String, T> readMap ( @NotNull ObjectInputStream stream ) throws IOException, ClassNotFoundException {
        int size = stream.readInt();
        Map<String,T> map = new HashMap<String, T>();
        for ( int i = 0; i < size; i++ ) {
            String key = stream.readUTF();
            T p = readObject(stream);
            map.put(key, p);
        }
        return map;
    }

    @NotNull
    protected abstract T readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException;

    protected abstract void writeObject ( ObjectOutputStream out, T o ) throws IOException;
}
