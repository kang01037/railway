-- 原子释放库存（订单取消 / 支付失败时回滚）
-- KEYS[1] = 库存 key
-- ARGV[1] = 释放数量 (正整数)
-- 返回: 1=成功, -1=key 不存在（视为幂等成功）

if redis.call('EXISTS', KEYS[1]) == 0 then
    return -1
end

redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
return 1
