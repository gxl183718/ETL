
--[[

    KEYS[1]:redis key->TSMConf.serverLeader,
    ARGV[1]:redis value->TSMConf.nodeName
    ARGV[2]:数据保留时间

    如果value不为空,value即为调度服务的主服务节点名，是本机则更新保留时间，不是则返回leader信息
    如果value为空，没有master节点，原子执行脚本选举leader节点

]]
local leader = redis.call('get', KEYS[1])
if leader then
    if (leader == ARGV[1]) then
        redis.call('set', KEYS[1], ARGV[1])
        redis.call('expire', KEYS[1], ARGV[2])
        return 'true, leader is myself ' .. leader
    else
        return 'false, leader is other node:' .. leader
    end
else
    redis.call('set', KEYS[1], ARGV[1])
    redis.call('expire', KEYS[1], ARGV[2])
    return 'true, elected new leader myself ' .. ARGV[1]
end
