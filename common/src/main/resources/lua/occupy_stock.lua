-- 分布式锁 + 原子预占库存
-- KEYS[1] = 库存 key, e.g. STOCK:G1234:2026-06-10:BUSINESS
-- KEYS[2] = 分布式锁 key, e.g. STOCK_LOCK:G1234:2026-06-10:BUSINESS
-- ARGV[1] = 预占数量 (正整数)
-- ARGV[2] = 锁 TTL (秒), e.g. 10
-- 返回: 1=成功, 0=库存不足, -1=库存 key 不存在, -2=获取锁失败

-- 1. 尝试获取分布式锁 (SETNX + EXPIRE)
if redis.call('SETNX', KEYS[2], '1') == 0 then
    return -2
end
redis.call('EXPIRE', KEYS[2], tonumber(ARGV[2]))

-- 2. 检查库存 key 是否存在
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    redis.call('DEL', KEYS[2])
    return -1
end

-- 3. 判断库存是否充足
local need = tonumber(ARGV[1])
if stock < need then
    redis.call('DEL', KEYS[2])
    return 0
end

-- 4. 原子扣减
redis.call('DECRBY', KEYS[1], need)

-- 5. 释放锁
redis.call('DEL', KEYS[2])
return 1
