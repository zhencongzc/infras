package com.cmbc.infras.health.redis;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;


/**
 * 操作Redis工具类
 */
@Component
public class DataRedisUtil {

    public static String redis_addrs;
    public static String redis_passwd;
    public static String isCluster;

    @Value("${redis.addrs}")
    public void setRedis_addrs(String redis_addrs) {
        DataRedisUtil.redis_addrs = redis_addrs;
    }

    @Value("${redis.passwd}")
    public void setRedis_passwd(String redis_passwd) {
        DataRedisUtil.redis_passwd = redis_passwd;
    }

    @Value("${redis.cluster}")
    public void setIsCluster(String isCluster) {
        DataRedisUtil.isCluster = isCluster;
    }

    private static final Logger logger = LogManager.getLogger(DataRedisUtil.class);

    private static RedisConnectionPool redisPool = RedisConnectionPool.getInstance();

    /**
     * 尝试获取分布式锁
     *
     * @param key        锁key
     * @param requestId  值(请求标识)
     * @param expireTime 超期时间
     * @return 是否获取成功
     */
    public static boolean lock(String key, String requestId, Long expireTime) {
        boolean res = false;
        try {
            if ("true".equals(isCluster)) {
                JedisCluster jcd = ClusterRedisConnectionPool.getInstance().getDataConnection(redis_addrs, redis_passwd);
                if (jcd != null) {
                    String result = jcd.set(key, requestId, "NX", "PX", expireTime);
                    if ("OK".equals(result)) {
                        res = true;
                    } else {
                        res = false;
                    }
                }
            } else {
                JedisPool pool = redisPool.getDataConnection(redis_addrs, redis_passwd);
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    String result = jedis.set(key, requestId, "NX", "PX", expireTime);
                    if ("OK".equals(result)) {
                        res = true;
                    } else {
                        res = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisPool.returnResource(pool, jedis);
                }
            }
        } catch (Exception e) {
            logger.error("添加缓存发生错误", e);
        }
        return res;
    }

    /**
     * 删除缓存
     *
     * @param key key
     * @return 添加结果
     */
    public static long delete(String key) {
        long res = 0;
        try {
            if ("true".equals(isCluster)) {
                JedisCluster jcd = ClusterRedisConnectionPool.getInstance().getDataConnection(redis_addrs, redis_passwd);
                if (jcd != null) {
                    res = jcd.del(key);
                }
            } else {
                JedisPool pool = redisPool.getDataConnection(redis_addrs, redis_passwd);
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    res = jedis.del(key);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisPool.returnResource(pool, jedis);
                }
            }
        } catch (Exception e) {
            logger.error("添加缓存发生错误", e);
        }
        return res;
    }

    /**
     * 把 String保存到缓存服务器中
     *
     * @param key   key值
     * @param value 保存数据
     * @return 添加结果
     */
    public static String addStringToRedis(String key, String value, Long expireTime) {
        String lastVal = null;
        try {
            if ("true".equals(isCluster)) {
                JedisCluster jcd = ClusterRedisConnectionPool.getInstance().getDataConnection(redis_addrs, redis_passwd);
                if (jcd != null) {
                    if (expireTime != null) {
                        lastVal = jcd.set(key, value, "NX", "PX", expireTime);
                    } else {
                        lastVal = jcd.set(key, value);
                    }
                }
            } else {
                JedisPool pool = redisPool.getDataConnection(redis_addrs, redis_passwd);
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    if (expireTime != null) {
                        lastVal = jedis.set(key, value, "NX", "PX", expireTime);
                    } else {
                        lastVal = jedis.set(key, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisPool.returnResource(pool, jedis);
                }
            }
        } catch (Exception e) {
            logger.error("添加缓存错误", e);
        }
        return lastVal;
    }

    /**
     * 从Redis中获取数据
     *
     * @param key Redis key
     * @return 数据
     */
    public static String getStringFromRedis(String key) {
        String value = null;
        try {
            if ("true".equals(isCluster)) {
                JedisCluster jcd = ClusterRedisConnectionPool.getInstance().getDataConnection(redis_addrs, redis_passwd);
                if (jcd != null) {
                    if (jcd.exists(key)) {
                        value = jcd.get(key);
                        value = !StringUtils.isEmpty(value) && !"nil".equalsIgnoreCase(value) ? value : null;
                    }
                }
            } else {
                JedisPool pool = redisPool.getSessionConnection(redis_addrs, redis_passwd);
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    value = jedis.get(key);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisPool.returnResource(pool, jedis);
                }
            }
        } catch (Exception e) {
            logger.error("获取缓存错误", e);
        }
        return value;
    }
}
