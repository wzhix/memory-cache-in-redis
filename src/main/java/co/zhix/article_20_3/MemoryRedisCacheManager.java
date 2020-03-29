package co.zhix.article_20_3;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.SimpleRedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 内存缓存加持的 Redis 缓存管理器，同时该类实现了 {@link SchedulingConfigurer} 来自动向容器注册定期清理内存缓存的 CRON 任务。
 *
 * <p>注：若容器没有配置 {@link EnableScheduling}，则 {@link #configureTasks(ScheduledTaskRegistrar)}
 * 不会执行，此时需要手动调用 {@link #clearMemoryCache()} 来清理内存缓存。
 *
 * @see RedisCacheManager
 * @see AbstractCacheManager
 * @see MemoryCacheDecorator
 * @author zhix
 * @version 2020/3/28
 */
@Slf4j
public class MemoryRedisCacheManager extends RedisCacheManager implements SchedulingConfigurer {

  /** 只有这里配置的缓存才会加持内存缓存层。 */
  private final List<String> decoratedCacheNameList;

  /** 配置内存缓存层自动清理的 CRON 表达式，默认为 30 秒清理一次。 */
  private final String clearCacheCronExpression;

  public MemoryRedisCacheManager(
      RedisConnectionFactory connectionFactory,
      RedisCacheConfiguration defaultCacheConfiguration,
      Map<String, RedisCacheConfiguration> initialCaches,
      List<String> decoratedCacheNameList) {
    this(
        connectionFactory,
        defaultCacheConfiguration,
        initialCaches,
        decoratedCacheNameList,
        // 默认每分钟的第 30 秒清理一次缓存
        "0/30 * * * * ?");
  }

  public MemoryRedisCacheManager(
      RedisConnectionFactory connectionFactory,
      RedisCacheConfiguration defaultCacheConfiguration,
      Map<String, RedisCacheConfiguration> initialCaches,
      List<String> decoratedCacheNameList,
      String clearCacheCronExpression) {
    super(
        new SimpleRedisCacheWriter(connectionFactory),
        defaultCacheConfiguration,
        MapUtils.emptyIfNull(initialCaches),
        false);
    this.decoratedCacheNameList = ListUtils.emptyIfNull(decoratedCacheNameList);
    this.clearCacheCronExpression = clearCacheCronExpression;
  }

  /**
   * 覆盖 {@link AbstractCacheManager} 的装饰缓存方法，若参数中的缓存包含 {@link #decoratedCacheNameList}
   * 中，则在将该对象包装成具有内存缓存能力的对象。
   *
   * @see AbstractCacheManager
   * @param cache 原缓存对象
   * @return 原缓存对象或具有内存缓存能力的对象
   */
  @NonNull
  @Override
  protected Cache decorateCache(@NonNull Cache cache) {
    // 先让基类包装一次
    Cache superCache = super.decorateCache(cache);
    // 判断是否为该缓存配置了内存缓存
    if (decoratedCacheNameList.contains(cache.getName())) {
      // 包装缓存
      return new MemoryCacheDecorator(superCache);
    }
    // 不包装缓存
    return superCache;
  }

  @Override
  public void configureTasks(@NonNull ScheduledTaskRegistrar taskRegistrar) {
    // 向容器注册清理缓存的 CRON 任务，若没有配置 @EnableScheduling，这里不会执行，需要手动调用 clearMemoryCache 清理
    taskRegistrar.addCronTask(this::clearMemoryCache, clearCacheCronExpression);
    log.info("Register cron task for clear memory cache, cron = {}", clearCacheCronExpression);
  }

  /** 手动清理所有内存缓存。 */
  public void clearMemoryCache() {
    Collection<String> cacheNames = getCacheNames();
    for (String cacheName : cacheNames) {
      Cache cache = getCache(cacheName);
      if (!(cache instanceof MemoryCacheDecorator)) {
        continue;
      }
      // 对所有 MemoryCacheDecorator 类型的缓存做清理
      MemoryCacheDecorator memoryCacheDecorator = (MemoryCacheDecorator) cache;
      memoryCacheDecorator.cleanMemoryCache();
    }
  }

  /**
   * 手动存储对象至内存缓存。
   *
   * @param cacheName 内存缓存名称
   * @param key 键
   * @param value 被缓存的对象
   * @return 是否至少有一个缓存接受了存储操作
   */
  public boolean storeMemoryCache(
      @NonNull String cacheName, @NonNull Object key, @NonNull Object value) {
    // 查找名为 cacheName 的 MemoryCacheDecorator 缓存
    Cache cache = getCache(cacheName);
    if (!(cache instanceof MemoryCacheDecorator)) {
      log.error("Missing memory cache for name {}", cacheName);
      return false;
    }
    MemoryCacheDecorator memoryCacheDecorator = (MemoryCacheDecorator) cache;
    memoryCacheDecorator.putMemoryCache(key, value);
    return true;
  }

  /**
   * 手动查询内存缓存中是否有给定键的对象。
   *
   * @param cacheName 内存缓存名称
   * @param key 键
   * @return 返回缓存对象
   */
  public Object getMemoryCache(@NonNull String cacheName, @NonNull Object key) {
    // 查找名为 cacheName 的 MemoryCacheDecorator 缓存
    Cache cache = getCache(cacheName);
    if (!(cache instanceof MemoryCacheDecorator)) {
      log.error("Missing memory cache for name {}", cacheName);
      return null;
    }
    MemoryCacheDecorator memoryCacheDecorator = (MemoryCacheDecorator) cache;
    return memoryCacheDecorator.get(key, Object.class);
  }
}
