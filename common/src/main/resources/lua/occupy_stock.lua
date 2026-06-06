-- 原子预占库存
-- KEYS[1] = 库存 key, e.g. STOCK:G1234:2026-06-10:BUSINESS
-- ARGV[1] = 预占数量 (正整数)
-- 返回: 1=成功, 0=库存不足, -1=key 不存在

local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil then
    return -1
end

local need = tonumber(ARGV[1])
if stock < need then
    return 0
end

redis.call('DECRBY', KEYS[1], need)
return 1
