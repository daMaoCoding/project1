--
-- Created by IntelliJ IDEA.
-- User: Administrator
-- Date: 2018/8/30
-- Time: 15:23
-- To change this template use File | Settings | File Templates.
--
local accountId = ARGV[1]; --锁定的账号id
local userId = ARGV[2]; --操作人id
local keysPrex = KEYS[1]; --锁key前缀
local currentUserKey = keysPrex .. ':' .. userId .. ':*'; --当前操作人的锁key
local keys = redis.call('keys', currentUserKey);
local deleteRet = 0;
if (keys ~= nil and #keys > 0) then
    for i = 1, #keys do
        local record = redis.call('smembers', keys[i]);
        if (record ~= nil and #record > 0) then
            for j, v in pairs(record) do
                if (v == accountId) then
                    deleteRet = redis.call('SREM', keys[i], v);
                    --发消息通知刷新页面
                    if (deleteRet == 1) then
                        redis.call('PUBLISH', 'FRESH_ACCOUNT_THIRDDRAW', 'FRESH');
                    end
                    break;
                end
            end
        end
    end
end
return deleteRet;