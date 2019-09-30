local accountId = ARGV[1]; --锁定的账号id
local userId = ARGV[2]; --操作人id
local fromId = ARGV[3]; --提现的第三方账号
local keysPrex = KEYS[1]; --锁key前缀
local currentUserKey = keysPrex .. ':' .. userId .. ':' .. fromId; --当前操作人的锁key
local f = redis.call('keys', keysPrex .. ':*'); --其他人锁定的记录
local count = #f; --其他人锁定的记录
local e = redis.call('exists', currentUserKey); --当前人锁定的记录
local ret = 0; --添加成功标识 0 不成功 1 成功
local exi = false;
-- 函数功能:根据当前人需要锁定的账号id 判定是否执行锁定
local function lockAcc()
    local add = 0;
    if e > 0 then
        local ex = redis.call('SMEMBERS', currentUserKey)
        local exf = false; --可以省略此步骤 但是为了可靠还是需要再一次检查是否已经锁定
        for i, v in pairs(ex) do
            if (v == accountId) then
                exf = true;
                break;
            end
        end
        if (not exf) then
            add = redis.call('sadd', currentUserKey, accountId);
            --设置过期时间防止在提现操作的时候出现删除异常而导致无法unlock  30分钟失效
            if (add ~= nil and add == 1) then
                redis.call('PEXPIRE', currentUserKey, 1800000);
            end
        end
    else
        add = redis.call('sadd', currentUserKey, accountId);
        if (add ~= nil and add == 1) then
            --设置过期时间防止在提现操作的时候出现删除异常而导致无法unlock
            redis.call('PEXPIRE', currentUserKey, 1800000);
        end
    end
    return add;
end

--先判断是否已经有他人锁定
if (f ~= nil and count > 0) then
    for i = 1, count do
        local record = redis.call('SMEMBERS', f[i]);
        if (record ~= nil and #record > 0) then
            for j = 1, #record do
                if (record[j] == accountId) then
                    exi = true;
                    break;
                end
            end
        end
        if (exi) then
            break;
        end
    end
    if (not exi) then
        ret = lockAcc();
    end
else
    ret = lockAcc();
end
if (ret == 1) then
    --发消息通知刷新页面
    redis.call('PUBLISH', 'FRESH_ACCOUNT_THIRDDRAW', 'FRESH');
end
return ret;