package co.zhix.article_20_3;

import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * 内存缓存装饰器，用于支持在基础缓存基础上增加二级的内存缓存。
 *
 * @author zhix
 * @version 2020/3/28
 */
@Slf4j
class MemoryCacheDecorator implements Cache {

  private final ConcurrentMapCache memoryCache;
  private final Cache targetCache;

  public MemoryCacheDecorator(@NonNull Cache targetCache) {
    this.memoryCache =
        new FastjsonSerializationConcurrentMapCache("memory-" + targetCache.getName());
    this.targetCache = targetCache;
  }

  @NonNull
  @Override
  public String getName() {
    // 该缓存是一个隐形的缓存层，外部模块感知不到这一层的存在（包括名称），因此基础的方法都转发到 targetCache 上。
    return targetCache.getName();
  }

  @NonNull
  @Override
  public Object getNativeCache() {
    return targetCache.getNativeCache();
  }

  @Nullable
  @Override
  public Cache.ValueWrapper get(@NonNull Object key) {
    ValueWrapper valueWrapper = memoryCache.get(key);
    if (valueWrapper != null) {
      recordHits(key);
      return valueWrapper;
    } else {
      recordMisses(key);
    }
    valueWrapper = targetCache.get(key);
    if (valueWrapper != null) {
      memoryCache.put(key, valueWrapper.get());
    }
    return valueWrapper;
  }

  @Nullable
  @Override
  public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
    T value = memoryCache.get(key, type);
    if (value != null) {
      recordHits(key);
      return value;
    } else {
      recordMisses(key);
    }
    value = targetCache.get(key, type);
    if (value != null) {
      memoryCache.put(key, value);
    }
    return value;
  }

  @Nullable
  @Override
  public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
    T value = memoryCache.get(key, valueLoader);
    if (value != null) {
      recordHits(key);
      return value;
    } else {
      recordMisses(key);
    }
    value = targetCache.get(key, valueLoader);
    if (value != null) {
      memoryCache.put(key, valueLoader);
    }
    return value;
  }

  @Override
  public void put(@NonNull Object key, @Nullable Object value) {
    memoryCache.put(key, value);
    targetCache.put(key, value);
  }

  @Nullable
  @Override
  public ValueWrapper putIfAbsent(@NonNull Object key, @Nullable Object value) {
    memoryCache.putIfAbsent(key, value);
    return targetCache.putIfAbsent(key, value);
  }

  @Override
  public void evict(@NonNull Object key) {
    memoryCache.evict(key);
    targetCache.evict(key);
  }

  @Override
  public void clear() {
    memoryCache.clear();
    targetCache.clear();
  }

  void cleanMemoryCache() {
    memoryCache.clear();
  }

  void putMemoryCache(@NonNull Object key, @Nullable Object value) {
    memoryCache.put(key, value);
  }

  private void recordHits(Object key) {
    log.debug("Record hit for cache {} with key {}", targetCache.getName(), key);
  }

  private void recordMisses(Object key) {
    log.debug("Record miss for cache {} with key {}", targetCache.getName(), key);
  }
}
