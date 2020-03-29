package co.zhix.article_20_3;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.SerializationDelegate;
import org.springframework.lang.NonNull;

/**
 * 内存缓存的序列化代理，保存在内存缓存中的对象应当被序列化来保证每次取缓存时都是全新的对象实例。
 *
 * @see ConcurrentMapCache
 * @see SerializationDelegate
 * @author zhix
 * @version 2020/3/28
 */
final class FastjsonSerializationDelegate implements Serializer<Object>, Deserializer<Object> {

  @NonNull
  @Override
  public Object deserialize(@NonNull InputStream inputStream) throws IOException {
    return JSON.parseObject(inputStream, Object.class);
  }

  @Override
  public void serialize(@NonNull Object object, @NonNull OutputStream outputStream)
      throws IOException {
    // 这里需要带上 SerializerFeature.WriteClassName，否则 List<Long> 经过序列化反序列化会变成 List<Integer>
    JSON.writeJSONString(outputStream, object, SerializerFeature.WriteClassName);
  }
}
