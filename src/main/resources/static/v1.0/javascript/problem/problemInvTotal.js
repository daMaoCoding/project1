var SysErrStatusLocking = 10 , SysErrStatusLocked = 20 , SysErrStatusFinishedNormarl = 3 , SysErrStatusFinishedFreeze = 4;
var NODATA = '<div style="margin-bottom:0px;font-size: 20px;width:100%;" class="alert alert-success center">无数据</div>';

var queryProblem4Mobile =function(pageNo){
    var pageId = 'mobilePage';
    pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#"+pageId+" .Current_Page").text()?$("#"+pageId+" .Current_Page").text()-1:0);
    pageNo = pageNo<0?0:pageNo;
    var pageSize=$.session.get('initPageSize');
    var eqpInvTab =  $("#eqpInvTab");
    var tbody =  eqpInvTab.find("table tbody");
    var tableFoot = eqpInvTab.find("#mobilePage");
    var $div = $("#searchButProblem4Dev");
    var status = $div.find("input[name='status']:checked").val();
    var batteryStatus = $div.find("input[name='batteryStatus']:checked").val();
    var offLineStatus = $div.find("input[name='offLineStatus']:checked").val();
    var dealStatus = new Array();
    $div.find("input[name='dealStatus']:checked").each(function(){
        dealStatus.push(this.value);
    });
    var dealStr = "";
    if(dealStatus.length > 0){
        dealStr=dealStatus.join(",");
        dealStr = "," + dealStr + ",";
    }
    var dealStatus = $div.find("input[name='dealStatus']:checked").val();
    var lockStatus = $div.find("input[name='lockStatus']:checked").val();
    var rebate_deal = $div.find("input[name='rebate_deal']:checked").val();
    var alias = $div.find("input[name=alias]").val();
    var appVersion = $div.find("input[name=appVersion]").val();
    var data = {
        pageNo:pageNo,
        pageSize:pageSize,
        status:status,
        batteryStatus:batteryStatus,
        dealStatus:dealStr,
        lockStatus:lockStatus,
        rebate_deal:rebate_deal,
        alias:alias,
        offLineStatus:offLineStatus,
        appVersion:appVersion
    };
    var idList=new Array();
    var mobileList=new Array();
    $.ajax({type:"post",url:"/r/problem/eqpInv4Mobile",data:data,dataType:'json',success:function(jsonObject){
        if(jsonObject.status!=1){
            showMessageForFail(jsonObject.message);
            return;
        }
        if(jsonObject.status==1 &&(!jsonObject.data || jsonObject.data.length == 0)){
            tbody.html('');
            tableFoot.html(NODATA);
            return;
        }
        var html = '';
        $.each(jsonObject.data,function(idx, obj) {
            idList.push({'id':obj.id});
            mobileList.push({'mobile':obj.mobile});
            html = html + '<tr>';
            html = html + '<td>'+_checkObj(obj.mobile,true)+'</td>';
            html = html + '<td><a class="bind_hover_card" data-toggle="accountInfoHover'+obj.id+'" data-placement="auto right" data-trigger="hover"><span>'+(obj.bankType?obj.bankType:'无')+'|'+(obj.owner?hideName(obj.owner):'无')+'</br>'+(obj.alias?obj.alias:'无')+'|'+(obj.account?hideAccountAll(obj.account):"无" )+'</span></a></td>';
            html = html + '<td>' + _checkObj(obj.userName,true) + '</td>';
            html = html + '<td>' + _checkObj(obj.appVersion,true) + '</td>';
            html = html + '<td>' + _checkObj(obj.model,true) + '</td>';
            html = html + '<td>' + _checkObj(obj.bankAppVersion,true) + '</td>';
            html = html + '<td>' + _checkObj(obj.netType,true) + '</td>';
            if(obj.status==0){
                html = html + '<td class="label-warning"><a class="bind_hover_card breakByWord" data-toggle="mobileInfoHover'+obj.mobile+'" data-placement="auto right" data-trigger="hover">'+obj.errMsg+'</a></td>';
            }else{
                html = html + '<td></td>';
            }
            html = html + '<td>' + _checkObj(obj.battery,true) +'</td>';
            if(obj.lockStatus == 1){
            	html = html + '<td>' + secondToDate(obj.dealTime) + '</td>';
            }else{
            	html = html + '<td></td>';
            }
            if(obj.operator){
                html = html + '<td>'+obj.operator+'</td>';
            }else{
                html = html + '<td></td>';
            }
            if (_checkObj(obj.remark)) {
            	 html += '<td>'
                     + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                     + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                     + ' data-content="' + _divideRemarks(obj.remark) + '">'
                     +  cutFlag4remark(obj.remark).substring(0, 4)+"..."
                     + '</a>'
                     + '</td>';
            } else {
                html += '<td></td>';
            }
            html = html + '<td>';
            if(obj.status==0){
                if(!obj.lockStatus || obj.lockStatus != 1){
                    html = html +'<button class="btn btn-xs btn-white btn-danger btn-bold" onclick="devInvLock('+obj.mobile+','+obj.id+')"><span>锁定</span></button>';
                    html = html +'<button class="btn btn-xs btn-white btn-danger btn-bold" onclick="showContract('+obj.mobile+')"><span>排查</span></button>';

                }else if(obj.operator && obj.operator == getCookie("JUID")){
                    html = html +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="devInvUnLock('+obj.mobile+')"><span>解锁</span></button>';
                    html = html +'<button class="btn btn-xs btn-white btn-danger btn-bold" onclick="showContract('+obj.mobile+')"><span>排查</span></button>';
                }else if(obj.operator && obj.operator != getCookie("JUID")){
                    html = html +'被 '+obj.operator+' 锁定';
                }
            }else{
                if(obj.operator && obj.operator == 'ADMIN'){
                    html = html +'兼职自处理';
                }else if(obj.operator && obj.operator != 'ADMIN'){
                    html = html +'操作：'+obj.operator;
                }
            }
            html = html + '</td>';
            html = html + '</tr>';
        });
        tbody.html(html);
        $("[data-toggle='popover']").popover();
        loadHover_deviceHover(mobileList);
        loadHover_accountInfoHover(idList);
        showPading(jsonObject.page,'mobilePage',queryProblem4Mobile,null,true,false);
    }});
};

/**
 * 循环生成手机号
 */
var loadHover_deviceHover=function(idList){
    // 发送任意空请求，刷新版本号信息 以保证服务切换时版本信息正确
    $.ajax({dataType:'json',async:false,type:"get",url:'/global/version',success:function(){}});
    $.ajax({type:"GET", async:false, dataType:'html', url : "/"+sysVersoin+"/html/problem/deviceStatus.html",
        success : function(html){
            $.each(idList,function(index,result){
                    $("[data-toggle='mobileInfoHover"+result.mobile+"']").popover({
                        html : true,
                        title: function(){
                            return '<center class="blue">耗时信息</center>';
                        },
                        delay:{show:0, hide:100},
                        content: function(){
                            var $div = $(html).find("#clientMobile");
                            var data = findDeviceStatus(result.mobile);
                            if(data){
                                $div.find("span[name=errTime]").text(data.errTime);
                                $div.find("span[name=lockTime]").text(data.lockTime);
                                $div.find("span[name=solveTime]").text(data.solveTime);
                                $div.find("span[name=operator]").text(data.operator);
                                $div.find("span[name=totalTime]").text(data.totalTime);
                            }
                            return $div;
                        }
                    });
            });
        }
    });
}

var findDeviceStatus = function(mobile){
    var result='';
    $.ajax({type:"POST", url:"/r/problem/getDeviceStatus", data:{"mobile":mobile}, dataType:'json', async:false,
        success:function(jsonObject){
            if(jsonObject.status == 1){
                result=jsonObject.data;
            }else{
                showMessageForFail("获取设备耗时异常");
            }
        }
    });
    return result;
}

var showConciliate = function(accId){
    var html =   '<div id="choiceExportModal_InOut" class="modal fade " tabindex="-1">';
    html = html+ '   <input type="hidden" id="accountId"/>';
    html = html+ '   <input type="hidden" id="operaType"/>';
    html = html+ '   <input type="hidden" id="exBankType"/>';
    html = html+ '   <div class="modal-dialog modal-lg" style="width:400px;">';
    html = html+ '      <div class="modal-content">';
    html = html+ '         <div class="modal-header no-padding text-center">';
    html = html+ '            <div class="table-header">';
    html = html+ '               <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>请选择对账时间</span>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '         <div class="modal-body">';
    html = html+ '            <div class="widget-box">';
    html = html+ '               <div class="widget-header hide"><h4 class="smaller"></h4></div>';
    html = html+ '                  <div class="widget-body">';
    html = html+ '                      <div class="widget-main">';
    html = html+ '                          <label class="control-label bolder blue">时间</label>&nbsp;&nbsp;';
    html = html+ '                          <span class="input-group-addon sr-only"><i class="fa fa-calendar"></i></span>';
    html = html+ '                          <input class="date-range-picker" type="text" placeholder="请选择对账日期" name="startAndEndTime_export" style="height: 32px;width:280px;"/>';
    html = html+ '                          <div class="control-group">&nbsp;&nbsp;&nbsp;</div>';
    html = html+ '                          <div style="text-align:center;">';
    html = html+ '                              <a class="btn btn-sm btn-success" id="checkButton">';
    html = html+ '                                  <i class="fa fa-check" aria-hidden="true"></i><span>对账</span>';
    html = html+ '                              </a>';
    html = html+ '                          </div>';
    html = html+ '                      </div>';
    html = html+ '                  </div>';
    html = html+ '              </div>';
    html = html+ '         </div>';
    html = html+ '      </div>';
    html = html+ '   </div>';
    html = html+ '</div>';
    var $div=$(html).clone().appendTo($("body"));
    $div.find("#accountId").val(accountId);
    var $timer = $div.find("[name=startAndEndTime_export]");
    $timer.daterangepicker({
        cancel: 'cancel.daterangepicker',
        apply: 'apply.daterangepicker',
        singleDatePicker: true,timePicker: false,
        locale: {
            "format": "YYYY-MM-DD", "separator": " ~ ",
            "applyLabel": "确定", "cancelLabel": "取消", "fromLabel": "从", "toLabel": "到",
            "customRangeLabel": "自定义", "dayNames": ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
            "daysOfWeek": ["日", "一", "二", "三", "四", "五", "六"],
            "monthNames": ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"],
            "firstDay": 1
        }
    });
    $div.find("#checkButton").click(function(){
        $.ajax({type: 'get',url: '/r/cabana/conciliate',data: {"accId":accId,date:$timer.val().split(' ')[0]},dataType: 'json',success: function (res) {
            if (res.status==1) {
                showMessageForSuccess('操作成功');
                $div.modal("toggle");
            }else{
                showMessageForFail(res.message);
            }
        }});
    });
    $div.modal("toggle");
    $div.on('hidden.bs.modal', function () { $div.remove(); });
};


var devInvLock = function (mobile,id) {
    bootbox.confirm("确定锁定该记录&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/problem/devInvLock',data: {"mobile":mobile,"id":id},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('锁定成功');
                    queryProblem4Mobile();
                }else{
                    showMessageForFail(res.message)
                }
            }});
        }
    });
};
var devInvUnLock = function (mobile) {
    bootbox.confirm("确定解锁该记录&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/problem/devInvUnLock',data: {"mobile":mobile},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('解锁成功');
                    queryProblem4Mobile();
                }else{
                    showMessageForFail(res.message)
                }
            }});
        }
    });
};
queryProblem4Mobile();




var queryProblem4AccInv = function (pageNo){
    var req = {};
    var problemAccInv =  $("#accInvTab");
    req.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#accInvPage .Current_Page").text()?$("#accInvPage .Current_Page").text()-1:0);
    req.pageNo = req.pageNo<0?0:req.pageNo;
    req.pageSize = $.session.get('initPageSize');
    req.tarHandicap = problemAccInv.find("select[name='tarHandicap']").val();
    req.tarBankType = problemAccInv.find("select[name=tarBankType]").val();
    req.tarAlias = problemAccInv.find("input[name=tarAlias]").val();
    req.tarFlag= problemAccInv.find("input[name='tarFlag']:checked").val();
    req.tarLevel= problemAccInv.find("input[name='tarLevel']:checked").val();
    req.balRangeType = problemAccInv.find("select[name='balRangeType']").val();
    req.stBal = problemAccInv.find("input[name='stBal']").val();
    req.edBal =  problemAccInv.find("input[name='edBal']").val();
    var startAndEndTime = problemAccInv.find("input[name='startAndEndTime']").val();
    if(startAndEndTime){
        var startAndEnd = startAndEndTime.split(" - ");
        req.stOcrTm = $.trim(startAndEnd[0]);
        req.edOcrTm = $.trim(startAndEnd[1]);
    }else{
        bootbox.alert("查询时间不能为空");
        return;
    }
    req.status =  problemAccInv.find("input[name='status']:checked").val();
    req.handicapId = $("select[name='tarHandicap']").val();
    if(! req.handicapId){
        //只查询当前人拥有的盘口账号信息
        var search_EQ_handicapId = [];
        $('select[name="tarHandicap"]').find('option:not(:first-child)').each(function () {
            search_EQ_handicapId.push($(this).val());
        });
        req.handicapId = search_EQ_handicapId.toString();
    }
    var tbody =  problemAccInv.find("table tbody");
    var tableFoot = problemAccInv.find("#accInvPage");
    tableFoot.html('');
    $.ajax({ dataType:'json',type:"get",url:'/r/problem/accInvTotal',data:req,success:function(jsonObject){
        if(jsonObject.status == 1){
            var data = jsonObject.data;
            if(!data||  data.length==0){
                tbody.html('');
                tableFoot.html(NODATA);
                return;
            }
            var html = '';
            var idList=new Array();
            $.each(data,function(idx, obj) {
                idList.push({'id':obj.target});
                html = html + '<tr>';
                html = html +   '<td>'+obj.handicapName+'</td>';
                html = html +   '<td>'+obj.levelName+'</td>';
                html = html +   '<td>'+obj.targetAlias+'</td>';
                html = html +   '<td><a class="bind_hover_card" data-toggle="accountInfoHover'+obj.target+'" data-placement="auto right" data-trigger="hover"><span>'+obj.simpName+'</span></a></td>';
                html = html +   '<td>'+obj.accStatusName+'</td>';
                html = html +   '<td>'+obj.flagName+'</td>';
                html = html +   '<td>'+(obj.balance?obj.balance:'')+'</td>';
                html = html +   '<td>'+(obj.bankBalance?obj.bankBalance:'')+'</td>';
                html = html +   '<td>'+obj.margin+'</td>';
                html = html +   '<td><p class="text-left col-sm-10">'+obj.timeSimp+'</p></td>';
                html = html +   '<td><span title="'+(!obj.remark?'备注:无':obj.remark)+'">'+obj.statusName+'</span></td>';
                html = html +   '<td>'+obj.collectorName+'</td>';
                html = html +   '<td>';
                if(obj.status == SysErrStatusLocking){
                    html = html +       '<button class="btn btn-xs btn-white btn-danger btn-bold" onclick="showInvstRemarkModal('+obj.id+')"><span>备注</span></button>';
                    html = html +       '<button class="btn btn-xs btn-white btn-danger btn-bold" onclick="accInvLock('+obj.id+')"><span>锁定</span></button>';
                    html = html +       '<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal('+obj.id+','+obj.target+')"><span>明细</span></button>';
                }else if(obj.status == SysErrStatusLocked){
                    html = html +       '<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="accInvUnLock('+obj.id+')"><span>解锁</span></button>';
                    html = html +       '<button class="btn btn-xs btn-white btn-success btn-bold" onclick="accInvDoing('+obj.id+','+obj.target+')"><span>排查</span></button>';
                    html = html +       '<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal('+obj.id+','+obj.target+')"><span>明细</span></button>';
                }else{
                    html = html +       '<button class="btn btn-xs btn-white btn-success btn-bold" onclick="showAccInvstWatchModal('+obj.id+','+obj.target+')"><span>排查记录</span></button>';
                    html = html +       '<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal('+obj.id+','+obj.target+')"><span>明细</span></button>';
                }
                html = html +   '</td>';
                html = html + '</tr>';
            });
            tbody.html(html);
            loadHover_accountInfoHover(idList);
            showPading(jsonObject.page,"accInvPage",queryProblem4AccInv,null,true,false);
        }else {
            tableFoot.html(NODATA);
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){tableFoot.html(NODATA); ;bootbox.alert(result);}});
};

var showInvstRemarkModal = function(id) {
    var $remark = $('#invistRemarkModal');
    $remark.find('#invistRemarkErrId').val(id);
    $remark.find('#invistRemarkModalRemark').val('');
    $remark.modal('show');
};

var  saveInvstRemark = function() {
    var $remark = $('#invistRemarkModal');
    var errorId =  $remark.find('#invistRemarkErrId').val();
    var remark = $remark.find('#invistRemarkModalRemark').val();
    $.ajax({type: 'post',url: '/r/problem/invstRemark',data: {"errorId": errorId, "remark": remark},dataType: 'json',
        success: function (res) {
            if (res.status ==1) {
                showMessageForSuccess('操作成功');
                $remark.find('#invistRemarkErrId').val('');
                $remark.modal('hide');
                var pageNo =0;
                if($("#accInvPage .Current_Page").text()){
                    pageNo = $("#accInvPage .Current_Page").text();
                    pageNo =  parseInt(pageNo)-1;
                }
                queryProblem4AccInv(pageNo);
            }else{
                showMessageForFail(res.message)
            }
        }
    });
};

var accInvLock = function (errId) {
    bootbox.confirm("确定锁定该记录&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/problem/accInvLock',data: {"errId":errId},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('锁定成功');
                    queryProblem4AccInv();
                }else{
                    showMessageForFail(res.message)
                }
            }});
        }
    });
};

var accInvUnLock = function (errId) {
    bootbox.confirm("确定解锁该记录&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/problem/accInvUnLock',data: {"errId":errId},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('解锁成功');
                    queryProblem4AccInv();
                }else{
                    showMessageForFail(res.message)
                }
            }});
        }
    });
};

var showContract = function(mobile){
	$.ajax({
		type:"GET",
		async:false,
		dataType:'html',
		url : "/"+sysVersoin+"/html/common/showInfoModal.html",
		success : function(html){
			var $div=$(html).find("#ProblemInvTotalRemarkModal").clone().appendTo($("body"));
			$div.find("#tableAdd td").css("padding-top","10px");
			$div.find("#tableAdd td.noPaddingTop").css("padding-top","0px");
			$.ajax({
				type: "POST",
				dataType: 'JSON',
				url: '/r/problem/getContractInfo',
				async: false,
				data:{
                  "mobile" : mobile
                },
				success: function (jsonObject) {
					if(jsonObject.status==1){
						var data=jsonObject.data;
						$div.find("[name=mobile]").html(hideAccountAll(mobile,true));
						$div.find("[name=credits]").html(data.credits?data.credits+"元":"暂无");
						$div.find("[name=creditsTime]").html(data.creditsTime?data.creditsTime:"暂无");
						$div.find("[name=contactor]").html(data.contactor?data.contactor:"暂无");
						$div.find("[name=contactorInfo]").html(data.contactorInfo?data.contactorInfo:"暂无");
						$div.find("[name=salesName]").html(data.salesName?data.salesName:"暂无");
						$div.find("[name=history_remark]").html(data.remark?data.remark.replace(/\n/g, "<br/>"):"暂无");
						$div.find("[name=lastUpdateTime]").html(data.lastUpdateTime?data.lastUpdateTime:"暂无");
						$div.find("[name=currLock]").html(data.currLock?data.currLock:"暂无");
						//转交到锁定人下拉框加载
						var options='<option selected="selected" value="">----------请选择----------</option>';
						$.ajax({ type:"post", url:API.r_user_findUserList4PermissionKey,data: {"permissionKey":"ProblemInvTotal:eqpInvTab:*"},dataType:'json',async: false,success:function(jsonObject){
							if(jsonObject.status == 1){
								$.each(jsonObject.data,function(index,record){
									options+='<option value="'+record.uid+'">'+record.uid+'</option>';
								});
							}
						}});
						$div.find('[name=select_operator]').chosen({
					        enable_split_word_search: true,
					        no_results_text: '没有此用户',
					        width:'200px',
					        search_contains: true
					    });
						$div.find("[name=select_operator]").empty().html(options);
						$div.find("[name=select_operator]").trigger('chosen:updated');
					}
				}
			});
			$div.modal("toggle");
			//保存备注
			$div.find("#problem_saveRemark").click(function(){
				var remark=$div.find("[name=remark]").val();
				if(remark.length<5||remark.length>100){
					showMessageForFail("请输入备注，5-100字之间");
					return false;
				}
				bootbox.confirm("确定保存备注信息?", function(result) {
					if (result) {
						$.ajax({
							type:"post",
							url:"/r/problem/devInvDeal",
							dataType:'json',
							async: false,
							data:{
								"mobile":mobile,
								"remark":remark
							},
							success:function(jsonObject){
								if(jsonObject.status == 1){
                                    queryProblem4Mobile();
									showMessageForSuccess("保存成功");
									$div.modal("toggle");
								}else{
									showMessageForFail("保存失败，"+jsonObject.message);
								}
							}
						});
					}
				});
			});
			//转交任务
			$div.find("#btnDevInvLock").click(function(){
				var operator=$div.find("[name=select_operator]").val();
				if(!operator){
					showMessageForFail("请选择转交人");
					return false;
				}
				bootbox.confirm("确定转交?", function(result) {
					if (result) {
						$.ajax({
							type:"post",
							url:"/r/problem/devInvLock",
							dataType:'json',
							async: false,
							data:{
								"mobile":mobile,
								"operator":operator
							},
							success:function(jsonObject){
								if(jsonObject.status == 1){
                                    queryProblem4Mobile();
									showMessageForSuccess("转交成功");
									$div.modal("toggle");
								}else{
									showMessageForFail("转交失败，"+jsonObject.message);
								}
							}
						});
					}
				});
			});

			$div.on('hidden.bs.modal', function () {
				//关闭窗口清除model
				$div.remove();
			});
			$div.find("#do_update").unbind("click").bind("click",function(){
				do_addIncomeAccount(addType);
			});
		}
	});
};

initTimePicker(true,$("[name=startAndEndTime]"),typeCustomLatestToday);

getHandicap_select($("select[name='tarHandicap']"),null,"全部");
getBankTyp_select($("select[name='tarBankType']"),null,"全部");

queryProblem4AccInv();

initRefreshSelect($("#accountFilter #refreshProblemAccInv"),$("#accountFilter #search-button"),75,"refresh_ProblemAccInv");
contentRight();
if($("#accInvTab").length<=0){
	//无账目排查的权限
	$("ul#myTab3 li:first").addClass("active");
	$("#eqpInvTab").addClass("active").addClass("in");
}
function secondToDate(result) {
    var h = Math.floor(result / 3600) < 10 ? '0'+Math.floor(result / 3600) : Math.floor(result / 3600);
    var m = Math.floor((result / 60 % 60)) < 10 ? '0' + Math.floor((result / 60 % 60)) : Math.floor((result / 60 % 60));
    var s = Math.floor((result % 60)) < 10 ? '0' + Math.floor((result % 60)) : Math.floor((result % 60));
    return result = h + ":" + m + ":" + s;
}
