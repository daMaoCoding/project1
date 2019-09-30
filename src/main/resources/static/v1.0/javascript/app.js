var sysVersoin = getCookie('JVERSION'), contentRightUpdate = false;
var accountType = {
    InBank: {typeId: 1, msg: '入款银行卡'},
    InThird: {typeId: 2, msg: '入款第三方'},
    InAli: {typeId: 3, msg: '入款支付宝'},
    InWechat: {typeId: 4, msg: '入款微信'},
    OutBank: {typeId: 5, msg: '出款银行卡'},
    OutThird: {typeId: 6, msg: '出款第三方'},
    ReserveBank: {typeId: 8, msg: '备用卡'},
    CashBank: {typeId: 9, msg: '现金卡'},
    BindWechat: {typeId: 10, msg: '微信专用'},
    BindAli: {typeId: 11, msg: '支付宝专用'},
    ThirdCommon: {typeId: 12, msg: '第三方专用'},
    BindCommon: {typeId: 13, msg: '公用银行卡'},
    BindCustomer: {typeId: 14, msg: '入款账号客户绑定卡'}
};
//公司用款 状态对应的Json  旧状态0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认到账,8-出款失败
var CompanyMoney_status={
//		0:{color:" blue " , text:"待审核"},
		1:{color:" green " , text:"待审核"},//原：财务审核通过
		2:{color:" green " , text:"审核通过"},//原：下发审核通过
//		3:{color:" red bolder " , text:"财务审核不通过"},
//		4:{color:" red bolder " , text:"下发审核不通过"},
		5:{color:" orange " , text:"等待到账"},
		6:{color:" green " , text:"完成出款"},
		7:{color:" orange " , text:"确认到账"},
		8:{color:" red bolder " , text:"出款失败"}
}
//银行流水抓取用 账号大类list 对应jsaccountType
var monitor_accountType = [
    {id: 1, msg: '入款'},// 入款银行卡
    {id: 2, msg: '出款'},// 出款银行卡
    {id: 3, msg: '下发/其它'}//下发卡  备用卡 备用卡

//	{id:1,msg:'入款银行卡'},
//	{id:2,msg:'入款第三方'},
//	{id:5,msg:'出款银行卡'},
//	{id:6,msg:'出款第三方'},
//	{id:8,msg:'备用卡'},
//	{id:9,msg:'现金卡'},
//	{id:10,msg:'微信专用'},
//	{id:11,msg:'支付宝专用'},
//	{id:12,msg:'第三方专用'},
//	{id:13,msg:'公用银行卡'}
];
var noOwnerBankOverTime = 24;//小时，超过此时间的流水未被匹配，则状态为未认领
var allocateIncomeAccountToArray = [];
var incomeRequestStatusMatching = 0, incomeRequestStatusMatched = 1, incomeRequestStatusUnmatching = 2,
    incomeRequestStatusCanceled = 3,MATCHED4SUBINBANKALI=4,CANCELED4SUBINBANKALI=5;
var bankLogStatusMatching = 0, bankLogStatusMatched = 1, bankLogStatusNoOwner = 3, bankLogStatusDisposed = 4,
    bankLogStatusFee = 5, bankLogStatusRefunding = 6, bankLogStatusRefunded = 7, bankLogStatusInterest = 8,
    bankLogStatusDeficit = 9;
var bankLogTypeBank = 1, bankLogTypeWechat = 2, bankLogTypeAlipay = 3, bankLogTypeManual = 4,
    bankLogTypeChangeBalance = 5;
var incomeRequestTypePlatFromAli = 1, incomeRequestTypePlatFromWechat = 2, incomeRequestTypePlatFromBank = 3,
    incomeRequestTypePlatFromThird = 4;
var incomeRequestTypeWithdrawAli = 101, incomeRequestTypeWithdrawWechat = 102, incomeRequestTypeWithdrawThird = 103,
    incomeRequestTypeIssueAli = 104, incomeRequestTypeIssueWechat = 105, incomeRequestTypeIssueCompBank = 106,
    incomeRequestTypeIssueComnBank = 107, incomeRequestTypeTransferOutThird = 108,
    incomeRequestTypeTransferOutBank = 109, incomeRequestTypeReserveToOutBank = 110,
    incomeRequestTypeWithdrawThirdToCustomer = 111, incomeRequestTypeCustomerToReserved = 112;

//账号来源  1手机 0PC 2返利网
var accountFlagMobile=1,accountFlagPC=0,accountFlagRefund=2;
var accountTypeInBank = 1, accountTypeInThird = 2, accountTypeInAli = 3, accountTypeInWechat = 4,
    accountTypeOutBank = 5, accountTypeOutThird = 6, accountTypeReserveBank = 8, accountTypeCashBank = 9,
    accountTypeBindWechat = 10, accountTypeBindAli = 11, accountTypeThirdCommon = 12, accountTypeBindCommon = 13,
    accountTypeBindCustomer = 14;
var accountStatusNormal = 1, accountStatusFreeze = 3, accountStatusDelete = -2,accountStatusExcep = -1, accountStatusStopTemp = 4,
    accountStatusEnabled = 5, accountInactivated=6,accountActivated=7;
//客户资料类型 1客户 2自用
var MobileTypeCustomer = 1, MobileTypeSelf = 2;
//客户资料状态
var mobileStatusNormal = 1, mobileStatusFreeze = 3, mobileStatusStopTemp = 4;
//数据匹配类型   出款：0，平账：999
var transactionLogTypeOutward = 0, transactionLogTypeFlat = 999;
var outwardTaskStatusUndeposit = 0, outwardTaskStatusDeposited = 1, outwardTaskStatusMgr = 2,
    outwardTaskStatusFeedbackPlatform = 3, outwardTaskStatusFrozen = 4, outwardTaskStatusMatched = 5,
    outwardTaskStatusFailure = 6, outwardTaskStatusInvalid = 7;

var monitorHostStatusOffLine = -1, monitorHostStatusNormal = 1,monitorHostStatusOnLine=5;
var monitorLog_TRANSFERINFO = 16;//转账记录的返回action状态
//银行抓取状态 对应RunningStatusEnum 按顺序排  OFFLINE 离线, NORMAL 在线, ERROR 错误, PAUSE 暂停, WARN 警告;
var monitorAccountStatusUnstart = 0, monitorAccountStatusAcquisition = 1, monitorAccountStatusPause = 3,
    monitorAccountStatusWarn = 4;
//银行流水 工作模式
var monitorAccountStatusNORMALMODE=18,monitorAccountStatusCAPTUREMODE=19,monitorAccountStatusTRANSMODE=20;
var userCategoryAdmin = -1, userCategoryGeneral = 0, userCategoryRobot = 1, userCategoryOutward = 100,
    userCategoryIncomeAudit = 200, userCategoryFinance = 300, userCategoryHandicapPrefix = 400,
    userCategoryAllocatedAndAll = 1000;
var systemWebSocketCategorySystem = 100, systemWebSocketCategoryOnlineStat = 200,
    systemWebSocketCategoryLevelList = 300, systemWebSocketCategoryMenuList = 400,
    systemWebSocketCategoryAccountAmountAlarm = 500, systemOutwardTaskOperation = 600,
    systemLockOutwardTaskOperation = 700, systemUnLockOutwardTaskOperation = 800,
    systemWebSocketCategoryCustomerService = 900, systemWebSocketCategoryOutwardTaskCancel = 1000,
    systemWebSocketCategoryMessageToAllUser = 1100,
    systemWebSocketAccountAlarmCount = 1200;
var userIdAdmin = 1;
//系统全部配置 加载index.html时候同步加载值
//MONEYLIMIT,AUDITLIMIT,SYSMONEYLIMIT,TIMEOUT_IN_APPROVE,TIMEOUT_OUT_APPROVE,TIMEOUT_CHECKOUT,TIMEOUT_CHECKIN,
//ERROR_RATE_SEPARATE_BILL,INCOME_LIMIT_CHECKIN_TODAY,OUTDRAW_LIMIT_CHECKOUT_TODAY,ERROR_RATE_CHECK,LIMIT_APPROVE_IN,LIMIT_APPROVE_OUT,LIMIT_OUT_DAILY ;
var sysSetting;
var accountStatInOutCategoryIn = 0, accountStatInOutCategoryOutTranfer = 1, accountStatInOutCategoryOutMember = 2,
    accountStatInOutCategoryFromTo = 'fromTo';
//盘口和层级 handicap_list_all所有盘口层级
var handicapId_list=[], handicap_list_all = [], handicap_list = [], level_list = [], ContentRight = {},zone_list_all = [];
//内外层
var currentSystemLevelOutter = 1, currentSystemLevelInner = 2, currentSystemLevelMiddle = 4 ,currentSystemLevelDesignated=8;
//银行流水抓取用内外层
var monitor_currSysLevel = [
    {id: currentSystemLevelOutter, msg: '外层'},
    {id: currentSystemLevelInner, msg: '内层'},
    {id: currentSystemLevelDesignated, msg: '指定层'}
];
var getObjectById=function(id,list){
	var result;
	$.map(list,function(record){
		if(record&&record.id*1==id*1){
			result= record;
			return false;
		}
	});
	return result;
}
//系统流水类型
var SysLogType_list=[
    {id:7,msg:'流水'},
    {id:6,msg:'排查'},
    {id:5,msg:'初始化'},
	{id:4,msg:'返利'},
	{id:3,msg:'出款'},
	{id:2,msg:'下发'},
	{id:1,msg:'入款'},
	{id:-1,msg:'其它'},//结息
	{id:-2,msg:'其它'},//不明收入
	{id:-3,msg:'重复出款'},
	{id:-4,msg:'费用'},
	{id:-5,msg:'盗刷'},
	{id:-6,msg:'冲正'}
];
var getSysLogTypeById=function(id,amount){
    var ret = getObjectById(id,SysLogType_list);
    if( id== 2)
        ret.msg=((amount&&amount>0)?'下入':'下出');
    return ret;
}
//系统流水状态
var SysLogStatus_list=[
	{id:20,msg:'未确认'},
	{id:1,msg:'完成'},
	{id:-1,msg:'未匹配'}
];
var getSysLogStatusById=function(id){
	return getObjectById(id,SysLogStatus_list);
}
//当前页面的 url
var currentPageLocation = window.location.href;
var JCURRENTMENUDATA = 'JCURRENTMENUDATA';
var IncomeRequestMatchRight = 'IncomeAuditComp:MatchIncomeReq:*',
    IncomeRequestSendMessageRight = 'IncomeAuditComp:CustomerSendMessage:*',
    IncomeRequestAddRemarkRight = 'IncomeAuditComp:CustomerAddRemark:*',
    IncomeRequestConcealOrderRight = 'IncomeAuditComp:AuditorConceal:*',
    IncomeRequestAddOrderRight = 'IncomeAuditComp:AuditorAddOrder:*';

var typeNormalLatestOneHour = -1, typeCustomLatestOneDay = -7, typeNormalLatestOneDay = -23, typeCustomLatestToday = 7,
    typeNormalLatestToday = 23, typeCustomLatestOneWeek = -707, typeNormalLatestOneWeek = -700;

var ACC_INVSGT = [];

var API = {
    r_user_findUserList4OutwardAsign: '/r/user/getOutWardTaskUser',
    r_account_findById: '/r/account/findById',
    r_account_toEnabled: '/r/account/toEnabled',
    r_account_toFreezeTemp: '/r/account/toFreezeTemp',
    r_account_toFreezeForver: '/r/account/toFreezeForver',
    r_account_toStopTemp: '/r/account/toStopTemp',
    r_account_toOutInCheck4OutwardAccount: '/r/account/toOutInCheck4OutwardAccount',
    r_account_asin4OutwardAccount: '/r/account/asin4OutwardAccount',
    r_account_asin4OutwardAccountByBatch: '/r/account/asin4OutwardAccountByBatch',
    r_account_recycle4OutwardAccount: '/r/account/recycle4OutwardAccount',
    r_account_recycle4OutwardAccountByBatch:'/r/account/recycle4OutwardAccountByBatch',
    r_account_bindOrUnbindForIssue: '/r/account/bindOrUnbindForIssue',
    r_account_findIncomeByIssueAccountId: '/r/account/findIncomeByIssueAccountId',
    r_account_add: '/r/account/add',
    r_account_upd: '/r/account/upd',
    r_account_del: '/r/account/delete',
    r_account_list: '/r/account/list',//普通查询
    r_account_list2: '/r/account/list2',//区分在线离线
    r_account_findBindList: '/r/account/findBindList',
    r_account_list4Host: '/r/account/list4Host',
    r_account_list4Trans: '/r/account/list4Trans',
    r_account_list4BindAliAndBindWechat: '/r/account/list4BindAliAndBindWechat',
    r_account_lockOrUnlock: '/r/account/lockOrUnlock',
    r_account_findByHandicapAndLevel: '/r/account/findByHandicapAndLevel',
    r_banklog_findbyfrom: '/r/banklog/findbyfrom',
    r_banklog_findStat4Matching: '/r/banklog/findStat4Matching',
    r_handicap_list: '/r/handicap/list',//此方法适用于只查盘口(全部),与用户数据权限无关的。list
    r_handicap_listZone: '/r/handicap/listZone',
    r_handicap_find4User: '/r/permission/find4User',//此方法适用于用户管理,权限(bizLevelMapList:已有层级权限,bizHandicapList:已有盘口权限,levelMapList:未给用户的层级,handicapList:未给用户的盘口。)
    r_handicap_findByPerm: '/r/handicap/findByPerm',//此方法适用于获取用户拥有数据权限的盘口.list
    r_handicap_findByPermFirstThenNoPerm: '/r/handicap/findByPermFirstThenNoPerm',//此方法适用于先获取用户拥有数据权限的盘口,如果没有则返回未绑定的盘口。list
    r_handicap_handicap2LevelListAll: '/r/handicap/handicap2LevelListAll',//此方法适用于获取所有的盘口层级信息:list<Map<k,v>>,k-盘口id,v:该盘口下的盘口层级信息 list
    r_handicap_handicap2LevelList4User: '/r/handicap/handicap2LevelList4User',//此方法适用于获取用户拥有权限的盘口层级信息:list<Map<k,v>>,k-盘口id,v:该盘口下的盘口层级信息 list
    r_level_list: '/r/level/list',
    r_level_findByPerm: '/r/level/findByPerm',
    r_income_findbyvo: '/r/income/findbyvo',
    r_income_match: '/r/income/match',
    r_income_reject2CurrSys: '/r/income/reject2CurrSys',
    r_outtask_list: '/r/outtask/list',
    r_outtask_match: '/r/outtask/match',
    r_outtask_reject: '/r/outtask/reject',
    r_outtask_findInfoById: '/r/outtask/findInfoById',
    r_outtask_matchWithoutBankLog: '/r/outtask/matchWithoutBankLog',
    r_host_list: '/r/host/list',
    r_host_delete: '/r/host/delete',
    r_host_addAccountToHost: '/r/host/addAccountToHost',
    r_host_removeAccountFromHost: '/r/host/removeAccountFromHost',
    r_host_startByCommand: '/r/host/startByCommand',
    r_host_stopByCommand: '/r/host/stopByCommand',
    r_host_pauseByCommand: '/r/host/pauseByCommand',
    r_host_resumeByCommand: '/r/host/resumeByCommand',
    r_host_changeMode: '/r/host/changeMode',
    r_host_set: '/r/host/set',
    r_host_findAccountListOfHost: '/r/host/findAccountListOfHost',
    r_host_getMessageEntity: '/r/host/getmessageEntity',
    r_host_alterSignAndHook: '/r/host/alterSignAndHook',
    r_host_updateIinterval: '/r/host/updateIinterval',
    r_match_findByTypeNotAndOrderIdIn: '/r/match/findByTypeNotAndOrderIdIn',
    r_accountMonitor_findRecList4OutAcc: '/r/accountMonitor/findRecList4OutAcc',
    r_accountMonitor_findFlowList: '/r/accountMonitor/findFlowList',
    r_accountMonitor_findFrFlowList4ToFlow: '/r/accountMonitor/findFrFlowList4ToFlow',
    r_accountMonitor_makeUpRec4Issue: '/r/accountMonitor/makeUpRec4Issue',
    r_accountMonitor_alterFlowToMatched: '/r/accountMonitor/alterFlowToMatched',
    r_accountMonitor_alterFlowToInterest: '/r/accountMonitor/alterFlowToInterest',
    r_accountMonitor_alterFlowToFee: '/r/accountMonitor/alterFlowToFee',
    r_accountMonitor_alterFlowToRefunding: '/r/accountMonitor/alterFlowToRefunding',
    r_accountMonitor_alterFlowToDeficit: '/r/accountMonitor/alterFlowToDeficit',
    r_accountMonitor_alterFlowToExtFunds: '/r/accountMonitor/alterFlowToExtFunds',
    r_accountMonitor_remark4Flow: '/r/accountMonitor/remark4Flow',
    r_accountMonitor_buildTrans:'/r/accountMonitor/buildTrans',
    r_accountMonitor_issueList:'/r/accountMonitor/IssueList',
    r_accountMonitor_remark4TransLock:'/r/accountMonitor/remark4TransLock',
    r_accountMonitor_remark4TransReq:'/r/accountMonitor/remark4TransReq',
    r_accountMonitor_cancelTransAck:'/r/accountMonitor/cancelTransAck',
    r_accountMonitor_listAccForAlarm:'/r/accountMonitor/listAccForAlarm',
    r_accountMonitor_alterFlowToDisposed:'/r/accountMonitor/alterFlowToDisposed',
    api_banklog_put: '/api/banklog/put',
    api_income_put: '/api/income/put',
    api_income_cloud_put: '/api/income/cloud/put',
    r_user_findUserList4PermissionKey: '/r/user/getPermissionKeyUser'
};
//国内主流银行集合
var bank_name_list = [
	 '招商银行','农业银行', '工商银行', '建设银行', '浦发银行', '民生银行', '云南农信', '交通银行', '中信银行', '平安银行',
     '柳州银行', '中国银行', '锦州银行', '南京银行', '包商银行', '成都银行', '兴业银行', '中原银行', '天府银行',
     '哈尔滨银行', '北京农商', '福建海峡银行', '兰州银行', '临商银行', '汉口银行', '广发银行', '威海市商业银行',
     '桂林银行', '广州农村商业银行', '盛京银行', '华夏银行','武汉农村商业银行','浙商银行','广西北部湾银行','江西银行','沧州银行','徽商银行',
     '内蒙古银行','广西农村信用社','光大银行','四川农信','廊坊银行','重庆银行','齐商银行','东营银行'];
// var bank_name_list = ['中国农业银行','中国工商银行','中国建设银行','上海浦东发展银行','中国民生银行','云南省农村信用社联合社',
//    '交通银行','中信银行','平安银行股份有限公司','柳州商业银行','中国银行','锦州银行股份有限公司','南京银行',
//    '包商银行股份有限公司','成都银行','兴业银行','中原银行股份有限公司','四川天府银行','哈尔滨银行',
//    '北京农商银行','福建海峡银行','兰州银行','临商银行股份有限公司','汉口银行','广东发展银行','威海市商业银行',
//    '桂林银行','广州农村商业银行','盛京银行','华夏银行','武汉农村商业银行','浙商银行股份有限公司','广西北部湾银行',
//    '江西银行','沧州银行股份有限公司','徽商银行','内蒙古银行'];
//第三方类别
var third_name_list = [
    '智付', '路德', '迅宝', '鼎易', '萝卜', '钱包', '汇合', '长城', 'W付', '月宝', '捷付', '仁信', '掌托', '瞬付', '高通',
    '金海哲', '立刻付', '云安付', '彩富宝', '奥斯特', '速汇宝', '在线宝', '智汇付', '迅捷通', '多得宝', '艾米森', '高通T1', '新云安付'
];
//公司入款手工匹配 匹配和取消操作常用备注
var manualToMatchRemarks = ['未备注支付确认码 '];
var manualToCancelRemarks = ['重复提单', '金额不符', '存款人姓名不一致', '未付款', '无交易流水'];
var manualFreezeAccountRemarks = ['余额不足', '系统余额不正确', '银行余额不正确', '未平账'];
var timeOutSearchReq = null;
var timeOutSearchTasks = null;
var timeOutSearchTasks4BankCard=null;
//页面页脚初始值
var pageInitial = {
    totalElements: 0,
    pageNo: 0,
    totalPages: 0,
    hasPrevious: false,
    hasNext: false
};
//公司用款目的
var companyExpencePurpose = ['广告费', '运营费'];
var companyExpencePurposeOperationCharges = ['办公室设备', '第三方费用', '开发者帐号', '联係费及话费', '网管费',
    '银行卡费用', '主包&马甲包费用', '软件产品', '佣金'];
//本地主机ip
var localHostIp = null;
$(function () {
	var router={
        //主页
        '/Home:*': {
            templateUrl: '../html/home/homePage.html',
            controller: '../html/home/homePage.js',
        },
        //入款管理->入款审核->银行卡入款
        '/IncomeAuditComp:*': {
            templateUrl: '../html/income/audit/approveForCompanyInBackup.html',
            controller: '../javascript/income/audit/approveForCompanyInCopyBackup.js'
        },
        //入款管理->入款审核->银行卡入款汇总
        '/IncomeAuditCompTotal:*': {
            templateUrl: '../html/income/audit/approveForCompanyInTotal.html',
            controller: '../javascript/income/audit/approveForCompanyInTotal.js'
        },
        //入款管理->入款审核->微信入款
        '/IncomeAuditWechatIn:*': {
            templateUrl: '../html/income/audit/approveForWechatIn2.html',
            controller: '../javascript/income/audit/approveForWechatIn2.js'
        },
        //入款管理->入款审核->微信入款（聊天室）
        '/IncomeAuditWechatIn2:*': {
            templateUrl: '../html/income/audit/approveForWechatIn3.html',
            controller: '../javascript/income/audit/approveForWechatIn3.js'
        },
        //入款管理->入款审核->微信入款汇总
        '/IncomeAuditWechatInTotal:*': {
            templateUrl: '../html/income/audit/approveForWechatInTotal.html',
            controller: '../javascript/income/audit/approveForWechatInTotal.js'
        },
        //入款管理->入款审核->支付宝入款
        '/IncomeAuditAliIn:*': {
            templateUrl: '../html/income/audit/approveForAliIn2.html',
            controller: '../javascript/income/audit/approveForAliIn2.js'
        },
      //入款管理->入款审核->支付宝入款（聊天室）
        '/IncomeAuditAli:*': {
            templateUrl: '../html/income/audit/approveForAliIn3.html',
            controller: '../javascript/income/audit/approveForAliIn3.js'
        },
        //入款管理->入款审核->支付宝入款汇总
        // '/IncomeAuditAliInTotal:*': {
        //     templateUrl: '../html/income/audit/approveForAliInSummary.html',
        //     controller: '../javascript/income/audit/approveForAliInSummary.js'
        // },
        '/IncomeAuditThird:*': {
            templateUrl: '../html/income/audit/approveForThirdIn.html',
            controller: '../javascript/income/audit/approveForThirdIn.js'
        },
        //入款管理->账号管理->公司账号
        '/IncomeAccountComp:*': {
            templateUrl: '../html/income/account/accountInComp.html',
            controller: '../javascript/income/account/accountInComp.js'
        },
        //入款管理->账号管理->平台同步信息修改
        '/accountSyncUpdate:*': {
            templateUrl: '../html/income/account/accountSyncUpdate.html',
            controller: '../javascript/income/account/accountSyncUpdate.js'
        },
        //入款管理->账号管理->第三方
        '/IncomeAccountThird:*': {
            templateUrl: '../html/income/account/accountInThird.html',
            controller: '../javascript/income/account/accountInThird.js'
        },
        //取现对账
        '/EncashCheck4Transfer:*': {
            templateUrl: '../html/common/encashCheck4Transfer.html',
            controller: '../javascript/common/encashCheck4Transfer.js'
        },
        '/EncashCheck4Outward:*': {
            templateUrl: '../html/common/encashCheck4Outward.html',
            controller: '../javascript/common/encashCheck4Outward.js'
        },
        '/EncashStatus4Transfer:*': {
            templateUrl: '../html/common/encashStatus4Transfer.html',
            controller: '../javascript/common/encashStatus4Transfer.js'
        },
        '/EncashStatus4TransferIncome:*': {
            templateUrl: '../html/common/encashStatus4TransferIncome.html',
            controller: '../javascript/common/encashStatus4TransferIncome.js'
        },
        //入款管理->账号管理->新支付账号明细
        '/NewPayTradeAccount:*': {
            templateUrl: '../html/income/account/newPayTradeAccount.html',
            controller: '../javascript/income/account/newPayTradeAccount.js'
        },
        //入款管理->账号管理->新支付交易明细
        '/NewPayTradeDetail:*': {
            templateUrl: '../html/income/account/newPayTradeDetail.html',
            controller: '../javascript/income/account/newPayTradeDetail.js'
        },
        //入款管理->账号管理->银行卡
        '/IncomeAccountIssue:*': {
            templateUrl: '../html/income/account/bindBankCard.html',
            controller: '../javascript/income/account/bindBankCard.js'
        },
        '/IncomeAccountMobileMonitor:*': {
            templateUrl: '../html/income/account/monitorMobile.html',
            controller: '../javascript/income/account/monitorMobile.js'
        },
        //入款管理->账号管理->入款账号客户绑定卡
        '/BindCustomer:*': {
        	templateUrl: '../phone/BindCustomer.html',
        	controller: '../phone/BindCustomer.js'
        },
        //入款管理->账号管理->企业支付宝
        '/AlipayManager:*': {
        	templateUrl: '../phone/enterprise/alipayManager.html',
        	controller: '../phone/enterprise/alipayManager.js'
        },
        //入款管理->账号管理->手机号
        '/IncomePhoneNumber:*': {
        	templateUrl: '../phone/customerManager.html',
        	controller: '../phone/customerManager.js'
        },
        //客户资料管理  开发中的页面 
        '/IncomePhoneNumberNew:*': {
        	templateUrl: '../phone/customerManager.html',
        	controller: '../phone/customerManager.js'
        },
        //入款管理->账号管理->佣金规则管理
        '/CommissionRule:*': {
        	templateUrl: '../phone/commissionRule.html',
        	controller: '../phone/commissionRule.js'
        },
        //今日收款 佣金详情页面
        '/CustomerDetail:*': {
            templateUrl: '../phone/customerDetail.html',
            controller: '../phone/customerDetail.js'
        },
        //入款管理->账号管理->新客户资料管理
        '/customerManager:*': {
            templateUrl: '../html/income/account/phoneNumber.html',
            controller: '../javascript/income/account/phoneNumber.js'
        },
        //入款管理->账号管理->云闪付
        '/IncomeAccountYSF:*': {
            templateUrl: '../phone/ysf/ysf.html',
            controller: '../phone/ysf/ysf.js'
        },
        //入款管理->下发管理
        '/IncomeAsignAlipay:*': {
            templateUrl: '../html/income/asign/inFromCompAli.html',
            controller: '../javascript/income/asign/inFromCompAli.js'
        },
        //-----------
        '/IncomeAsignAlipay1:*': {
            templateUrl: '../html/income/asign/inFromCompAli1.html',
            controller: '../javascript/income/asign/inFromCompAli1.js'
        },
        //------------
        '/IncomeAsignWechat:*': {
            templateUrl: '../html/income/asign/inFromCompWechat.html',
            controller: '../javascript/income/asign/inFromCompWechat.js'
        },
        '/IncomeAsignCompBank:*': {
            templateUrl: '../html/income/asign/inFromCompBank.html',
            controller: '../javascript/income/asign/inFromCompBank.js'
        },
        '/IncomeAsignComnBank:*': {
            templateUrl: '../html/income/asign/inFromCommBank.html',
            controller: '../javascript/income/asign/inFromCommBank.js'
        },
        '/IncomeAsignMonitorStat:*': {
            templateUrl: '../html/income/asign/monitorStat.html',
            controller: '../javascript/income/asign/monitorStat.js'
        },
        //下发任务
        '/ThirdDrawTask:*': {
            templateUrl: '../html/income/asign/thirdDrawTask.html',
            controller: '../javascript/income/asign/thirdDrawTask.js'
        },
        //出款管理
        '/OutwardAudit:*': {
            templateUrl: '../html/outward/outwardRequest.html',
            controller: '../javascript/outward/outwardRequest.js'
        },
        '/OutwardAuditTotal:*': {
            templateUrl: '../html/outward/outwardRequestTotal.html',
            controller: '../javascript/outward/outwardRequestTotal.js'
        },
        '/OutwardTask:*': {
            templateUrl: '../html/outward/outTaskList.html',
            controller: '../javascript/outward/outTaskList.js'
        },
        '/TaskTroubleshoot:*': {
            templateUrl: '../html/outward/outTaskTroubleshoot.html',
            controller: '../javascript/outward/outTaskTroubleshoot.js'
        },
        '/OutwardTaskTotal:*': {
            templateUrl: '../html/outward/outTaskTotalList.html',
            controller: '../javascript/outward/outTaskTotalList.js'
        },
        //出款管理->账号管理->银行卡
        '/OutwardAccountBankUsed:*': {
            templateUrl: '../html/outward/account/bank/accountOutBankUsed.html',
            controller: '../javascript/outward/account/bank/accountOutBankUsed.js'
        },
      //出款管理->支付宝出款（聊天室）
        '/OutAuditAli:*': {
            templateUrl: '../html/outward/approveForAliOut.html',
            controller: '../javascript/outward/approveForAliOut.js'
        },
        //出款对账
        '/OutwardAccountBankUsedOutCheck:*': {
            templateUrl: '../html/outward/account/bank/accountOutBankUsedOutCheck.html',
            controller: '../javascript/outward/account/bank/accountOutBankUsedOutCheck.js'
        },
        '/AccountOutComp:*': {
            templateUrl: '../html/outward/account/accountOutComp.html',
            controller: '../javascript/outward/account/accountOutComp.js'
        },
        '/OutwardAccountBankEnabled:*': {
            templateUrl: '../html/outward/account/bank/accountOutBankEnabled.html',
            controller: '../javascript/outward/account/bank/accountOutBankEnabled.js'
        },
        '/OutwardAccountBankStop:*': {
            templateUrl: '../html/outward/account/bank/accountOutBankStop.html',
            controller: '../javascript/outward/account/bank/accountOutBankStop.js'
        },
        '/OutwardAccountBankFreezed:*': {
            templateUrl: '../html/outward/account/bank/accountOutBankFreeze.html',
            controller: '../javascript/outward/account/bank/accountOutBankFreeze.js'
        },
        //出款管理->账号管理->第三方
        '/OutwardAccountThirdUsed:*': {
            templateUrl: '../html/outward/account/third/accountOutThirdUsed.html',
            controller: '../javascript/outward/account/third/accountOutThirdUsed.js'
        },
        '/OutwardAccountThirdEnabled:*': {
            templateUrl: '../html/outward/account/third/accountOutThirdEnabled.html',
            controller: '../javascript/outward/account/third/accountOutThirdEnabled.js'
        },
        '/OutwardAccountThirdStop:*': {
            templateUrl: '../html/outward/account/third/accountOutThirdStop.html',
            controller: '../javascript/outward/account/third/accountOutThirdStop.js'
        },
        '/OutwardAccountThirdFreezed:*': {
            templateUrl: '../html/outward/account/third/accountOutThirdFreeze.html',
            controller: '../javascript/outward/account/third/accountOutThirdFreeze.js'
        },
        //出款管理->账号管理->储备银行卡
        '/OutwardAccountStorageReserved:*': {
            templateUrl: '../html/outward/account/storage/accountOutStorageReserved.html',
            controller: '../javascript/outward/account/storage/accountOutStorageReserved.js'
        },
        '/OutwardAccountStorageCash:*': {
            templateUrl: '../html/outward/account/storage/accountOutStorageCash.html',
            controller: '../javascript/outward/account/storage/accountOutStorageCash.js'
        },
        '/OutwardAccountStorageFreezed:*': {
            templateUrl: '../html/outward/account/storage/accountOutStorageFreeze.html',
            controller: '../javascript/outward/account/storage/accountOutStorageFreeze.js'
        },
        '/OutwardAccountAll:*': {
            templateUrl: '../html/outward/account/accountOutAll.html',
            controller: '../javascript/outward/account/accountOutAll.js'
        },
        //第三方代付
        '/ThirdAccount:*': {
            templateUrl: '../third/thirdAccount.html',
            controller: '../third/thirdAccount.js'
        },
        //财务管理>旧公司用款
        '/financeCompanyExpenditure:*': {
            templateUrl: '../html/finmanagement/finCompanyExpenditure/companyExpenditureStatistics.html',
            controller: '../javascript/finmanagement/finCompanyExpenditure/companyExpenditureStatistics.js'
        },
        //财务管理>新公司用款
        '/FinanceCompanyMoney:*': {
        	templateUrl: '../html/finmanagement/companyMoney.html',
        	controller: '../javascript/finmanagement/companyMoney.js'
        },
        //财务管理>公司用款明细
        '/financeCompanyExpenditureDetail:*': {
            templateUrl: '../html/finmanagement/finCompanyExpenditure/companyExpenditure.html',
            controller: '../javascript/finmanagement/finCompanyExpenditure/companyExpenditure.js'
        },
        //财务管理>公司用款汇总
        '/financeCompanyExpenditureAll:*': {
            templateUrl: '../html/finmanagement/finCompanyExpenditure/companyExpenditureAll.html',
            controller: '../javascript/finmanagement/finCompanyExpenditure/companyExpenditureAll.js'
        },
        //财务管理>现金出款
        '/financeCashExpenditure:*': {
            templateUrl: '../html/finmanagement/finCashExpenditure/cashExpenditure.html',
            controller: '../javascript/finmanagement/finCashExpenditure/cashExpenditure.js'
        },
        //财务管理>出款明细(按账号统计、盘口统计在一个页面)
        '/FinanceOutward:*': {
            templateUrl: '../html/finmanagement/finoutstat/finOutStat.html',
            controller: '../javascript/finmanagement/finoutstat/finOutStat.js'
        },
        //财务管理>出款明细>系统明细(按账号统计)
        '/finOutStatSys:*': {
            templateUrl: '../html/finmanagement/finoutstat/finOutStatSys.html',
            controller: '../javascript/finmanagement/finoutstat/finOutStatSys.js'
        },
        //财务管理>出款明细>按盘口统计 明细
        '/finOutHandicap:*': {
            templateUrl: '../html/finmanagement/finoutstat/finOutStatMatch.html',
            controller: '../javascript/finmanagement/finoutstat/finOutStatMatch.js'
        },
        //财务管理>出款明细>银行明细(按账号统计)
        '/finOutStatFlow:*': {
            templateUrl: '../html/finmanagement/finoutstat/finOutStatFlow.html',
            controller: '../javascript/finmanagement/finoutstat/finOutStatFlow.js'
        },
        //财务管理>入款明细
        '/FinInStat:*': {
            templateUrl: '../html/finmanagement/fininstat/finInStat.html',
            controller: '../javascript/finmanagement/fininstat/finInStat.js'
        },
        //财务管理>入款明细>系统明细
        '/finInStatMatch:*': {
            templateUrl: '../html/finmanagement/fininstat/finInStatMatch.html',
            controller: '../javascript/finmanagement/fininstat/finInStatMatch.js'
        },
        //财务管理>入款明细>银行明细
        '/finInStatMatchBank:*': {
            templateUrl: '../html/finmanagement/fininstat/finInStatMatchBank.html',
            controller: '../javascript/finmanagement/fininstat/finInStatMatchBank.js'
        },//财务管理>入款明细>下发银行卡>系统明细
        '/finInSendCardStatMatch:*': {
            templateUrl: '../html/finmanagement/fininstat/finInSendCardStatMatch.html',
            controller: '../javascript/finmanagement/fininstat/finInSendCardStatMatch.js'
        },
        //财务管理>中转明细
        '/FinTransStat:*': {
            templateUrl: '../html/finmanagement/fintransstat/finTransStat.html',
            controller: '../javascript/finmanagement/fintransstat/finTransStat.js'
        },
        //财务管理>中转明细>流水明细&系统明细（页面一样）
        '/finTransStatMatch:*': {
            templateUrl: '../html/finmanagement/fintransstat/finTransStatMatch.html',
            controller: '../javascript/finmanagement/fintransstat/finTransStatMatch.js'
        },//财务管理>出入财务汇总
        '/FinMoreStat:*': {
            templateUrl: '../html/finmanagement/finmorestat/finMoreStat.html',
            controller: '../javascript/finmanagement/finmorestat/finMoreStat.js'
        },//财务管理>出入财务汇总>明细
        '/finMoreStatMatch:*': {
            templateUrl: '../html/finmanagement/finmorestat/finMoreStatLevel.html',
            controller: '../javascript/finmanagement/finmorestat/finMoreStatLevel.js'
        },//财务管理>余额明细
        '/FinBalanceStat:*': {
            templateUrl: '../html/finmanagement/finbalancestat/finBalanceStat.html',
            controller: '../javascript/finmanagement/finbalancestat/finBalanceStat.js'
        },//财务管理>余额明细>明细
        '/finBalanceStatMatch:*': {
            templateUrl: '../html/finmanagement/finbalancestat/finBalanceStatCard.html',
            controller: '../javascript/finmanagement/finbalancestat/finBalanceStatCard.js'
        },//财务管理>余额明细>明细>系统明细
        '/finTransBalanceSys:*': {
            templateUrl: '../html/finmanagement/finbalancestat/finTransBalanceSys.html',
            controller: '../javascript/finmanagement/finbalancestat/finTransBalanceSys.js'
        },//财务管理>余额明细>明细>流水明细(银行流水)
        '/finTransBalanceFlow:*': {
            templateUrl: '../html/finmanagement/finbalancestat/finTransBalanceFlow.html',
            controller: '../javascript/finmanagement/finbalancestat/finTransBalanceFlow.js'
        },//财务管理>亏损统计
        '/FinLessStat:*': {
            templateUrl: '../html/finmanagement/finLessStat/finLessStat.html',
            controller: '../javascript/finmanagement/finLessStat/finLessStat.js'
        },//财务管理>出入卡清算
        '/FinCardLiquidation:*': {
            templateUrl: '../html/finmanagement/cardLiquidation/finCardLiquidation.html',
            controller: '../javascript/finmanagement/cardLiquidation/finCardLiquidation.js'
        },
        '/finOutStat:*': {
            templateUrl: '../html/system/sysUser.html',
            controller: '../javascript/system/sysUser.js'
        },
        //系统管理
        '/SystemUser:*': {
            templateUrl: '../html/system/sysUser.html',
            controller: '../javascript/system/sysUser.js'
        },
        //公告发布
        '/HomeSetting:*': {
        	templateUrl: '../html/home/homeSetting.html',
        	controller: '../html/home/homeSetting.js',
        },
        '/SystemUserDetail:*': {
            templateUrl: '../html/system/sysUserDetail.html',
            controller: '../javascript/system/sysUserDetail.js'
        },
        '/SystemRole:*': {
            templateUrl: '../html/system/sysRole.html',
            controller: '../javascript/system/sysRole.js'
        },
        '/SystemRole:*': {
            templateUrl: '../html/system/sysRole.html',
            controller: '../javascript/system/sysRole.js'
        },
        '/SystemRoleMenu:*': {
            templateUrl: '../html/system/sysRoleMenu.html',
            controller: '../javascript/system/sysRoleMenu.js'
        },
        '/SystemSetting:*': {
            templateUrl: '../html/system/sysSetting.html',
            controller: '../javascript/system/sysSetting.js'
        },
        //入账核对
        '/flowCheckByToAccount:*': {
            templateUrl: '../html/income/audit/flowCheckByToAccount.html',
            controller: '../javascript/income/audit/flowCheckByToAccount.js'
        },
        '/Host:*': {
            templateUrl: '../html/monitor/host.html',
            controller: '../javascript/monitor/host.js'
        },
        '/HostDetail:*': {
        	templateUrl: '../html/monitor/hostDetail.html',
        	controller: '../javascript/monitor/hostDetail.js'
        },
        '/MonitorLog:*': {
        	templateUrl: '../html/monitor/monitorLog_new.html',
        	controller: '../javascript/monitor/monitorLog_new.js'
        },
        '/MonitorLogDetail:*': {
            templateUrl: '../html/monitor/monitorLogDetail.html',
            controller: '../javascript/monitor/monitorLogDetail.js'
        },
        '/SystemLevel:*': {
            templateUrl: '../html/system/sysLevel.html',
            controller: '../javascript/system/sysLevel.js'
        },
        '/SystemImportBankLog:*': {
            templateUrl: '../html/system/sysImportBankLog.html',
            controller: '../javascript/system/sysImportBankLog.js'
        },
        '/SystemStopOrder:*': {
            templateUrl: '../html/system/sysStopOrder.html',
            controller: '../javascript/system/sysStopOrder.js'
        },
        '/SystemMaintainBank:*': {
            templateUrl: '../html/system/systemMaintainBank.html',
            controller: '../javascript/system/systemMaintainBank.js'
        },
        // '/BizAccountMonitor:*': {// 需求 7441
        //     templateUrl: '../html/biz/accountMonitor.html',
        //     controller: '../javascript/biz/accountMonitor.js'
        // },
        '/BizAccountAlarm:*': {// 需求 7441
             templateUrl: '../html/biz/accountAlarm.html',
             controller: '../javascript/biz/accountAlarm.js'
        },
        '/BizAccountAlarmDetail:*': {
            templateUrl: '../html/biz/accountAlarmDetail.html',
            controller: '../javascript/biz/accountAlarmDetail.js'
        },
        '/BizAccountInvsgt:*': {
            templateUrl: '../html/biz/accountInvsgt.html',
            controller: '../javascript/biz/accountInvsgt.js'
        },
        '/BizMobileMonitor:*': { //需求 7441
             templateUrl: '../html/biz/monitorMobile.html',
             controller: '../javascript/biz/monitorMobile.js'
        },
        '/BizMobileLogs:*': {
            templateUrl: '../html/biz/mobileLogs.html',
            controller: '../javascript/biz/mobileLogs.js'
        },
        '/OutwardBankFreezedChoice:*': {
            templateUrl: '../html/system/OutwardBankFreezedChoice.html',
            controller: '../javascript/system/OutwardBankFreezedChoice.js'
        },
        '/accountOutStorageExcep:*': { // 需求7441
            templateUrl: '../html/system/accountOutStorageExcep.html',
            controller: '../javascript/system/accountOutStorageExcep.js'
        },
        '/UpdateSpecialAccount:*': {
        	templateUrl: '../html/system/accountSpecialUpdate.html',
        	controller: '../javascript/system/accountSpecialUpdate.js'
        },
        '/BonusDetail:*': {
            templateUrl: '../html/common/bonusDetail.html',
            controller: '../javascript/common/bonusDetail.js'
        },
        '/SystemPersonnelRole:*': {
        	templateUrl: '../html/system/sysPersonnelRole.html',
            controller: '../javascript/system/sysPersonnelRole.js'
        },'/FeedBack:*': {
        	templateUrl: '../html/feedback/feedback.html',
            controller: '../javascript/feedback/feedback.js'
        },'/NewOpinion:*': {
        	templateUrl: '../html/feedback/newOpinion.html',
            controller: '../javascript/feedback/newOpinion.js'
        },'/FeedBackShowDetails:*': {
        	templateUrl: '../html/feedback/feedbackShowDetails.html',
            controller: '../javascript/feedback/feedbackShowDetails.js'
        },'/SystemHandicap:*':{
            templateUrl: '../html/system/sysHandicap.html',
            controller: '../javascript/system/sysHandicap.js'
        },
         '/AccountExpOpr:*':{ // 需求 7441
             templateUrl: '../html/biz/accountExpOpr.html',
             controller: '../javascript/biz/accountExpOpr.js'
         }
        ,'/RebateTaskTotal:*':{
            templateUrl: '../html/rebate/rebateTaskTotal.html',
            controller: '../javascript/rebate/rebateTaskTotal.js'
        },'/AccountAudit:*': {  //账号审核
            templateUrl: '../html/outward/account/accountAuditNew.html',
            controller: '../javascript/outward/account/accountAuditNew.js'
        },'/AccountAuditNew:*': {  //账号审核
            templateUrl: '../html/outward/account/accountAuditNews.html',
            controller: '../javascript/outward/account/accountAuditNews.js'
        }
        // ,'/BankCardService:*': {  //银行卡维护列表  //需求 7441 删除
        //     templateUrl: '../phone/business/bankCardService.html',
        //     controller: '../phone/business/bankCardService.js'
        // }
        ,'/AccountDelete:*': {//需求 7441 删除
            templateUrl: '../html/outward/account/accountDelete.html',
             controller: '../javascript/outward/account/accountDelete.js'
         }
        ,'/ProblemEqpInv:*': {
            templateUrl: '../html/problem/problemEqpInv.html',
            controller: '../javascript/problem/problemEqpInv.js'
        },'/ProblemAccInv:*': {
            templateUrl: '../html/problem/problemAccInv.html',
            controller: '../javascript/problem/problemAccInv.js'
        },'/ProblemInvTotal:*': {
            templateUrl: '../html/problem/problemInvTotal.html',
            controller: '../javascript/problem/problemInvTotal.js'
        },'/ProblemInv:*': {
            templateUrl: '../html/problem/problemAccInv.html',
            controller: '../javascript/problem/problemAccInv.js'
        },'/Derating:*': {
            templateUrl: '../html/rebate/rebateDerating.html',
            controller: '../javascript/rebate/rebateDerating.js'
        },
        //Default
        'defaults': '/Home:*'// 不符合上述路由时，默认跳至
    };
	//禁止查看入款汇总敏感信息
	if(isHideOutAccountAndModifyNouns){
		router['/IncomeAuditCompTotal:*']={
            templateUrl: '../html/income/audit/incomeHideTotalIn.html',
            controller: '../javascript/income/audit/incomeHideTotalIn.js'
        };
	}
    try {
        jqueryRouter.start({
            view: '.ui-view',// 装载视图的dom
            router: router,
            errorTemplateId: '#error'
        });
    } catch (e) {
        console.log(e);
    }
});


/**
 *去重（Array）
 */
function removeDuplicatedItem(ar) {
    var ret = [];
    for (var i = 0, j = ar.length; i < j; i++) {
        if (ret.indexOf(ar[i]) === -1) {
            ret.push(ar[i]);
        }
    }
    return ret;
}

function uuid(len, radix) {
    var chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('');
    var uuid = [], i;
    radix = radix || chars.length;
    if (len) {
        for (i = 0; i < len; i++) uuid[i] = chars[0 | Math.random() * radix];
    } else {
        var r;
        uuid[8] = uuid[13] = uuid[18] = uuid[23] = '-';
        uuid[14] = '4';
        for (i = 0; i < 36; i++) {
            if (!uuid[i]) {
                r = 0 | Math.random() * 16;
                uuid[i] = chars[(i == 19) ? (r & 0x3) | 0x8 : r];
            }
        }
    }
    return uuid.join('');
}

function fillDataToModel4Item(_data, _model) {
    var _str = '';
    if (!_data || !_model) {
        return _str;
    }
    var _str = _model;
    for (var _x in _data) {
        var reger = new RegExp("{" + _x + "}", "gm");
        _str = _str.replace(reger, _data[_x] ? _data[_x] : '');
    }
    return _str;
}

function fillDataToModel4Array(_dataToArray, _model4ForItem) {
    var _str = '';
    if (!_dataToArray || _dataToArray.length == 0 || !_model4ForItem) {
        return _str;
    }
    for (var index in _dataToArray) {
        _str = _str + fillDataToModel4Item(_dataToArray[index], _model4ForItem);
    }
    return _str;
}

/**
 * 时间处理
 */
function timeStamp2yyyyMMddHHmmss(time) {
    if (time) {
        var datetime = new Date();
        datetime.setTime(time);
        var year = datetime.getFullYear();
        var month = datetime.getMonth() + 1;
        if (month > 0 && month < 10) {
            month = '0' + month;
        }
        var day = datetime.getDate();
        if (day > 0 && day < 10) {
            day = '0' + day;
        }

        var hour = datetime.getHours();
        if (hour && hour > 0 && hour <= 9) {
            hour = '0' + hour;
        }
        if (hour == 0) {
            hour = "00";
        }
        var minute = datetime.getMinutes();
        if (minute && minute > 0 && minute <= 9) {
            minute = '0' + minute;
        }
        if (minute == 0) {
            minute = '00';
        }
        var second = datetime.getSeconds();
        if (second && second > 0 && second <= 9) {
            second = '0' + second;
        }
        if (second == 0) {
            second = '00';
        }
        return year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
    }
    else return '';
}
//完整时间转为时分秒
var hhmmss=function(yyyyMMddHHmmss){
	if(yyyyMMddHHmmss&&yyyyMMddHHmmss.length>12){
		var times=yyyyMMddHHmmss.split(" ");
		if(times&&times.length>1){
			return times[1];
		}else{
			return " -- ";
		}
	}else{
		return " -- ";
	}
}
function formatDuring(mss) {
    var str = '';
    var days = parseInt(mss / (1000 * 60 * 60 * 24));
    var dh = 0;
    var hours = parseInt((mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    var minutes = parseInt((mss % (1000 * 60 * 60)) / (1000 * 60));
    var seconds = Math.round(parseFloat((mss % (1000 * 60)) / 1000));
    if (days) {
        dh += days * 24;
    }
    hours += dh;
    if (hours) {
        if (hours > 0 && hours < 10) {
            str += "0" + hours;
        } else {
            str += hours;
        }
    } else {
        str += "00"
    }
    if (minutes) {
        if (hours) {
            str += ":";
            if (minutes > 0 && minutes < 10) {
                str += "0" + minutes;
            } else {
                str += minutes;
            }
        } else {
            if (minutes > 0 && minutes < 10) {
                str += ":0" + minutes;
            } else {
                str += ":" + minutes;
            }
        }
    } else {
        str += ":00";
    }
    if (seconds) {
        if (minutes) {
            str += ":";
            if (seconds > 0 && seconds < 10) {
                str += "0" + seconds;
            } else {
                str += seconds;
            }
        } else {
            if (seconds > 0 && seconds < 10) {
                str += ":0" + seconds;
            } else {
                str += ":" + seconds;
            }
        }

    } else {
        str += ":00";
    }
    return str;
}
function _checkObj(obj,showLine,diyStr) {
    if(diyStr||diyStr==0||diyStr=='0'){
        return obj || diyStr;
    }else if(showLine){
        return obj ||'--';
    }else{
        return obj ||'';
    }
}
//操作成功的提示
function showMessageForSuccess(message,time) {

    $.gritter.add({
        title: '消息提示',
        text: message,
        // image: 'admin/clear/notif_icon.png',
        sticky: false,
        time: time?time:500,
        speed: 100,
        position: 'bottom-right',
        class_name: 'gritter-success'// gritter-center
    });

}

// 操作失败的提示
function showMessageForFail(message,time) {
    $.gritter.add({
        title: '消息提示',
        text: message,
        // image: 'admin/clear/notif_icon.png',
        sticky: false,
        time: time?time:500,
        speed: 100,
        position: 'top-right',
        class_name: 'gritter-center'// gritter-center
    });
}

function htmlDailyAmount(income0outward1, limitAmount, dailyAmount) {
    var limit = limitAmount ? limitAmount : (income0outward1 == 1 ? sysSetting.OUTDRAW_LIMIT_CHECKOUT_TODAY : sysSetting.INCOME_LIMIT_CHECKIN_TODAY);
    var data = {
        classOfColor: (dailyAmount >= eval(limit) ? 'red' : 'blue'),
        dailyAmount: (dailyAmount + ''),
        titleName: (income0outward1 == 0 ? '当日收款' : '当日出款')
    };
    var model = '<span class="badge badge-transparent tooltip-error" title="{titleName}：{dailyAmount}"><i class="ace-icon fa fa-question bigger-130 {classOfColor}" style="cursor:pointer "></i></span>';
    return fillDataToModel4Item(data, model);
}

/**
 * 异步加载系统配置
 */
var loadSysSetting = function () {
    $.ajax({
        type: "POST", url: '/r/set/findAllToMap', dataType: 'JSON', success: function (res) {
            if (res.status != 1) {
                return;
            }
            sysSetting = res.data;
        }
    });
};

/**
 * 盘口，层级，入款账号 联动
 * @param $handicapEle
 *            盘口Jquery对象
 * @param $levelEle
 *            层级Jquery对象
 * @param $accountEle
 *            账号Jquery对象
 * @param typeArray
 *            入款账号分类
 * @author 
 */
var loadHandicap_Level = function ($handicapEle, $levelEle, $accountEle, typeArray) {
    var cnst = this;
    this.props = function ($Ele) {
        try {
            return {'style': ('width:' + parseInt($Ele.prop('class').split('idth')[1]) + 'px')};
        } catch (e) {
        }
        return {'style': 'width:150px'};
    };
    this.defOpt = '<option value="" selected="selected">全部</option>';
    this.propsHand = cnst.props($handicapEle);
    this.propsLevl = cnst.props($levelEle);
    this.propsAcnt = cnst.props($accountEle);
    this.chosen = {
        enable_split_word_search: true, search_contains: true, inherit_select_classes: true, no_results_text: '无结果'
    };
    var change = function () {
        if (!$accountEle) {
            return;
        }
        var handicapId = $handicapEle ? $handicapEle.val() : null;
        var levelId = $levelEle ? $levelEle.val() : null;
        if (!handicapId && !levelId) {
            $accountEle.empty().append(cnst.defOpt).trigger("chosen:updated");
        } else {
            var params = {
                handicapId: handicapId,
                levelId: levelId,
                incomeTypeArray: (typeArray ? typeArray.toString() : null)
            };
            $.ajax({
                dataType: 'json',
                async: false,
                url: API.r_account_findByHandicapAndLevel,
                data: params,
                success: function (jsonObject) {
                    if (jsonObject.status == 1) {
                        $accountEle.empty().append(cnst.defOpt);
                        $(jsonObject.data).each(function (i, val) {
                            $accountEle.append('<option value="' + val.id + '" account="' + val.account + '">' + (val.type == accountTypeInThird ? (val.bankName ? val.bankName : '无') : (val.account ? val.account : '无')) + '</option>');
                        });
                        $accountEle.trigger("chosen:updated");
                    }
                }
            });
        }
    };

    !$handicapEle || $handicapEle.html((function () {
        var category = eval(getCookie('JUSERCATEGORY'));
        category = category > userCategoryHandicapPrefix ? (category - userCategoryHandicapPrefix) : null;
        var option = [];
        option.push(cnst.defOpt);
        $.each(handicap_list, function (i, temp) {
            if (!category || category && category == temp.id) {
                option.push('<option value="' + temp.id + '" handicapcode="' + temp.code + '">' + temp.name + '</option>');
            }
        });
        return option.join('');
    })()).change(function () {
        change();
        if (!$levelEle) {
            return;
        }
        $levelEle.empty().append(cnst.defOpt);
        if (this.value) {
            $.each(level_list[this.value], function (i, levelInfo) {
                $levelEle.append('<option value="' + levelInfo.id + '" levelcode="' + levelInfo.code + '">' + levelInfo.name + '</option>');
            });
        }
        $levelEle.trigger("chosen:updated");
    }).chosen(cnst.chosen).next().prop(cnst.propsHand);

    !$levelEle || $levelEle.empty().append(cnst.defOpt).chosen(cnst.chosen).change(function () {
        change();
    }).next().prop(cnst.propsLevl);

    !$accountEle || $accountEle.empty().append(cnst.defOpt).chosen(cnst.chosen).next().prop(cnst.propsAcnt);
};
var loadAllHandicap = function () {
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        url: API.r_handicap_list,
        data: {"userId": getCookie('JUSERID')},
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data) {
                handicap_list_all = jsonObject.data;
            }
        }
    });
};

var loadAllZone = function () {
    $.ajax({dataType: 'JSON', type: "POST", url: API.r_handicap_listZone,success: function (jsonObject) {
        if (jsonObject.status == 1 && jsonObject.data) {
            zone_list_all = jsonObject.data;
        }
    }});
}


//暂时废弃 勿删，有用
var getHandicap_Level_remark = function () {
    //读取当前账号拥有的盘口角色（权限只到盘口，未到层级）
	handicap_list = [], level_list = [];
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        url: "/r/permission/find4User",
        data: {"userId": getCookie('JUSERID')},
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data) {
            	handicap_list = jsonObject.data.bizHandicapList;
                level_list = jsonObject.data.bizLevelMapList;
            }
        }
    });
};

var getHandicap_Level = function () {
    //读取当前账号拥有的盘口角色（权限只到盘口，未到层级）
	handicapId_list=[], handicap_list = [], level_list = [];
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        url: "/r/permission/find4User",
        data: {"userId": getCookie('JUSERID')},
        success: function (jsonObject) {
            if (jsonObject.status == 1 && jsonObject.data && jsonObject.data.bizHandicapList) {
                $.each(jsonObject.data.bizHandicapList, function (i, result) {
                    handicap_list.push(result);
                    handicapId_list.push(result.id);
//                    废弃勿删 不循环查层级
//                    $.ajax({
//                        dataType: 'JSON',
//                        type: "POST",
//                        url: API.r_level_list,
//                        async: false,
//                        data: {handicapCode: result.code, enabled: 1},
//                        success: function (jsonObjectLevel) {
//                            if (jsonObjectLevel.status == 1 && jsonObjectLevel.data) {
//                                level_list[result.id] = jsonObjectLevel.data;
//                            }
//                        }
//                    });
                });
            }
        }
    });
};

function buildContentRight(data) {
    if (!data || !data.children || !data.action || data.action.split(':').length > 2) {
        return (data && data.action && data.action.split(':').length > 2 ) ? data.action : null;
    }
    var children = {};
    for (var index in data.children) {
        var action = buildContentRight(data.children[index]);
        action ? children[action] = 1 : null;
    }
    ContentRight[data.action] = children;
}

/**
 *params
 * {
 *      'OutwardAccountBankUsed:Recycle:*':function($obj,hasRight){
 *
 *      },
 *      'OutwardAccountBankUsed:Asign:*':function($obj,hasRight){
 *
 *      }
 * }
 */
function contentRight(data) {
    try {
        var currentMenuAction = JSON.parse(getCookie(JCURRENTMENUDATA)).action;
        var Right = ContentRight[currentMenuAction], Right = Right ? Right : {};
        $('.contentRight').each(function () {
            var t = $(this);
            var contentRight = t.attr('contentRight');
            if (!contentRight || ( (!data || !data[contentRight]) && contentRight && !Right[contentRight] )) {
                contentRight ? t.remove() : null;
                return;
            }
            data && data[contentRight] ? data[contentRight](t, Right && Right[contentRight]) : null;
        });
    } catch (e) {
        console.log(e);
    }
}

function showCaptureImg(url) {
    if (url) {
        $('#captureImg').attr('src', url);
        $('#captureModal').modal('show');
    }
}


function getCookie(sName) {
    var aCookie = document.cookie.split("; ");
    for (var i = 0; i < aCookie.length; i++) {
        var aCrumb = aCookie[i].split("=");
        if (sName == aCrumb[0])
            return unescape(aCrumb[1]);
    }
    return null;
}

function setCookie(name, value) {
    document.cookie = name + "=" + escape(value) + ";expires=Session;Path=/";
}
/**平台传来的信息：最近入款：入款类型 iDepositType显示 （1支付接口在线入款 2 公司入款 3 快速入款 4 人工入款 (补点+公司收入) 5 冲帐-取消出款 (补点+公司收入)
 6 冲帐-重覆出款 (补点+公司收入) 7 存款优惠 (补点) 8 返点优惠 (补点) 9 活动优惠 (补点) 10 负数额度归零 (补点) 11其它 (补点)
 12 优惠补点(补点)-锁定转点功能 13 人工存提-紅利 16人工转点(入款) 17汇款优惠 18首存优惠 19公司入款优惠
 20派彩 21退水 22退码 23返点 24微信入款 25 支付宝入款 26退佣 ）*/
function _showDepositType(type) {
    var typeDesc = '';
    if (type) {
        switch (type) {
            case 1:
                typeDesc = '支付接口在线入款';
                break;
            case 2:
                typeDesc = '公司入款';
                break;
            case 3:
                typeDesc = '快速入款';
                break;
            case 4:
                typeDesc = '人工入款 (补点+公司收入)';
                break;
            case 5:
                typeDesc = '冲帐-取消出款 (补点+公司收入)';
                break;
            case 6:
                typeDesc = '冲帐-重覆出款 (补点+公司收入)';
                break;
            case 7:
                typeDesc = '存款优惠 (补点) ';
                break;
            case 8:
                typeDesc = '返点优惠 (补点)';
                break;
            case 9:
                typeDesc = '活动优惠 (补点)';
                break;
            case 10:
                typeDesc = '负数额度归零 (补点)';
                break;
            case 11:
                typeDesc = '其它 (补点)';
                break;
            case 12:
                typeDesc = '优惠补点(补点)-锁定转点功能 ';
                break;
            case 13:
                typeDesc = '人工存提-紅利';
                break;
            case 16:
                typeDesc = '人工转点(入款)';
                break;
            case 17:
                typeDesc = '汇款优惠';
                break;
            case 18:
                typeDesc = '首存优惠';
                break;
            case 19:
                typeDesc = '公司入款优惠';
                break;
            case 20:
                typeDesc = '派彩';
                break;
            case 21:
                typeDesc = '退水';
                break;
            case 22:
                typeDesc = '退码';
                break;
            case 23:
                typeDesc = '返点';
                break;
            case 24:
                typeDesc = '微信入款';
                break;
            case 25:
                typeDesc = '支付宝入款';
                break;
            case 26:
                typeDesc = '退佣';
                break;
        }
    }
    return typeDesc;
}

function _showAccountStatus(status) {
    var desc = "";
    if (status) {
        switch (status) {
            case accountStatusNormal :
                desc = "在用";
                break;
            case accountStatusFreeze :
                desc = "冻结";
                break;
            case accountStatusStopTemp :
                desc = "暂停";
                break;
            case accountStatusEnabled:
                desc = "可用";
                break;
            case accountStatusDelete :
                desc = "删除";
                break;
            case accountStatusExcep :
            	desc = "异常";
            	break;
            case accountInactivated :
            	desc = "未激活";
            	break;
            case accountActivated :
            	desc = "已激活";
            	break;
        }
    }
    return desc;
}


/**
 * 金额计算后丢失精度时调用，恢复到丢失精度前的数字
 */
var setAmountAccuracy = function (amount) {
	amount=amount*1;
    return (parseInt((amount > 0 ? amount + 0.009 : amount - 0.009) * 100)) / 100;
};
function _showDefaultForALL(timeInputObj, start, end) {
    if (start && end) {
        $(timeInputObj).val(start.format('YYYY-MM-DD HH:mm:ss') + '~' + end.format('YYYY-MM-DD HH:mm:ss'));
        if (!$(timeInputObj).val()) {
            $(timeInputObj).prop('placeholder', start.format('YYYY-MM-DD HH:mm:ss') + '~' + end.format('YYYY-MM-DD HH:mm:ss'));
        }
    }
}
function getElemPos(e) {
    var offset = e[0].offsetLeft;
    if (e.offsetParent()[0].offsetParent) {
        offset += getElemPos(e.offsetParent());
    }
    return offset;
}
//时间控件
function _datePickerForAll(timeInputObj) {
    var start = '', end = '';
    if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
        start = moment().hours(07).minutes(0).seconds(0);
        end = moment().add(1, 'days').hours(06).minutes(59).seconds(59);
    }
    if ((moment() >= moment().hours(0).minutes(0).seconds(0) && moment() < moment().hours(07).minutes(0).seconds(0) )) {
        start = moment().subtract(1, 'days').hours(07).minutes(0).seconds(0);
        end = moment().hours(06).minutes(59).seconds(59);
    }
    var todayStart = '';
    if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
        todayStart = moment().hours(07).minutes(0).seconds(0);
    } else {
        todayStart = moment().subtract(1, 'days').hours(07).minutes(0).seconds(0);
    }
    var todayEnd = '';
    if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
        todayEnd = moment().add(1, 'days').hours(06).minutes(59).seconds(59);
    } else {
        todayEnd = moment().hours(06).minutes(59).seconds(59);
    }
    var yestStart = '', yestEnd = '';
    if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
        yestStart = moment().hours(07).minutes(0).seconds(0).subtract('days', 1);
        yestEnd = moment().add(1, 'days').hours(06).minutes(59).seconds(59).subtract('days', 1);
    }
    if ((moment() >= moment().hours(0).minutes(0).seconds(0) && moment() < moment().hours(07).minutes(0).seconds(0) )) {
        yestStart = moment().subtract(1, 'days').hours(07).minutes(0).seconds(0).subtract('days', 1);
        yestEnd = moment().hours(06).minutes(59).seconds(59).subtract('days', 1);
    }
    var near7Start = '', near7End = '';
    if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
        near7Start = moment().hours(07).minutes(0).seconds(0).subtract('days', 2);
        near7End = moment().add(1, 'days').hours(06).minutes(59).seconds(59);
    }
    if ((moment() >= moment().hours(0).minutes(0).seconds(0) && moment() < moment().hours(07).minutes(0).seconds(0) )) {
        near7Start = moment().subtract(1, 'days').hours(07).minutes(0).seconds(0).subtract('days', 2);
        near7End = moment().hours(06).minutes(59).seconds(59);
    }
    var opensVal = 'left';
    if (timeInputObj.length == 0) {
        return;
    }
    var inputObjLocation = getElemPos(timeInputObj);
    if (inputObjLocation !== 0 && inputObjLocation < 662) {
        opensVal = 'right';
    }
    timeInputObj.daterangepicker({
        timePicker: true,
        timePickerIncrement: 1,
        timePicker24Hour: true,
        autoUpdateInput: true,
        timePickerSeconds: true,
        startDate: start, //设置开始日期
        endDate: end, //设置开始日期
        dateLimit : {
            days : 3
        },
        ranges: {
            '最近1小时': [moment().subtract('hours', 1), moment()],
            '今日': [todayStart, todayEnd],
            '昨日': [yestStart, yestEnd],
            '最近3日': [near7Start, near7End]
        },
        opens: opensVal, //日期选择框的弹出位置
        locale: {
            "format": "YYYY-MM-DD HH:mm:ss",
            "separator": " ~ ",
            "applyLabel": "确定",
            "cancelLabel": "取消",
            "fromLabel": "从",
            "toLabel": "到",
            "customRangeLabel": "自定义(最多只能查3天)",
            "dayNames": [
                "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
            ],
            "daysOfWeek": [
                "日", "一", "二", "三", "四", "五", "六"
            ],
            "monthNames": [
                "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"
            ],
            "firstDay": 1
        }
    }, _showDefaultForALL(timeInputObj, start, end)).on('apply.daterangepicker', function (ev, picker) {
        timeInputObj.val(picker.startDate.format('YYYY-MM-DD HH:mm:ss') + ' ~ ' + picker.endDate.format('YYYY-MM-DD HH:mm:ss'));
    }).on('cancel.daterangepicker', function (ev, picker) {
        timeInputObj.val('');
    });
}


/**
 * 重写JQ 去掉前后、中间所有空格
 */
$.trim = function (str, trimCenter) {
    if (!str) return "";
    if (trimCenter) {
        //去掉中间空格
        var result = str.toString().replace(/(^\s+)|(\s+$)/g, "").replace(/\s/g, "");
    } else {
        //只去掉前后空格
        var result = str.toString().replace(/(^\s+)|(\s+$)/g, "");
    }
    return result;
};
//--------------------------------------------------------

function _searchAfterAutoUpdateTimeReq() {
    if (timeOutSearchReq) {
        clearInterval(timeOutSearchReq);
    }
    var time = parseInt($('#autoUpdateTimeReq').val());
    if (time != 0 && currentPageLocation.indexOf('OutwardAuditTotal:*') > -1) {
        timeOutSearchReq = setInterval(function () {
            _searchOutRequestTotal();
        }, time * 1000);
    }
}
var openModal = function () {
    $('#clearAccountDateListModal').modal('toggle');
};

var openThird_EnCash_Modal = function () {
    $('#Third_EnCash_Modal').modal('toggle');
};

var timeOutSearchAli=null,awCountFlowsTaskTime=null;
//支付宝 微信入款 审核定时任务
function _searchIncomeAWTimeTask(type) {

    if(type=='alipayToMatch' || type=='wechatToMatch'){
        if (timeOutSearchAli) {
            clearInterval(timeOutSearchAli);
        }
        var time = 0;
        if (type=='alipayToMatch'){
            time = parseInt($('#approveAlipayIn   #autoUpdateTimeAWIn').val());
        }
        if (type=='wechatToMatch'){
            time = parseInt($('#approveWechatIn   #autoUpdateTimeAWIn').val());
        }
        if (time && time >0 && currentPageLocation.indexOf('IncomeAuditComp:*') > -1) {
            timeOutSearchAli = setInterval(function () {
                _awMatchingFlows() ;_awMatchingOrder();
            }, time * 1000);
        }
    }else{
        if (timeOutSearchAli) {
            clearInterval(timeOutSearchAli);
        }
    }
    if (awCountFlowsTaskTime) {
        clearInterval(awCountFlowsTaskTime);
    }
    if (type !='alipayToMatch' && type !='wechatToMatch'  && currentPageLocation.indexOf('IncomeAuditComp:*') > -1) {
        awCountFlowsTaskTime = setInterval(function () {
            _getFlowsTotalTask();
        }, 10 * 1000);
    }else{
        if (awCountFlowsTaskTime) {
            clearInterval(awCountFlowsTaskTime);
        }
    }
}
var timeOutSearchWechatIn=null,timeOutSearchAliIn=null,timeOutSearchRebateDrawing=null,timeOutSearchRebate=null,timeOutSearchCheck=null;
//微信入款定时任务
function _searchIncomeAuditWechatInTimeTask(type) {
    if (timeOutSearchWechatIn) {
        clearInterval(timeOutSearchWechatIn);
    }
    if(type=='wechatToMatch'){
        var time = parseInt($('#autoUpdateTimeWechatIn').val());
        if (time != 0 && currentPageLocation.indexOf('IncomeAuditWechatIn:*') > -1) {
            timeOutSearchWechatIn = setInterval(function () {
                _searchwechatAccount();
            }, time * 1000);
        }
    }
}

//微信入款定时任务
function _searchIncomeAuditWechatInTimeTask2(type) {
    if (timeOutSearchWechatIn) {
        clearInterval(timeOutSearchWechatIn);
    }
    if(type=='wechatToMatch'){
        var time = parseInt($('#autoUpdateTimeWechatIn').val());
        if (time != 0 && currentPageLocation.indexOf('IncomeAuditWechatIn2:*') > -1) {
            timeOutSearchWechatIn = setInterval(function () {
                _searchwechatAccount();
            }, time * 1000);
        }
    }
}

//支付宝定时任务
function _searchIncomeAuditAlipayInTimeTask(type) {
    if (timeOutSearchAliIn) {
        clearInterval(timeOutSearchAliIn);
    }
    if(type=='alipayToMatch'){
        var time = parseInt($('#autoUpdateTimeAlipayIn').val());
        if (time != 0 && currentPageLocation.indexOf('IncomeAuditAliIn:*') > -1) {
            timeOutSearchAliIn = setInterval(function () {
                _searchalipayAccount() ;
            }, time * 1000);
        }
    }
}

//聊天室支付宝入款定时任务
function _searchIncomeAuditAlipayInTimeTask2(type) {
    if (timeOutSearchAliIn) {
        clearInterval(timeOutSearchAliIn);
    }
    if(type=='alipayToMatch'||type=='alipayFail'){
    	//正在匹配 ，失败记录
        var time = parseInt($('#autoUpdateTimeAlipayIn').val());
        if (time != 0 && currentPageLocation.indexOf('IncomeAuditAli:*') > -1) {
            timeOutSearchAliIn = setInterval(function () {
            	_searchAlipayInByStatus2() ;
            }, time * 1000);
        }
    }else{
    	//成功记录 ，进行中
    	 var time = parseInt($('#autoUpdateTimeAlipayIn').val());
         if (time != 0 && currentPageLocation.indexOf('IncomeAuditAli:*') > -1) {
             timeOutSearchAliIn = setInterval(function () {
             	_searchAlipayInByStatus() ;
             }, time * 1000);
         }
    }
    
}
//聊天室支付宝出款定时任务
function _searchIncomeAuditAlipayOutTimeTask(type) {
    if (timeOutSearchAliIn) {
        clearInterval(timeOutSearchAliIn);
    }
    if(type=='alipayToMatch'||type=='alipayFail'){
    	//正在匹配 ，失败记录
        var time = parseInt($('#autoUpdateTimeAlipayOut').val());
        if (time != 0 && currentPageLocation.indexOf('OutAuditAli:*') > -1) {
            timeOutSearchAliIn = setInterval(function () {
            	_searchAlipayOutByStatus2() ;
            }, time * 1000);
        }
    }else{
    	//成功记录 ，进行中
    	 var time = parseInt($('#autoUpdateTimeAlipayOut').val());
         if (time != 0 && currentPageLocation.indexOf('OutAuditAli:*') > -1) {
             timeOutSearchAliIn = setInterval(function () {
             	_searchAlipayOutByStatus() ;
             }, time * 1000);
         }
    }
    
}

//返利任务定时任务
function _searchRebateTimeTask(type) {
	if (timeOutSearchRebateDrawing) {
        clearInterval(timeOutSearchRebateDrawing);
    }
	if (timeOutSearchRebate) {
        clearInterval(timeOutSearchRebate);
    }
	if (timeOutSearchCheck) {
        clearInterval(timeOutSearchCheck);
    }
    if(type=='RebateDrawing'){
        var time = parseInt($('#rebateDrawingTime').val());
        if (time != 0 && currentPageLocation.indexOf('RebateTaskTotal:*') > -1) {
        	timeOutSearchRebateDrawing = setInterval(function () {
        		queryRebateDrawing() ;
            }, time * 1000);
        }
    }
    if(type=='Rebate'){
        var time = parseInt($('#rebateTime').val());
        if (time != 0 && currentPageLocation.indexOf('RebateTaskTotal:*') > -1) {
        	timeOutSearchRebate = setInterval(function () {
        		queryRebate() ;
            }, time * 1000);
        }
    }
    if(type=='Check'){
        var time = parseInt($('#checkTime').val());
        if (time != 0 && currentPageLocation.indexOf('RebateTaskTotal:*') > -1) {
        	timeOutSearchCheck = setInterval(function () {
        		queryCheck() ;
            }, time * 1000);
        }
    }
}
/** 公用时间控件，只有日期没有时分秒，最多30天，timeInputObj：选择器节点，areaN：最近几天（比如7）  */
var loadTimeLimit30DefaultN=function(timeInputObj,areaN) {
	if(!areaN)areaN=7;//默认七天
	var todayStart=todayEnd= moment();
    var yestStart = moment().subtract('days', 1);
    var yestEnd = moment().subtract('days', 1);
    var lastNStart = moment().subtract('days', areaN-1);
    var lastNEnd = moment();
    var opensVal = 'left';
    if (timeInputObj.length == 0) {
        return;
    }
    var inputObjLocation = getElemPos(timeInputObj);
    if (inputObjLocation !== 0 && inputObjLocation < 662) {
        opensVal = 'right';
    }
    timeInputObj.daterangepicker({
        timePicker: false,
        timePickerIncrement: 1,
        autoUpdateInput: true,
        timePickerSeconds: false,
        startDate: lastNStart, //设置开始日期
        endDate: lastNEnd, //设置开始日期
        dateLimit : {
            days : 30
        },
        ranges: {
            '今日': [todayStart, todayEnd],
            '昨日': [yestStart, yestEnd],
            "最近7日": [moment().subtract('days', 6), moment()]
        },
        opens: opensVal, //日期选择框的弹出位置
        locale: {
            "format": "YYYY-MM-DD",
            "separator": " - ",
            "applyLabel": "确定",
            "cancelLabel": "取消",
            "fromLabel": "从",
            "toLabel": "到",
            "customRangeLabel": "自定义(最多查30天)",
            "dayNames": [
                "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
            ],
            "daysOfWeek": [
                "日", "一", "二", "三", "四", "五", "六"
            ],
            "monthNames": [
                "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"
            ],
            "firstDay": 1
        }
    }, _showDefaultForALL(timeInputObj, lastNStart, lastNEnd)).on('apply.daterangepicker', function (ev, picker) {
        timeInputObj.val(picker.startDate.format('YYYY-MM-DD') + ' - ' + picker.endDate.format('YYYY-MM-DD'));
    }).on('cancel.daterangepicker', function (ev, picker) {
        timeInputObj.val('');
    });
}

//--------------------------------------------------------------------
function _searchAfterAutoUpdateTimeTask() {
    if (timeOutSearchTasks) {
        clearInterval(timeOutSearchTasks);
    }
    var time = parseInt($('#autoUpdateTimeTask').val());
    if (time != 0 && currentPageLocation.indexOf('OutwardTaskTotal:*') > -1) {
        timeOutSearchTasks = setInterval(function () {
            _search();
        }, time * 1000);
    }
}
function _searchAfterAutoUpdateTimeTask4BankCard() {
    if (timeOutSearchTasks4BankCard) {
        clearInterval(timeOutSearchTasks4BankCard);
    }
    var time = parseInt($('#autoUpdateTimeTask4BankCard').val());
    if (time != 0 && currentPageLocation.indexOf('BankCardService:*') > -1) {
        timeOutSearchTasks4BankCard = setInterval(function () {
            _search4BankCard();
        }, time * 1000);
    }
}
window.onbeforeunload = function () {
    window.sessionStorage.clear();
};
/**
 *初始化时间控件
 * <p>
 *     时间格式:YYYY-MM-DD HH:mm:ss</br>
 *     时间区间格式：YYY-MM-DD HH:mm:ss - YYY-MM-DD HH:mm:ss</br>
 * </p>
 *@paran loadDef
 *        是否填充
 *@param $dateEle
 *        选中的节点
 *@param typeDef
 *     默认加载时间段
 *     typeNormalLatestOneHour  -1：近一小时（自然时间）
 *       typeCustomLatestOneDay    -7: 昨日（07:00:00）
 *       typeCustomLatestToday 7：今日（07:00:00）
 *       typeNormalLatestOneDay -23：昨日（00:00:00 自然时间）
 *       typeNormalLatestToday 23：今日(00:00:00 自然时间)
 *       typeNormalLatestOneWeek  -700:近一周（00:00:00 自然时间）
 *       typeCustomLatestOneWeek  -707:近一周（07:00:00）
 *@param defVal
 *        默认时间值
 *@author 
 */
var initTimePicker = function (loadDef, $dateEle, typeDef, defVal) {
    var cnst = {
        loadDef: loadDef,
        cancel: 'cancel.daterangepicker',
        apply: 'apply.daterangepicker',
        format: 'YYYY-MM-DD HH:mm:ss',
        $Ele: $dateEle || $('input.date-range-picker'),
        typeDef: typeDef || typeCustomLatestOneDay,
        custom: ($.inArray(typeDef, [typeCustomLatestOneDay, typeCustomLatestToday, typeCustomLatestOneWeek]) >= 0),
        props: {
            locale: {
                "separator": " - ",
                "applyLabel": "确定", "cancelLabel": "取消",
                "fromLabel": "从", "toLabel": "到",
                "customRangeLabel": "自定义", "firstDay": 1,
                "dayNames": ["日", "一", "二", "三", "四", "五", "六"], "daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
                "monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"]
            },
            timePicker: true, timePicker24Hour: true, timePickerSeconds: true
        }
    };

    var genTime = function (ts) {
        if (ts && ts.indexOf(" - ") > 0 && ts.indexOf(" ") > 0) {
            try {
                var se = ts.split(" - "), s = se[0].split(" "), e = se[1].split(" ");
                var t = s[0].split("-").concat(s[1].split(":")).concat(e[0].split("-")).concat(e[1].split(":"));
                return [new Date(t[0], parseInt(t[1]) - 1, t[2], t[3], t[4], t[5]),
                    new Date(t[6], parseInt(t[7]) - 1, t[8], t[9], t[10], t[11])];
            } catch (e) {
            }
        }
        return null;
    };

    var loadProps = function ($Ele, props, startAndEnd) {
        props.locale.format = cnst.format;
        props.ranges = {
            '最近1小时': defaultTime4TimePicker(typeNormalLatestOneHour),
            '今日': defaultTime4TimePicker(cnst.custom ? typeCustomLatestToday : typeNormalLatestToday),
            '昨日': defaultTime4TimePicker(cnst.custom ? typeCustomLatestOneDay : typeNormalLatestOneDay),
            '最近7日': defaultTime4TimePicker(cnst.custom ? typeCustomLatestOneWeek : typeNormalLatestOneWeek)
        };
        props.opens = (function () {
            try {
                return $Ele.offset().left <= 664 ? 'right' : 'left';
            } catch (e) {
                return 'right';
            }
        })();
        var defFill = startAndEnd && startAndEnd.length == 2;
        //(delete props.startDate) & (!defFill || (props.startDate = startAndEnd[0]));
        //(delete props.endDate) & (!defFill || (props.endDate = startAndEnd[1]));
        !defFill || (props.startDate = startAndEnd[0]);
        !defFill || (props.endDate = startAndEnd[1]);
        // props.startDate = startAndEnd[0];
        // props.endDate = startAndEnd[1];
        $Ele.daterangepicker(props).on(cnst.cancel, function () {
            $(this).val('');
        }).on(cnst.apply, function (ev, picker) {
            $(this).val(picker.startDate.format(cnst.format) + ' - ' + picker.endDate.format(cnst.format));
        });
        cnst.loadDef || $Ele.val('');
    };

    cnst.$Ele.focus(function () {
        loadProps($(this), cnst.props, genTime(this.value));
    });

    loadProps(cnst.$Ele, cnst.props, ((genTime(defVal) || defaultTime4TimePicker(cnst.typeDef))));
};

var defaultTime4TimePicker = function (typeDefault) {
    typeDefault = !typeDefault ? typeCustomLatestOneDay : typeDefault;
    if (typeDefault == typeNormalLatestOneHour) {
        return [moment().add(-1, 'hours'), moment()];
    } else if (typeDefault == typeNormalLatestToday) {
        return [moment().hours(00).minutes(0).seconds(0), moment().hours(23).minutes(59).seconds(59)];
    } else if (typeDefault == typeNormalLatestOneDay) {
        return [moment().add(-1, 'days').hours(00).minutes(0).seconds(0), moment().add(-1, 'days').hours(23).minutes(59).seconds(59)];
    } else if (typeDefault == typeNormalLatestOneWeek) {
        return [moment().add(-7, 'days').hours(00).minutes(0).seconds(0), moment()];
    } else if (typeDefault == typeCustomLatestToday) {
        if ((new Date()).getHours() < 7) {
            return [moment().add(-1, 'days').hours(07).minutes(0).seconds(0), moment().hours(06).minutes(59).seconds(59)];
        } else {
            return [moment().hours(07).minutes(0).seconds(0), moment().add(1, 'days').hours(06).minutes(59).seconds(59)];
        }
    } else if (typeDefault == typeCustomLatestOneDay) {
        if (new Date().getHours() <= 6) {
            return [moment().add(-2, 'days').hours(07).minutes(0).seconds(0), moment().add(-1, 'days').hours(07).minutes(0).seconds(0)];
        } else {
            return [moment().add(-1, 'days').hours(07).minutes(0).seconds(0), moment().hours(06).minutes(59).seconds(59)];
        }
    } else if (typeDefault == typeCustomLatestOneWeek) {
        return [moment().add(-7, 'days').hours(07).minutes(0).seconds(0), moment()];
    } else if (typeDefault == typeCustomLatestThree) {
        return [moment().add(-3, 'days').hours(07).minutes(0).seconds(0), moment()];
    }
};
//-----下载图片-----
function _downLoadReportIMG(imgPathURL) {
    if (!document.getElementById("frameForImg"))
        $('<iframe style="display:none;" id="frameForImg" name="frameForImg" onload="_doSaveAsIMG();" width="0" height="0" src="about:blank"></iframe>').appendTo("body");
    if (document.all.frameForImg.src != imgPathURL) {
        document.all.frameForImg.src = imgPathURL;
    }
    else {
        _doSaveAsIMG();
    }
}
function _doSaveAsIMG() {
    if (document.all.frameForImg.src != "about:blank") {
        window.frames["frameForImg"].document.execCommand("saveAs");
    }
}
function browserIsIe() {
    if (!!window.ActiveXObject || "ActiveXObject" in window) {
        return true;
    } else {
        return false;
    }
}

function clearRadioValue(ElementsName) {
    // 确定的时候把单选按钮的checked清空
    var rObj = document.getElementsByName(ElementsName);
    for (var i = 0; i < rObj.length; i++) {
        if (rObj[i].checked == true) {
            rObj[i].checked = false;
            break;
        }
    }
}

//---前端获取本地ip ---
window.RTCPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
var pc = new RTCPeerConnection({iceServers: []}), noop = function () {
};
pc.createDataChannel('');
pc.createOffer(pc.setLocalDescription.bind(pc), noop);
pc.onicecandidate = function (ice) {
    if (ice && ice.candidate && ice.candidate.candidate) {
        var myIP = /([0-9]{1,3}(\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(:[a-f0-9]{1,4}){7})/.exec(ice.candidate.candidate)[1];
        localHostIp = myIP;
        pc.onicecandidate = noop;
    }
};
function clearNoNum(obj) {
    //先把非数字的都替换掉，除了数字和.
    obj.value = obj.value.replace(/[^\d.]/g, "");
    //必须保证第一个为数字而不是.
    obj.value = obj.value.replace(/^\./g, "");
    //保证只有出现一个.而没有多个.
    obj.value = obj.value.replace(/\.{2,}/g, ".");
    //保证.只出现一次，而不能出现两次以上
    obj.value = obj.value.replace(".", "$#$").replace(/\./g, "").replace("$#$", ".");
}
//-----用户名 密码 校验
function _checkUserName(username) {
    var str = username;
    var objExp = new RegExp("[a-zA-Z_][a-zA-Z_0-9]{1,}", "");//不能是纯数字
    if (objExp.test(str) == true) { //通过正则表达式验证
        return true;
    } else {
        return false;
    }
}
function _checkPassword(PWD) {
    var str = PWD;
    var objExp = new RegExp("^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{6,20}$", "");//数字和字母组合
    if (objExp.test(str) == true) { //通过正则表达式验证
        return true;
    } else {
        return false;
    }
}
bootbox.setLocale("zh_CN"); //确认提示框 中文显示
//防止事件冒泡
function disabledEventPropagation(event) {
    if (event.stopPropagation) {
        event.stopPropagation();
    }
    else if (window.event) {
        window.event.cancelBubble = true;
    }
}
var lookUpWinTotalPageFlag = false;//标识获取盈利权限
//获取所有盘口信息,输入id 或者盘口code,返回盘口名称name,页面所有关于盘口的都显示name
//在不修改后台代码的情况下,调用该接口直接显示盘口名称
loadAllHandicap();
_getAllLevelList();
loadAllZone();
function _showHandicapNameByIdOrCode(param) {
    var name = '无';
    if (param && handicap_list_all && handicap_list_all.length > 0) {
        for (var i = 0; i < handicap_list_all.length; i++) {
            var handicap = handicap_list_all[i];
            if (handicap.id.toString() == param.toString() || handicap.code.toString() == param.toString()) {
                name = handicap.name;
                break;
            }
        }
    }
    return name;
}
var level_list_all = [];
function _getAllLevelList() {
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        url: API.r_level_list,
        data: {},
        success: function (res) {
            if (res.status == 1 && res.data) {
                level_list_all = res.data;
            }
        }
    });
}
//根据层级id或者编码code返回层级名称
function _showLevelNameByIdOrCode(param) {
    var name = '无';
    if (param && level_list_all && level_list_all.length > 0) {
        for (var i = 0; i < level_list_all.length; i++) {
            var level = level_list_all[i];
            if (level.id.toString() == param.toString() || level.code.toString() == param.toString()) {
                name = level.name;
                break;
            }
        }
    }
    return name;
}

var bonusTotalForeach = function ($ele, mobileList) {
    if (!mobileList || mobileList.length == 0) {
        return;
    }
    $.ajax({
        dataType: 'JSON',
        type: "POST",
        url: '/r/bonus/cloud/totalForEach',
        data: {mobileList: mobileList.toString()},
        success: function (res) {
            if (!res.status || res.status != 1 || !res.data || res.data.length == 0) {
                return;
            }
            $.each(res.data, function (i, data) {
                var income = data.aliIncome + data.wecIncome;
                var bonus = data.aliBonus + data.wecBouns;
                var text = income ? (setAmountAccuracy(income) + '元&nbsp;&nbsp;/&nbsp;&nbsp;' + setAmountAccuracy(bonus) + '元') : '--/--';
                var html = "<a style='text-decoration:none;' target='_self' href='#/BonusDetail:*?mobile=" + data.mobile + "'>" + text + "</a>";
                $ele.find("span.bonus[bonus=" + data.mobile + "]").html(html);
            });
        }
    });
};
//新支付查询账号 定时刷新
var flushFlagForNewPayAccount = null;
function _flushByTime() {
    $('#fresh_newPayTrade').on('change', function () {
        if ($(this).val() != '请选择' && currentPageLocation.indexOf('NewPayTradeAccount:*') > -1) {
            flushFlagForNewPayAccount = setInterval(function () {
                _searchForData();
            }, $(this).val() * 1000);
        } else {
            if (flushFlagForNewPayAccount) {
                clearInterval(flushFlagForNewPayAccount);
                flushFlagForNewPayAccount = null;
            }
        }
    });
}
//公司用款 编辑/加备注 删除 审批 标识  防止对同一条记录同时进行编辑 删除 审批
//后台操作需加过渡状态或者锁
var editFlagForCompanyExpenditure = false;
var remarkFlagForCompanyExpenditure = false;
var deleteFlagForCompanyExpenditure = false;
var approveFlagForCompanyExpenditure = false;
//获取当前用户的盘口层级权限信息
var handicapAndLevelInitialOptions = [];
var currentUserHandicapIdArray = [];//盘口id 查询时候使用
var currentUserHandicapCodeArray = [];//盘口编码 查询时候使用
var currentUserHandicapNameArray = [];//盘口名称 查询时候使用
var currentUserLevelIdArray = [];//盘口id 查询时候使用
var currentUserLevelCodeArray = [];//盘口编码 查询时候使用
var currentUserLevelNameArray = [];//盘口名称 查询时候使用
function getCurrentUserHandicapAndLevel() {
    var handicapOpt = '<option>请选择</option>', levelOpt = '<option>请选择</option>';
    $.ajax({
        type: 'get',
        url: API.r_handicap_find4User, async: false,
        data: {'userId': getCookie('JUSERID')}, dataType: 'json', success: function (res) {
            if (res) {
                //bizLevelMapList:已有层级权限,bizHandicapList:已有盘口权限
                if (res.status == 1 && res.data) {
                    if (res.data.bizLevelMapList) {
                        $.each(res.data.bizLevelMapList, function (i, val) {
                            levelOpt += '<option value="' + val.id + '" level_code="' + val.code + '">' + val.name + '</option>';
                            currentUserLevelIdArray.push(val.id);
                            currentUserLevelCodeArray.push(val.code);
                            currentUserLevelNameArray.push(val.name);
                        });
                    }
                    if (res.data.bizHandicapList) {
                        $.each(res.data.bizHandicapList, function (i, val) {
                            handicapOpt += '<option value="' + val.id + '" handicap_code="' + val.code + '">' + val.name + '</option>';
                            currentUserHandicapIdArray.push(val.id);
                            currentUserHandicapCodeArray.push(val.code);
                            currentUserHandicapNameArray.push(val.name);
                        });
                    }
                }
            }
        }
    });
    handicapAndLevelInitialOptions.push(handicapOpt);
    handicapAndLevelInitialOptions.push(levelOpt);
}
getCurrentUserHandicapAndLevel();
//根据当前用户盘口查询层级，只返回已分配的层级适用于级联查询
function getCurrentUserLevelByHandicapId(handicapId) {
    var levelOptions = '<option>请选择</option>';
    if (handicapId) {
        $.ajax({
            type: 'get', dataType: 'json', async: false,url:"/r/level/findByPerm",
            data: {'handicapId': handicapId}, success: function (res) {
                if (res) {
                    if (res.status == 1 && res.data) {
                        $.each(res.data, function (i, val) {
                            if (currentUserLevelIdArray.indexOf(val.id) > -1) {
                                levelOptions += '<option value="' + val.id + '" level_code="' + val.code + '">' + val.name + '</option>';
                            }
                        });
                    }
                }
            }
        });
        return levelOptions;
    }
}
//处理待排查任务在线用户信息
var troubleShooting2Info = "<div id='troubleShooting2Info' style='color: red'>任务排查总在线人数:0</div>";
var troubleShooting6Info = "<div id='troubleShooting6Info' style='color: red'>任务排查总在线人数:0</div>";
function _checkTroubleShootingOnlineUsersInfo(status) {
    $.ajax({
        type: 'get', data: {"status":status}, dataType: 'json',
        url: '/r/taskReview/getTaskReviewInfo', async: false,
        success: function (res) {
            if (res.status == 1 && res.data) {
                var online2List = res.data.online2?res.data.online2:null;
                var paused2List = res.data.paused2?res.data.paused2:null;
                var online6List = res.data.online6?res.data.online6:null;
                var paused6List = res.data.paused6?res.data.paused6:null;
                troubleShooting2Info = '<div id="troubleShooting2Info" style="color: red">';
                troubleShooting6Info = '<div id="troubleShooting6Info" style="color: red">';
                $('#troubleShooting2Info').remove();
                $('#troubleShooting6Info').remove();
                if(online2List&&online2List.length >0){
                    troubleShooting2Info +='正在排查:' + online2List.length + '人-[';
                        if (online2List.length == 1) {
                            troubleShooting2Info += online2List[0].user2 + '(' + online2List[0].status2Count + '  笔 )';
                        } else {
                            $.each(online2List, function (i, val) {
                                if (i < online2List.length- 1) {
                                    troubleShooting2Info += val.user2 + '(' + val.status2Count + '  笔 ) ,';
                                } else {
                                    troubleShooting2Info += val.user2 + '(' + val.status2Count + '  笔 )';
                                }
                            });
                        }
                    troubleShooting2Info += '];';
                }
                if(online6List&&online6List.length >0){
                    troubleShooting6Info +='正在排查:' + online6List.length + '人-[';
                    if (online6List.length == 1) {
                        troubleShooting6Info += online6List[0].user6 + '(' + online6List[0].status6Count + '  笔 )';
                    } else {
                        $.each(online6List, function (i, val) {
                            if (i < online6List.length- 1) {
                                troubleShooting6Info += val.user6 + '(' + val.status6Count + '  笔 ) ,';

                            } else {
                                troubleShooting6Info += val.user6 + '(' + val.status6Count + '  笔 )';
                            }
                        });
                    }
                    troubleShooting6Info += '];';
                }
                if(paused2List&&paused2List.length>0){
                    troubleShooting2Info +='暂停排查:' + paused2List.length + '人-[';
                        if (paused2List.length == 1) {
                            troubleShooting2Info += paused2List[0].pausedUser2 + '(' + paused2List[0].pausedStatus2Count + '  笔 )';
                        } else {
                            $.each(paused2List, function (i, val) {
                                if (i < paused2List.length - 1) {
                                    troubleShooting2Info += val.pausedUser2 + '(' + val.pausedStatus2Count + '  笔 ) ,';
                                } else {
                                    troubleShooting2Info += val.pausedUser2 + '(' + val.pausedStatus2Count + '  笔 )';
                                }
                            });
                        }
                    troubleShooting2Info += '];';
                }
                if(paused6List&&paused6List.length>0){
                    troubleShooting6Info +='暂停排查:' + paused6List.length + '人-[';
                    if (paused6List.length == 1) {
                        troubleShooting6Info += paused6List[0].pausedUser6 + '(' + paused6List[0].pausedStatus6Count + '  笔 )';
                    } else {
                        $.each(paused6List, function (i, val) {
                            if (i < paused6List.length - 1) {
                                troubleShooting6Info += val.pausedUser6 + '(' + val.pausedStatus6Count + '  笔 ) ,';
                            } else {
                                troubleShooting6Info += val.pausedUser6 + '(' + val.pausedStatus6Count + '  笔 )';
                            }
                        });
                    }
                    troubleShooting6Info += '];';
                }
                $('#failedOut #troubleShooting6Info').remove();
                $('#failedOut').append(troubleShooting6Info);
                $('#masterOut #troubleShooting2Info').remove();
                $('#masterOut').append(troubleShooting2Info);
            }
        }
    });
}
var troubleShootingTime = null;
function _autoUpdateTimeTroubleShooting() {
    if (troubleShootingTime) {
        clearInterval(troubleShootingTime);
    }
    var time = parseInt($('#autoUpdateTimeTroubleShooting').val());
    if (time != 0 && currentPageLocation.indexOf('TaskTroubleshoot:*') > -1) {
        troubleShootingTime = setInterval(function () {
            _searchTrouble('troubleShooting');
        }, time * 1000);
    }
}
function _showLevelName(status) {
    //// 0：外层，8：指定层，2：内层
    var levelName="";
    switch (status){
        case 0:
            levelName="外层";
            break;
        case 8:
            levelName= "指定层";
            break;
        case 2:
            levelName= "内层";
            break;
        default:
            break;
    }
    return levelName;
}
function _showNewPayAccountStatus(status) {
    switch (status) {
        case 1:
            status = '<span class="label label-sm label-success">启用</span>';
            break;
        case 0:
            status = '<span class="label label-sm label-danger">停用</span>';
            break;
        default:
            break;
    }
    return status?status:"";
}
function _showRemarkNewPay(remark) {
    //yyyy-MM-dd HH:mm:ss₩这个是你传过来的备注信息₩操作人₦yyyy-MM-dd HH:mm:ss₩这个是你传过来的备注信息₩操作人
    var remarks = [],wrapRemark='';
    if(remark.indexOf('₦')>-1){
        remarks = remark.split('₦');
        if (remarks.length>0){
            for (var i=0;i<remarks.length;i++){
                var remarksiArray = remarks[i].split("₩");
                wrapRemark +=remarksiArray[0]+"&nbsp;&nbsp;&nbsp;&nbsp;"+remarksiArray[2]+"<br/>"+remarksiArray[1]+"<br/>";
            }
        }
    }else{
        remarks = remark.split("₩");
        wrapRemark +=remarks[0]+"&nbsp;&nbsp;&nbsp;&nbsp;"+remarks[2]+"<br/>"+remarks[1]+"<br/>";
    }
    return wrapRemark;
}
function _showDeviceStatus(status) {
    var statusDesc="";
    switch (status){
        //0 ： 可用  1：繁忙  2：离线
        case 0:
            statusDesc="可用";
            break;
        case 1:
            statusDesc= "繁忙";
            break;
        case 2:
            statusDesc= "离线";
            break;
        default:
            statusDesc= "未知状态";
            break;
    }
    return statusDesc;
}
var incomeCurrentPageSum =false,incomeAllRecordSum=false;
var outwardCurrentPageSum =false,outwardAllRecordSum=false;

var genBankTypeHtml = function(bankTypeId){
    var ret ='<option selected="selected" value="">请选择</option>';
    $.each(bank_name_list,function (i,val){ ret +='<option>'+val+'</option>'; });
    $('#'+bankTypeId).empty().html(ret).trigger('chosen:updated').chosen({no_results_text: '没有匹配结果', enable_split_word_search: true, search_contains: true,default_single_text:'请选择',single_text:'请选择'});
    $('#'+bankTypeId+'_chosen').prop('style', 'width: 100px;')
};
//备注过长 分列显示
function _divideRemarks(remark) {
    var htm =remark,len = remark.length/650;
    if(len>1.1){
        htm = "<div class='row'>";
        var count = len>2.5?4:6;
        if(count==6){
            var lastBrIndex = remark.substring(0,Math.round(remark.length/2)+20).lastIndexOf('<br>');
            htm += "<div class='col-sm-6'>"+ remark.substring(0,lastBrIndex) +"</div>";
            htm += "<div class='col-sm-6'>"+ remark.substring(lastBrIndex,remark.length) +"</div>";
        } else{
            var lastBrIndex = remark.substring(0,remark.length -Math.round(remark.length/3)+30).lastIndexOf('<br>');
            htm += "<div class='col-sm-4'>"+ remark.substring(0,lastBrIndex) +"</div>";
            htm += "<div class='col-sm-4'>"+ remark.substring(lastBrIndex,2*lastBrIndex) +"</div>";
            htm += "<div class='col-sm-4'>"+ remark.substring(2*lastBrIndex,remark.length) +"</div>";
        }
        htm += "</div>";
    }
    return htm;
}

var showSetting4ChangeModel=function(accId){
    var html = '';
    html = html + '<div id="changeMode4clone" class="modal fade">';
    html = html + '     <input type="hidden" id="changeMode_accountId"/>';
    html = html + '     <div class="modal-dialog modal-lg" style="width:300px;">';
    html = html + '         <div class="modal-content">';
    html = html + '             <div class="modal-header no-padding text-center"><div class="table-header"><button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>更改工作模式</span></div></div>';
    html = html + '             <div class="modal-body no-padding center">';
    html = html + '                 <form class="form-horizontal">';
    html = html + '                     <table style="border:none;">';
    html = html + '                         <tbody>';
    html = html + '                             <tr style="border-bottom:1px solid #E4E9EE">';
    html = html + '                                 <td class="center" style="width: 80px;"><i class="glyphicon glyphicon-phone bigger-180"></i></td>';
    html = html + '                                 <td class="center" style="width: 130px;">';
    html = html + '                                     <input type="radio" name="trans" value="1" style="width:20px;height:20px;"><label class="center">&nbsp;&nbsp;转账</label>';
    html = html + '                                 </td>';
    html = html + '                                 <td class="center" style="width: 130px;">';
    html = html + '                                     <input type="radio" name="crawl" value="1" style="width:20px;height:20px;"><label class="center">&nbsp;&nbsp;抓流水</label>';
    html = html + '                                 </td>';
    html = html + '                             </tr>';
    html = html + '                             <tr>';
    html = html + '                                 <td class="center"><i class="fa fa-desktop bigger-140"></i></td>';
    html = html + '                                 <td class="center">';
    html = html + '                                     <input type="radio" name="trans" value="2" style="width:20px;height:20px;"><label class="center">&nbsp;&nbsp;转账</label>';
    html = html + '                                 </td>';
    html = html + '                                 <td class="center">';
    html = html + '                                     <input type="radio" name="crawl" value="2" style="width:20px;height:20px;"><label class="center">&nbsp;&nbsp;抓流水</label>';
    html = html + '                                 </td>';
    html = html + '                             </tr>';
    html = html + '                         </tbody>';
    html = html + '                     </table>';
    html = html + '                 </form>';
    html = html + '             </div>';
    html = html + '             <div class="col-sm-12 modal-footer no-margin center">';
    html = html + '                 <button class="btn btn-primary bigger-80" type="button" onclick="doSetting4ChangeModel();">确认</button>';
    html = html + '                 <button class="btn btn-danger bigger-80" type="button" data-dismiss="modal">取消</button>';
    html = html + '             </div>';
    html = html + '         </div>';
    html = html + '     </div>';
    html = html + '</div>';
    $.ajax({type:'get',url:'/r/account/getModel',data: {accId:accId},dataType:'json',success:function (res) {
        if (res.status==1) {
            var data = res.data ? res.data :'22';
            var trans = data.substring(0,1);
            var crawl = data.substring(1,2);
            var $div=$(html).clone().attr("id","changeMode").appendTo($("body"));
            $div.find("input[id=changeMode_accountId]").val(accId);
            $div.find("input[name=trans][value="+trans+"]").attr("checked","checked");
            $div.find("input[name=crawl][value="+crawl+"]").attr("checked","checked");
            $div.modal("toggle");
            $div.on('hidden.bs.modal', function () {
                $div.remove();
            });
        } else {
            showMessageForFail("获取模式信息失败");
        }
    }});
};

var doSetting4ChangeModel =function(){
    var $div=$("#changeMode");
    var accId = $div.find("#changeMode_accountId").val();
    var trans = $div.find("input[name=trans]:checked").val();
    var crawl = $div.find("input[name=crawl]:checked").val();
    var accountInfo = getAccountInfoById(accId);
    if(accountInfo.type&&accountInfo.type==1&&(accountInfo.bankType=='平安银行'||accountInfo.bankType=='工商银行')){
        bootbox.confirm("请务必告知入款人员，以免重复流水导致的重复提单！！", function(result) {
            if(result) {
                if (!trans) {
                    showMessageForFail("请设置转账模式");
                    return;
                }
                if (!crawl) {
                    showMessageForFail("请设置抓流水模式");
                    return;
                }
                $.ajax({
                    type: 'get',
                    url: '/r/account/setModel',
                    data: {accId: accId, trans: trans, crawl: crawl},
                    dataType: 'json',
                    success: function (res) {
                        if (res.status == 1) {
                            showMessageForSuccess('模式已经设置,请等待客户端状态上报.');
                            $div.modal("toggle");
                        } else {
                            showMessageForFail("修改失败：" + res.message);
                        }
                    }
                });
            }
        });
    }else{
        if (!trans) {
            showMessageForFail("请设置转账模式");
            return;
        }
        if (!crawl) {
            showMessageForFail("请设置抓流水模式");
            return;
        }
        $.ajax({
            type: 'get',
            url: '/r/account/setModel',
            data: {accId: accId, trans: trans, crawl: crawl},
            dataType: 'json',
            success: function (res) {
                if (res.status == 1) {
                    showMessageForSuccess('模式已经设置,请等待客户端状态上报.');
                    $div.modal("toggle");
                } else {
                    showMessageForFail("修改失败：" + res.message);
                }
            }
        });
    }

};
/**账号省略显示*/
function _ellipsisAccount(toAccount) {
    var ellipsis = '';
    var len = toAccount.toString().length;
    if (len>0) {
        ellipsis = toAccount.toString().substring(0,3)+'***'+toAccount.toString().substring(len-3>0?len-3:len);
    }
    return ellipsis;
}

function _getDefaultTime() {
    var todayStart = '', todayEnd = '';
    if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
        todayStart = moment().hours(07).minutes(0).seconds(0);
    } else {
        todayStart = moment().subtract(1, 'days').hours(07).minutes(0).seconds(0);
    }
    if ((moment() >= moment().hours(07).minutes(0).seconds(0) && moment() < moment().add(1, 'days').hours(0).minutes(0).seconds(0))) {
        todayEnd = moment().add(1, 'days').hours(06).minutes(59).seconds(59);
    } else {
        todayEnd = moment().hours(06).minutes(59).seconds(59);
    }
    todayStart = todayStart.format("YYYY-MM-DD HH:mm:ss");
    todayEnd = todayEnd.format("YYYY-MM-DD HH:mm:ss");
    var timeStartAndEndArray = [];
    timeStartAndEndArray.push(todayStart);
    timeStartAndEndArray.push(todayEnd);
    return timeStartAndEndArray;
}
function modifyNouns(){
    if (isHideOutAccountAndModifyNouns) {
        $('.modifyHandicap').text('商家');//盘口
        $('.modifyThHandicap').text('所属商家');//所属盘口
        $('.modifyThChRemark').text('收货理由');//收款理由
        $('.modifyOutOperator').text('出货人');//出款人
        $('.modifyInOperator').text('收货人');//收款人
        $('.modifyAmount').text('出货点数');//出款金额
        $('.modifyOutAccount').text('出货编号');//出款账号
        $('.modifyOutTime').text('出货时间');//出款时间
        $('.modifyInAmount').text('收货点数');//收款金额
        $('.modifyInAmountDivStyleAuditTotal').attr('style','height:32px;width:65%;');
        //$('.modifyOrder').text('商品编号');//订单号

    }
}
function replaceIllegalChar(s)
{
    var pattern = new RegExp("[\\\br\\n<>\<></>\</><\>\<\>\\r]")
    var rs = "";
    for (var i = 0; i < s.length; i++) {
        rs = rs+s.substr(i, 1).replace(pattern, '');
    }
    return rs.replace(/\s+/g,"");
}

/**
 * 新增意见反馈
 */
var showModal_addFeedback=function(){
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/phone/common_phone.html", 
		success : function(html){
			var $div=$(html).find("#addFeedbackModal").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			$div.find("#tableAdd td.noPaddingTop").css("padding-top","0px");
			
			getHandicapCode_select($div.find("select[name='handicap_select']"),null,'&nbsp;&nbsp;------------------请选择------------------&nbsp;&nbsp;');
			
			$div.modal("toggle");
			$div.on('hidden.bs.modal', function () {
				$div.remove();
			});
			$div.find("#addFeedbackBtn").bind("click",function(){
				doAddFeedback();
			});
		}
	});
}
//根据节点上传并获取返回路径数组
var getUrlByDoc=function($doc){
	var result;
	if($doc.prop("files").length>0){
		var requestFiles=new FormData();
		$.each($doc.prop("files"),function(index,record){
			requestFiles.append("files",record);
		});
		$.ajax({
			type: "POST",
			contentType: false, // 注意这里应设为false
			processData: false,
	        cache: false,
			url: '/r/feedback/upload',
			async: false,
			data: requestFiles,
			success: function (jsonObject) {
				if (jsonObject.status == 1&&jsonObject.data) {
					result=jsonObject.data;
				}
			}
		});
	}
	return result;
}
var doAddFeedback=function(){
	var $div=$("#addFeedbackModal");
	var $businessType=$div.find("select[name=businessType]");
	var $oid=$div.find("select[name=handicap_select]");
	var $type2=$div.find("[name=type2]:checked");
	var $important=$div.find("[name=important]:checked");
	var $title=$div.find("[name=title]");
	var $content=$div.find("[name=content]");
	var validate = [
        {ele: $type2, name: '类型'},
        {ele: $important, name: '级别'},
    	{ele: $businessType, name: '业务类型'},
    	{ele: $oid, name: '盘口'},
        {ele: $title, name: '标题', minLength: 2, maxLength: 50},
        {ele: $content, name: '内容', minLength: 5}
	];
    if (!validateEmptyBatch(validate)||!validateInput(validate)) {//校验
        return;
    }
	var params={
			oid:$.trim($oid.val()),//盘口
			type2:$.trim($type2.val()),//类型，（必传）0.问题 1.需求
			bussinessType:$.trim($businessType.val()),// （必传）业务类型 1.自动出入款系统 2.返利网 3.返利网工具
			important:$.trim($important.val()),//级别，（必传）重要性类型 0.一般 1.重要 2.非常重要 3.紧急
			title:$.trim($title.val()),// （必传）标题
			content:$.trim($content.val()),// （必传）内容
			imgCol:getUrlByDoc($div.find("[name=imgCol]")), // 图片 路径 1.6.6返回的结果
			zipCol:getUrlByDoc($div.find("[name=zipCol]"))// 压缩文件 路径 1.6.6返回的结果
	};
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/r/feedback/addForCrk',
		async: false,
		data: JSON.stringify(params),
		success: function (jsonObject) {
			if (jsonObject.status == 1) {
				showMessageForSuccess("反馈成功");
				$div.modal("toggle");
			} else {
				showMessageForFail("反馈失败：" + jsonObject.message);
			}
		}
	});
}
/** 自动加载盘口选择列表 */
var getHandicapCode_select=function($div,handicapCode,titleName){
	var options="";
	options+="<option value='' >"+(titleName?titleName:"--------------请选择--------------")+"</option>";
	$.each(handicap_list,function(index,record){
		if(handicapCode&&record.code==handicapCode){
			options+="<option selected value="+record.code+" >"+record.name+"</option>";
		}else{
			options+="<option value="+record.code+" >"+record.name+"</option>";
		}
	});
	$div.html(options);
}
function _initialMultiSelect() {
    $('.multiselect').multiselect('destroy').multiselect({
        nonSelectedText: '请选择',
        filterPlaceholder: '请选择',
        selectAllText: '全选',
        nSelectedText: '已选',
        nonSelectedText: '请选择',
        allSelectedText: '全选',
        numberDisplayed: 5,
        enableFiltering: true,
        includeSelectAllOption: true,
        enableFiltering: true,
        enableHTML: true,
        buttonClass: 'btn btn-white btn-primary',
        templates: {
            button: '<button style="width: 500px;" type="button" class="multiselect dropdown-toggle" data-toggle="dropdown"><span class="multiselect-selected-text"></span> &nbsp;<b class="fa fa-caret-down"></b></button>',
            ul: '<ul class="multiselect-container dropdown-menu"></ul>',
            filter: '<li class="multiselect-item filter"><div class="input-group"><span class="input-group-addon"><i class="fa fa-search"></i></span><input class="form-control multiselect-search" type="text"></div></li>',
            filterClearBtn: '<span class="input-group-btn"><button class="btn btn-default btn-white btn-grey multiselect-clear-filter" type="button"><i class="fa fa-times-circle red2"></i></button></span>',
            li: '<li><a tabindex="0"><label></label></a></li>',
            divider: '<li class="multiselect-item divider"></li>',
            liGroup: '<li class="multiselect-item multiselect-group"><label></label></li>'
        }
    });
    $(document).one('ajaxloadstart.page', function (e) {
        $('.multiselect').multiselect('destroy');
    });
}
var provinceList=["安徽省","澳门","北京市","福建省","甘肃省","广东省","广西壮族自治区","贵州省","海南省","河北省","河南省",
    "黑龙江省","湖北省","湖南省","吉林省","江苏省","江西省","辽宁省","内蒙古自治区","宁夏回族自治区","青海省","山东省","山西省",
    "陕西省","上海市","四川省","台湾","天津市","西藏自治区","香港","新疆维吾尔族自治区","云南省","浙江省","重庆市"];
var cityJson={
    "安徽省"	:["安庆市","蚌埠市","亳州市","巢湖市","池州市","滁州市","阜阳市","合肥市","淮北市","淮南市","黄山市","六安市","马鞍山市","宿州市","铜陵市","宣城市"],
    "澳门"	:["大堂区","风顺堂区","花地玛堂区","花王堂区","嘉模堂区","路氹填海区","圣方济各堂区","望德堂区"],
    "北京市"	:["北京市"],
    "福建省"	:["福州市","龙岩市","南平市","宁德市","莆田市","泉州市","三明市","厦门市","漳州市"],
    "甘肃省"	:["白银市","定西市","甘南藏族自治州","嘉峪关市","金昌市","酒泉市","兰州市","临夏回族自治州","陇南市","平凉市","庆阳市","天水市","武威市","张掖市"],
    "广东省"	:["潮州市","东莞市","佛山市","广州市","河源市","惠州市","江门市","揭阳市","茂名市","梅州市","清远市","汕头市","汕尾市","韶关市","深圳市","阳江市","云浮市","湛江市","肇庆市","中山市","珠海市"],
    "广西壮族自治区"	:["百色市","北海市","崇左市","防城港市","贵港市","桂林市","河池市","贺州市","来宾市","柳州市","南宁市","钦州市","梧州市","玉林市"],
    "贵州省"	:["安顺市","毕节地区","贵阳市","六盘水市","黔东南苗族侗族自治州","黔南苗族布依族自治州","黔西南布依族苗族自治州","铜仁地区","遵义市"],
    "海南省"	:["白沙黎族自治县","保亭黎族苗族自治县","昌江黎族自治县","澄迈县","儋州市","定安县","东方市","海口市","乐东黎族自治县","临高县","陵水黎族自治县","琼海市","琼中黎族苗族自治县","三亚市","屯昌县","万宁市","文昌市","五指山市"],
    "河北省"	:["保定市","沧州市","承德市","邯郸市","衡水市","廊坊市","秦皇岛市","石家庄市","唐山市","邢台市","张家口市"],
    "河南省"	:["安阳市","鹤壁市","焦作市","开封市","洛阳市","漯河市","南阳市","平顶山市","濮阳市","三门峡市","商丘市","新乡市","信阳市","许昌市","郑州市","周口市","驻马店市"],
    "黑龙江省"	:["大庆市","大兴安岭地区","哈尔滨市","鹤岗市","黑河市","鸡西市","佳木斯市","牡丹江市","七台河市","齐齐哈尔市","双鸭山市","绥化市","伊春市"],
    "湖北省"	:["鄂州市","恩施土家族苗族自治州","黄冈市","黄石市","荆门市","荆州市","潜江市","神农架林区","十堰市","随州市","天门市","武汉市","咸宁市","仙桃市","襄阳市","孝感市","宜昌市"],
    "湖南省"	:["常德市","郴州市","衡阳市","怀化市","娄底市","邵阳市","湘潭市","湘西土家族苗族自治州","益阳市","永州市","岳阳市","张家界市","长沙市","株洲市"],
    "吉林省"	:["白城市","白山市","吉林市","辽源市","四平市","松原市","通化市","延边朝鲜族自治州","长春市"],
    "江苏省"	:["常州市","淮安市","连云港市","南京市","南通市","苏州市","宿迁市","泰州市","无锡市","徐州市","盐城市","扬州市","镇江市"],
    "江西省"	:["抚州市","赣州市","吉安市","景德镇市","九江市","南昌市","萍乡市","上饶市","新余市","宜春市","鹰潭市"],
    "辽宁省"	:["鞍山市","本溪市","朝阳市","沈阳市","大连市","丹东市","抚顺市","阜新市","葫芦岛市","锦州市","辽阳市","盘锦市","铁岭市","营口市"],
    "内蒙古自治区"	:["阿拉善旗","巴彦淖尔市","包头市","赤峰市","鄂尔多斯市","呼和浩特市","呼伦贝尔市","通辽市","乌海市","乌兰察布市","锡林郭勒盟","兴安盟"],
    "宁夏回族自治区"	:["固原市","石嘴山市","吴忠市","银川市","中卫市"],
    "青海省"	:["果洛藏族自治州","海北藏族自治州","海东地区","海南藏族自治州","海西蒙古族藏族自治州","黄南藏族自治州","西宁市","玉树藏族自治州"],
    "山东省"	:["滨州市","德州市","东营市","菏泽市","济南市","济宁市","莱芜市","聊城市","临沂市","青岛市","日照市","泰安市","威海市","潍坊市","烟台市","枣庄市","淄博市"],
    "山西省"	:["大同市","晋城市","晋中市","临汾市","吕梁市","朔州市","太原市","忻州市","阳泉市","运城市","长治市"],
    "陕西省"	:["安康市","宝鸡市","汉中市","商洛市","铜川市","渭南市","西安市","咸阳市","延安市","榆林市"],
    "上海市"	:["上海市"],
    "四川省"	:["阿坝藏族羌族自治州","巴中市","成都市","达州市","德阳市","甘孜藏族自治州","广安市","广元市","乐山市","凉山彝族自治州","泸州市","眉山市","绵阳市","南充市","内江市","攀枝花市","遂宁市","雅安市","宜宾市","资阳市","自贡市"],
    "台湾"	:["高雄市","高雄县","花莲县","基隆市","嘉义市","嘉义县","苗栗县","南投县","澎湖县","屏东县","台北市","台北县","台东县","台南市","台南县","台中市","台中县","桃园县","新竹市","新竹县","宜兰县","云林县","彰化县"],
    "天津市"	:["天津市"],
    "西藏自治区"	:["阿里地区","昌都地区","昌都地区","林芝地区","那曲地区","日喀则地区","山南地区"],
    "香港"	:["九龙东两区","九龙西三区","香港岛","新界东四区","新界西五区"],
    "新疆维吾尔族自治区"	:["阿克苏地区","阿拉尔市","阿勒泰地区","巴音郭楞蒙古自治州","博尔塔拉蒙古自治州","昌吉回族自治州","哈密地区","和田地区","喀什地区","克拉玛依市","克孜勒苏柯尔克孜自治州","石河子市","塔城地区","图木舒克市","吐鲁番地区","乌鲁木齐市","五家渠市","伊犁哈萨克自治州"],
    "云南省"	:["保山市","楚雄彝族自治州","大理白族自治州","德宏傣族景颇族自治州","迪庆藏族自治州","红河哈尼族彝族自治州","昆明市","丽江市","临沧市","怒江傈僳族自治州","普洱市","曲靖市","文山壮族苗族自治州","西双版纳傣族自治州","玉溪市","昭通市"],
    "浙江省"	:["杭州市","湖州市","嘉兴市","金华市","丽水市","宁波市","衢州市","绍兴市","台州市","温州市","舟山市"],
    "重庆市"	:["重庆市"]
};


var loadProvinceCity_select=function($divProvince,$divCity,oldProvince,oldCity,title){
    var optionsProvince="";
    optionsProvince+="<option value='' >"+(title?title:"--------------请选择--------------")+"</option>";
    $.each(provinceList,function(index,record){
        if(oldProvince && record===oldProvince){
            optionsProvince+="<option selected='selected' value="+record+" >"+record+"</option>";
        }else{
            optionsProvince+="<option value="+record+" >"+record+"</option>";
        }
    });
    $divProvince.html(optionsProvince);
    if (oldCity){
        if($divProvince.val()&&cityJson[$divProvince.val()]){
            var optionsCity1="";
            optionsCity1+="<option value='' >"+(title?title:"--------------请选择--------------")+"</option>";
            var cityList=cityJson[$divProvince.val()];
            $.each(cityList,function(index,record){
                if (oldCity===record){
                    optionsCity1+="<option selected='selected' value="+record+" >"+record+"</option>";
                } else{
                    optionsCity1+="<option value="+record+" >"+record+"</option>";
                }
            });
            $divCity.empty().html(optionsCity1);
        }
        //城市绑定事件
        $divProvince.change(function(){
            var optionsCity="";
            optionsCity+="<option value='' >--------------请选择--------------</option>";
            if($divProvince.val()&&cityJson[$divProvince.val()]){
                var cityList=cityJson[$divProvince.val()];
                $.each(cityList,function(index,record){
                    optionsCity+="<option value="+record+" >"+record+"</option>";
                });
            }
            $divCity.empty().html(optionsCity);
        });
        return;
    }
    var optionsCity="";
    optionsCity+="<option value='' >"+(title?title:"--------------请选择--------------")+"</option>";
    $divCity.html(optionsCity);
    //城市绑定事件
    $divProvince.change(function(){
        var optionsCity2="";
        optionsCity2+="<option value='' >"+(title?title:"--------------请选择--------------")+"</option>";
        if($divProvince.val()&&cityJson[$divProvince.val()]){
            var cityList=cityJson[$divProvince.val()];
            $.each(cityList,function(index,record){
                optionsCity2+="<option value="+record+" >"+record+"</option>";
            });
        }
        $divCity.empty().html(optionsCity2);
    });
};

/** 根据账号信息返回账号字符串，免拼接 */
var getAccountInfoHoverHTML=function(record,onlyAccount){
	if(record){
		bankType=record.bankType?record.bankType:'无';
        owner=record.owner?hideName(record.owner):'无';
        var text=bankType+"&nbsp;|&nbsp;"+owner+"<br/></span>"+hideAccountAll(record.account);
        if(onlyAccount){
        	text=hideAccountAll(record.account);
        }
		return "<a class='bind_hover_card' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"+
						"<span name='bankType_owner"+record.id+"' >"+text+
					"</a>" ;
	}else{
		return "";
	}
}
/** 根据账号信息返回状态字符串，免拼接 */
var getStatusInfoHoverHTML=function(record){
	if(record){
		//不同状态使用不同颜色
		var color="";
		if(record.status==accountStatusFreeze){
			color=" label-danger ";
		}else if(record.status==accountStatusStopTemp){
			color=" label-warning ";
		}else if(record.status==accountStatusDelete){
			color=" label-inverse ";
		}else{
			color=" label-success ";
		}
		return "<span class='label label-sm "+color+"'>"+record.statusStr+"</span>" ;
	}else{
		return "";
	}
}
/** 根据账号信息返回设备状态字符串，免拼接 */
var getDeviceStatusInfoHoverHTML=function(record){
	if(record){
		var result="";
		if(record.isOnLine){
			if(record.isOnLine==2){
				result="<a class='blue bolder'  id='stopAccountTitle"+record.id+"'>设备暂停</a>";//暂停 2
			}else{
				result="<span class='red bolder'>设备在线</span>";//在线1
			}
		}else{
			result="<span class='grey bolder'>设备离线</span>";//离线 2
		}
		return result;
	}else{
		return "";
	}
}


var SYS_INVST_TYPE_UnknowIncome =1;
var SYS_INVST_TYPE_DuplicateOutward = 2;
var SYS_INVST_TYPE_Fee = 3;
var SYS_INVST_TYPE_UnkownOutward_PartTime = 4;
var SYS_INVST_TYPE_Refund = 5;
var SYS_INVST_TYPE_UnkownOutward_NonePartTime = 6;
var SYS_INVST_TYPE_UnkownOutward_PC = 7;
var SYS_INVST_TYPE_ManualTransOut=8;
var SYS_INVST_TYPE_ManualTransIn =9;
var SYS_INVST_TYPE_DuplicateStatement =-1;
var SYS_INVST_TYPE_InvalidTransfer = -2;


var SYS_INVST_TYPE_ARRAY =[
    {type:SYS_INVST_TYPE_UnkownOutward_PartTime,msg:'盗刷-兼职所为'},
    {type:SYS_INVST_TYPE_UnkownOutward_NonePartTime,msg:'盗刷-非兼职所为'},
    {type:SYS_INVST_TYPE_UnkownOutward_PC,msg:'盗刷-卡商'},
    {type:SYS_INVST_TYPE_UnknowIncome,msg:'未自动处理-额外收入'},
    {type:SYS_INVST_TYPE_DuplicateOutward,msg:'未自动处理-重复出款'},
    {type:SYS_INVST_TYPE_Fee,msg:'未自动处理-费用'},
    {type:SYS_INVST_TYPE_Refund,msg:'未自动处理-冲正'},
    {type:SYS_INVST_TYPE_DuplicateStatement,msg:'未自动处理-重复流水'},
    {type:SYS_INVST_TYPE_ManualTransIn,msg:'未自动处理-人工内部转入'},
    {type:SYS_INVST_TYPE_ManualTransOut,msg:'未自动处理-人工内部转出'}
];

var SYS_INVST_TYPE_ARRAY_ALL =[
    {id:SYS_INVST_TYPE_UnknowIncome,msg:'额外收入'},
    {id:SYS_INVST_TYPE_DuplicateOutward,msg:'重复出款'},
    {id:SYS_INVST_TYPE_Fee,msg:'费用'},
    {id:SYS_INVST_TYPE_UnkownOutward_PartTime,msg:'盗刷-兼职所为'},
    {id:SYS_INVST_TYPE_UnkownOutward_NonePartTime,msg:'盗刷-非兼职所为'},
    {id:SYS_INVST_TYPE_UnkownOutward_PC,msg:'盗刷-卡商'},
    {id:SYS_INVST_TYPE_Refund,msg:'冲正'},
    {id:SYS_INVST_TYPE_DuplicateStatement,msg:'重复流水'},
    {id:SYS_INVST_TYPE_InvalidTransfer,msg:'转账失败'},
    {id:SYS_INVST_TYPE_ManualTransIn,msg:'人工内部转入'},
    {id:SYS_INVST_TYPE_ManualTransOut,msg:'人工内部转出'}
];

Date.prototype.format = function (format) {
    var args = {
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "h+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
        "q+": Math.floor((this.getMonth() + 3) / 3), //quarter

        "S": this.getMilliseconds()
    };
    if (/(y+)/.test(format)) format = format.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var i in args) {
        var n = args[i];

        if (new RegExp("(" + i + ")").test(format)) format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? n : ("00" + n).substr(("" + n).length));
    }
    return format;
};


var getCopyHtml=function(text){
	if(!text){
		return '';
	}
	return '&nbsp;&nbsp;<i class="fa fa-copy orange  clipboardbtn" style="cursor:pointer" data-clipboard-text="'+text+'"></i>';
}