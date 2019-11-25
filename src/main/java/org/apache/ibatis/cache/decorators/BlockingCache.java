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
package org.apache.ibatis.cache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * Simple blocking decorator
 * 简易阻塞装饰者缓存模式
 * Simple and inefficient version of EhCache's BlockingCache decorator.
 * It sets a lock over a cache key when the element is not found in cache.
 * This way, other threads will wait until this element is filled instead of hitting the database.
 * 它是EhCache's BlockingCache装饰的简单有效实现，它对缓存key值设置锁，当元素在缓存中找不到时。
 * 这种方式，会使得其他线程等待直到该元素被填充，而不是去访问数据库
 * @author Eduardo Macarron
 *
 */
public class BlockingCache implements Cache {

  private long timeout;
  private final Cache delegate;// 被装饰的缓存一般是指PerpetualCache
  private final ConcurrentHashMap<Object, ReentrantLock> locks;

  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  // 有一种场景是相当适用的。当获取key对应的键值时，若是该key值不存在，则会进行阻塞，其他线程是获取不到对应键值的。
  // 而当有put操作后，而首个持有锁的线程就会释放锁，则其他线程就能够获取该key值对应键值
  @Override
  public void putObject(Object key, Object value) {
    try {
      delegate.putObject(key, value);
    } finally {// 设置对象后，就要释放锁
      releaseLock(key);
    }
  }

  @Override
  public Object getObject(Object key) {
    // 获取值前先锁定key值，这样其他线程就不能访问key值为键的value值
    acquireLock(key);
    Object value = delegate.getObject(key);
    if (value != null) {// 若该值存在，则释放锁，并返回该值
      releaseLock(key);
    }
    return value;
  }

  @Override
  public Object removeObject(Object key) {// 移除对象的操作，只是释放该key值持有的锁，并没有将key和value从被装饰的缓存中删除
    // despite of its name, this method is called only to release locks
    releaseLock(key);
    return null;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  // 获取该key值对应的锁，若是不存在，则创建一个锁放到locks中。
  private ReentrantLock getLockForKey(Object key) {
    return locks.computeIfAbsent(key, k -> new ReentrantLock());
  }

  private void acquireLock(Object key) {
    // 获取key值对应的锁
    Lock lock = getLockForKey(key);
    if (timeout > 0) {// 若有设置超时时间
      try {
        // 锁定对应的时间，以毫秒计算
        boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        if (!acquired) {// 若是返回false，则说明不能锁
          throw new CacheException("Couldn't get a lock in " + timeout + " for the key " +  key + " at the cache " + delegate.getId());
        }
      } catch (InterruptedException e) {
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    } else {
      // 设置锁
      lock.lock();
    }
  }

  private void releaseLock(Object key) {
    ReentrantLock lock = locks.get(key);
    if (lock.isHeldByCurrentThread()) {// 如果当前线程持有并发锁，则释放锁
      lock.unlock();
    }
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}
