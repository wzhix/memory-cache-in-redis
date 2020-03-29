package org.springframework.data.redis.cache;

import java.time.Duration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.lang.Nullable;

/**
 * 该类的唯一作用是将 {@link DefaultRedisCacheWriter} 暴露出来。
 *
 * @author zhix
 * @version 2020/3/28
 */
public class SimpleRedisCacheWriter implements RedisCacheWriter {

  private final RedisCacheWriter defaultRedisCacheWriter;

  public SimpleRedisCacheWriter(RedisConnectionFactory connectionFactory) {
    defaultRedisCacheWriter = new DefaultRedisCacheWriter(connectionFactory);
  }

  @Override
  public void put(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
    defaultRedisCacheWriter.put(name, key, value, ttl);
  }

  @Nullable
  @Override
  public byte[] get(String name, byte[] key) {
    return defaultRedisCacheWriter.get(name, key);
  }

  @Nullable
  @Override
  public byte[] putIfAbsent(String name, byte[] key, byte[] value, @Nullable Duration ttl) {
    return defaultRedisCacheWriter.putIfAbsent(name, key, value, ttl);
  }

  @Override
  public void remove(String name, byte[] key) {
    defaultRedisCacheWriter.remove(name, key);
  }

  @Override
  public void clean(String name, byte[] pattern) {
    defaultRedisCacheWriter.clean(name, pattern);
  }
}
