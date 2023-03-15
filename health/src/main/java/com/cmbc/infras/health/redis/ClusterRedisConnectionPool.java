package com.cmbc.infras.health.redis;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;

/**
 * reids集群连接类
 */
public class ClusterRedisConnectionPool {

    private static final Logger logger = LogManager.getLogger("ClusterRedisConnectionPool");

    private static ClusterRedisConnectionPool clusterRedisConnection = new ClusterRedisConnectionPool();

    private static final int DEFAULT_TIMEOUT = 2000;

    private static final int DEFAULT_REDIRECTIONS = 5;

    public ClusterRedisConnectionPool() {

    }

    public static ClusterRedisConnectionPool getInstance() {
        return clusterRedisConnection;
    }

    /**
     * 打开连接
     */
    public JedisCluster getDataConnection(String redis_addrs, String redis_passwd) {
        JedisCluster jcd = null;
        if (StringUtils.isNotBlank(redis_addrs)) {
            String[] redisAddrStrings = redis_addrs.split(",");
            Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
            for (int i = 0; i < redisAddrStrings.length; i++) {
                String[] ips = redisAddrStrings[i].split(":");
                String ip = ips[0].trim();
                Integer port = Integer.parseInt(ips[1].trim());
                jedisClusterNode.add(new HostAndPort(ip, port));
            }
            if (jedisClusterNode.size() > 0) {
                if (StringUtils.isEmpty(redis_passwd)) {
                    jcd = new JedisCluster(jedisClusterNode);
                } else {
                    jcd = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_REDIRECTIONS, redis_passwd, new GenericObjectPoolConfig());
                }
            }

        }
        return jcd;
    }

    /**
     * 关闭连接
     */
    public void closeRedisConnect(JedisCluster jcd) {
        if (jcd != null) {
            try {
                jcd.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
