/**
 * 这个包：用于入款卡和出款卡的余额调配：
 *   1. 出款卡的余额分配出款任务
 *   2. 入款卡的余额多了，下发到出款卡
 *   3. 出款卡的余额少了，从入款卡补钱 
 *   等等。
 * 类说明：
 * 	 1. AccountFactory 工厂类，用于生成entity目录下的类实例
 *   2. AssignBalance  入款卡的余额多了，下发到出款卡; 出款卡的余额少了，从入款卡补钱 
 *   3. AssignOutwardTask 出款卡的余额分配出款任务
 *   4. AvailableCardCache 入款卡和出款卡的可用卡的缓存，没30S刷新一次。
 *   5. Constants 常量类
 *   entity 目录
 *   1. Account 所有账号的父类，直接子类有 InAccount 入款账号, OutAccount 出款账号
 *   2. InAccount 入款账号，直接子类有 InOnly 专注入款卡
 *   3. OutAccount 出款账号，直接子类有 OutOnly 专注出款卡，OutManual 人工出款， OutThridPart 第三方出款，OutInTurn 入款卡变出款卡， OutInSameTime 边入边出卡
 *   4. OutInSameTime 边入边出卡，直接子类有  OutPcInMobile PC转账手机抓流水 ， OutYunSF 云闪付
 */
/**
 * @author Eric
 *
 */
package com.xinbo.fundstransfer.assign;