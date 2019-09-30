--
-- Created by IntelliJ IDEA.
-- User: Administrator
-- Date: 2018/9/1
-- Time: 9:45
-- To change this template use File | Settings | File Templates.
--
local k = KEYS[1];
local ex = redis.call('exists', k);
local targetAccIds;
if (ex ~= nil and ex == 1) then
    targetAccIds = redis.call('smembers', k);
end
if (targetAccIds ~= nil and #targetAccIds > 0) then
    targetAccIds = table.concat(targetAccIds, ',');
end
return targetAccIds;

