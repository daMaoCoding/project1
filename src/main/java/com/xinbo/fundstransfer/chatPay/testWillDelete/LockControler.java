package com.xinbo.fundstransfer.chatPay.testWillDelete;

import com.xinbo.fundstransfer.chatPay.commons.enums.RedisLockKeyEnums;
import com.xinbo.fundstransfer.service.RedisService;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/t")
public class LockControler {

    @Autowired
    RedisService redisService;


    /**
     * redis 锁测试
     */
    @GetMapping("/testLock")
    public void addInBankAccount() {
        // redisService.getValueOperations().set("count",50);
        RLock lock = redisService.getRedisLock(RedisLockKeyEnums.CLEAN_NOT_LOGIN_ROOM.concat("abc"));
        RLock lock2 = redisService.getRedisLock(RedisLockKeyEnums.CLEAN_NOT_LOGIN_ROOM.getLockKey());
        boolean isLock = false;
        try {
             isLock = lock.tryLock(60, 10, TimeUnit.SECONDS);    //尝试获取分布式锁
            if (isLock) {
                String key = "count";
                int i = (Integer) redisService.getValueOperations().get(key);
                if(i>0){
                    i--;
                    redisService.getValueOperations().set(key,i);
                    redisService.getValueOperations().set("AA",(Integer) redisService.getValueOperations().get("AA")+1);
                    System.out.println("库存剩余："+i);
                }
            }
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(isLock)
              lock.unlock();
        }







    }
}
