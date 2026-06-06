-- 幂等键设置（SETNX + EXPIRE 原子化）
-- KEYS[1] = 幂等键
-- ARGV[1] = 过期秒数
-- 返回: 1=首次请求（已记录幂等键）, 0=重复请求

if redis.call('EXISTS', KEYS[1]) == 1 then
    return 0
end

redis.call('SET', KEYS[1], '1', 'EX', tonumber(ARGV[1]))
return 1
