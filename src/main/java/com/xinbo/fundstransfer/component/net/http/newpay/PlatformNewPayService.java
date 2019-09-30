package com.xinbo.fundstransfer.component.net.http.newpay;

import com.xinbo.fundstransfer.ali4enterprise.outputdto.PageOutPutDTO;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.pojo.FindContentOutputDTO;
import com.xinbo.fundstransfer.domain.pojo.FindForOfb4DemandOutputDTO;
import com.xinbo.fundstransfer.domain.pojo.FindForOfbOutputDTO;
import com.xinbo.fundstransfer.domain.pojo.FindForOidOutputDTO;
import com.xinbo.fundstransfer.newpay.outdto.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.*;
import rx.Observable;

import java.util.List;

/**
 * Created by Administrator on 2018/7/11. 平台新支付接口
 */
public interface PlatformNewPayService {
	// 1.1.1 新增资料
	@POST("/ownerNewpayConfig/add")
	Observable<ResponseDataNewPay<AddNewPayOutputDTO>> add(@Body RequestBody body);

	// 1.1.2 修改基本信息
	@POST("/ownerNewpayConfig/modifyInfo")
	Observable<ResponseDataNewPay<ModifyInfoOutputDTO>> modifyInfo(@Body RequestBody body);

	// 1.1.3 修改账号资料
	@POST("/ownerNewpayConfig/modifyAccount")
	Observable<ResponseDataNewPay<ModifyAccountOutputDTO>> modifyAccount(@Body RequestBody body);

	// 1.1.4 修改状态
	@POST("/ownerNewpayConfig/modifyStatus")
	Observable<ResponseDataNewPay<ModifyStatusOutputDTO>> modifyStatus(@Body RequestBody body);

	// 1.1.5 修改密码
	@POST("/ownerNewpayConfig/modifyPwd")
	Observable<ResponseDataNewPay> modifyPwd(@Body RequestBody body);

	// 1.1.6 删除
	@POST("/ownerNewpayConfig/remove")
	Observable<ResponseDataNewPay> remove(@Body RequestBody body);

	// 1.1.7 条件查询分页列表
	@POST("/ownerNewpayConfig/findByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindByConditionOutputDTO>>> findByCondition(@Body RequestBody body);

	// 1.1.8 查询微信、支付宝绑定的银行卡
	@POST("/ownerNewpayConfig/findBank")
	Observable<ResponseDataNewPay<FindBankOutputDTO>> findBank(@Body RequestBody body);

	// 1.1.9 查询兼职人员微信、支付宝、银行卡的余额、转入转出金额
	@POST("/ownerNewpayConfig/findBalanceInfo")
	Observable<ResponseDataNewPay<List<FindBalanceInfoOutputDTO>>> findBalanceInfo(@Body RequestBody body);

	// 1.1.10 查询账号信息，account带*
	@POST("/ownerNewpayConfig/findAccountInfo")
	Observable<ResponseDataNewPay<FindAccountInfoOutputDTO>> findAccountInfo(@Body RequestBody body);

	// 1.1.11 查询账号信息，account没有带*
	@POST("/ownerNewpayConfig/findAccountInfo2")
	Observable<ResponseDataNewPay<FindAccountInfoOutputDTO>> findAccountInfo2(@Body RequestBody body);

	// 1.1.12 查询手机信息
	@POST("/ownerNewpayConfig/findTelInfo")
	Observable<ResponseDataNewPay<FindTelInfoOutputDTO>> findTelInfo(@Body RequestBody body);

	// 1.1.13 点击今日收款/佣金列
	@POST("/ownerNewpayLog/find8ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find8ByConditionOutputDTO>>> find8ByCondition(@Body RequestBody body);

	// 1.1.14 点击今日收款/佣金列，返佣记录
	@POST("/ownerNewpayCommissionDetail/findByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindCommissionDetailOutputDTO>>> findCommissionDetailByCondition(
			@Body RequestBody body);

	// 1.1.15 查询二维码分页列表
	@POST("/ownerNewpayUrl/findByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindQRByConditionOutputDTO>>> findQRByCondition(@Body RequestBody body);

	// 1.1.16 批量生成二维码地址
	@POST("/ownerNewpayUrl/add")
	Observable<ResponseDataNewPay> batchAddQR(@Body RequestBody body);

	// 1.1.17 批量删除二维码
	@POST("/ownerNewpayUrl/removeAnymore")
	Observable<ResponseDataNewPay> batchDeleteQR(@Body RequestBody body);

	// 1.1.18 查询银行下拉列表
	@GET("/bank/findAll")
	Observable<ResponseDataNewPay<List<FindBankAllOutputDTO>>> findBankAll();

	// 1.1.19 查询密码是否已被设置
	@POST("/ownerNewpayConfig/findPwdExists")
	Observable<ResponseDataNewPay<FindPwdExistsOutputDTO>> findPwdExists(@Body RequestBody body);

	// 1.1.20 修改收款理由前缀后缀
	@POST("/ownerNewpayConfig/modifyFix")
	Observable<ResponseDataNewPay<ModifyFixOutputDTO>> modifyFix(@Body RequestBody body);

	// 1.1.21 查询业主未删除的新支付通道
	@POST("/crk/findPOCForCrk")
	Observable<ResponseDataNewPay<List<FindPOCForCrkOutputDTO>>> findPOCForCrk(@Body RequestBody body);

	// 1.1.22 绑定支付通道和客户资料
	@POST("/newpayAisleConfig/bind")
	Observable<ResponseDataNewPay> newpayAisleConfigBind(@Body RequestBody body);

	// 1.1.23 查询客户资料已绑定的支付通道
	@POST("/newpayAisleConfig/findBind")
	Observable<ResponseDataNewPay> newpayAisleConfigFindBind(@Body RequestBody body);

	// 1.1.24 银行的余额同步
	@POST("/ownerNewpayConfig/syncBankBalance")
	Observable<ResponseDataNewPay> syncBankBalance(@Body RequestBody body);

	// 1.1.25 形容词名词 - 新增
	@POST("/ownerNewpayWord/add")
	Observable<ResponseDataNewPay<ContentOutputDTO>> contentAdd(@Body RequestBody body);

	// 1.1.26 形容词名词 – 修改
	@POST("/ownerNewpayWord/modify")
	Observable<ResponseDataNewPay<ContentOutputDTO>> contentModify(@Body RequestBody body);

	// 1.1.27 形容词名词 – 启用、停用
	@POST("/ownerNewpayWord/enable")
	Observable<ResponseDataNewPay<ContentOutputDTO>> contentEnable(@Body RequestBody body);

	// 1.1.28 形容词名词 – 删除
	@POST("/ownerNewpayWord/remove")
	Observable<ResponseDataNewPay> contentRemove(@Body RequestBody body);

	// 1.1.29 形容词名词 – 分页查询
	@POST("/ownerNewpayWord/findByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<ContentOutputDTO>>> findContentByCondition(@Body RequestBody body);

	// 1.1.30 修改未确认出款金额开关
	@POST("/ownerNewpayConfig/modifyUoFlag")
	Observable<ResponseDataNewPay> modifyUoFlag(@Body RequestBody body);

	// 1.1.31 生成常用金额/非常用金额二维码
	@POST("/qrCode/genANMultQr")
	Observable<ResponseDataNewPay> genANMultQr(@Body RequestBody body);

	// 1.1.32 统计常用金额、非常用金额已生成二维码个数和总个数
	@POST("/ownerNewpayWord/statisticsMWR")
	Observable<ResponseDataNewPay<StatisticsMWROutputDTO>> statisticsMWR(@Body RequestBody body);

	// 1.1.33 形容词类型 – 新增
	@POST("/ownerNewpayWordType/add")
	Observable<ResponseDataNewPay<WordTypeOutputDTO>> addWordType(@Body RequestBody body);

	// 1.1.34 形容词类型 – 查询列表
	@POST("/ownerNewpayWordType/findAll")
	Observable<ResponseDataNewPay<List<WordTypeOutputDTO>>> findWordType(@Body RequestBody body);

	// 1.1.35 形容词类型 – 删除
	@POST("/ownerNewpayWordType/remove")
	Observable<ResponseDataNewPay> removeWordType(@Body RequestBody body);

	// 1.1.36 兼职绑定词语
	@POST("/mobile/bindingWordType")
	Observable<ResponseDataNewPay> bindingWordType(@Body RequestBody body);

	// 1.1.37 查询词库绑定分页列表
	@POST("/ownerNewpayConfig/findForBind")
	Observable<ResponseDataNewPay<PageOutputDTO<BindOutputDTO>>> findForBind(@Body RequestBody body);

	// 1.2.1 查询银行卡分页列表
	@POST("/ownerNewpayWithdraw/findByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindBankCardByConditionOutputDTO>>> findBankCardByCondition(
			@Body RequestBody body);

	// 1.2.2 询微信、支付宝分页列表
	@POST("/ownerNewpayLog/findByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindAWByConditionOutputDTO>>> findAWByCondition(@Body RequestBody body);

	// 1.2.3 银行卡转入记录点击数字
	@POST("/ownerNewpayWithdraw/find2ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find2ByConditionOutputDTO>>> find2ByCondition(@Body RequestBody body);

	// 1.2.4 微信、支付宝转入记录点击数字
	@POST("/ownerNewpayLog/find2ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindAWIn2ByConditionOutputDTO>>> findAWIN2ByCondition(
			@Body RequestBody body);

	// 1.2.5 微信、支付宝、银行卡转出记录点击数字
	@POST("/ownerNewpayWithdraw/find3ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindAWB3OutByConditionOutputDTO>>> find3ByCondition(
			@Body RequestBody body);

	// 1.2.6 微信、支付宝流水
	@POST("/ownerNewpayLog/find3ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindAWLOG3ByConditionOutputDTO>>> findAWLog3ByCondition(
			@Body RequestBody body);

	// 1.2.7 银行卡流水
	@POST("/ownerNewpayLog/find9ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindBLog9ByConditionOutputDTO>>> find9ByCondition(
			@Body RequestBody body);

	// 1.2.8 对账
	@POST("/ownerNewpayConfig/verifyAccount")
	Observable<ResponseDataNewPay> verifyAccount(@Body RequestBody body);

	// 1.3.1 微信、支付宝正在匹配查询分页列表
	@POST("/ownerNewpayLog/find4ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find4ByConditionOutputDTO>>> find4ByCondition(@Body RequestBody body);

	// 1.3.2 微信、支付宝未匹配、已匹配查询分页列表
	@POST("/ownerNewpayLog/find5ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find5ByConditionOutputDTO>>> find5ByCondition(@Body RequestBody body);

	// 1.3.3 微信、支付宝未认领查询分页列表
	@POST("/ownerNewpayLog/find6ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find6ByConditionOutputDTO>>> find6ByCondition(@Body RequestBody body);

	// 1.3.4 微信、支付宝已取消查询分页列表
	@POST("/ownerNewpayLog/find7ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find7ByConditionOutputDTO>>> find7ByCondition(@Body RequestBody body);

	// 1.3.5 点击“待处理流水”列，第一个tab
	@POST("/ownerNewpayLog/find10ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find10ByConditionOutputDTO>>> find10ByCondition(@Body RequestBody body);

	// 1.3.6 第一个tab取消
	@POST("/userIn/cancel")
	Observable<ResponseDataNewPay> cancel(@Body RequestBody body);

	// 1.3.7 第一个tab新增备注
	@POST("/userIn/modifyRemark")
	Observable<ResponseDataNewPay> modifyRemark(@Body RequestBody body);

	// 1.3.8 点击“待处理流水”列，第二个tab
	@POST("/ownerNewpayLog/find11ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find11ByConditionOutputDTO>>> find11ByCondition(@Body RequestBody body);

	// 1.3.9 第二个tab新增备注
	@POST("/ownerNewpayLog/addRemark")
	Observable<ResponseDataNewPay> addRemark(@Body RequestBody body);

	// 1.3.10 第二个tab补提单
	@POST("/ownerNewpayLog/putPlus")
	Observable<ResponseDataNewPay> putPlus(@Body RequestBody body);

	// 1.3.11 第一个tab和第二个tab匹配
	@POST("/ownerNewpayLog/matching")
	Observable<ResponseDataNewPay> matching(@Body RequestBody body);

	// 1.3.12 统计指定device的正在匹配总数
	@POST("/ownerNewpayLog/statistics")
	Observable<ResponseDataNewPay<List<StatisticsOutputDTO>>> statistics(@Body RequestBody body);

	// 1.4.1 新支付下发记录
	@POST("/ownerNewpayWithdraw/find4ByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<Find4WByConditionOutputDTO>>> find4WByCondition(@Body RequestBody body);

	// 1.4.2 返佣规则 – 查询分页列表
	@POST("/ownerNewpayCommissionRule/findByCondition")
	Observable<ResponseDataNewPay<PageOutputDTO<FindCRByConditionOutputDTO>>> findCRByCondition(@Body RequestBody body);

	// 1.4.3 返佣规则 – 新增
	@POST("/ownerNewpayCommissionRule/add")
	Observable<ResponseDataNewPay<AddCROutputDTO>> addCR(@Body RequestBody body);

	// 1.4.4 返佣规则 – 修改
	@POST("/ownerNewpayCommissionRule/modify")
	Observable<ResponseDataNewPay<ModifyCROutputDTO>> modifyCR(@Body RequestBody body);

	// 1.4.5 返佣规则 – 删除
	@POST("/ownerNewpayCommissionRule/remove")
	Observable<ResponseDataNewPay> removeCR(@Body RequestBody body);

	// 1.6.1 出款确认
	@POST("/reqOutMoney/confirm")
	Observable<ResponseDataNewPay> confirm(@Body RequestBody body);

	// 1.6.2 重置信用额度
	@POST("/deviceCreditMoney/reset")
	Observable<ResponseDataNewPay> reset(@Body RequestBody body);

	// 1.6.3 自动重置信用额度
	@POST("/deviceCreditMoney/autoReset")
	Observable<ResponseDataNewPay> autoReset(@Body RequestBody body);

	// 1.6.4 银行卡-修改状态
	@POST("/payOwnerConfig/modifyBindCardStatus2")
	Observable<ResponseDataNewPay> modifyBindCardStatus2(@Body RequestBody body);

	// 1.6.5 新增反馈 自动出入款系统
	@POST("ownerFeedback/addForCrk")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay> addForCrk(@Body RequestBody body);

	// 1.6.5 新增反馈 返利网
	@POST("ownerFeedback/addForFlw")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay> addForFlw(@Body RequestBody body);

	// 1.6.5 新增反馈 工具_返利网
	@POST("ownerFeedback/addForFlwTool")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay> addForFlwTool(@Body RequestBody body);

	// 1.6.5 新增反馈 工具_PC端
	@POST("ownerFeedback/addForPCTool")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay> addForPCTool(@Body RequestBody body);

	// 1.6.6 上传文件
	@POST("file/upload")
	@Headers({ "FEEDBACK:RESTFUL" })
	@Multipart
	Observable<ResponseDataNewPay> upload(@Part() List<MultipartBody.Part> part);

	// 1.6.6-2 上传文件
	@POST("file/upload")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay> upload2(@Body MultipartBody body);

	// 1.1.1 查询反馈列表
	@POST("ownerFeedback/findForOid")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay<PageOutPutDTO<FindForOidOutputDTO>>> findForOid(@Body RequestBody body);

	// 1.1.2 业主反馈 查看反馈内容
	@POST("ownerFeedback/findContent")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay<PageOutPutDTO<FindContentOutputDTO>>> findContent(@Body RequestBody body);

	// 1.1.3 查询回复记录
	@POST("ownerFeedbackReply/findForOfb")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay<PageOutPutDTO<FindForOfbOutputDTO>>> findForOfb(@Body RequestBody body);

	// 1.1.4 查询指定业主反馈的需求进度
	@POST("demand/findForOfb")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay<PageOutPutDTO<FindForOfb4DemandOutputDTO>>> findForOfb2(@Body RequestBody body);

	// 1.1.5 标记反馈信息已解决
	@POST("ownerFeedback/solve")
	@Headers({ "FEEDBACK:RESTFUL" })
	Observable<ResponseDataNewPay> solve(@Body RequestBody body);

	// 1.5.9 通道可用银行卡告警
	@POST("/bank/bankWarn")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<ResponseDataNewPay> bankWarn(@Body RequestBody body);

	// 1.5.10 修改银行卡卡号、删除银行卡、停用、冻结银行卡时调用平台接口
	@POST("/bank/bankModified")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<ResponseDataNewPay> bankModified(@Body RequestBody body);

}