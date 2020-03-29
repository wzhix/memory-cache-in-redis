package co.zhix.article_20_3;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.NonNull;

/**
 * 扩展了 {@link ConcurrentMapCache}，基于 FastJson 序列化。
 *
 * <p>默认的 {@link ConcurrentMapCache}
 * 没有序列化机制，即直接缓存对象的引用，这会导致取出对象时多个线程共享同一个缓存对象，若其中某个线程修改了缓存对象，则影响是全局的。
 *
 * <p>为了避免这个问题则需要加入序列化机制，这里使用基于 FastJson 的序列化，每次缓存对象时，缓存的内容是对象序列化后的字节数组。
 *
 * @see ConcurrentMapCache
 * @see FastjsonSerializationDelegate
 * @author zhix
 * @version 2020/3/28
 */
final class FastjsonSerializationConcurrentMapCache extends ConcurrentMapCache {

  /** 该缓存的 FastJson 序列化实现。 */
  private static final FastjsonSerializationDelegate SERIALIZATION_DELEGATE =
      new FastjsonSerializationDelegate();

  public FastjsonSerializationConcurrentMapCache(@NonNull String name) {
    super(
        name,
        new ConcurrentHashMap<>(256),
        true,
        new SerializationDelegate(SERIALIZATION_DELEGATE, SERIALIZATION_DELEGATE));
  }
}
