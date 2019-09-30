var showModalDaiFuDetail=function(configId,handicapId,platStatus){
	$.ajax({ 
		type:"GET",
		async:false, 
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html", 
		success : function(html){
			var $div=$(html).find("#thirdDaiFuDetail_modal").clone().appendTo($("body"));
			$div.find("#handicapId").val(handicapId);
			$div.find("#configId").val(configId);
			$div.modal("toggle");
			initTimePicker(false,$div.find("[name=startAndEndTime]"));
			if(platStatus=="0"||platStatus=="1"||platStatus=="2"){
				$div.find("[name=platStatus]").val(platStatus);
			}
			searchDaiFuDetail(0);
			$(document).keypress(function(e){
				if(event.keyCode == 13) {
					$div.find("#searchBtn button").click();
				}
			});
			$div.find("[name=platStatus]").change(function(){
				searchDaiFuDetail();
			});
			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
		}
	});
}
var searchDaiFuDetail=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#thirdDaiFuDetail_Page .Current_Page").text();
	var $div=$("#thirdDaiFuDetail_modal");
	var $filter =$div.find("#filter");
	var param = {
			handicapId:$div.find("#handicapId").val(),
			outConfigId:$div.find("#configId").val(),
			platPayCode:$filter.find("[name='platPayCode']").val(),
			outwardRequestOrderNo:$filter.find("[name='outwardRequestOrderNo']").val(),
			platStatus:$filter.find("select[name='platStatus']").val(),
			exactMoneyBegin:$filter.find("[name='exactMoneyBegin']").val(),
			exactMoneyEnd:$filter.find("[name='exactMoneyEnd']").val(),
	        pageNo:CurPage<=0?0:CurPage-1,
	        pageSize:$.session.get('initPageSize')?$.session.get('initPageSize'):10
	    };
	var startAndEndTime=$div.find("[name=startAndEndTime]").val();
	if(startAndEndTime&&startAndEndTime.length>0){
		var startAndEndTimeToArray=getTimeArray($div.find("[name=startAndEndTime]").val());
		if(startAndEndTimeToArray&&startAndEndTimeToArray.length>0){
			param.createTimeBegin=new Date(startAndEndTimeToArray[0]).getTime();
		}
		if(startAndEndTimeToArray&&startAndEndTimeToArray.length>1){
			param.createTimeEnd=  new Date(startAndEndTimeToArray[1]).getTime();
		}
	}
    //发送请求
	$.ajax({
        dataType:'JSON', 
		contentType: 'application/json;charset=UTF-8',
        type:"POST", 
		async:false,
        url:'/daifuInfo/findByOutConfigId',
        data:JSON.stringify(param),
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			var $tbody=$div.find("tbody");
			var idList=new Array();
			var tr="";
			$.each(jsonObject.data,function(index,record){
				tr+="<tr>";
				tr+="<td>"+timeStamp2yyyyMMddHHmmss(record.createTime)+"</td>";
				tr+="<td>"+_checkObj(record.platPayCode)+"</td>";//代付订单号
				tr+="<td>"+_checkObj(record.outwardRequestOrderNo)+"</td>";//出款订单号
				tr+="<td>"+_checkObj(record.exactMoney)+"</td>";//金额
				if(record.platStatus==2){
					//取消2
					tr+="<td><span class='badge badge-inverse'>取消</span></td>";
				}else if(record.platStatus==1){
					//完成1
					tr+="<td><span class='badge badge-success'>完成</span></td>";
				}else {
					//0-未知 3-正在支付 都属于待处理
					tr+="<td><span class='badge badge-warning'>待处理</span></td>";
				}
				tr+="<td>"+_checkObj(record.operatorUid)+"</td>";//处理结果 
				tr+="<td>"+timeStamp2yyyyMMddHHmmss(record.uptime)+"</td>";//最后更新时间
				tr+="<td>"+getHTMLremark(record.errorMsg,120)+"</td>";
				tr+="<td>"+getHTMLremark(record.remark,120)+"</td>";
				tr+="</tr>";
			});
			$tbody.html(tr);
			showPading(jsonObject.page,"thirdDaiFuDetail_Page",searchDaiFuDetail,null,true);
        }
	});
}
/** 查询余额 */
var searchNewBalance=function(configId,handicapId){
	$.ajax({
		type: "GET",
		dataType: 'JSON',
		url: '/passage/getDaifuBalance',
		async: false,
		data: {
			"handicapId":handicapId,
			"outConfigId":configId
		},
		success: function (jsonObject) {
			if (jsonObject && jsonObject.status == 1) {
				showMessageForSuccess("最新余额："+jsonObject.data);
			} else {
				showMessageForFail("查询失败" + jsonObject.message);
			}
		}
	});
}

/** 同步全部服务商的银行类别 */
var sync_BankTypeAll=function(){
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/daifu/synBankType',
		async: false,	
		data:JSON.stringify({}),
		success: function (jsonObject) {
			if (jsonObject && jsonObject.status == 1) {
				showMessageForSuccess("成功同步平台全部服务商的银行类别");
			} else {
				showMessageForFail("同步平台全部服务商的银行类别失败" + jsonObject.message);
			}
		}
	});
}
/** 同步单个服务商的银行类别 */
var sync_BankTypeByChannelName=function(channelName){
	$.ajax({
		type: "POST",
		contentType: 'application/json;charset=UTF-8',
		dataType: 'JSON',
		url: '/daifu/synBankType',
		async: false,
		data:JSON.stringify({
            "provider" : channelName
          }),
		success: function (jsonObject) {
			if (jsonObject && jsonObject.status == 1) {
				showMessageForSuccess("成功同步平台服务商【"+channelName+"】的银行类别");
			} else {
				showMessageForFail("同步平台服务商【"+channelName+"】的银行类别失败" + jsonObject.message);
			}
		}
	});
}

/** 修改银行类别 */
var showUpdateBankTypeInfoModal=function(channelName){
	var html='<div id="UpdateBankTypeInfoModal" class="modal fade" aria-hidden="false" >\
						<div class="modal-dialog modal-lg"  style="width:800px;">\
							<div class="modal-content">\
								<div class="modal-header no-padding text-center">\
									<div class="table-header">\
										<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button>\
										修改“<span name="title"></span>”银行类别\
									</div>\
								</div>\
								<div class="modal-body">\
									<h6 class="header smaller center lighter blue no-padding no-margin-top">列表为该服务商在平台所支持的全部银行，勾选则为出入款支持的银行</h6>\
									<input type="hidden" name="channelName" />\
									<input type="hidden" name="bankTypeId" />\
									<div id="bankTypeZoneList" class="row"></div>\
								</div>\
								<div class="col-sm-12 modal-footer no-margin center">\
									<button type="button" class="btn btn-danger btn-white btn-bold btn-round" onclick="resetBankTypeInfo()">\
								        <i class="ace-icon fa fa-refresh icon-on-right"></i>&nbsp;重置\
								    </button>\
									<button type="button" class="btn btn-success btn-white btn-bold btn-round" onclick="doUpdateBankTypeInfoModal()">\
							        	<i class="ace-icon fa fa-check icon-on-right"></i>&nbsp;保存\
								    </button>\
								</div>\
							</div>\
						</div>\
					</div>';
	var $div=$(html).clone().appendTo($("body"));
	$div.modal("toggle");
	$div.find("[name=channelName]").val(channelName);
	$div.find("[name=title]").text(channelName);
	//重置参数
	resetBankTypeInfo();
	$div.on('hidden.bs.modal', function () {
		// 关闭窗口清除model
		$div.remove();
	});
}
var resetBankTypeInfo=function(){
	var $div=$("#UpdateBankTypeInfoModal");
	var channelName=$div.find("[name=channelName]").val();
	var bankTypeInfo=getBankTypeVOByProvider(channelName);
	if(!bankTypeInfo){
		showMessageForFail("无数据，请先同步");
		return false;
	}
	var allBankType=bankTypeInfo.allBankType,supportBankType=bankTypeInfo.supportBankType;
	if(!allBankType){
		showMessageForFail("服务商“"+channelName+"”在平台无可用银行类别");
		return false;
	}
	$div.find("[name=bankTypeId]").val(bankTypeInfo.id);
	var HTML="",closeBtn='';
	$.each(allBankType.split(","),function(i,bankType){
		if(bankType){
			var lable_color=' label-grey ';
			var checked='';
			if(supportBankType&&supportBankType.indexOf(bankType)!=-1){
				//检测出入款支持的银行 是否在平台也支持
				lable_color=' label-primary ';
				checked=' checked="checked" ';
			}
			HTML+='<div class="col-sm-4">'+
						'<span style="margin-bottom:10px;" class="label label-xlg '+lable_color+' ">'+
							'<label style="width:220px;text-align:left;" title="'+bankType+'">'+
								'<input type="checkbox" '+checked+' name="checkbox_bankTypeName" class="ace" value="'+bankType+'"/>'+
								'&nbsp;<span class="lbl">'+bankType+'</span>'+
							'</label>'+
						'</span>'+
					'</div>';
		}
	});
	$div.find("#bankTypeZoneList").html(HTML);
}
var doUpdateBankTypeInfoModal=function(){
	bootbox.confirm("<span class='red bolder'>请确认【第三方服务商支持的银行类别】是否已设置好。</span>确定保存？", function(result) {
		if (result) {
			var $div=$("#UpdateBankTypeInfoModal");
			var supportBankType=new Array();
			$.each($div.find("[name=checkbox_bankTypeName]:checked"),function(i,result){
				supportBankType.push($(result).val());
			});
			if(supportBankType&&supportBankType.length>0){
				$.ajax({
					type: "POST",
					contentType: 'application/json;charset=UTF-8',
					dataType: 'JSON',
					url: '/daifu/updateSurpport',
					async: false,
					data:JSON.stringify({
		            	"id":$div.find("[name=bankTypeId]").val(),
		            	"provider" : $div.find("[name=channelName]").val(),
		            	"supportBankType":supportBankType.toString()
	                }),
					success: function (jsonObject) {
						if (jsonObject && jsonObject.status == 1) {
							showMessageForSuccess("保存成功");
							$div.modal("toggle");
						} else {
							showMessageForFail("保存失败："+jsonObject.message);
						}
					}
				});
			}
		}
	});
}

/** 根据服务商读取银行类别信息 */
var  getBankTypeVOByProvider=function(provider){
	var bankTypeVO;
	if(provider){
		$.ajax({
			type: "POST",
			contentType: 'application/json;charset=UTF-8',
			dataType: 'JSON',
			url: '/daifu/surpportBankType',
			async: false,
			data:JSON.stringify({
				"provider":provider,
				"pageSize" : 10,
				"pageNo":0
	        }),
			success: function (jsonObject) {
				if (jsonObject && jsonObject.status == 1) {
					if(jsonObject.data&&jsonObject.data.length>0){
						bankTypeVO=jsonObject.data[0];
					}
				} else {
					showMessageForFail("查询失败："+jsonObject.message);
				}
			}
		});
	}
	return bankTypeVO;
}
/** 根据id读取第三方配置信息 */
var getDaiFuConfigById=function(configId){
	var result='';
	$.ajax({
        dataType:'JSON', 
        type:"POST", 
		async:false,
        url:'/passage/list',
        data:{
        	search_EQ_id:configId,
            pageNo:0,
            pageSize:10
        },
        success:function(jsonObject){
			if(jsonObject.status == 1){
				if(jsonObject.data&&jsonObject.data.length>0){
					result=jsonObject.data[0];
				}
			}else{
				showMessageForFail("获取第三方配置信息异常，"+jsonObject.message);
			}
		}
	});
	return result;
}

/** 第三方代付悬浮提示 */
var loadHover_thirdDaiFuHover=function(configIdList){
	$.each(configIdList,function(index,configId){
		$("[data-toggle='thirdDaiFuHover"+configId+"']").popover({
			html : true,
			title: function(){
				return title_thirdDaiFuHover(configId);
			},
			delay:{show:0, hide:100},
			content: function(){
				return content_thirdDaiFuHover(configId);
			}
		});
	});
}
var title_thirdDaiFuHover=function(configId){
    var title='第三方代付信息&nbsp;(id:'+configId+')&nbsp;&nbsp;&nbsp;';
	return '<center>'+title+'</center>';
}
var content_thirdDaiFuHover=function(configId){
	var html='<div  style="width:600px" >\
						<div class="col-sm-12">\
							<div class="col-xs-2 text-right"><strong>盘口</strong></div>\
							<div class="col-xs-4 no-padding-lr"><span name="handicapName"></span></div>\
						</div>\
						<div class="col-sm-12">\
							<div class="col-xs-2 text-right"><strong>层级</strong></div>\
							<div class="col-xs-10 no-padding-lr"><span name="levelNameList"></span></div>\
						</div>\
						<div class="col-sm-12">\
							<div class="col-xs-2 text-right"><strong>状态</strong></div>\
							<div class="col-xs-4 no-padding-lr"><span name="platformStatus"></span></div>\
							<div class="col-xs-2 text-right"><strong>商号</strong></div>\
							<div class="col-xs-4 no-padding-lr"><span name="memberId"></span></div>\
						</div>\
						<div class="col-sm-12">\
							<div class="col-xs-2 text-right"><strong>服务商</strong></div>\
							<div class="col-xs-4 no-padding-lr"><span name="channelName"></span></div>\
							<div class="col-xs-2 text-right"><strong>通道名称</strong></div>\
							<div class="col-xs-4 no-padding-lr"><span name="aliasName"></span></div>\
						</div>\
					</div>';
   var $div = $(html);
   var daiFuConfig=getDaiFuConfigById(configId);
   if(daiFuConfig){
	   $div.find("[name=handicapName]").text(_checkObj(daiFuConfig.handicapName));
	   $div.find("[name=levelNameList]").text((daiFuConfig.levelNameList&&daiFuConfig.levelNameList.length>0?daiFuConfig.levelNameList.join(" | "):"暂无"));
	   $div.find("[name=platformStatus]").text(daiFuConfig.platformStatus==1?"在用":"停用");
	   $div.find("[name=memberId]").text(hideMemberId(daiFuConfig.memberId));
	   $div.find("[name=channelName]").text(_checkObj(daiFuConfig.channelName));
	   $div.find("[name=aliasName]").text(_checkObj(daiFuConfig.aliasName));
   }else{
	   $div=$('<div style="font-size: 20px;" class="center">无数据</div>');
   }
   return $div;
}

/** 开启第三方代付的盘口设定 */
var showUpdateDaiFuHandicapModal=function(){
	var html='<div id="UpdateDaiFuHandicapModal" class="modal fade" aria-hidden="false" >\
		<div class="modal-dialog modal-lg"  style="width:1000px;">\
			<div class="modal-content">\
				<div class="modal-header no-padding text-center">\
					<div class="table-header">\
						<button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button>\
						开启第三方代付的盘口设定\
					</div>\
				</div>\
				<div class="modal-body">\
					<h6 class="header smaller center lighter blue no-padding no-margin-top">对勾选的盘口开启第三方代付</h6>\
					<div id="handNameList" class="row"></div>\
				</div>\
				<div class="col-sm-12 modal-footer no-margin center">\
					<button type="button" class="btn btn-danger btn-white btn-bold btn-round" onclick="resetDaiFuHandicapModal()">\
				        <i class="ace-icon fa fa-refresh icon-on-right"></i>&nbsp;重置\
				    </button>\
					<button type="button" class="btn btn-success btn-white btn-bold btn-round" onclick="doUpdateDaiFuHandicapModal()">\
			        	<i class="ace-icon fa fa-check icon-on-right"></i>&nbsp;保存\
				    </button>\
				</div>\
			</div>\
		</div>\
	</div>';
	var $div=$(html).clone().appendTo($("body"));
	$div.modal("toggle");
	//重置参数
	resetDaiFuHandicapModal();
	$div.on('hidden.bs.modal', function () {
	// 关闭窗口清除model
	$div.remove();
	});
}
var resetDaiFuHandicapModal=function(){
	var $handicap_div=$("#UpdateDaiFuHandicapModal #handNameList");
	var enableHandicap=sysSetting.OUTWARD_THIRD_INSTEAD_PAY;
	var HTML="";
	$.each(handicap_list_all,function(i,handicap){
		if(!handicap||handicap.status!=1||!handicap.zone){
			//无区域或未开启的盘口跳过
			return true;
		}
		if(handicap.id){
			var lable_color=' label-grey ';
			var checked='';
			if(enableHandicap&&enableHandicap.indexOf(";"+handicap.id+";")!=-1){
				//已勾选的盘口
				lable_color=' label-primary ';
				checked=' checked="checked" ';
			}
			HTML+='<div class="col-sm-2">'+
						'<span style="margin-bottom:10px;" class="label label-xlg '+lable_color+' ">'+
							'<label style="width:110px;text-align:left;">'+
								'<input type="checkbox" '+checked+' name="checkbox_handicapName" class="ace" value="'+handicap.id+'"/>'+
								'&nbsp;<span class="lbl">'+handicap.name+'</span>'+
							'</label>'+
						'</span>'+
					'</div>';
		}
	});
	$handicap_div.html(HTML);
}
var doUpdateDaiFuHandicapModal=function(){
	bootbox.confirm("<span class='red bolder'>请确认【对勾选的盘口开启第三方代付】是否已设置好。</span>确定保存？", function(result) {
		if (result) {
			var $handicap_div=$("#UpdateDaiFuHandicapModal #handNameList");
			var enable_handicap=new Array();
			$.each($handicap_div.find("[name=checkbox_handicapName]:checked"),function(i,result){
				enable_handicap.push($(result).val());
			});
			var keysArray = new Array(), valsArray = new Array();
			//可用盘口
			keysArray.push("OUTWARD_THIRD_INSTEAD_PAY");
			if(enable_handicap&&enable_handicap.length>0&&enable_handicap[0]){
				valsArray.push(";"+enable_handicap.join(";")+";");
			}else{
				valsArray.push(";");
			}
			$.ajax({
				type: "PUT",
				dataType: 'JSON',
				url: '/r/set/update',
				async: false,
				data: {
					"keysArray": keysArray.toString(),
					"valsArray": valsArray.toString()
				},
				success: function (jsonObject) {
					if (jsonObject && jsonObject.status == 1) {
						//异步刷新系统配置全局变量
						loadSysSetting();
						showMessageForSuccess("保存成功");
						$("#UpdateDaiFuHandicapModal").modal("toggle");
					} else {
						showMessageForFail("保存失败" + jsonObject.message);
					}
				}
			});
		}
		setTimeout(function(){
			$('body').addClass('modal-open');
		},500);
	});
}

/** 商号 */
var hideMemberId=function(memberId){
	if (memberId.length>4) {
		memberId = '**'+memberId.substring(memberId.length-4);
    }
	return memberId;
}