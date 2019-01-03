package org.tron.core.db2.common;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Longs;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import org.tron.core.db.KhaosDatabase.KhaosBlock;
import org.tron.core.db.common.WrappedByteArray;

public class TxCacheDB implements DB<byte[], byte[]>, Flusher {
  private final int BLOCK_COUNT = 70_000; // > 65_536(= 2^16) blocks

  private Map<Key, Long> db = new WeakHashMap<>();
  private Multimap<Long, Key> blockNumMap = ArrayListMultimap.create();

  @Override
  public byte[] get(byte[] key) {
    return Longs.toByteArray(db.get(Key.of(key)));
  }

  @Override
  public void put(byte[] key, byte[] value) {
    Key k = Key.copyOf(key);
    Long v = Longs.fromByteArray(value);
    blockNumMap.put(v, k);
    db.put(k, v);
    removeEldest();
  }

  private void removeEldest() {
    Set<Long> keys = blockNumMap.keySet();
    if (keys.size() > BLOCK_COUNT) {
      keys.stream()
          .min(Long::compareTo)
          .ifPresent(blockNumMap::removeAll);
    }
  }

  @Override
  public long size() {
    return db.size();
  }

  @Override
  public boolean isEmpty() {
    return db.isEmpty();
  }

  @Override
  public void remove(byte[] key) {
    db.remove(Key.of(key));
  }

  @Override
  public Iterator<Map.Entry<byte[],byte[]>> iterator() {
    return Iterators.transform(db.entrySet().iterator(),
        e -> Maps.immutableEntry(e.getKey().getBytes(), Longs.toByteArray(e.getValue())));
  }

  @Override
  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    batch.forEach((k, v) -> this.put(k.getBytes(), v.getBytes()));
  }

  @Override
  public void close() {
    reset();
    db = null;
    blockNumMap = null;
  }

  @Override
  public void reset() {
    db.clear();
    blockNumMap.clear();
  }
}