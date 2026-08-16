package com.tws.mes.common.lock;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Redis 分布式锁（过站防重的第一道防线）。
 *
 * 实现：SET key value NX PX ttl —— 只在 key 不存在时设置，带过期时间防死锁；
 * 释放时用 Lua 比较 value 再删除，保证"只删自己的锁"（避免误删别人的）。
 *
 * 为什么不用 Redisson：手写 SET NX + Lua 更能看清互斥的本质，依赖也更少。
 * 兜底：即使锁失效（如 Redis 抖动），station_log.checkin_key 唯一索引仍能拦住重复写入。
 */
@Component
@RequiredArgsConstructor
public class RedisLockService {

    private static final String PREFIX = "mes:lock:";
    private final StringRedisTemplate redis;

    /** 尝试加锁，成功返回解锁用的 token，失败返回 null */
    public String tryLock(String key, long ttlMillis) {
        String token = IdUtil.fastSimpleUUID();
        Boolean ok = redis.opsForValue().setIfAbsent(PREFIX + key, token, java.time.Duration.ofMillis(ttlMillis));
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /** 释放锁：value 相等才删除（Lua 保证"比较+删除"原子执行） */
    public void unlock(String key, String token) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(PREFIX + key), token);
    }
}
