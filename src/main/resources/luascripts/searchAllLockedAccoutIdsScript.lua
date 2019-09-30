--
-- Created by IntelliJ IDEA.
-- User: Administrator
-- Date: 2018/9/1
-- Time: 9:42
-- To change this template use File | Settings | File Templates.
--
local k = KEYS[1]; --查询所有人针对某一个第三方账号提现而锁定的目标账号记录
local keys = redis.call('keys', k);
local targetAccIds;
if (keys ~= nil and #keys > 0) then
    targetAccIds = {};
    for i, v in pairs(keys) do
        local target = redis.call('smembers', v);
        if (target ~= nil and #target > 0) then
            for j, k in pairs(target) do
                table.insert(targetAccIds, k);
            end
        end
    end
end
if (targetAccIds ~= nil and #targetAccIds > 0) then
    targetAccIds = table.concat(targetAccIds, ',');
end
return targetAccIds;
