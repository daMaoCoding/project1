//给主机添加账号的js

var showAccountList=function(CurPage){
    var $div = $('#accountModal');
    if(!!!CurPage) CurPage=$div.find("#accountModalPage .Current_Page").text();
    var defaultAccountType=$div.find("#defaultAccountType").val();
    var $tbody =$div.find("table#accountModalTable tbody").html(""),$thead=$div.find("table#accountModalTable thead");
    var data={
		"pageNo":CurPage<=0?0:CurPage-1, 
		"pageSize":$.session.get('initPageSize'),
		"search_EQ_alias":$div.find("[name=search_EQ_alias]").val().trim(),
		"search_LIKE_account":$div.find("[name=search_LIKE_account]").val().trim(),
		"search_LIKE_owner":$div.find("[name=search_LIKE_owner]").val().trim(),
		"search_ISNULL_gps":"111",//值随便给 保证key传到后端，只查GPS为空的数据（说明未挂起）
		"search_IN_handicapId":handicapId_list.toString(),
		"typeToArray":[defaultAccountType].toString(),
		"statusToArray":[accountStatusNormal,accountStatusStopTemp,accountStatusEnabled,accountInactivated,accountStatusFreeze,accountStatusExcep].toString()
	};
    $.ajax({
    	dataType:'json',
    	type:"get",
    	async:false, 
        url:API.r_account_list,
    	data:data,
        success:function(jsonObject){
            if(jsonObject.status==-1){
                showMessageForFail("查询警告："+jsonObject.message);return;
            }
            var $tbody=$div.find("tbody"),trs="",idList=new Array();
            $.map(jsonObject.data,function(record){
            	idList.push({'id':record.id});
            	trs+="<tr>";
            	trs+="<td>"+_checkObj(record.alias)+"</td>";
            	trs+="<td>" +
						"<a class='bind_hover_card' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"+
							hideAccountAll(record.account)+
						"</a>" +
					"</td>";
            	trs+="<td>"+_checkObj(record.bankType)+"</td>";
            	trs+="<td>"+_checkObj(record.bankName)+"</td>";
            	trs+="<td>"+_checkObj(record.owner)+"</td>";
            	trs+="<td>" +
            			"<button type='button' class='btn btn-xs btn-white btn-warning btn-bold green'" +
	            			" onclick='doAddAccountToHost("+record.id+","+record.signAndHook+",\""+(record.bankType?record.bankType:'')+"\")'>" +
	            			"<span>添加</span>" +
            			"</button>" +
            		"</td>";
            	trs+="</tr>";
            });
            $tbody.html(trs);
            showPading(jsonObject.page,"accountModalPage",showAccountList);
			loadHover_accountInfoHover(idList);
        }
    });
}

/** 新增账号到主机 */
var doAddAccountToHost=function(accountId,signAndHook,bankType){
	var host=$("#accountModal #accountModal_host_IP").val();
	if(!host){
		return;
	}
    if(!getMessageEntity(host)){
    	showMessageForFail("添加失败，IP“"+host+"”在工具端不存在！");
    	return;
    }
    if(signAndHook&&bankType){
        $.ajax({ 
        	dataType:'json',
        	type:"get",
        	url:API.r_host_addAccountToHost,
        	data:{
        		host:host,
        		accountId:accountId
        	},
        	success:function(jsonObject){
	            if(jsonObject.status == 1){
                	showMessageForSuccess("新增成功");
	                showAccountList();
	            }else {
	            	showMessageForFail(jsonObject.message);
	            }
	        },
	        error:function(result){ 
	        	showMessageForFail(result);
	        }
		});
    }else{
        showSignAndHookModal(accountId,host,'应先设置账号及密码与账号类别',true);
    }
}
var signAndHookModal = '<div id="signAndHookModal" class="modal fade" aria-hidden="false" data-backdrop="static">\
    <div class="modal-dialog width-30">\
        <div class="modal-content">\
            <div class="modal-header text-center no-padding"><h5>账号：{hideAccount}</h5></div>\
            <div class="modal-body">\
                <div class="row">\
                    <div class="col-sm-13" style="">\
                        <form role="form" class="form-horizontal">\
                            <div class="form-group">\
                               <label class="col-sm-4 control-label"><span>账号类别</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><select class="form-control" name="bankType">{options}</select></div>\
                            </div>\
                            <div class="form-group">\
                               <label class="col-sm-4 control-label"><span>登陆账号</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><input placeholder="请输入账号" class="form-control" name="sign"  type="text"/></div>\
                            </div>\
                            <div class="form-group">\
                               <label class="col-sm-4 control-label"><span>登陆密码</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><input placeholder="请输入密码" class="form-control" name="hook"  onfocus="this.type=\'password\'"  type="text"/></div>\
                            </div>\
							<div class="form-group">\
								<label class="col-sm-4 control-label"><span>支付密码</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><input placeholder="请输入密码" class="form-control" name="hub"  onfocus="this.type=\'password\'"  type="text"/></div>\
							</div>\
							<div class="form-group">\
								<label class="col-sm-4 control-label"><span>U盾密码</span>&nbsp;&nbsp;&nbsp;</label><div class="col-sm-7"><input placeholder="请输入密码" class="form-control" name="bing"  onfocus="this.type=\'password\'"  type="text"/></div>\
							</div>\
							<div class="form-group">\
								<label class="col-sm-4 control-label"><span>抓取间隔/秒</span><i class="fa fa-asterisk red"></i></label><div class="col-sm-7"><input placeholder="请输入抓取间隔" class="form-control" name="interval"  type="number"/></div>\
							</div>\
							<div class="form-group">\
								<label class="col-sm-4 control-label"><span>明文显示密码</span></label>\
								&nbsp;&nbsp;&nbsp;&nbsp;<input onchange="changePwdShowType(this);" type="checkbox" class="ace"><span class="lbl" style="padding-top:7px;"></span>\
							</div>\
                        </form>\
                    </div>\
                </div>\
            </div>\
            <div class="modal-footer">\
                <div class="red">{titleTip}&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</div>\
                <button class="btn btn-primary" id="updateIintervalBtn" onclick="updateIinterval({id},\'{ip}\');" type="button">修改抓取间隔</button>\
				<button class="btn btn-primary" onclick="signAndHook({id},\'{ip}\',{hideBtn_updateIinterval});" type="button">修改</button>\
                <button class="btn btn-default" type="button" data-dismiss="modal">取消</button>\
            </div>\
        </div>\
    </div>\
</div>';
/** 未设置密码的账号先设置 */
var showSignAndHookModal=function(accountId,ip,titleTip,hideBtn_updateIinterval){
    var messageEntity=getMessageEntity(ip);
    if(!messageEntity){
    	showMessageForFail("添加失败，IP“"+ip+"”在工具端不存在！");
    	return;
    }
    var accountInfo =  getAccountInfoById(accountId);
    accountInfo.titleTip=titleTip?titleTip:'';
    $("body").find("#signAndHookModal").remove();
    var options= new Array();
    options.push("<option value=''>--请选择--</option>");
    $.each(bank_name_list,function(index,record){
        if(accountInfo.bankType == record){
            options.push("<option value='"+record+"' selected>"+record+"</option>");
        }else{
            options.push("<option value='"+record+"'>"+record+"</option>");
        }
    });
    accountInfo.options=options.join('');
    accountInfo.ip=ip;
    accountInfo.hideBtn_updateIinterval=hideBtn_updateIinterval;
    accountInfo.hideAccount=hideAccountAll(accountInfo.account);
    var $div =$(fillDataToModel4Item(accountInfo,signAndHookModal)).appendTo("body");
    if(hideBtn_updateIinterval){
    	//添加时无修改抓取时间间隔按钮
    	$div.find('#updateIintervalBtn').hide();
    }
    //循环填充单个账号抓取时间间隔  如果没有，默认填充180s
    var isUpdate=false;
    $.each(messageEntity.data,function(index,result){
    	if(accountId==result.id){
    		isUpdate=true;
    		$div.find("input[name=interval]").val(result.interval?result.interval:'180');
    	}
    });
    if(!isUpdate){
    	$div.find("input[name=interval]").val('180');
    }
    $div.find("form[role='form']").bootstrapValidator({
        message : 'This value is not valid',
        feedbackIcons : { valid : 'glyphicon glyphicon-ok', invalid : 'glyphicon glyphicon-remove', validating : 'glyphicon glyphicon-refresh' },
        excluded : [ ':disabled' ],
        fields : {
            bankType:{ validators : { notEmpty : {message : '请选择账号类别！'}  }  },
            sign : {
                validators : {
                    notEmpty : {message : '账号不能为空！'},
                    stringLength : {min : 4, max : 45, message : '账号长度必须在4到45之间！'}
                }
            },
            hook : {
                validators : {
                    notEmpty : {message : '登录密码不能为空！'},
                    stringLength : {min : 4, max : 45, message : '登录密码长度必须在4到45之间！'}
                }
            },
            hub : {
                validators : {
                    notEmpty : {message : '支付密码不能为空！'},
                    stringLength : {min : 4, max : 45, message : '支付密码长度必须在4到45之间！'}
                }
            },
            bing : {
                validators : {
                    stringLength : {min : 4, max : 45, message : 'U盾密码长度必须在4到45之间！'}
                }
            }            
        }
    });
    $div.modal("toggle");
}

var signAndHook=function(accountId,host,hideBtn_updateIinterval){
	if(!host){
		return;
	}
    var $div = $("body").find("#signAndHookModal");
    var bootstrapValidator = $div.find("form[role='form']").data('bootstrapValidator');
    bootstrapValidator.validate();
    if (bootstrapValidator.isValid()) {
        var interval = $div.find("input[name='interval']").val();
    	if(!interval||interval<5||interval>180){
            showMessageForFail("请输入抓取时间间隔，值应该在[5-180]之间");return;
        }
        var sign = $.trim($div.find("input[name='sign']").val(),true);
        var hook = $.trim($div.find("input[name='hook']").val(),true);
        var hub = $.trim($div.find("input[name='hub']").val(),true);
        var bing = $.trim($div.find("input[name='bing']").val(),true);
        var bankType = $div.find("select[name='bankType']").val();
        if(!!!sign||!!!hook||!!!bankType||!!!hub){
            showMessageForFail("账号类别、登录账号、登录密码、支付密码不可为空！");return ;
        }
        $.ajax({
        	async:false,
        	dataType:'json',
        	type:"get",
        	url:API.r_host_alterSignAndHook,
        	data:{
        		host:host,
        		accountId:accountId,
        		sign:sign,
        		hook:hook,
        		hub:hub,
        		bing:bing,
        		bankType:bankType,
        		interval:interval
        	},
        	success:function(jsonObject){
	            if(jsonObject.status != 1){
	            	showMessageForFail(jsonObject.message);
	            }else{
	            	//添加时刷新账号列表
	            	if(hideBtn_updateIinterval){
		                showAccountList(0);
		            	showMessageForSuccess("新增成功");
	            	}else{
		            	showMessageForSuccess("修改成功");
	            	}
	            }
	            $div.modal("toggle");
        	},
        	error:function(result){
        		showMessageForFail(result);
        	}
        });
    }
}
/** 修改抓取时间间隔 */
var updateIinterval=function(accountId,ip){
    var $div = $("body").find("#signAndHookModal");
    var interval = $div.find("input[name='interval']").val();
	if(!interval||interval<5||interval>600){
        showMessageForFail("请输入抓取时间间隔，值应该在[5-600]秒之间");return;
    }
	 $.ajax({
		 async:false,
		 dataType:'json',
		 type:"get",
		 url:API.r_host_updateIinterval,
		 data:{
			 host:ip,
			 accountId:accountId,
			 interval:interval
		},
		 success:function(jsonObject){
	         if(jsonObject.status != 1){
	        	 showMessageForFail(jsonObject.message);
	         }else{
             	showMessageForSuccess("修改成功");
             	$div.modal("toggle");
             	init_hostDetail();//页面初始化
	         }
	     },
	     error:function(result){
	    	showMessageForFail(result);
	     }
	});
}

/** 获取本地host redis上存储的信息 */
var getMessageEntity=function(ip){
	var messageEntity;
    $.ajax({ 
    	dataType:'JSON', 
    	type:"GET",
    	url:API.r_host_getMessageEntity,
    	data:{host:ip},
    	async:false,
    	success:function(jsonObject){
	    	if(jsonObject.status == 1&&jsonObject.data){
	    		messageEntity=jsonObject.data;
	    	}
    	}
   });
	return messageEntity;
}
/** 密码明文切换 */
var changePwdShowType=function(e){
	if($(e).prop("checked")==true){
		$("#signAndHookModal").find("input[name=hub],input[name=hook],input[name=bing]").attr("onfocus","").attr("type","text");
	}else{
		$("#signAndHookModal").find("input[name=hub],input[name=hook],input[name=bing]").attr("onfocus","this.type=\'password\'").attr("type","password");
	}
}

/** 账号列表 */
var showAccountModal=function(ip){
    var $div=$("#accountModal4clone").clone().attr("id","accountModal").appendTo($("body"));
    $div.find("#accountModalPage4clone").attr("id","accountModalPage");
    $div.find("#accountModal_host_IP").val(ip);
    $div.find("[name=accountModal_host]").text(ip);
    //根据
	$div.modal("toggle");
	$div.on('hidden.bs.modal', function () {
		//关闭窗口清除model 刷新页面
		$div.remove();
		init_hostDetail();
	});
    showAccountList(0);
}

var changeAccountTabInit=function(accountType){
	//下次打开时默认选中上一次选择的TAB页
	$("#accountModal4clone,#accountModal").find("#defaultAccountType").val(accountType);
    showAccountList(0);
}

