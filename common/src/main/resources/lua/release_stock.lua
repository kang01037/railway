-- 分布式锁 + 原子释放库存（订单取消 / 支付失败时回滚）
-- KEYS[1] = 库存 key
-- KEYS[2] = 分布式锁 key
-- ARGV[1] = 释放数量 (正整数)
-- ARGV[2] = 锁 TTL (秒), e.g. 10
-- 返回: 1=成功, -1=库存 key 不存在（视为幂等成功）, -2=获取锁失败

-- 1. 尝试获取分布式锁 (SETNX + EXPIRE)
if redis.call('SETNX', KEYS[2], '1') == 0 then
    return -2
end
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[2]))

-- 2. 检查库存 key 是否存在
if redis.call('EXISTS', KEYS[1]) == 0 then
    redis.call('DEL', KEYS[2])
    return -1
end

-- 3. 原子回补
redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))

-- 4. 释放锁
redis.call('DEL', KEYS[2])
return 1
