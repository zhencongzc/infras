package com.cmbc.infras.redis;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.cmbc.infras.util.YmlConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;


/**
 * 操作Redis缓存的基础java API<br>
 * 1）所有需要存放到Data服务器的redis缓存器中
 *
 * @author
 * @version v1.0
 */
public class DataRedisUtil {

    private static final Logger log = LogManager.getLogger(DataRedisUtil.class);

    private static RedisConnectionPool redisPool = RedisConnectionPool.getInstance();

    /**
     * 删除缓存
     *
     * @param key key
     * @return 添加结果
     */
    public static long delete(String key) {
        String isCluster = YmlConfig.redisCluster;
        String redis_addrs = YmlConfig.redisAddress;
        String redis_passwd = YmlConfig.redisPassword;
        try {
            if ("true".equals(isCluster)) {
                JedisCluster jcd = ClusterRedisConnectionPool.getInstance().getDataConnection(redis_addrs, redis_passwd);
                if (jcd != null) {
                    return jcd.del(key);
                }
                //JedisCluster会自动释放
                //ClusterRedisConnectionPool.getInstance().closeRedisConnect(jcd);
            } else {
                JedisPool pool = redisPool.getDataConnection(redis_addrs,
                        redis_passwd);
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    return jedis.del(key);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisPool.returnResource(pool, jedis);
                }
            }
        } catch (Exception e) {
            log.error("添加缓存发生错误", e);
        }
        return 0;
    }

    /**
     * 添加缓存
     *
     * @param key        key
     * @param value      value
     * @param expireTime 过期时间 单位是s
     * @return 添加结果
     */
    public static String addStringToRedis(String key, String value,
                                          int expireTime) {
        String isCluster = YmlConfig.redisCluster;
        String redis_addrs = YmlConfig.redisAddress;
        String redis_passwd = YmlConfig.redisPassword;
        String lastVal = null;
        try {
            if ("true".equals(isCluster)) {
                JedisCluster jcd = ClusterRedisConnectionPool.getInstance().getDataConnection(redis_addrs, redis_passwd);
                if (jcd != null) {
                    lastVal = jcd.getSet(key, value);
                    jcd.expire(key, expireTime);
                }
            } else {
                JedisPool pool = redisPool.getDataConnection(redis_addrs, redis_passwd);
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    lastVal = jedis.getSet(key, value);
                    jedis.expire(key, expireTime);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisPool.returnResource(pool, jedis);
                }
            }
        } catch (Exception e) {
            log.error("添加缓存发生错误", e);
        }
        return lastVal;
    }

    /**
     * 把String保存到缓存服务器中，可以设置超时时间，单位毫秒
     *
     * @param key   key值
     * @param value 保存数据
     * @return 添加结果
     */
    public static String addStringToRedisByExpireTime(String key, String value, Long expireTime) {
        String isCluster = YmlConfig.redisCluster;
        String redis_addrs = YmlConfig.redisAddress;
        String redis_passwd = YmlConfig.redisPassword;
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
            log.error("添加缓存错误", e);
        }
        return lastVal;
    }

    /**
     * 把 String保存到缓存服务器中
     *
     * @param key   key值
     * @param value 保存数据
     * @return 添加结果
     */
    public static String addStringToRedis(String key, String value) {
        String isCluster = YmlConfig.redisCluster;
        String redis_addrs = YmlConfig.redisAddress;
        String redis_passwd = YmlConfig.redisPassword;
        String lastVal = null;
        try {
            if ("true".equals(isCluster)) {
                JedisCluster jcd = ClusterRedisConnectionPool.getInstance().getDataConnection(redis_addrs, redis_passwd);
                if (jcd != null) {
                    lastVal = jcd.getSet(key, value);
                }
            } else {
                JedisPool pool = redisPool.getDataConnection(redis_addrs, redis_passwd);
                Jedis jedis = null;
                try {
                    jedis = pool.getResource();
                    lastVal = jedis.getSet(key, value);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    redisPool.returnResource(pool, jedis);
                }
            }
        } catch (Exception e) {
            log.error("添加缓存错误", e);
        }
        return lastVal;
    }

    /**
     * 获取缓存
     *
     * @param key key
     * @param clz 对应的泛型
     * @return 缓存数据
     */
    public static <T> T getStringFromRedis(String key, Class<T> clz) {
        T userVO = null;
        try {
            String value = getStringFromRedis(key);
            if (StringUtils.isEmpty(value)) return null;
            userVO = JSONUtil.toBean(value, clz);
        } catch (Exception e) {
            log.error("获取缓存发生错误{}", e);
        }
        return userVO;
    }

    /**
     * 从Redis中获取数据
     *
     * @param key Redis key
     * @return 数据
     */
    public static String getStringFromRedis(String key) {
        String isCluster = YmlConfig.redisCluster;
        String redis_addrs = YmlConfig.redisAddress;
        String redis_passwd = YmlConfig.redisPassword;
        String value = null;
        try {
            if ("true".equals(isCluster)) {
                JedisCluster jcd = ClusterRedisConnectionPool.getInstance().getDataConnection(redis_addrs, redis_passwd);
                if (jcd != null) {
                    if (jcd.exists(key)) {
                        value = jcd.get(key);
                        value = !StringUtils.isEmpty(value)
                                && !"nil".equalsIgnoreCase(value) ? value
                                : null;
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
            log.error("获取缓存错误", e);
        }
        return value;
    }

}
