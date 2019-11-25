/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * SPI for cache providers.
 * 用于缓存提供程序的SPI
 * <p>
 * One instance of cache will be created for each namespace.
 * 每个命名空间会创建一个缓存实例
 * <p>
 * The cache implementation must have a constructor that receives the cache id as an String parameter.
 *  缓存实现必须有一个构造器，将缓存id作为String参数
 * <p>
 * MyBatis will pass the namespace as id to the constructor.
 * MyBatis将会把命名空间作为id传给构造器
 * <pre>
 * public MyCache(final String id) {
 *  if (id == null) {
 *    throw new IllegalArgumentException("Cache instances require an ID");
 *  }
 *  this.id = id;
 *  initialize();
 * }
 * </pre>
 *
 * @author Clinton Begin
 */

public interface Cache {

  /**
   * @return The identifier of this cache
   * 该缓存的识别id
   */
  String getId();

  /**
   * @param key Can be any object but usually it is a {@link CacheKey}
   * @param value The result of a select.
   */
  void putObject(Object key, Object value);

  /**
   * @param key The key
   * @return The object stored in the cache.
   */
  Object getObject(Object key);

  /**
   * As of 3.3.0 this method is only called during a rollback
   * for any previous value that was missing in the cache.
   * This lets any blocking cache to release the lock that
   * may have previously put on the key.
   * A blocking cache puts a lock when a value is null
   * and releases it when the value is back again.
   * This way other threads will wait for the value to be
   * available instead of hitting the database.
   * 从3.3.0版本开始，仅在回滚期间针对缓存中丢失的任何先前值调用此方法。
   * 该方法让之前设置key值的阻塞缓存释放锁。当value值为null时阻塞
   * 缓存设置了一个所，并且当值返回后释放锁。这样，其他线程将等待该值可用，而不是访问数据库。
   *
   * @param key The key
   * @return Not used
   */
  Object removeObject(Object key);

  /**
   * Clears this cache instance.
   * 清楚这个缓存实例
   */
  void clear();

  /**
   * Optional. This method is not called by the core.
   * 可选，该方法不会作为核心调用
   * @return The number of elements stored in the cache (not its capacity).
   */
  int getSize();

  /**
   * Optional. As of 3.2.6 this method is no longer called by the core.
   * <p> 可选，该方法不再作为核心调用
   * Any locking needed by the cache must be provided internally by the cache provider.
   * 缓存所需的任何锁定都必须由缓存提供程序内部提供。
   * @return A ReadWriteLock
   */
  default ReadWriteLock getReadWriteLock() {
    return null;
  }

}
