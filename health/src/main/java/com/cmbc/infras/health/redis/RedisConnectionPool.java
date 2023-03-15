package com.cmbc.infras.health.redis;


import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis连接池
 */
public class RedisConnectionPool {

    private static JedisPool pool = null;

    private static final Logger log = LogManager.getLogger("RedisConnectionPool");

    /**
     * 获取config文件的参数
     */
    private static RedisConnectionPool redisConnPool = new RedisConnectionPool();

    //系统默认参数
    private int waitMillis = 1000;
    private int port = 6379;

    public RedisConnectionPool() {
    }

    public static RedisConnectionPool getInstance() {
        return redisConnPool;
    }

    /**
     * 构建redis连接池
     *
     * @param maxTotal //最大连接数
     * @param maxIdle  //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
     * @param maxWait  //表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
     * @param ip
     * @param port
     * @param passWord
     */
    private void createPool(int maxTotal, int maxIdle, int maxWait, String ip, int port, String passWord) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
        config.setTestOnBorrow(true);
        if (StringUtils.isEmpty(passWord)) {
            pool = new JedisPool(config, ip, port, maxWait);
        } else {
            pool = new JedisPool(config, ip, port, maxWait, passWord);
        }

    }

    /**
     * 得到session缓存服务器连接池
     *
     * @return
     */
    public JedisPool getSessionConnection(String redis_addrs, String redis_passwd) {
        if (null == pool && !StringUtils.isBlank(redis_addrs)) {
            int sessionPort = port;
            int sessionWaitMillis = waitMillis;
            int sessionMaxTotal = 1000;//最大连接数
            int sessionMaxIdle = 10;  //最大空闲
            String sessionIP = redis_addrs.split(":")[0];
            createPool(sessionMaxTotal, sessionMaxIdle, sessionWaitMillis, sessionIP, sessionPort, redis_passwd);
            if (null != pool) {
                if (log.isInfoEnabled()) {
                    log.info("Session Redis 连接池创建成功！ ");
                }
            }
        }
        return pool;
    }

    /**
     * 得到数据缓存服务器连接池
     *
     * @return
     */
    public JedisPool getDataConnection(String redis_addrs, String redis_passwd) {
        if (null == pool && !StringUtils.isBlank(redis_addrs)) {
            String dataIP = redis_addrs.split(":")[0];
            int dataPort = Integer.parseInt(redis_addrs.split(":")[1]);
            int dataWaitMillis = waitMillis;
            int dataMaxTotal = 1000;//最大连接数
            int dataMaxIdle = 10;  //最大空闲
            createPool(dataMaxTotal, dataMaxIdle, dataWaitMillis, dataIP, dataPort, redis_passwd);
            if (null != pool) {
                if (log.isInfoEnabled()) {
                    log.info("Data Redis 连接池创建成功！ ");
                }
            }
        }
        return pool;
    }

    /**
     * 返还到连接池
     *
     * @param pool
     * @param redis
     */
    public void returnResource(JedisPool pool, Jedis redis) {
        if (redis != null) {
            pool.returnResourceObject(redis);
        }
    }

}
