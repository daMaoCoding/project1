package com.xinbo.fundstransfer.utils.randutil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

public class JedisLock {

    private static final Lock NO_LOCK = new Lock(new UUID(0l,0l));
    
    private static final int ONE_SECOND = 1000;

    public static final int DEFAULT_EXPIRY_TIME_MILLIS = Integer.getInteger("com.github.jedis.lock.expiry.millis", 60 * ONE_SECOND);
    public static final int DEFAULT_ACQUIRE_TIMEOUT_MILLIS = Integer.getInteger("com.github.jedis.lock.acquiry.millis", 10 * ONE_SECOND);
    public static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = Integer.getInteger("com.github.jedis.lock.acquiry.resolution.millis", 100);

    private final StringRedisTemplate jedis;

    private final String lockKeyPath;

    private final int lockExpiryInMillis;
    private final int acquiryTimeoutInMillis;
    private final UUID lockUUID;

    private Lock lock = null;

    protected static class Lock {
        private UUID uuid;

        protected Lock(UUID uuid) {
            this.uuid = uuid;
        }
        
        protected static Lock fromString(String text) {
            try {
                UUID theUUID = UUID.fromString(text);
                return new Lock(theUUID);
            } catch (Exception any) {
                return NO_LOCK;
            }
        }
        
        public UUID getUUID() {
            return uuid;
        }

        @Override
        public String toString() {
            return uuid.toString();  
        }

        boolean isExpiredOrMine(UUID otherUUID) {
            return this.getUUID().equals(NO_LOCK) || this.getUUID().equals(otherUUID);
        }
        
        boolean isMine(UUID otherUUID) {
            return this.getUUID().equals(otherUUID);
        }
    }
    
    
    /**
     * Detailed constructor with default acquire timeout 10000 msecs and lock
     * expiration of 60000 msecs.
     * 
     * @param jedis
     * @param lockKey
     *            lock key (ex. account:1, ...)
     */
    public JedisLock(StringRedisTemplate jedis, String lockKey) {
        this(jedis, lockKey, DEFAULT_ACQUIRE_TIMEOUT_MILLIS, DEFAULT_EXPIRY_TIME_MILLIS);
    }

    /**
     * Detailed constructor with default lock expiration of 60000 msecs.
     * 
     * @param jedis
     * @param lockKey
     *            lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis
     *            acquire timeout in miliseconds (default: 10000 msecs)
     */
    public JedisLock(StringRedisTemplate jedis, String lockKey, int acquireTimeoutMillis) {
        this(jedis, lockKey, acquireTimeoutMillis, DEFAULT_EXPIRY_TIME_MILLIS);
    }

    /**
     * Detailed constructor.
     * 
     * @param jedis
     * @param lockKey
     *            lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis
     *            acquire timeout in miliseconds (default: 10000 msecs)
     * @param expiryTimeMillis
     *            lock expiration in miliseconds (default: 60000 msecs)
     */
    public JedisLock(StringRedisTemplate jedis, String lockKey, int acquireTimeoutMillis, int expiryTimeMillis) {
        this(jedis, lockKey, acquireTimeoutMillis, expiryTimeMillis, UUID.randomUUID());
    }

    /**
     * Detailed constructor.
     * 
     * @param jedis
     * @param lockKey
     *            lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis
     *            acquire timeout in miliseconds (default: 10000 msecs)
     * @param expiryTimeMillis
     *            lock expiration in miliseconds (default: 60000 msecs)
     * @param uuid
     *            unique identification of this lock
     */
    public JedisLock(StringRedisTemplate jedis, String lockKey, int acquireTimeoutMillis, int expiryTimeMillis, UUID uuid) {
        this.jedis = jedis;
        this.lockKeyPath = lockKey;
        this.acquiryTimeoutInMillis = acquireTimeoutMillis;
        this.lockExpiryInMillis = expiryTimeMillis+1;
        this.lockUUID = uuid;;
    }
    
    /**
     * @return lock uuid
     */
    public UUID getLockUUID() {
        return lockUUID;
    }

    /**
     * @return lock key path
     */
    public String getLockKeyPath() {
        return lockKeyPath;
    }

    /**
     * Acquire lock.
     * 
     * @return true if lock is acquired, false acquire timeouted
     * @throws InterruptedException
     *             in case of thread interruption
     */
    public synchronized boolean acquire() throws InterruptedException {
        return acquire(jedis);
    }

    /**
     * Acquire lock.
     * 
     * @param jedis
     * @return true if lock is acquired, false acquire timeouted
     * @throws InterruptedException
     *             in case of thread interruption
     */
    protected synchronized boolean acquire(StringRedisTemplate jedis) throws InterruptedException {
        int timeout = acquiryTimeoutInMillis;
        while (timeout >= 0) {

            final Lock newLock = asLock();
            
            Boolean reply = jedis.execute(new RedisCallback<Boolean>() {
				@Override
				public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
					return connection.set(lockKeyPath.getBytes(), newLock.toString().getBytes(), Expiration.milliseconds(lockExpiryInMillis), SetOption.SET_IF_ABSENT);
				}
			});
            if (reply) {
                this.lock = newLock;
                return true;
            }

            final String currentValueStr = jedis.opsForValue().get(lockKeyPath);
            final Lock currentLock = Lock.fromString(currentValueStr);
            if (currentLock.isExpiredOrMine(lockUUID)) {
                String oldValueStr = jedis.opsForValue().getAndSet(lockKeyPath, newLock.toString());
                if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                	jedis.expire(lockKeyPath, (long)lockExpiryInMillis, TimeUnit.MILLISECONDS);
                    this.lock = newLock;
                    return true;
                }
            }

            timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
            Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
        }

        return false;
    }
    
    /**
     * Renew lock.
     * 
     * @return true if lock is acquired, false otherwise
     * @throws InterruptedException
     *             in case of thread interruption
     */
    public boolean renew() throws InterruptedException {
        final Lock lock = Lock.fromString(jedis.opsForValue().get(lockKeyPath));
        if (!lock.isExpiredOrMine(lockUUID)) {
            return false;
        }

        return acquire(jedis);
    }

    /**
     * Acquired lock release.
     */
    public synchronized void release() {
        release(jedis);
    }

    /**
     * Acquired lock release.
     * @param jedis
     */
    protected synchronized void release(StringRedisTemplate jedis) {
        if (isLocked()) {
            final String currentValueStr = jedis.opsForValue().get(lockKeyPath);
            final Lock currentLock = Lock.fromString(currentValueStr);
            if (currentLock.isMine(lockUUID)) {        	
	            jedis.delete(lockKeyPath);
	            this.lock = null;
            }
        }
    }

    /**
     * Check if owns the lock
     * @return  true if lock owned
     */
    public synchronized boolean isLocked() {
        return this.lock != null;
    }
    
    private Lock asLock() {
        return new Lock(lockUUID);
    }


}
