--
-- Created by IntelliJ IDEA.
-- User: Administrator
-- Date: 2019/3/12
-- Time: 13:22
-- To change this template use File | Settings | File Templates.
--  更新账号余额脚本 key 和 argv参数都是下标1 开始的
local key1 = KEYS[1]; --账号实时余额key  ACC_REAL_BAL='AccRealBal'
local key2 = KEYS[2]; --账号实时余额上报时间key ACC_REAL_BAL_RPT_TM='AccRealBalRptTm'
local key3 = KEYS[3]; --api调用上报时间 key AccApiRevokeTm='AccApiRevokeTm'
local key4 = KEYS[4]; --账号实时余额改变时间 ACC_BAL_CHG_TM='AccBalChgTm'

local accId = ARGV[1]; --账号id
local relBal = ARGV[2]; --上报的余额
--local keyRelBal = ARGV[3];
--local keyRelBalRptTm = ARGV[4];
--local keyApiRevokeTm = ARGV[5];
local currMillis = tonumber(ARGV[3]); --更新时间
local updApiRevokeTm = tonumber(ARGV[4]); --是否更新api调用时间
--local keyAccBalChgTm = ARGV[8];

local relBal_ = redis.call('hget', key1, accId);
if relBal == '' or relBal == nil then
    relBal = nil;
else
    relBal = tonumber(relBal);
end
if relBal_ == '' or relBal_ == nil then
    relBal_ = nil;
else
    relBal_ = tonumber(relBal_);
end
if relBal == nil and relBal_ == nil then
    redis.call('hdel', key1, accId);
    redis.call('hdel', key2, accId);
    redis.call('hdel', key3, accId);
    return 'error';
end
if relBal_ == nil then
    redis.call('hset', key1, accId, relBal);
    redis.call('hset', key2, accId, currMillis);
    redis.call('hset', key3, accId, currMillis);
    redis.call('hset', key4, accId, currMillis);
    return 'ok';
end
if relBal == nil then
    if updApiRevokeTm == 1 then
        redis.call('hset', key3, accId, currMillis);
    end
    return 'error';
end
if relBal_ < relBal then
    redis.call('hset', key1, accId, relBal);
    redis.call('hset', key2, accId, currMillis);
    redis.call('hset', key3, accId, currMillis);
    redis.call('hset', key4, accId, currMillis);
    return 'ok';
end
if relBal_ >= relBal then
    redis.call('hset', key1, accId, relBal);
    if updApiRevokeTm == 1 then
        redis.call('hset', key3, accId, currMillis);
    end
    if relBal_ > relBal then
        redis.call('hset', key4, accId, currMillis);
    end
    return 'ok';
end
return 'error';

