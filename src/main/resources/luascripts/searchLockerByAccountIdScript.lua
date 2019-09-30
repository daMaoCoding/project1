--
-- Created by IntelliJ IDEA.
-- User: Administrator
-- Date: 2018/9/3
-- Time: 14:58
-- To change this template use File | Settings | File Templates.
--
local k = KEYS[1];
local accountId = ARGV[1];
local allKeys = redis.call('keys', k);
local locker;
local function splitStr(str, reps)
    local resultStrList = {}
    string.gsub(str, '[^' .. reps .. ']+', function(w)
        table.insert(resultStrList, w);
    end)
    return resultStrList
end

if (allKeys ~= nil and #allKeys > 0) then
    for i = 1, #allKeys do
        local lockedRecords = redis.call('smembers', allKeys[i]);
        if (lockedRecords ~= nil and #lockedRecords > 0) then
            for j = 1, #lockedRecords do
                if (lockedRecords[j] == accountId) then
                    local currentLockerKey = splitStr(allKeys[i], ':');
                    if (currentLockerKey ~= nil and #currentLockerKey > 0) then
                        locker = currentLockerKey[#currentLockerKey - 1]
                    end
                end
            end
        end
    end
end
return locker;



