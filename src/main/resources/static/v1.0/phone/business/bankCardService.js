currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};

//出款银行卡 在用 代码开始
currentPageLocation = window.location.href;
var authRequest = { "pageNo":0,"pageSize":$.session.get('initPageSize')};
var Outward_trHtml='<tr class="noLeftRightPadding">\
				<td><input type="checkbox" name="checkboxAccId" value="{id}"/></td>\
                <td><span>{handicapName}</span></td>\
				<td><span>{currSysLevelName}</span></td>\
				<td><span>{alias}</span></td>\
                <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a></td>\
				<td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
				<td><div class="SysLogEvent" target="{id}"><span class="amount">{balance}</span></div></td>\
				<td><span class="platAcked" accountStatInOutId="{id}">{platAcked}</span></td>\
                <td><span class="bankAcked" accountStatInOutId="{id}">{bankAcked}</span></td>\
                <td>\
                    <a {outCountMappingEle} target="_self" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
                    <a {outCountMappedEle}  target="_self" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
                    <a {outCountCancelEle}  target="_self" accountStatInOutCategory="{accountStatInOutCategoryOutMember}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-danger" title="待排查">{outCountCancel}</span></a>\
                    <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
                </td>\
			    <td>\
				    <a {inCountMappingEle} target="_self" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
				    <a {inCountMappedEle}  target="_self" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
				    <a {inCountCancelEle}  target="_self" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
				    <span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId={id}>{matchingAmtIn}</span>\
				</td>\
                <td><span>{holderStr}</span></td>\
				<td><span>{flagStr}</span></td>\
                <td>'
                    +'<button class="btn btn-xs btn-white {recycleBntClass} btn-bold  " onclick="showRecycleModal({id},{transBlackTo},Outward_refreshContent)"><i class="ace-icon fa fa-reply bigger-100 {recycleIconClass}"></i><span>回收</span></button>'
                    +'<button class="btn btn-xs btn-white btn-warning btn-bold orange " onclick="showStopTempModal({id},Outward_refreshContent)"><i class="ace-icon fa fa-stop bigger-100 red"></i><span>停用</span></button>'
                    +'<button class="btn btn-xs btn-white btn-warning btn-bold orange " onclick="showFreezeModal({id},Outward_refreshContent)"><i class="ace-icon fa fa-remove bigger-100 red"></i><span>冻结</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange {updateClass}" onclick="showUpdateOutAccount({id},Outward_refreshContent)"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange {updateFlag2Class}"  onclick="showUpdateflag2Account({id},Outward_refreshContent)"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改限额</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange {OperatorLogBtn}" onclick="showModal_accountExtra({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>\
                </td>\
           </tr>';

function Outward_showContent(data,page){
    var $tbody = $('#Outward table').find('tbody').html('');
    var totalBalanceByBank =0 ,totalDailyOutward=0,idList=new Array,idArray= new Array();
    $.each(data,function(idx, obj) {
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
        totalDailyOutward = totalDailyOutward + obj.outwardAmountDaily;
        if(obj.flag&&obj.flag*1==2){
        	//返利网账号（flag=2)
        	obj.holderStr="返利网";
        	if(obj.peakBalance&&obj.peakBalance>0){
        		//余额峰值(保证金)大于0 可以修改限额
            	obj.updateFlag2Class='';
        	}else{
            	obj.updateFlag2Class=' hidden ';
        	}
        	//修改其它信息的按钮置空
        	obj.updateClass=' hidden ';
        }else{
        	//其它账号
        	obj.updateClass='';
        	obj.updateFlag2Class=' hidden ';
        }
        obj.flagStr=getFlagStr(obj.flag);
        obj.OperatorLogBtn=OperatorLogBtn;
        obj.recycleBntClass =   obj.transBlackTo?'btn-primary':'btn-warning';
        obj.recycleIconClass =  obj.transBlackTo?'blue':'red';
        obj.transBlackTo = obj.transBlackTo?obj.transBlackTo:'0';
        obj.DailyAmount =htmlDailyAmount(1,obj.limitOut,obj.outwardAmountDaily);
        obj.incomeRequestStatusMatching=incomeRequestStatusMatching+'';
        obj.incomeRequestStatusMatched=incomeRequestStatusMatched+'';
        obj.incomeRequestStatusCanceled=incomeRequestStatusCanceled+'';
        obj.outwardTaskStatusDeposited =outwardTaskStatusDeposited+'';
        obj.outwardTaskStatusMatched=outwardTaskStatusMatched+'';
        obj.outwardTaskStatusFailure=outwardTaskStatusFailure+'';
        obj.inCountMapped=!!!obj.inCount.mapped?"0":obj.inCount.mapped;
        obj.inCountMapping=!!!obj.inCount.mapping?"0":obj.inCount.mapping;
        obj.inCountCancel=!!!obj.inCount.cancel?"0":obj.inCount.cancel;
        obj.outCountMapped=!!!obj.outCount.mapped?"0":obj.outCount.mapped;
        obj.outCountMapping=!!!obj.outCount.mapping?"0":obj.outCount.mapping;
        obj.outCountCancel=!!!obj.outCount.cancel?"0":obj.outCount.cancel;
        obj.inCountMappedEle=(obj.inCount.mapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.inCountMappingEle=(obj.inCount.mapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.inCountCancelEle=(obj.inCount.cancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+obj.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountMappedEle=(obj.outCount.mapped!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+obj.id+'&outwardTaskStatus='+outwardTaskStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountMappingEle=(obj.outCount.mapping!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+obj.id+'&outwardTaskStatus='+outwardTaskStatusDeposited+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outCountCancelEle=(obj.outCount.cancel!=0)?(' href="#/EncashCheck4Outward:*?fromAccountId='+obj.id+'&outwardTaskStatus='+outwardTaskStatusFailure+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
        obj.outwardAmountDaily = obj.outwardAmountDaily+'';
        obj.balance = obj.balance?obj.balance:'0';
        obj.limitBalanceIcon=getlimitBalanceIconStr(obj);
        obj.accountStatInOutCategoryIn = accountStatInOutCategoryIn+'';
        obj.accountStatInOutCategoryOutMember=accountStatInOutCategoryOutMember+'';
        obj.alias =$.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName =obj.currSysLevelName?obj.currSysLevelName:'';
        obj.handicapName =obj.handicapName?obj.handicapName:'';
        obj.hideAccount=hideAccountAll(obj.account);
        obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.owner?obj.owner:'无')+'</br>'+(obj.hideAccount?obj.hideAccount:"无" );
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.matchingAmtOut ='';
        obj.matchingAmtIn ='';
        obj.platAcked ='---';
        obj.bankAcked ='---';
		obj.sysBalance = '---';
        idList.push({'id':obj.id});
		idList.push({'id':obj.id,type:'transAskMonitorRiskHover'});
        idArray.push(obj.id);
    });
    $tbody.html(fillDataToModel4Array(data,Outward_trHtml));
    if(data){
        loadHover_accountInfoHover(idList);
    	showSubAndTotalStatistics4Table($tbody,{column:14, subCount:data.length,count:page.totalElements,6:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance}});
    	loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutMember],idArray);
        SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
    }
   
}

function Outward_refreshContent(pageNo){
	var $div=$("#Outward");
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($div.find("#Outward_accountPage .Current_Page").text()?$div.find("#Outward_accountPage .Current_Page").text()-1:0);
    authRequest.pageNo = authRequest.pageNo<0?0:authRequest.pageNo;
    authRequest.search_IN_handicapId = $div.find("select[name='search_EQ_handicapId']").val().toString();
    authRequest.typeToArray= [accountTypeOutBank].toString();
    authRequest.statusToArray= [accountStatusNormal].toString();
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.currSysLevel=$div.find("input[name='currSysLevel']:checked").val();
    authRequest.search_NOTEQ_flag=0;//不查PC
	if($div.find("[name=holderType]:checked").length==1){
		authRequest.holderType=$("[name=holderType]:checked").val();
	}else{
		$div.find("[name=holderType]").attr("checked","checked");
		authRequest.holderType=null;
	}
	authRequest.transBlackTo=1;
    $.ajax({ dataType:'json', type:"get",url:API.r_account_list,data:authRequest, success:function(jsonObject){
        if(jsonObject.status == 1){
            Outward_showContent(jsonObject.data,jsonObject.page);
            showPading(jsonObject.page,"Outward_accountPage",Outward_refreshContent,null,false,true);
        }else {
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){ bootbox.alert(result);},initPage:function(his){
        var $form = $div.find('#Outward_accountFilter');
        $form.find('input[name=search_LIKE_account]').val(his.search_LIKE_account);
        $form.find('input[name=search_LIKE_alias]').val(his.search_LIKE_alias);
        $form.find('input[name=search_LIKE_operator]').val(his.operator);
        $form.find('select[name=search_LIKE_bankType]').val(his.bankType);
    }});
}


function Outward_searchByFilter(){
	var $div=$("#Outward");
    authRequest.operator = $div.find("input[name='search_LIKE_operator']").val();
    authRequest.search_LIKE_alias = $div.find("input[name='search_LIKE_alias']").val();
    authRequest.search_LIKE_account = $div.find("input[name='search_LIKE_account']").val();
    authRequest.bankType=$.trim($div.find("select[name='search_LIKE_bankType']").val());
	if(!authRequest.bankType||authRequest.bankType=='请选择'){
		authRequest.bankType = null;
	}
    Outward_refreshContent(0);
}
bootbox.setLocale("zh_CN");

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});

var selectAllOrNotAll = function(obj){
	var checked = obj.checked;
	$('input[name=checkboxAccId]').attr('checked',checked);
};

var Outward_saveRecycleByBatch = function(){
	var array = [];
	$('input[name=checkboxAccId]:checked').each(function(index,obj){
		array.push(obj.value);
	});
	if(array && array.length ==0){
		showMessageForFail('请勾选要批量回收的账号.');
	    return;
	}
	var tips ="<span class='red'>将停止下发给该账号,当金额过低时,将自动回收到可用状态,(自动回收,只对机器出款有效)</span>";
	bootbox.dialog({
		message: "<span class='bigger-110'>确定批量回收账号（转可用）<br/>"+tips+"</span>",
		buttons:{
			"click" :{"label" : "回收","className" : "btn-sm btn-primary","callback": function() {
				$.ajax({ type:"post", url:API.r_account_recycle4OutwardAccountByBatch,data:{"accArray":array.toString()},dataType:'json',success:function(jsonObject){
					if(jsonObject.status == 1){
						Outward_refreshContent();
					}else{
						showMessageForFail(jsonObject.message);
					}
				}});
			}},
			"click2" :{"label" : "取消","className" : "btn btn-sm btn-default"}
		}
	});
};



$("#Outward_accountFilter [name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
	Outward_searchByFilter();
});
$("#Outward_accountFilter [name=holderType],[name=currSysLevel]").click(function(){
	Outward_searchByFilter();
});

getHandicap_select($("#Outward_accountFilter select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("#Outward_accountFilter select[name='search_EQ_bankType']"),null,"全部");
getBankTyp_select($("#Outward_accountFilter select[name='search_LIKE_bankType']"),null,"全部");

//出款银行卡 在用 代码结束


//下发监控 代码开始
IDARRAY = [],IDARRAY_SYSEVENT = [];

var genAccHoverHtml = function(id,alias,acc,owner,bankType,rptTm){
    IDARRAY.push({'id':id});
    alias = _checkObj(alias);
    acc = _checkObj(acc);
    owner = _checkObj(owner);
    var ret = (alias ? alias: '无') + (acc ? '|'+(acc.substring(0, 3) + "**" + acc.substring(acc.length - 4)): '无') + (owner ? '|'+owner: '无')+(bankType ? '|'+bankType: '无');
    ret = ret == '无无无无'? '': ret;
    ret =  ret ? ('<a class="bind_hover_card breakByWord" data-toggle="accountInfoHover' + id + '" data-placement="auto left" data-trigger="hover">'+ ret +'</a>'):'';
    if(!rptTm){
        return ret;
    }
    return '<div class="BankLogEvent" target="'+id+'"><span class="amount">'+ret+'</span><span class="time"></span></div>';
};

var genAsignTimeHtml = function(asignTime){
    var tm =  timeStamp2yyyyMMddHHmmss(asignTime);
    return '<a class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="'+ tm +'" data-original-title="分配时间">'+ tm.substring(11, 19) +'</a></td>';
};

var genRemarkHtml = function(remark){
    return remark?('<a class="bind_hover_card breakByWord" title="备注信息" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="'+ remark +'">'+ remark.replace(/<br>/g,"").substring(0, 4) +'</a>'):'';
};

var genConsumeStype = function(currMillis,time){
    var consumeMins = (currMillis - time)/(60000);
    return (consumeMins >=5)?'color: white;background-color: indianred':(consumeMins>=2.5 && consumeMins <5?'color: white;background-color: limegreen':'');
};

var TRANS_DETAIL = null , TRANS_NEED = null;

var outNeedAmt = 0,outNeedNum = 0,outTotalAmt = 0,outTotalNum = 0,inTotalAmt = 0,inTotalNum = 0,reTotalAmt = 0,reTotalNum = 0,conTotalAmt = 0,conTotalNum = 0;

var getMonitorStat=function () {
    $.ajax({
        dataType:'json',type:"PUT",async:false,url:API.r_accountMonitor_buildTrans,
        success:function(jsonObject){
            if(jsonObject.status == -1){
                showMessageForFail("操作失败："+jsonObject.message);
                return;
            }
            outNeedAmt = 0,outNeedNum = 0,outTotalAmt = 0,outTotalNum = 0,inTotalAmt = 0,inTotalNum = 0,reTotalAmt = 0,reTotalNum = 0,conTotalAmt = 0,conTotalNum = 0;
            drawCanvas( jsonObject.data.stat);
            TRANS_DETAIL =  jsonObject.data.detail;
            TRANS_NEED   = jsonObject.data.need;
            var href = $('div ul li.monitorStatTab.active a[data-toggle=tab]').attr('href');
            try{
                if(href == '#tabTransing'){
                    fillTransingContent();
                }else if(href == '#tabWaiting'){
                    fillWaitingContent();
                }else if(href == '#tabTransferFailure' || href == '#tabTransferSuccess'){
                    fillAckedContent(0,href);
                }
            }catch(e){
                console.info(e);
            }
            try{
                IDARRAY_SYSEVENT = [];
                if(TRANS_DETAIL && TRANS_DETAIL.length > 0){
                    TRANS_DETAIL.forEach(function(trans){
                        IDARRAY_SYSEVENT.push(trans.frId);
                        IDARRAY_SYSEVENT.push(trans.toId);
                    });
                }
                if(TRANS_NEED && TRANS_NEED.length > 0){
                    TRANS_NEED.forEach(function(trans){
                        IDARRAY_SYSEVENT.push(trans.toId);
                    });
                }
                if(IDARRAY_SYSEVENT && IDARRAY_SYSEVENT.length>0){
                    SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,IDARRAY_SYSEVENT);
                }
            }catch(e){
                console.info(e);
            }
        }
    });
};

var drawCanvas = function(msg){
    var outNeedAmt = 0,outNeedNum = 0,outTotalAmt = 0,outTotalNum = 0,inTotalAmt = 0,inTotalNum = 0,reTotalAmt = 0,reTotalNum = 0,conTotalAmt = 0,conTotalNum = 0;
    $.each(msg,function(index,record){
        if (record.accType == accountTypeOutBank){//the funds the out-account's balance.
            outNeedNum = outNeedNum + record.accNum;
            outNeedAmt = outNeedAmt + Math.abs(record.amount) / 10000;
            outTotalNum = outTotalNum + record.totalNum;
            outTotalAmt = outTotalAmt + record.totalBal / 10000;
        }else if(record.accType == accountTypeInBank){//the funds the in-account's balance
            inTotalNum = inTotalNum + record.totalNum;
            inTotalAmt = inTotalAmt + record.totalBal / 10000;
        }else if(record.accType == accountTypeReserveBank){//the funds the reserve-account's balance
            reTotalNum = reTotalNum + record.totalNum;
            reTotalAmt = reTotalAmt + record.totalBal / 10000;
        }else if(record.accType == accountTypeBindWechat || record.accType == accountTypeBindAli || record.accType == accountTypeThirdCommon || record.accType == accountTypeBindCommon){
            conTotalNum = conTotalNum + record.totalNum;
            conTotalAmt = conTotalAmt + record.totalBal / 10000;
        }
    });
    outNeedAmt = outNeedAmt.toFixed(2);
    outTotalAmt = outTotalAmt.toFixed(2);
    inTotalAmt = inTotalAmt.toFixed(2);
    reTotalAmt = reTotalAmt.toFixed(2);
    conTotalAmt = conTotalAmt.toFixed(2);
    var d1 = [
        { label: "银行卡数", data: [[0,conTotalNum] ,[1,reTotalNum],[2,inTotalNum],[3,outNeedNum],[4,outTotalNum]],color: '#68BC31' },
        { label: "金额万元", data: [[0,conTotalAmt] ,[1,reTotalAmt],[2,inTotalAmt],[3,outNeedAmt],[4,outTotalAmt]],  color: '#2091CF'}
    ];

    var previousPoint = null;
    var $tooltip = $("<div class='tooltip top in'><div class='tooltip-inner'></div></div>").hide().appendTo('body');

    $('#transCanvas').plot(d1,{
        series: {  bars: { show: true }},
        bars: {align: "center", barWidth: 0.5},
        grid: { hoverable: true },
        xaxis: {
            show: true, tickSize: 2,  axisLabelUseCanvas: true, axisLabelFontSizePixels: 14,
            axisLabelFontFamily: 'Verdana, Arial', axisLabelPadding: 10,
            ticks: [ [0,'下发卡 ( &nbsp;'+conTotalAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+conTotalNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;银行卡数&nbsp;）'],
                     [1,'备用卡 ( &nbsp;'+reTotalAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+reTotalNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;银行卡数&nbsp;）'],
                     [2,'入款卡 ( &nbsp;'+inTotalAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+inTotalNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;银行卡数&nbsp;）'],
                     [3,'出款卡 ( &nbsp;'+outNeedAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+outNeedNum+'&nbsp;张&nbsp;)</br>（&nbsp;当前所需金额&nbsp;/&nbsp;银行卡数&nbsp;）'],
                     [4,'出款卡 ( &nbsp;'+outTotalAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+outTotalNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;银行卡数&nbsp;）']
            ]
        }
    }).on('plothover', function (event, pos, item) {
        if(!item){
            $tooltip.hide();
            previousPoint = null;
            return
        }
        var dataIndex = item.dataIndex;
        var text = '';
        if(dataIndex == 4 ){
            text = '总金额&nbsp;:&nbsp;'+outTotalAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+outTotalNum+'&nbsp;张';
        }else if(dataIndex == 3 ){
            text = '所需金额&nbsp;:&nbsp;'+outNeedAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+outNeedNum+'&nbsp;张';
        }else if(dataIndex == 2 ){
            text = '总金额&nbsp;:&nbsp;'+inTotalAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+inTotalNum+'&nbsp;张';
        }else if(dataIndex == 1 ){
            text = '总金额&nbsp;:&nbsp;'+reTotalAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+reTotalNum+'&nbsp;张';
        }else if(dataIndex == 0 ){
            text = '总金额&nbsp;:&nbsp;'+conTotalAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+conTotalNum+'&nbsp;张';
        }
        if (previousPoint != item.seriesIndex) {
            previousPoint = item.seriesIndex;
            $tooltip.show().children(0).html(text);
        }
        $tooltip.css({top:pos.pageY + 10, left:pos.pageX + 10});
    });
};

var fillTransingContent = function(){
    IDARRAY = [];
    var $tab = $('#tabTransing');
    if(!TRANS_DETAIL || TRANS_DETAIL.length == 0){
        $tab.find('table tbody').html('');
        $tab.find('div.noDataTipsPage').removeClass('dsn');
        return;
    }
    var dataList = [], csl = '';
    $tab.find("input[name='currSysLevel1']:checked").each(function(){ csl = csl+''+this.value; });
    var accountType =  $.trim($tab.find('input[name=search_LIKE_accountType]:checked').val());
    var search_LIKE_bankType =$tab.find('select[name=search_LIKE_bankType]').val();
    var bankType = search_LIKE_bankType =='请选择'?'':search_LIKE_bankType;
    TRANS_DETAIL.forEach(function(trans){
        var check = true;
        if(check && csl && (trans.frCsl &&  csl.indexOf(trans.frCsl) == -1) && (trans.toCsl &&  csl.indexOf(trans.toCsl) == -1))
            check = false;
        if(check && accountType && trans.frType &&  accountType.indexOf(','+trans.frType+',') == -1 )
            check = false;
        if(check && bankType && ( trans.frBankType && trans.frBankType.indexOf(bankType) == -1) && ( trans.toBankType && trans.toBankType.indexOf(bankType) == -1))
            check = false;
        if(check)
            dataList.push(trans);
    });
    if(!dataList || dataList.length == 0 ){
        $tab.find('table tbody').html('');
        $tab.find('div.noDataTipsPage').removeClass('dsn');
        return;
    }
    var html = '';
    var currMillis = new Date().getTime(),subTotal =0;
    for(var index in dataList){
        var trans = dataList[index];
        subTotal = subTotal + trans.transAmt;
        trans.toCslName = !trans.toCsl?'未知':(trans.toCsl == 1?'外层':(trans.toCsl == 2?'内层':'指定层'));
        trans.frTypeName = !trans.frType?'未知':(trans.frType == accountTypeInBank?'入款卡':(trans.frType == accountTypeReserveBank?'备用卡':(trans.frType == accountTypeBindCustomer?'客户绑定卡':'下发卡')));
        html = html +'<tr>'
            + ' <td>'+ (1+parseInt(index)) +'</td>'
            + ' <td>'+ $.trim(trans.toHandiCapName) +'</td>'
            + ' <td>'+ trans.toCslName +'</td>'
            + ' <td>'+ trans.orderNo +'</td>'
            + ' <td>'+ trans.frTypeName +'</td>'
            + ' <td>'+ genAccHoverHtml(trans.frId,trans.frAlias,trans.frAcc,trans.frOwner,trans.frBankType,true) +'</td>'
            + ' <td>'+ genAccHoverHtml(trans.toId,trans.toAlias,trans.toAcc,trans.toOwner,trans.toBankType,true) +'</td>'
            + ' <td>'+ trans.transAmt +'</td>'
            + ' <td>'+ genAsignTimeHtml(trans.createTime) +'</td>'
            + ' <td style="'+genConsumeStype(currMillis,trans.createTime)+'">'+trans.timeConsume +'</td>'
            + ' <td>'+ genRemarkHtml(trans.remark) +'</td>'
            + ' <td><button type="button" onclick="openRemarkModal(\''+$.trim(trans.orderNo)+'\')" class="btn btn-xs btn-white btn-warning btn-bold"><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button></td>'
            + '</tr>';
    }
    var $tbody = $tab.find('table tbody');
    $tbody.html(html);
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:dataList.length,8:{subTotal:subTotal}});
    $tbody.find('tr:last').remove();
    $tab.find('div.noDataTipsPage').addClass('dsn');
    $("[data-toggle='popover']").popover();
    loadHover_accountInfoHover(IDARRAY);
};

var fillWaitingContent = function(){
    IDARRAY = [];
    var $tab = $('#tabWaiting');
    if(!TRANS_NEED){
        $tab.find('table tbody').html('');
        $tab.find('div.noDataTipsPage').removeClass('dsn');
        return;
    }
    var dataList = [], csl = '';
    $tab.find("input[name='currSysLevel2']:checked").each(function(){ csl = csl+''+this.value; });
    var search_LIKE_bankType =$tab.find('select[name=search_LIKE_bankType]').val();
    var bankType = search_LIKE_bankType =='请选择'?'':search_LIKE_bankType;
    TRANS_NEED.forEach(function(trans){
        var check = true;
        if(csl && (trans.toCsl &&  csl.indexOf(trans.toCsl) == -1))
            check = false;
        if(bankType && ( trans.toBankType && trans.toBankType.indexOf(bankType) == -1))
            check = false;
        if(check)
            dataList.push(trans);
    });
    if(!dataList || dataList.length == 0 ){
        $tab.find('table tbody').html('');
        $tab.find('div.noDataTipsPage').removeClass('dsn');
        return;
    }
    var html = '' ,subTotal =0;;
    for(var index in dataList){
        var trans = dataList[index];
        subTotal = subTotal + trans.transAmt;
        trans.hasTask = trans.priority == 4 ? '有任务':'无任务';
        trans.toCslName = !trans.toCsl?'未知':(trans.toCsl == 1?'外层':(trans.toCsl == 2?'内层':'指定层'));
        html = html + '<tr>'
            + ' <td>'+ (1+parseInt(index)) +'</td>'
            + ' <td>'+ $.trim(trans.toHandiCapName) +'</td>'
            + ' <td>'+ trans.toCslName+'</td>'
            + ' <td>'+ genAccHoverHtml(trans.toId,trans.toAlias,trans.toAcc,trans.toOwner,trans.toBankType,true) +'</td>'
            + ' <td>'+ trans.transAmt +'</td>'
            + ' <td>'+ genAsignTimeHtml(trans.createTime) +'</td>'
            + ' <td style="'+genConsumeStype(currMillis,trans.createTime)+'">'+ trans.timeConsume +'</td>'
            + ' <td>'+trans.hasTask+'</td>'
            + '</tr>';
    }
    var $tbody = $tab.find('table tbody');
    $tab.find('table tbody').html(html);
    $tbody.html(html);
    showSubAndTotalStatistics4Table($tbody,{column:8, subCount:dataList.length,5:{subTotal:subTotal}});
    $tbody.find('tr:last').remove();

    $tab.find('div.noDataTipsPage').addClass('dsn');
    $("[data-toggle='popover']").popover();
    loadHover_accountInfoHover(IDARRAY);
} ;

var html = '';
var currMillis = new Date().getTime();
var fillAckedContent = function(pageNo,tabId){
    tabId = !tabId?($('div ul li.monitorStatTab.active a[data-toggle=tab]').attr('href')):tabId;
    var classDsn = tabId=='#tabTransferFailure' ? 'dsn':'';
    var pageId = tabId.slice(1)+'Page';
    pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:($("#"+pageId+" .Current_Page").text()?$("#"+pageId+" .Current_Page").text()-1:0);
    pageNo = pageNo<0?0:pageNo;
    var pageSize=$.session.get('initPageSize');
    var $tab = $(tabId), csl = [],startTime=null,endTime=null;
    var search_LIKE_bankType =$tab.find('select[name=search_LIKE_bankType]').val();
    var bankType = search_LIKE_bankType =='请选择'?'':search_LIKE_bankType;
    var status = tabId != '#tabTransferFailure'?[incomeRequestStatusMatching,incomeRequestStatusMatched]:[incomeRequestStatusCanceled];
    var accref = $tab.find("input[name='account_outDrawing']").val();
    $tab.find("input[name='"+(tabId=='#tabTransferFailure' ?"currSysLevel3":"currSysLevel4")+"']:checked").each(function(){
        csl.push(this.value);
    });
    csl = (!csl || csl.length == 3)?([]):csl;
    var startAndEndTime = $tab.find("input[name='startAndEndTime']").val();
    if(startAndEndTime){
        var startAndEnd = startAndEndTime.split(" - ");
        startTime = $.trim(startAndEnd[0]);
        endTime = $.trim(startAndEnd[1]);
    }
    $.ajax({dataType:'json',url:API.r_accountMonitor_issueList,data:{pageSize:pageSize,pageNo:pageNo,status:status.toString(),startTime:startTime,endTime:endTime,level:csl.toString(),bankType:bankType,accRef:accref},success:function(jsonObject){
        if(jsonObject.status != 1){
            showMessageForFail("操作失败："+jsonObject.message);
            return;
        }
        IDARRAY = [];
        var data = jsonObject.data;
        var html = '',subTotalAmt = 0;
        $.each(data,function(idx, trans) {
            subTotalAmt = subTotalAmt + trans.amount;
            var currMillis =  trans.createtime + trans.timeconsuming*1000;
            trans.cslName = !trans.level?'未知':(trans.level == 1?'外层':(trans.level == 2?'内层':'指定层'));
            html = html + '<tr>'
                + ' <td>'+ $.trim(trans.handicapName) +'</td>'
                + ' <td>'+ trans.cslName +'</td>'
                + ' <td>'+ $.trim(trans.order_no) +'</td>'
                + ' <td>'+ $.trim(trans.frtypename) +'</td>'
                + ' <td>'+ genAccHoverHtml(trans.frId,trans.fralias,trans.fraccount,trans.frowner,trans.frbanktype) +'</td>'
                + ' <td>'+ genAccHoverHtml(trans.toId,trans.toalias,trans.toaccount,trans.toowner,trans.tobanktype) +'</td>'
                + ' <td>'+ trans.amount +'</td>'
                + ' <td>'+ genAsignTimeHtml(trans.createtime) +'</td>'
                + ' <td style="'+genConsumeStype(currMillis,trans.createtime)+'">'+ trans.timeconsumingFmt +'</td>'
                + ' <td>'+ genRemarkHtml(trans.remark) +'</td>'
                + ' <td><button type="button" onclick="openAckRemModal(\''+$.trim(trans.id)+'\')" class="btn btn-xs btn-white btn-warning btn-bold"><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>&nbsp;&nbsp;<button type="button" onclick="cancelTransAck(\''+$.trim(trans.id)+'\',\''+trans.amount+'\')" class="'+(classDsn=='dsn'?'dsn':'btn btn-xs btn-white btn-warning btn-bold')+'"><i class="ace-icon fa fa-reply bigger-100 orange"></i>撤销</button></td>'
                + '</tr>';
        });
        var $tbody = $tab.find('table tbody');
        $tbody.html(html);
        showSubAndTotalStatistics4Table($tbody,{column:11, subCount:data.length,count:jsonObject.page.totalElements,7:{subTotal:subTotalAmt,total:jsonObject.page.header.totalTransAmount}});
        loadHover_accountInfoHover(IDARRAY);
        showPading(jsonObject.page,pageId,fillAckedContent,null,false,false);
        $("[data-toggle='popover']").popover();
    }});
};

var openRemarkModal = function(orderNo){
    var $Msg =  $('#transMonitorMessageModal');
    $Msg.find('#messageCont').val('');
    $Msg.find('input[name=orderNo]').val(orderNo);
    $Msg.modal('show');
};

var remark4TransLock = function(){
    var $Msg =  $('#transMonitorMessageModal');
    var remark = $Msg.find('#messageCont').val();
    var orderNo =  $Msg.find('input[name=orderNo]').val();
    if(!remark || !orderNo){
        showMessageForFail('参数不能为空');
        return;
    }
    $.ajax({dataType:'json',data:{ orderNo:orderNo,remark:remark},url:API.r_accountMonitor_remark4TransLock,success:function(jsonObject){
        if(jsonObject.status == -1){
            showMessageForFail("操作失败："+jsonObject.message);
            return;
        }
        $Msg.modal('hide');
        $Msg.find('#messageCont').val('');
        $Msg.find('input[name=orderNo]').val('');
        getMonitorStat();
    }});
};


var openAckRemModal = function(reqId){
    var $Msg =  $('#transMonitorAckedMsgModal');
    $Msg.find('#messageCotent').val('');
    $Msg.find('input[name=reqId]').val(reqId);
    $Msg.modal('show');
};

var remark4TransReq = function(){
    var $Msg =  $('#transMonitorAckedMsgModal');
    var remark = $Msg.find('#messageCotent').val();
    var reqId =  $Msg.find('input[name=reqId]').val();
    if(!remark || !reqId){
        showMessageForFail('参数不能为空');
        return;
    }
    $.ajax({dataType:'json',data:{ reqId:reqId,remark:remark},url:API.r_accountMonitor_remark4TransReq,success:function(jsonObject){
        if(jsonObject.status == -1){
            showMessageForFail("操作失败："+jsonObject.message);
            return;
        }
        $Msg.modal('hide');
        $Msg.find('#messageCont').val('');
        $Msg.find('input[name=orderNo]').val('');
        fillAckedContent();
    }});
};

var cancelTransAck = function(reqId,amt){
    bootbox.dialog({
        message: "<span class='bigger-120'>确定撤销该订单&nbsp;(&nbsp;金额&nbsp;:&nbsp;"+amt+"&nbsp;元&nbsp;)&nbsp;?</span>",
        buttons:{
            "click1":{"label":"确定","className":"btn btn-sm btn-primary","callback": function(){
                $.ajax({dataType:'json',data:{reqId:reqId},url:API.r_accountMonitor_cancelTransAck,success:function(jsonObject){
                    if(jsonObject.status == -1){
                        showMessageForFail("操作失败："+jsonObject.message);
                        return;
                    }
                    fillAckedContent();
                }});
            }},
            "click2":{"label":"取消","className":"btn btn-sm btn-primary","callback": function(){}}
        }
    });
};
var showModal_AsignInfo=function(){
	var $div=$("#AsginInfoModal").modal("toggle");
	//重置参数
	resetAsignInfo();
}
var resetAsignInfo=function(){
	var $out_need_widget=$("#AsginInfoModal");
    var zone_options="";
    zone_options+="<option value='' >"+("-----请选择-----")+"</option>";
    $.each(zone_list_all,function(index,record){
        if(record.id == getCookie('JUSERZONE')){
            zone_options+="<option value="+record.id+" code="+record.code+" selected=\"selected\" >"+record.name+"</option>";
        }else{
            zone_options+="<option value="+record.id+" code="+record.code+" >"+record.name+"</option>";
        }
    });
    $out_need_widget.find("select[name='zone_sync']").html(zone_options);
}


var saveAsignInfo=function(){
	var $out_need_widget=$("#AsginInfoModal");
    var zone = $out_need_widget.find("select[name='zone_sync']").val();
    if(!zone){
        showMessageForFail('请选择区域.');
        return;
    }
	bootbox.confirm("确定修改系统设置?", function(result) {
		if (result) {
		    var on1stop0 = $out_need_widget.find("[name=out_need_type]:checked").val();
		    var triglimit = $out_need_widget.find("[name=out_need_trig_limit]").val();
		    var uplimit = $out_need_widget.find("[name=out_need_up_limit]").val();
		    var lastTime = $out_need_widget.find("[name=out_need_last_time]").val();
		    $.ajax({
		        type: 'PUT', url: '/r/set/saveOutUpLim', data: {
		            on1stop0: on1stop0, uplimit: uplimit, lastTime: lastTime,triglimit:triglimit,zone:zone
		        }, dataType: 'json', success: function (res) {
		            if (res && res.status == 1) {
		                showMessageForSuccess("保存成功");
		                resetAsignInfo();
		            } else {
		                showMessageForFail(res.message);
		            }
		        }
		    });
		}
	});
}
initTimePicker(true,$("[name=startAndEndTime]"),7);
initRefreshSelect($("#refreshMonitorTransferSelect"),$("#refreshMonitorStatEvent"),null,"refresh_MonitorTransfer");

genBankTypeHtml('tabTransing_bankType');
genBankTypeHtml('tabWaiting_bankType');
genBankTypeHtml('tabTransferFailure_bankType');
genBankTypeHtml('tabTransferSuccess_bankType');
//下发监控 代码结束


//下发银行卡 代码开始
var IncomeAccountIssue_trHtml = '<tr>\
	<td><span>{handicapName}</span></td>\
	<td><span>{currSysLevelName}</span></td>\
	<td><span>{alias}</span></td>\
	<td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"  >{accountInfo}</a></td>\
	<td>{typeStr}</td>\
	<td><span class="label label-sm {classOfStatus}">{statusName}</span></td>\
	<td>{flagStr}</td>\
	<td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span><span class="time"></span></div></td>\
	<td><div class="SysLogEvent" target="{id}"><span class="amount">{balance}</span></div></td>\
	<td><span class="bankAcked" accountStatInOutId="{id}">{bankAcked}</span></td>\
	<td>\
		<a {inCountMappingEle} target="_self" accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{inCountMapping}</span></a>\
		<a {inCountMappedEle} target="_self"  accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{inCountMapped}</span></a>\
		<a {inCountCancelEle} target="_self"  accountStatInOutCategory="{accountStatInOutCategoryIn}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{inCountCancel}</span></a>\
		<span style="display:block;width:100%;" class="matchingAmtIn" accountStatInOutId={id}>{matchingAmtIn}</span>\
	</td>\
	<td>\
	 <a {outCountMappingEle} target="_self" accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapping"><span class="badge badge-warning" title="匹配中">{outCountMapping}</span></a>\
	 <a {outCountMappedEle} target="_self"  accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="mapped"><span class="badge badge-success" title="已匹配">{outCountMapped}</span></a>\
	 <a {outCountCancelEle} target="_self"  accountStatInOutCategory="{accountStatInOutCategoryOutTranfer}" accountStatInOutId="{id}" accountStatInOut="cancel"><span class="badge badge-inverse" title="已驳回">{outCountCancel}</span></a>\
	 <span style="display:block;width:100%;" class="matchingAmtOut" accountStatInOutId={id}>{matchingAmtOut}</span>\
	</td>\
	<td>'
		+'<button class="btn btn-xs btn-white btn-bold btn-warning  {classHideOperate} {updateClass}" onclick="showUpdateBindAccount({id},IncomeAccountIssue_showAccountList)" ><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改</span></button>'
		+'<button class="btn btn-xs btn-white btn-bold btn-warning  {classHideOperate} {updateFlag2Class}" onclick="showUpdateflag2Account({id},IncomeAccountIssue_showAccountList)" ><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>修改限额</span></button>'
		+'<button class="btn btn-xs btn-white btn-bold btn-warning  {classHideOperate}" onclick="showChangeStatusModal({id},IncomeAccountIssue_showAccountList)" ><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>状态</span></button>'
		+'<button class="btn btn-xs btn-white btn-bold btn-info {OperatorLogBtn}" onclick="showModal_accountExtra({id})" ><i class="ace-icon fa fa-list bigger-100 blue"></i><span>操作记录</span></button>'
		+'<button class="btn btn-xs btn-white btn-bold btn-info" onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 blue"></i><span>明细</span></button>'
		+'<button class="btn btn-xs btn-white btn-bold btn-info {classHiddenOfBtnBindList}" onclick="showBindAccountListModal({id})" ><i class="ace-icon fa fa-sitemap bigger-100 blue"></i><span>绑定记录</span></button>'
	+'</td>\
</tr>';


/**
* 根据账号Type拼接对应数据
*/
var IncomeAccountIssue_showAccountList=function(CurPage){
var $div = $("#accountFilter3");
if(!!!CurPage&&CurPage!=0) CurPage=$("#IncomeAccountIssue_accountPage .Current_Page").text();
authRequest.pageNo = CurPage<=0?0:CurPage-1;
authRequest.search_LIKE_account = $.trim($div.find("input[name='search_LIKE_account']").val());
authRequest.search_LIKE_owner = $.trim($div.find("input[name='search_LIKE_owner']").val());
authRequest.bankType=$.trim($div.find("select[name='search_LIKE_bankType']").val());
if(!authRequest.bankType||authRequest.bankType=='请选择'){
    authRequest.bankType = null;
}
authRequest.search_LIKE_alias = $.trim($div.find("input[name='search_LIKE_alias']").val());
authRequest.search_IN_handicapId = $div.find("select[name='search_EQ_handicapId']").val().toString();
var statusToArray = new Array();
$div.find("input[name='search_IN_status']:checked").each(function(){  statusToArray.push(this.value); });
if(statusToArray.length==0){
    statusToArray=[accountStatusNormal,accountStatusStopTemp];
}
authRequest.statusToArray = statusToArray.toString(),
authRequest.typeToArray = [accountTypeBindWechat,accountTypeBindAli,accountTypeThirdCommon,accountTypeBindCommon].toString();
authRequest.sortProperty='status';
authRequest.sortDirection=0;
authRequest.pageSize=$.session.get('initPageSize');
authRequest.currSysLevel=$div.find("input[name='currSysLevel']:checked").val();
$.ajax({ dataType:'JSON',type:"POST", async:false, url:API.r_account_list, data:authRequest,success:function(jsonObject){
		if(jsonObject.status !=1){
			showMessageForFail("查询失败："+jsonObject.message);
			return;
		}
		var $tbody=$("table#accountListTable").find("tbody");
        var totalBalanceBySys = 0,totalBalanceByBank =0 ,totalIncomeAmountDaily=0;
		$.each(jsonObject.data,function(index,record){
			record.flagStr=getFlagStr(record.flag);
	        if(record.flag&&record.flag*1==2){
	        	//返利网账号（flag=2)
	        	if(record.peakBalance&&record.peakBalance>0){
	        		//余额峰值(保证金)大于0 可以修改状态与限额
	        		record.updateFlag2Class='';
	        	}else{
	        		record.updateFlag2Class=' hidden ';
	        	}
	        	//修改其它信息的按钮置空
	        	record.updateClass=' hidden ';
	        }else{
	        	//其它账号
	        	record.updateClass='';
	        	record.updateFlag2Class=' hidden ';
	        }
	        record.OperatorLogBtn=OperatorLogBtn;
			record.handicapName=record.handicapName?record.handicapName:'';
			record.alias=(record.alias&&record.alias!='null')?record.alias:'';
			record.currSysLevelName=record.currSysLevelName?record.currSysLevelName:'';
			record.classOfStatus=(record.status==accountStatusFreeze || record.status==accountStatusStopTemp)?'label-danger':((record.status ==accountStatusEnabled)?'label-purple':'label-success');
			record.limitIn=(!!!record.limitIn)?eval(sysSetting.INCOME_LIMIT_CHECKIN_TODAY):record.limitIn;
			record.DailyAmount=htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily);
			record.classHiddenOfBtnBindList =  record.type==accountTypeThirdCommon ?'hidden':'';
			totalIncomeAmountDaily+=record.incomeAmountDaily*1;
			totalBalanceBySys+=record.balance*1;
			totalBalanceByBank+=record.bankBalance*1;
			record.balance = record.balance?record.balance:'0';
			record.limitBalanceIcon=getlimitBalanceIconStr(record);
			record.bankBalance=record.bankBalance?record.bankBalance:'0';
			record.incomeDetail=getRecording_Td(record.id,"incomeDetail");
			record.outDetail=getRecording_Td(record.id,"outDetail");
			record.inCountMapping ='0';
			record.inCountMapped ='0';
			record.inCountCancel ='0';
			record.matchingAmtIn ='';
			record.outCountMapping ='0';
			record.outCountMapped ='0';
			record.outCountCancel ='0';
			record.matchingAmtOut ='';
			record.bankAcked='---';
			record.accountStatInOutCategoryOutTranfer=accountStatInOutCategoryOutTranfer+'';
			record.accountStatInOutCategoryIn=accountStatInOutCategoryIn+'';
			record.inCountMappingEle=(record.inCountMapping!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
			record.inCountMappedEle=(record.inCountMapped!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
			record.inCountCancelEle=(record.inCountCancel!=0)?(' href="#/EncashCheck4Transfer:*?toAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
			record.outCountMappingEle=(record.outCountMapping!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatching+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
			record.outCountMappedEle=(record.outCountMapped!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusMatched+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
			record.outCountCancelEle=(record.outCountCancel!=0)?('href="#/EncashStatus4Transfer:*?fromAccountId='+record.id+'&incomeReqStatus='+incomeRequestStatusCanceled+'" style="text-decoration:none;" '):(' style="cursor:not-allowed;text-decoration:none;" ');
			record.hideAccount=hideAccountAll(record.account);
			record.accountInfo =record.bankType+'|'+hideName(record.owner)+'<br/>'+record.hideAccount ;
			if(record.status==accountStatusFreeze){
				record.classHideOperate=" hide ";
			}
		});
		$tbody.html(fillDataToModel4Array(jsonObject.data,IncomeAccountIssue_trHtml));
		showSubAndTotalStatistics4Table($tbody,{column:13,subCount:jsonObject.data.length,count:jsonObject.page.totalElements,8:{subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance}});
		showPading(jsonObject.page,"IncomeAccountIssue_accountPage",IncomeAccountIssue_showAccountList);
		//账号悬浮提示
		var idList=new Array(),idArray = new Array();
		$.each(jsonObject.data,function(index,result){
			idList.push({'id':result.id});
			idList.push({'id':result.id,type:'transAskMonitorRiskHover'});
			idArray.push(result.id);
		});
		loadHover_accountInfoHover(idList);
	    loadEncashCheckAndStatus([accountStatInOutCategoryIn,accountStatInOutCategoryOutTranfer],idArray,null);
	SysEvent.on(SysEvent.EVENT_OFFLINE,bankLogOffline,idArray);
    }
});
}



getHandicap_select($("#accountFilter3 select[name='search_EQ_handicapId']"),0,"全部");
$("#accountFilter3").find("[name=currSysLevel],[name=search_IN_status],[name=search_IN_status]").click(function(){
IncomeAccountIssue_showAccountList();
});
$("#accountFilter3").find("[name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
IncomeAccountIssue_showAccountList();
});
$("#accountFilter3").keypress(function(e){
if(event.keyCode == 13) {
	$("#accountFilter3 #searchBtn button").click();
}
});
getBankTyp_select($("#accountFilter3 select[name='search_LIKE_bankType']"),null,"全部")
//下发银行卡 代码结束


//手机监控 代码开始
var monitorMobile_trHtml='<tr class="noLeftRightPadding">\
                   <td><span>{handicapName}</span></td>\
				   <td><span>{currSysLevelName}</span></td>\
				   <td><span>{mobile}</span><span target="{id}" class="time4St bal label label-grey" style="display:block;width:99%;float:left;" title="">离线</span></td>\
                   <td><a class="bind_hover_card" data-toggle="accountInfoHover{id}" data-placement="auto right" data-trigger="hover"><span>{accountInfo}</span></a><span class="errorAlarm error{id}">{error}</span><span target="{id}" class="time4St flow label label-grey" style="display:block;width:99%;float:left;" title="">&nbsp;&nbsp;</span></td>\
                   <td><span>{typeStr}</span></td>\
                   <td><span class="mode4Acc" target="{id}">默认</span></td>\
                   <td><span class="label label-sm {classOfStatus}">{statusName}</span></td>\
                   <td><span>{peakBalance}</span></td>\
                   <td><div class="BankLogEvent" target="{id}"><span class="amount">{bankBalance}{DailyAmount}</span></div></td>\
                   <td>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showLogin({id})"><i class="ace-icon fa fa-pencil-square-o bigger-100 orange"></i><span>登录</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showConciliate({id});" ><i class="ace-icon fa fa-check bigger-100 orange"></i><span>对账</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showReAck({id});" ><i class="ace-icon fa fa-reply bigger-100 orange"></i><span>任务回收</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showLogDate({id});" ><i class="ace-icon fa fa-download bigger-100 orange"></i><span>日志</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showUpdPWD({id});" ><i class="ace-icon fa fa-asterisk bigger-100 orange"></i><span>密码</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="showSetting4ChangeModel({id});" ><i class="ace-icon fa fa-cog bigger-100 orange"></i><span>设置</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="doScreen({id});" ><i class="ace-icon fa fa-film bigger-100 orange"></i><span>截屏</span></button>'
                    +'<button class="btn btn-xs btn-white btn-primary btn-bold orange"  onclick="showInOutListModal({id})"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>'
                    +'<button class="dsn" smstarget="{id}" onclick="showSMS({id},\'{mobile}\',\'{accountInfo}\')"><i class="ace-icon fa fa-envelope-o bigger-100 orange"></i><span>短信</span></button>'
                    +'</td>\
               </tr>';

var showLogin = function(accId){
    bootbox.confirm("确定重新登陆&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/cabana/login',data: {"accId":accId},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                }
            }});
        }
    });
};

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

var showReAck = function(accId){
    bootbox.confirm("确定进行回收任务&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/cabana/reAck',data: {"accId":accId},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                }
            }});
        }
    });
};

var showLogs = function(accId){
    bootbox.confirm("确定同步该账号的日志&nbsp;?&nbsp;", function(result) {
        if (result) {
            $.ajax({type: 'get',url: '/r/cabana/logs',data: {"accId":accId},dataType: 'json',success: function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                }
            }});
        }
    });
};

/** 已绑账号密码弹出框 */
var showUpdPWD = function (id) {
    var $div = $("#updatePWD4clone").clone().attr("id", "updatePWD");
    $div.find("#accId").val(id);
    $div.find("td").css("padding-top", "10px");
    var accInfo = getAccountInfoById(id);
    if(accInfo.flag&&accInfo.flag ==2){
        showMessageForFail('返利网账号不能修改密码信息')
        return;
    }
    $div.modal("toggle");
    if(accInfo){
        $div.find("[name=bankHide]").val(accInfo.alias+'-'+accInfo.bankType);
        $div.find("[name=signBank]").attr("placeholder",accInfo.sign?"********":"");//登陆账号
        $div.find("[name=hookBank]").attr("placeholder",accInfo.hook?"********":"");//登陆密码
        $div.find("[name=hubBank]").attr("placeholder",accInfo.hub?"********":"");//交易密码
    }else{
        $div.find("[name=signBank],[name=hookBank],[name=hubBank]").attr('disabled','disabled');
    }
    $div.on('hidden.bs.modal', function () { $div.remove();});
};

/** 修改已绑账号密码 */
var doUpdPWD=function(){
    var $div=$("#updatePWD");
    bootbox.confirm("确定修改密码信息?", function (result) {
        if (result) {
            var params = {
                accountId:$div.find("#accId").val(),
                sign:$.trim($div.find("[name=signBank]").val(),true),
                hook:$.trim($div.find("[name=hookBank]").val(),true),
                hub:$.trim($div.find("[name=hubBank]").val(),true)
            };
            $.ajax({type:'get',url:'/r/host/alterPWD4Trans',data: params,dataType:'json',success:function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                    $div.modal("toggle");
                } else {
                    showMessageForFail("修改失败：" + res.message);
                }
            }});
        }
    });
};

var doScreen=function(accId){
    bootbox.confirm("确定截图该信息?", function (result) {
        if (result) {
            $.ajax({type:'get',url:'/r/cabana/screen',data:{accId:accId},dataType:'json',success:function (res) {
                if (res.status==1) {
                    showMessageForSuccess('操作成功');
                    $div.modal("toggle");
                } else {
                    showMessageForFail("修改失败：" + res.message);
                }
            }});
        }
    });
};

function monitorMobile_showContent(data,page){
    var $tbody = $('#dynamic-table').find('tbody').html('');
    var  totalBalanceByBank =0 ,idList=new Array,idArray = new Array();
    $.each(data,function(idx, obj) {
        totalBalanceByBank = totalBalanceByBank + obj.bankBalance ;
        obj.classOfStatus=(obj.status == accountStatusFreeze ||obj.status == accountStatusStopTemp)?'label-danger':(obj.status == accountStatusEnabled?'label-warning':'label-success');
        obj.DailyAmount =htmlDailyAmount(1,obj.limitOut,obj.outwardAmountDaily);
        obj.balance = obj.balance?obj.balance:'0';
        obj.bankBalance=obj.bankBalance?obj.bankBalance:'0';
        obj.bankName = obj.bankName?obj.bankName:'';
        obj.bankType = obj.bankType?obj.bankType:'';
        obj.bankName = obj.type==accountTypeOutThird?obj.bankName:(obj.bankType?obj.bankType:obj.bankName);
        obj.owner=obj.owner?obj.owner:'';
        obj.alias =$.trim(obj.alias)?obj.alias:'';
        obj.currSysLevelName =obj.currSysLevelName?obj.currSysLevelName:'';
        obj.handicapName =obj.handicapName?obj.handicapName:'';
        obj.isThird = obj.type==accountTypeOutThird;
        obj.hideAccount=hideAccountAll(obj.account);
        obj.accountInfo =(obj.bankType?obj.bankType:'无')+'|'+(obj.alias?obj.alias:'无')+'|'+(obj.hideAccount?obj.hideAccount:'无')+'|'+(obj.owner?obj.owner:'无') ;
        obj.error = '';
        obj.peakBalance = obj.peakBalance?obj.peakBalance:'0';
        idList.push({'id':obj.id,'type':(obj.type==accountTypeOutThird||obj.type==accountTypeInThird?'third':'Bank')});
        idArray.push(obj.id);
    });
    $tbody.html(fillDataToModel4Array(data,monitorMobile_trHtml));
    loadHover_accountInfoHover(idList);
    if(!page){
        page = {totalElements:0,header:{totalAmountBankBalance:0}};
    }
    showSubAndTotalStatistics4Table($tbody,{column:12, subCount:data.length,count:page.totalElements,9:{subTotal:totalBalanceByBank,total:page.header.totalAmountBankBalance}});
    rereshAccSt(idArray);
    contentRight();
}

var rereshAccSt = function(idList){
    $.ajax({type:'get',url:'/r/cabana/status',data: {accIdArray:idList.toString()},dataType:'json',success:function (res) {
        if (res.status==1&&res.data&&res.data.length>0) {
            var nowTm = new Date().getTime();
            $('span.errorAlarm').html('');
            $('span.time4St.bal').text('离线').attr('class','time4St bal label label-grey').attr('style','display:block;width:99%;float:left;');
            $('span.time4St.flow').html('&nbsp;&nbsp;').attr('class','time4St flow label label-grey').attr('style','display:block;width:99%;float:left;');
            res.data.forEach(function(val,index) {
                if(val.error){
                    var error = '';
                    error = error + '<span class="badge badge-transparent tooltip-error" title="'+val.error+'" onclick="doError('+val.id+',\''+val.error+'\');">';
                    error = error + '  <i class="ace-icon fa fa-exclamation-triangle red bigger-130"></i>';
                    error = error + '</span>';
                    $('span.errorAlarm.error'+val.id).html(error);
                }
                {
                    var classInf = val.time ==0||(val.time > 0 && (nowTm-val.time)< 180000)?'label-success':'label-warning';
                    if(val.time && val.time > 0 &&((nowTm-val.time)>= 600000)){
                        classInf = 'label-danger';
                    }
                    $('span.time4St.bal[target='+val.id+']').each(function(){
                        $(this).text(val.time >0?(geeTime4Crawl(val.time)):'已连接').attr('class','time4St bal label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
                    });
                }
                {
                    var classInf = val.logtime ==0||(val.logtime > 0 && (nowTm-val.logtime)< 180000)?'label-success':'label-warning';
                    if(val.logtime && val.logtime > 0 &&((nowTm-val.logtime)>= 600000)){
                        classInf = 'label-danger';
                    }
                    $('span.time4St.flow[target='+val.id+']').each(function(){
                        $(this).html(val.logtime >0?(geeTime4Crawl(val.logtime)):'&nbsp;&nbsp;').attr('class','time4St flow label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
                    });
                }
                $('span.mode4Acc[target='+val.id+']').text(((val.time||val.logtime)?(val.mode ==1 ?'抓流水':(val.mode ==2?'转账':'默认')):'---'));
                {
                    var flag =  val.flag;//1-simulator, 0-phone
                    if(flag && flag ==1){
                        $('button[smstarget='+val.id+']').attr('class','btn btn-xs btn-white btn-primary btn-bold orange contentRight');
                    }else{
                        $('button[smstarget='+val.id+']').attr('class','dsn contentRight');
                    }
                }
            });
        }
    }});
};

var doError = function(id,error){
    bootbox.dialog({
        message: "<span class='bigger-120'>确定该账号的告警&nbsp;&nbsp;&nbsp;&nbsp;"+error+"&nbsp;&nbsp;&nbsp;&nbsp;已处理</span>",
        buttons:{
            "click0":{"label":"确定","className":"btn btn-sm btn-primary","callback": function(){
                $.ajax({ dataType:'json',type:"get", url:'/r/cabana/error',data:{accId:id},success:function(jsonObject){
                    if(jsonObject.status == 1){
                        refreshContent();
                    }else{
                        bootbox.alert(jsonObject.message);
                    }
                },error:function(result){  bootbox.alert(result); }});
            }},
            "click1":{"label":"取消","className":"btn btn-sm"}
        }
    });
};
var $dilogSMS = null;
var showSMS = function(accId,mobile,accountInfo){
    var html =   '<div id="choiceExportModal_SMS" class="modal fade " tabindex="-1">';
    html = html+ '   <div class="modal-dialog modal-lg" style="width:400px;">';
    html = html+ '      <div class="modal-content">';
    html = html+ '         <div class="modal-header no-padding text-center">';
    html = html+ '            <div class="table-header">';
    html = html+ '               <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>'+accountInfo +"|"+mobile+'</span>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '         <div class="modal-body">';
    html = html+ '            <div class="widget-box">';
    html = html+ '               <div class="widget-body">';
    html = html+ '                  <div class="widget-main">';
    html = html+ '                      <div class="smsContent"></div>';
    html = html+ '                      <div style="text-align:center;">';
    html = html+ '                          <a class="btn btn-xs btn-bold btn-success" onclick="showSMSContent('+mobile+')"><i class="fa fa-refresh" aria-hidden="true"></i><span>刷新</span></a>';
    html = html+ '                      </div>';
    html = html+ '                  </div>';
    html = html+ '               </div>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '      </div>';
    html = html+ '   </div>';
    html = html+ '</div>';
    $dilogSMS=$(html).clone().appendTo($("body"));
    $dilogSMS.modal("toggle");
    var $sms = $dilogSMS.find("div.smsContent").html('正在加载,请等待...');
    $.ajax({type: 'get',url: '/r/cabana/hisSMS',data: {"mobile":mobile},dataType:'json',success: function (res) {
        if (res.status==1) {
            if(!res.data||res.data.length==0){
                $sms.html('暂时无新的短信，请等待...');
            }else{
                var html = '';
                for(var index in res.data){
                    html = html +res.data[index] +'</br>';
                }
                $sms.html(html);
            }
        }else{
            $sms.html('加载失败...');
        }
    }});
    $dilogSMS.on('hidden.bs.modal', function () { $dilogSMS.remove();$dilogSMS = null;});
};

var showSMSContent = function(mobile){
    $.ajax({type: 'get',url: '/r/cabana/hisSMS',data: {"mobile":mobile},dataType:'json',success: function (res) {
        if(!$dilogSMS)
            return;
        var $sms = $dilogSMS.find("div.smsContent");
        if (res.status==1) {
            if(!res.data||res.data.length==0){
                $sms.html('暂时无新的短信，请等待...');
            }else{
                var html = '';
                for(var index in res.data){
                    html = html +res.data[index] +'</br>';
                }
                $sms.html(html);
            }
        }else{
            $sms.html('加载失败...');
        }
    }});
};

var showLogDate = function(accId){
    var html =   '<div id="choiceLogDateModal_InOut" class="modal fade " tabindex="-1">';
    html = html+ '   <input type="hidden" id="accountId"/>';
    html = html+ '   <input type="hidden" id="operaType"/>';
    html = html+ '   <input type="hidden" id="exBankType"/>';
    html = html+ '   <div class="modal-dialog modal-lg" style="width:400px;">';
    html = html+ '      <div class="modal-content">';
    html = html+ '         <div class="modal-header no-padding text-center">';
    html = html+ '            <div class="table-header">';
    html = html+ '               <button type="button" class="close" data-dismiss="modal" aria-hidden="true"><span class="white">&times;</span></button><span>请选择日志文件日期</span>';
    html = html+ '            </div>';
    html = html+ '         </div>';
    html = html+ '         <div class="modal-body">';
    html = html+ '            <div class="widget-box">';
    html = html+ '               <div class="widget-header hide"><h4 class="smaller"></h4></div>';
    html = html+ '                  <div class="widget-body">';
    html = html+ '                      <div class="widget-main">';
    html = html+ '                          <label class="control-label bolder blue">日期</label>&nbsp;&nbsp;';
    html = html+ '                          <span class="input-group-addon sr-only"><i class="fa fa-calendar"></i></span>';
    html = html+ '                          <input class="date-range-picker" type="text" placeholder="请选择日志文件日期" name="startAndEndTime_export" style="height: 32px;width:280px;"/>';
    html = html+ '                          <div class="control-group">&nbsp;&nbsp;&nbsp;</div>';
    html = html+ '                          <div style="text-align:center;">';
    html = html+ '                              <a class="btn btn-sm btn-success" id="checkButton">';
    html = html+ '                                  <i class="fa fa-check" aria-hidden="true"></i><span>下载日志</span>';
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
        $.ajax({type: 'get',url: '/r/cabana/logs',data: {"accId":accId,date:$timer.val().split(' ')[0]},dataType: 'json',success: function (res) {
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

function refreshContent(pageNo){
    authRequest.pageNo = (pageNo&&pageNo>0||pageNo==0)?pageNo:$("#BizMobile_accountPage .Current_Page").text()?$("#BizMobile_accountPage .Current_Page").text()-1:0;
    authRequest.pageNo=authRequest.pageNo<0?0:authRequest.pageNo;
    if(!authRequest.typeToArray){
        authRequest.typeToArray= [accountTypeInBank,accountTypeOutBank,accountTypeReserveBank,accountTypeBindAli,accountTypeBindWechat,accountTypeBindCommon,accountTypeThirdCommon,accountTypeBindCustomer].toString();
    }
    if(!authRequest.search_EQ_status){
        authRequest.statusToArray= [accountStatusNormal,accountStatusEnabled,accountStatusFreeze,accountStatusStopTemp,accountInactivated].toString();
    }else{
        authRequest.statusToArray = [authRequest.search_EQ_status].toString();
    }
    authRequest.sortProperty='mobile';
    authRequest.sortDirection=1;
    authRequest.pageSize=$.session.get('initPageSize');
    authRequest.search_IN_handicapId = $("select[name='search_EQ_handicapId']").val().toString();
    $.ajax({ dataType:'json',type:"get", url:API.r_account_list,data:authRequest,success:function(jsonObject){
        if(jsonObject.status == 1){
            monitorMobile_showContent(jsonObject.data?jsonObject.data:[],jsonObject.page);
            showPading(jsonObject.page,"BizMobile_accountPage",refreshContent,null,false,false);
            contentRight();
        }else{
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){  bootbox.alert(result); }});
}

function addAccount(){
    bootbox.dialog({
        message: "<span class='bigger-180'>请选择新增账号类型</span>",
        buttons:{
            "click1":{"label":"出款银行卡","className":"btn btn-sm btn-primary","callback": function(){showAddOutAccount(accountTypeOutBank,accountInactivated ,function(){ refreshContent();});}},
            "click2":{"label":"出款第三方","className":"btn btn-sm btn-primary","callback":function(){ showAddOutAccount(accountTypeOutThird,accountStatusEnabled,function(){ refreshContent();},true);}},
            "click3":{"label":"备用卡","className":"btn btn-sm btn-primary","callback":function(){showAddOutAccount(accountTypeReserveBank,accountInactivated ,function(){refreshContent();});}},
            "click4":{"label":"现金卡","className":"btn btn-sm btn-primary","callback":function(){showAddOutAccount(accountTypeCashBank,accountInactivated ,function(){refreshContent();});}},
            "click5":{"label":"取消","className":"btn btn-sm"}
        }
    });
}

function monitorMobile_searchByFilter(){
	var $div=$("#BizMobileMonitor");
    authRequest.search_EQ_status = $div.find("input[name='search_EQ_status']:checked").val();
    authRequest.search_LIKE_alias = $div.find("input[name='search_LIKE_alias']").val();
    authRequest.search_LIKE_account = $div.find("input[name='search_LIKE_account']").val();
    authRequest.search_EQ_currSysLevel = $div.find("input[name='search_EQ_currSysLevel']:checked").val();
    authRequest.bankType=$.trim($div.find("select[name='search_LIKE_bankType']").val());
    authRequest.search_LIKE_mobile=$.trim($div.find("input[name='search_LIKE_mobile']").val());
    if(!authRequest.bankType||authRequest.bankType=='请选择'){
        authRequest.bankType = null;
    }
    var typeToArray = new Array();
    $div.find("input[name='search_IN_type']:checked").each(function(){
        if(this.value){
            var tmp0 = this.value.split(",");
            for(var index1 in tmp0){
                typeToArray.push(tmp0[index1]);
            }
        }
    });
    authRequest.typeToArray= (typeToArray && typeToArray.length>0)? typeToArray.toString():null;
    refreshContent(0);
}

bootbox.setLocale("zh_CN");

var options= new Array();
$.each(bank_name_list,function(index,record){
    options.push("<option value='"+record+"'>"+record+"</option>");
});

$("#accountFilter2").find("[name=search_EQ_handicapId],[name=search_LIKE_bankType]").change(function(){
    monitorMobile_searchByFilter();
});
$("#accountFilter2").find("[name=search_EQ_currSysLevel],[name=search_IN_type],[name=search_EQ_status]").click(function(){
    monitorMobile_searchByFilter();
});
getHandicap_select($("#accountFilter2 select[name='search_EQ_handicapId']"),0,"全部");
genBankTypeHtml('tabTransing_bankType');

initRefreshSelect($("#accountFilter2 #refreshAccountListSelect"),$("#accountFilter2 #search-button"),75,"refresh_monitorMobile");
//手机监控 代码结束


//已激活 汇总 代码开始
var typeALL=[accountTypeInBank,accountTypeOutBank,accountTypeReserveBank,accountTypeBindWechat,accountTypeBindAli,accountTypeThirdCommon,accountTypeBindCommon,accountTypeBindCustomer];

/**
 * 根据账号Type拼接对应数据
 */
var BankCardService_showAccountList=function(CurPage){
	if(!!!CurPage&&CurPage!=0) CurPage=$("#accountList_page .Current_Page").text();
	//封装data
	var $div = $("#accountFilter1");
    var statusToArray = new Array();
    if($("#tabStatus").val()*1==1){
    	//未激活
    	statusToArray=[accountInactivated];
    }else if($("#tabStatus").val()*1==2){
        //已激活
        statusToArray=[accountActivated];
    }else if($("#tabStatus").val()*1==3){
    	//待提额 汇总
        $div.find("input[name='search_IN_status']:checked").each(function(){
            statusToArray.push(this.value);
        });
        if(statusToArray.length==0){
            statusToArray=[accountStatusNormal,accountStatusFreeze,accountStatusExcep,accountStatusStopTemp,accountStatusEnabled];
        }
    }
    var type=$div.find("[name=search_EQ_accountType]").val();
    if(!type){
    	type=typeALL;
    }
    var data = {
        search_LIKE_account:$.trim($div.find("input[name='search_LIKE_account']").val()),
        search_LIKE_bankType:$.trim($div.find("[name='search_LIKE_bankType']").val()),
        search_LIKE_owner:$.trim($div.find("input[name='search_LIKE_owner']").val()),
        search_EQ_alias:$.trim($div.find("input[name='search_EQ_alias']").val()),
        currSysLevel:$div.find("input[name='currSysLevel']:checked").val(),
        statusToArray:statusToArray.toString(),
        search_IN_handicapId:$div.find("select[name='search_EQ_handicapId']").val(),
        typeToArray:type.toString(),
        search_NOTEQ_flag:0,//不查PC
        pageNo:CurPage<=0?0:CurPage-1,
        pageSize:$.session.get('initPageSize')
    };
    var requesturl = "/r/account/list";
    if($("#tabStatus").val()*1==4){
        requesturl = "/r/account/toberaised"
    }
    //发送请求
	$.ajax({
        dataType:'JSON',
        type:"POST", 
		async:false,
        url:requesturl,
        data:data,
        success:function(jsonObject){
			if(jsonObject.status !=1){
				if(-1==jsonObject.status){
					showMessageForFail("查询失败："+jsonObject.message);
				}
				return;
			}
			//已匹配和汇总 增加信用额度
			var showActiving=($("#tabStatus").val()*1==1);
            var showActived=($("#tabStatus").val()*1==2);
            var showOther=($("#tabStatus").val()*1==3||$("#tabStatus").val()*1==4);
			$("#activing_show,#actived_show,#others_show").hide();
			if(showActiving){
				$("#activing_show").show();
			}else if(showActived){
				$("#actived_show").show();
			}else{
                $("#others_show").show();
            }
			var $tbody=$("#accountListTable tbody");
			$tbody.html("");
			var totalBalanceByBank =0 ,idList=new Array(),idArray = new Array();
			$.each(jsonObject.data,function(index,record){
				!record.bankBalance?record.bankBalance=0:null;
				totalBalanceByBank+=record.bankBalance*1;
				idArray.push(record.id);
				var tr="";
				tr+="<td><span>"+record.handicapName+"</span></td>";
                if(showOther) {
                    tr += "<td><span>" + _checkObj(record.currSysLevelName) + "</span></td>";
                }
				tr+="<td><span>"+_checkObj(record.alias)+"</span></td>";
				//手机余额状态
				tr+="<td><span>"+hideAccountAll(record.mobile,true)+"</span><span target='"+record.id+"' class='time4St bal label label-grey' style='display:block;width:99%;float:left;' >离线</span></td>";
				tr+="<td style='padding-left:0px;padding-right:0px;'>" +
						"<a class='bind_hover_card' data-toggle='accountInfoHover"+record.id+"' data-placement='auto right' data-trigger='hover'  >"
				record.bankType=record.bankType?record.bankType:'无';
                record.owner=record.owner?record.owner:'无';
				tr+="<span>"+record.bankType+"&nbsp;|&nbsp;"+record.owner+"</span><br/>";
				idList.push({'id':record.id});
				tr+=hideAccountAll(record.account);
				tr+=	"</a>" +
				    "</td>";
				//不同状态使用不同颜色
				if(record.status==accountStatusFreeze){
					tr+="<td><span class='label label-sm label-danger'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountStatusStopTemp){
					tr+="<td><span class='label label-sm label-primary'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountInactivated){
					tr+="<td><span class='label label-sm label-warning'>"+record.statusStr+"</span></td>";
				}else if(record.status==accountStatusExcep){
					tr+="<td><span class='label label-sm label-inverse'>"+record.statusStr+"</span></td>";
				}else{
					tr+="<td><span class='label label-sm label-success'>"+record.statusStr+"</span></td>";
				}
                if(showOther) {
                    tr += "<td><span class=''>" + record.typeStr + "</span></td>";
                }
				//余额
				tr+='<td>';
				tr+="<span>"+record.bankBalance+htmlDailyAmount(0,record.limitIn,record.incomeAmountDaily)+"</span>";
				//手机流水状态
				tr+='<span class="errorAlarm error'+record.id+'"></span><span target="'+record.id+'" class="time4St flow label label-grey" style="display:block;width:99%;float:left;" >&nbsp;&nbsp;</span>';
				tr+='</td>';
				if(showOther || showActived){
					tr+="<td><span class=''>"+_checkPeakBalance(record.peakBalance)+"</span></td>";
			    }
				//操作  修改/修改限额 冻结/激活 操作记录  明细
				tr+="<td>";
                var updateBtn="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
						onclick='showUpdateOutAccount("+record.id+",BankCardService_showAccountList)'>\
						<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改</span></button>";
                var updateLimitBtn="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
						onclick='showUpdateflag2Account("+record.id+",BankCardService_showAccountList)'>\
						<i class='ace-icon fa fa-pencil-square-o bigger-100 orange'></i><span>修改限额</span></button>";

                var enableBtn="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
						onclick='showUpdateOutAccount("+record.id+",BankCardService_showAccountList,false,true)'>\
						<i class='ace-icon fa fa-exchange bigger-100 red'></i><span>启用</span></button>";
                var enableLimitBtn="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
						onclick='showUpdateflag2Account("+record.id+",BankCardService_showAccountList,true)'>\
						<i class='ace-icon fa fa-exchange bigger-100 red'></i><span>启用</span></button>";
				if($("#tabStatus").val()*1==1){
					//未激活 
					tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
						onclick='activeAccount("+record.id+")'>\
						<i class='ace-icon fa fa-check-square-o bigger-100 orange'></i><span>激活</span></button>";
				}else if($("#tabStatus").val()*1==2){
                    //已激活
                    if(record.flag&&record.flag*1==2){
                        tr+=enableLimitBtn;
                    }else{
                        //其它账号
                        tr+=enableBtn;
                    }
                }else{
					//汇总
					tr+="<button class='btn btn-xs btn-white btn-warning btn-bold contentRight' \
						onclick='showFreezeModal("+record.id+",BankCardService_showAccountList)'>\
						<i class='ace-icon fa fa-remove bigger-100 orange'></i><span>冻结</span></button>";
                    if(record.flag&&record.flag*1==2){
                        //返利网账号（flag=2) 不允许修改基本信息
                        if(record.peakBalance&&record.peakBalance>0){
                            //余额峰值(保证金)大于0才 可以修改限额
                            tr+=updateLimitBtn;
                        }
                    }else{
                        //其它账号
                        tr+=updateBtn;
                    }
				}
				tr+="<button class='btn btn-xs btn-white btn-info btn-bold "+OperatorLogBtn+" ' \
						onclick='showModal_accountExtra("+record.id+")' >\
						<i class='ace-icon fa fa-list bigger-100 blue'></i><span>操作记录</span></button>";
				tr+="<button class='btn btn-xs btn-white btn-primary btn-bold' " +
						"onclick='showInOutListModal("+record.id+")'>"+
						"<i class='ace-icon fa fa-list bigger-100 blue'></i><span>明细</span></button>";
				tr+="</td>";
				$tbody.append($("<tr id='mainTr"+record.id+"'>"+tr+"</tr>"));
			});
			//有数据时，显示总计 小计
			if(jsonObject.page&&(jsonObject.page.totalElements*1)){
                var status = $("#tabStatus").val();
                var totalRows;
                if(status == 1 || status == 2){
                    totalRows={
                        column:12,
                        subCount:jsonObject.data.length,
                        count:jsonObject.page.totalElements
                    };
                    totalRows[6]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
                }else{
                    totalRows={
                        column:13,
                        subCount:jsonObject.data.length,
                        count:jsonObject.page.totalElements
                    };
                    totalRows[8]={subTotal:totalBalanceByBank,total:jsonObject.page.header.totalAmountBankBalance};
                }
				showSubAndTotalStatistics4Table($tbody,totalRows);
			}
			//加载账号悬浮提示
			loadHover_accountInfoHover(idList);
			//分页初始化
			showPading(jsonObject.page,"accountList_page",BankCardService_showAccountList,null,true);
			contentRight();
			rereshAccSt(idArray);
        }
	});
}

var rereshAccSt = function(idList){
    $.ajax({type:'get',url:'/r/cabana/status',data: {accIdArray:idList.toString()},dataType:'json',success:function (res) {
        if (res.status==1&&res.data&&res.data.length>0) {
            var nowTm = new Date().getTime();
            $('span.errorAlarm').html('');
            $('span.time4St.bal').text('离线').attr('class','time4St bal label label-grey').attr('style','display:block;width:99%;float:left;');
            $('span.time4St.flow').html('&nbsp;&nbsp;').attr('class','time4St flow label label-grey').attr('style','display:block;width:99%;float:left;');
            res.data.forEach(function(val,index) {
                if(val.error){
                    var error = '';
                    error = error + '<span class="badge badge-transparent tooltip-error" title="'+val.error+'" onclick="doError('+val.id+',\''+val.error+'\');">';
                    error = error + '  <i class="ace-icon fa fa-exclamation-triangle red bigger-130"></i>';
                    error = error + '</span>';
                    $('span.errorAlarm.error'+val.id).html(error);
                }
                {
                    var classInf = val.time ==0||(val.time > 0 && (nowTm-val.time)< 180000)?'label-success':'label-warning';
                    if(val.time && val.time > 0 &&((nowTm-val.time)>= 600000)){
                        classInf = 'label-danger';
                    }
                    $('span.time4St.bal[target='+val.id+']').each(function(){
                        $(this).text(val.time >0?(geeTime4Crawl(val.time)):'已连接').attr('class','time4St bal label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
                    });
                }
                {
                    var classInf = val.logtime ==0||(val.logtime > 0 && (nowTm-val.logtime)< 180000)?'label-success':'label-warning';
                    if(val.logtime && val.logtime > 0 &&((nowTm-val.logtime)>= 600000)){
                        classInf = 'label-danger';
                    }
                    $('span.time4St.flow[target='+val.id+']').each(function(){
                        $(this).html(val.logtime >0?(geeTime4Crawl(val.logtime)):'&nbsp;&nbsp;').attr('class','time4St flow label').attr('style','display:block;width:99%;float:left;').addClass(classInf);
                    });
                }
                $('span.mode4Acc[target='+val.id+']').text((val.time?(val.mode ==1 ?'抓流水':(val.mode ==2?'转账':'默认')):'---'));
                {
                    var flag =  val.flag;//1-simulator, 0-phone
                    if(flag && flag ==1){
                        $('button[smstarget='+val.id+']').attr('class','btn btn-xs btn-white btn-primary btn-bold orange contentRight');
                    }else{
                        $('button[smstarget='+val.id+']').attr('class','dsn contentRight');
                    }
                }
            });
        }
    }});
};



function activeAccount(accountId){
    var accountInfo = getAccountInfoById(accountId);
    bootbox.confirm("请先确保账户（"+accountInfo.bankType+"|"+ accountInfo.owner+"|"+ accountInfo.account.substr(accountInfo.account.length-4)+"）处于正常连接状态，确定向此账户推送一笔测试任务？", function(result) {
        if (result) {
            $.ajax({ dataType:'json',type:"get", url:'/r/account/getActiveTrans',data:{accountId:accountId},success:function(jsonObject){
                if(jsonObject.status == 1){
                    if(jsonObject.data){
                        var message = "账户（"+accountInfo.bankType+"|"+ accountInfo.owner+"|"+ accountInfo.account.substr(accountInfo.account.length-4)+"）将向"+"<br>"+"账户（"+
                            jsonObject.data.bankType+"|"+ jsonObject.data.owner+"|"+ jsonObject.data.account.substr(jsonObject.data.account.length-4)+"）转一笔账"
                        showMessageForSuccess(message,2000);
                    }else{
                        showMessageForSuccess("未获取到测试转账任务！",2000);
                    }
                }
            },error:function(result){  bootbox.alert(result); }});
        }
    });
}
getHandicap_select($("#accountFilter1 select[name='search_EQ_handicapId']"),0,"全部");
getBankTyp_select($("#accountFilter1 select[name='search_LIKE_bankType']"),null,"全部");
getAccountType_select($("#accountFilter1 select[name='search_EQ_accountType']"),null,"全部");
$("#accountFilter1").find("[name=search_EQ_handicapId],[name='search_EQ_accountType'],[name='search_LIKE_bankType']").change(function(){
	BankCardService_showAccountList(0);
});
$("#accountFilter1").find("[name='search_IN_status'],[name=currSysLevel]").click(function(){
	BankCardService_showAccountList(0);
});
//已激活 汇总 代码结束




function changeStatus(status) {
    searchPageType=null; $('#freshOutwardTaskLi4BankCard').hide();
    $("#tabStatus").val(status);
    if(status=='2'||status=='3'){
    	//已激活 汇总 
        BankCardService_showAccountList(0);
    }else if(status=='BizMobileMonitor'){
    	//手机监控
    	monitorMobile_searchByFilter();
    }else if(status=='IncomeAccountIssue'){
    	//下发银行卡
    	IncomeAccountIssue_showAccountList(0);
    }else if(status=='monitorStat'){
    	//下发监控
    	getMonitorStat();
    }else if(status=='Outward'){
    	//下发监控
    	Outward_refreshContent();
    }else{
    	//正在出款 待排查
        if (status=='outDrawing' || status=='failedOut'){
            $.each(ContentRight['OutwardTaskTotal:*'], function (name, value) {
                if (name == 'OutwardTaskTotal:Note:*') {
                    Noteflag = true;
                } else if (name == 'OutwardTaskTotal:NewGenerationTask:*') {
                    NewGenerationTaskflag = true;
                } else if (name == 'OutwardTaskTotal:FinishTask:*') {
                    FinishTask = true;
                } else if (name == 'OutwardTaskTotal:NotifyPlatForm:*') {
                    NotifyP = true;
                } else if (name == 'OutwardTaskTotal:uploadReceipt:*') {
                    UploadReceipt = true;
                }
            });
            $.each(ContentRight['Outward:*'], function (name, value) {
                if (name == 'Outward:currentpageSum:*') {
                    outwardCurrentPageSum=true;
                }
                if (name =='Outward:allRecordSum:*'){
                    outwardAllRecordSum=true;
                }
            });
            _addTimeSelect();_initialMultiSelect();
            modifyNouns();
            modifyStyle();$('#freshOutwardTaskLi4BankCard').show();
            searchPageType=status;_dateP4BankCard();
            _initialSelectChosen(status);
            _search4BankCard();
            _getPathAndHtml('historyDetail4BankCard');
            _searchAfterAutoUpdateTimeTask4BankCard();
        }
    }
}

function _checkPeakBalance(obj) {
    var ob = 0;
    if (obj) {
        ob = obj;
    }
    return ob;
}
BankCardService_showAccountList(0);
//=========================================正在出款 待排查 start=========================
var searchPageType=null;
function modifyStyle(){
    if (isHideOutAccountAndModifyNouns) {
        $('.modifyAmountException').html('金额<div class="action-buttons inline" id="amountSortDiv" onclick="_sortSearch(this);">\n' +
            '                        <a href="javascript:void(0);" class="green bigger-140 show-details-btn"\n' +
            '                           title="降序(默认出货点数出货时间耗时降序)">\n' +
            '                            <i class="ace-icon fa fa-angle-down"></i>\n' +
            '                        </a>\n' +
            '                    </div>');
        $('.DrawingAmountInput').attr('style','height:32px;width:8%;');
        $('.ToDrawAmountInput').attr('style','height:32px;width:30%;');
        $('.FailOutAmountInput').attr('style','height:32px;width:29.5%;');
        $('.ManagerDealDiv').attr('style','display:none;');
        $('.modifyOutAccountFail').text('出货账号');
    }
}
function accountPopOver(idList) {
    if(!isHideAccount){
        loadHover_accountInfoHover(idList);
    }
}
function _addTimeSelect() {
    var opt = '<option  value="0" >不刷新</option><option  selected="selected" value="15">15秒</option><option  value="30">30秒</option>' +
        '<option  value="60">60秒</option><option  value="120">120秒</option><option  value="180">180秒</option>';
    $('#autoUpdateTimeTask4BankCard').empty().append(opt);
}
function _initialSelectChosen(type) {
    $('#'+type  +'  .chosen-select').chosen({
        //allow_single_deselect:true,
        enable_split_word_search: true,
        no_results_text: '没有匹配结果',
        search_contains: true
    });
    if (handicapAndLevelInitialOptions) {
        $('#handicap_' + type).empty().html(handicapAndLevelInitialOptions[0]);
        $('#handicap_' + type).trigger('chosen:updated');
        $('#level_' + type).empty().html(handicapAndLevelInitialOptions[1]);
        $('#level_' + type).trigger('chosen:updated');
    }
    if (type=='outDrawing') {
        $('#handicap_' + type + '_chosen').prop('style', 'width: 30%;');
    }else{
        $('#handicap_' + type + '_chosen').prop('style', 'width: 78%;');
    }
    $('#level_' + type + '_chosen').prop('style', 'width: 78%;');
    if(type=='outDrawing'||type=='failedOut'){
        var opt1 ='<option>请选择</option>';
        $.each(bank_name_list,function (i,val) {
            opt1 +='<option value="'+val+'">'+val+'</option>';
        });
        $('#bank_'+type).empty().html(opt1);
        $('#bank_'+type).trigger('chosen:updated');
        if(type=='failedOut'){
            $('#bank_'+type+ '_chosen').prop('style', 'width: 68%;');
        }else{
            $('#bank_'+type+ '_chosen').prop('style', 'width: 30%;');
        }
    }
}
function _dateP4BankCard() {
    if (searchPageType=='failedOut'){
        $('input.timeScope_failedOut_4BankCard').on("mouseup", function () {
            _datePickerForAll($('input.timeScope_failedOut_4BankCard'));
        });
    } else if (searchPageType && searchPageType != 'toOutDraw' && $('#' + searchPageType).find('input.date-range-picker')) {
        _datePickerForAll($('#' + searchPageType).find('input.date-range-picker'));
    }
}
function _distribution(id, type) {
    $('#distributionTaskId4BankCard').val(id);
    var opt = '';
    $(bank_name_list).each(function (i, bankType) {
        opt += '<option>' + bankType + '</option>';
    });
    $('#form-field-select-allocateTask14BankCard').empty().html(opt);
    _initialMultiSelect();
    $('#allocateTaskRemark4BankCard').val('');
    $('input[name="distributeObject4BankCard"]:radio').prop('checked', false);
    $('#modal-distribution-header4BankCard').text('重新生成并分配任务');
    $('#modal-distribution4BankCard').modal('show');
}
function _distributionTask() {
    var taskId = $('#distributionTaskId4BankCard').val();
    var bankTypes = $('#form-field-select-allocateTask14BankCard').val();
    var remark = $('#allocateTaskRemark4BankCard').val();
    var distributeObject = '';
    $('input[name="distributeObject4BankCard"]').each(function () {
        if ($(this).is(":checked")) {
            distributeObject = $(this).val();
        }
    });
    var url = '/r/outtask/recreate';
    $.ajax({
        type: 'post',
        url:url ,
        data: {'taskId': taskId, 'type': distributeObject, "bankType": bankTypes.toString(), "remark": remark},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 500,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
                if (res.status == 1) {
                    $('#modal-distribution4BankCard').modal('hide');
                    $('#distributionTaskId4BankCard').val('');
                    _search4BankCard();
                }
            }
        }
    });
}
function SCustomerserviceRemark(id) {
    $('#CustomerserviceRemark4BankCard').val('');
    $('#CustomerserviceRemark_id4BankCard').val(id);
    $('#CustomerserviceRemark_modal4BankCard').modal('show');
    $('#totalTaskFinishBTN4BankCard').attr('onclick', 'save_CustomerserviceRemark4BankCard();');
}
function save_CustomerserviceRemark4BankCard() {
    var remark = $.trim($('#CustomerserviceRemark4BankCard').val());
    if (!remark) {
        $('#prompt_remark4BankCard').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    var taskId = $('#CustomerserviceRemark_id4BankCard').val();
    $.ajax({
        type: 'post',
        url: '/r/outtask/remark',
        data: {"taskId": taskId, "remark": remark},
        dataType: 'json',
        success: function (res) {
            if (res) {
                $.gritter.add({
                    time: 300,
                    class_name: '',
                    title: '系统消息',
                    text: res.message,
                    sticky: false,
                    image: '../images/message.png'
                });
            }
            $('#CustomerserviceRemark_modal4BankCard').modal('hide');
            _search4BankCard();
        }
    });
}
var _beforeTotalTurntoFinished = function (id) {
    $('#CustomerserviceRemark4BankCard').val('');
    $('#CustomerserviceRemark_id4BankCard').val(id);
    $('#CustomerserviceRemark_modal4BankCard').modal('show');
    $('#totalTaskFinishBTN4BankCard').attr('onclick', '_exectuTransferFromFailTotal4BankCard();');
};
function _exectuTransferFromFailTotal4BankCard() {
    if (!$('#CustomerserviceRemark4BankCard').val()) {
        $('#prompt_remark4BankCard').text('请填写备注').show(10).delay(1500).hide(10);
        return;
    }
    $.ajax({
        type: 'post',
        url: '/r/outtask/finsh',
        data: {
            "type": 'failedOutToMatched',
            "taskId": $('#CustomerserviceRemark_id4BankCard').val(),
            "remark": $('#CustomerserviceRemark4BankCard').val()
        },
        dataType: 'json',
        async: false,
        success: function (res) {
            $.gritter.add({
                time: 1000,
                class_name: '',
                title: '系统消息',
                text: res.message,
                sticky: false,
                image: '../images/message.png'
            });
            $('#totalTaskFinishBTN4BankCard').attr('onclick', '');
            $('#CustomerserviceRemark_modal4BankCard').modal('hide');
            _search4BankCard();
        }
    });
}
function _search4BankCard(){
    if(!searchPageType){return;}
    var type=searchPageType;
    if (timeOutSearchTasks4BankCard && currentPageLocation.indexOf('BankCardService:*') <= -1) {
        clearInterval(timeOutSearchTasks4BankCard);
        timeOutSearchTasks4BankCard = null;
        return;
    }
    var maintain_toOutDraw = null;
    var handicap = '', handId = $('#handicap_' + type).val();
    if (handId && handId.indexOf('请选择') < 0) {
        handicap = $('#handicap_' + type+'  option[value="'+handId+'"]').attr('handicap_code');
    }
    //正在出款 未出款 主管处理 待排查 层级改为内外中 查询出款卡的层级的
    var level = '';
    var orderNo = null,orderNoR = $('#orderNo_' + type).val();
    if (orderNoR) {
        if (orderNoR.indexOf('%') >= 0)
            orderNo = orderNoR.replace(new RegExp(/%/g), "?");
        else
            orderNo = orderNoR;
    }
    var member = null, memberR = $('#member_' + type).val();
    if (memberR) {
        if (memberR.indexOf('%') >= 0)
            member = memberR.replace(new RegExp(/%/g), "?");
        else
            member = memberR;
    }
    var fromAccount = null,operator = null;
    var robot = '', manual = '';
    if ($('#account_' + type).val() && $('#account_' + type).val().indexOf('请选择') < 0) {
        fromAccount = $('#account_' + type).val();
    }
    if ($('#operator_' + type).val()) {
        operator = $('#operator_' + type).val();
    }
    if (type == 'failedOut') {
        if ($('#robot_' + type).prop('checked')) {
            robot = $('#robot_' + type).val();
        }
        if ($('#manual_' + type).prop('checked')) {
            manual = $('#manual_' + type).val();
        }
        if (robot && manual) {
            robot = '', manual = '';
        }
    }
    var startTime = '', endTime = '';
    var startAndEnd = $('#timeScope_' + type).val();
    if (startAndEnd) {
        if (startAndEnd.indexOf('~') > 0) {
            startAndEnd = startAndEnd.split('~');
            startTime = startAndEnd[0];
            endTime = startAndEnd[1];
        }
    } else {
        if (type == 'failedOut') {
        } else {
            startTime = _getDefaultTime()[0];
            endTime = _getDefaultTime()[1];
        }
    }
    var fromMoney = '', toMoney = '';
    if ($('#fromMoney_' + type).val()) {
        fromMoney = $('#fromMoney_' + type).val();
    }
    if ($('#toMoney_' + type).val()) {
        toMoney = $('#toMoney_' + type).val();
    }
    var CurPage = $("#" + type + "_footPage").find(".Current_Page").text();
    CurPage = CurPage?CurPage>0?CurPage-1:0:0;
    var data = {}, url = '';
    if(type=='outDrawing' || type=='failedOut'){
        level = $('input[name="level_'+type+'"]:checked').val();
        level = level==3?"":level;
    }
    url = '/r/outtask/total';
    data = {
        "handicap": handicap,"level": level,"orderNo": orderNo,"startTime": $.trim(startTime),
        "endTime": $.trim(endTime),"member": member,"fromMoney": fromMoney,"toMoney": toMoney,
        "accountAlias": fromAccount,"operatorName": operator,"manual": manual,"robot": robot,"flag": type,"maintain": maintain_toOutDraw,
        "pageNo": CurPage,"pageSize": $.session.get('initPageSize')?$.session.get('initPageSize'):10
    }

    if(type=='outDrawing'){
        var bankType = $('#bank_'+type).val();//银行类型
        if(bankType&&bankType!='请选择'){
            bankType = $('#bank_'+type).val();
        }else{
            bankType = '';
        }
        data = $.extend(data, {"bankType": bankType});
    }
    $.ajax({
        type: 'get', url: url, data: data, async: false, dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    if (type == 'failedOutDealed'){
                    }else{
                        if (type == 'outDrawing') {
                            _fillDataForOutDrawing(res.data, type);
                            if ($('#outDrawing  #promptMessageTotalOutDrawing')) {
                                $('#outDrawing  #promptMessageTotalOutDrawing').remove();
                            }
                            $('#outDrawing').append('<div id="promptMessageTotalOutDrawing"><span style="color: mediumvioletred;font-size: 15px">温馨提示：红色<span style="background-color: indianred">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过10分钟。' +
                                '绿色<span style="background-color: lightgreen">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过3分钟。</span></div>');
                        }
                        if (type == 'failedOut') {
                            _fillDataForFailOut(res.data, type);
                            if ($('#failedOut  #promptMessageTotalFailedOut')) {
                                $('#failedOut  #promptMessageTotalFailedOut').remove();
                            }
                            //_checkTroubleShootingOnlineUsersInfo(6);
                            //浅绿色<span style="background-color: lightgreen">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示该条记录状态为"已匹配流水"。</span>
                            $('#failedOut').append('<div id="promptMessageTotalFailedOut" ><span style="color: mediumvioletred;font-size: 15px" >温馨提示：红色<span style="background-color: indianred">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>表示已超过5分钟。</div> ');
                        }
                        if (type == 'failedOut') {
                            $('#' + type).find('th').addClass('no-padding-right-td');
                            $('#' + type).find('td').addClass('no-padding-right-td');
                        }
                        showPading(res.page, type + '_footPage', _search4BankCard);
                        if(outwardAllRecordSum){
                            _getOutwardTaskTotalSum(data, type);
                        }
                        _getOutwardTaskTotalCount(data, type);
                    }
                }
            }

        }
    });
}
function _getOutwardTaskTotalCount(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/outtask/getOutwardTaskTotalCount',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#outwardTaskTotalCurrentCount' + type).empty().text('小计：' + ((res.page.totalElements - res.page.pageSize * (res.page.pageNo)) >= 0 ? res.page.pageSize : (res.page.totalElements - res.page.pageSize * (res.page.pageNo - 1) )) + '条记录');
                    $('#outwardTaskTotalAllCount' + type).empty().text('合计：' + res.page.totalElements + '条记录');
                }
                showPading(res.page, type + '_footPage', _search4BankCard);
            }
        }
    });
}
function _getOutwardTaskTotalSum(data, type) {
    $.ajax({
        type: 'get',
        url: '/r/outtask/getOutwardTaskTotalSum',
        data: data,
        dataType: 'json',
        success: function (res) {
            if (res) {
                if (res.status == 1 && res.data) {
                    $('#outwardTaskTotalSum' + type).text(parseFloat(res.data.sumAmount).toFixed(3));
                    if (type == 'successOut') {
                        $.each(ContentRight['OutwardTaskTotal:*'], function (name, value) {
                            if (name == 'OutwardTaskTotal:lookUpFinishedTotalAmount:*') {
                                lookUpFinishedTotalAmount = true;
                            }
                        });
                        if (!lookUpFinishedTotalAmount) {
                            $('#outwardTaskTotalSum' + type).text('无权限查看');
                        }
                    }
                }
            }
        }
    });
}
function _fillDataForFailOut(data, subPageType) {
    var idList = [];
    var tr = '';
    var trs = '';
    $('#tbody_' + subPageType).empty();
    if (data) {
        var amount = 0;
        $(data).each(function (i, val) {
            var screenshotArr = null;
            if (_checkObj(val.successPhotoUrl)) {
                screenshotArr = val.successPhotoUrl.split('/');
            }
            var hasScreenshot = false;
            if (screenshotArr && screenshotArr instanceof Array) {
                if (screenshotArr.length > 3) {
                    hasScreenshot = (screenshotArr[3]).indexOf('screenshot') > -1;
                } else if (screenshotArr.length>1){
                    hasScreenshot = (screenshotArr[2]).indexOf('screenshot') > -1;
                } else{
                    hasScreenshot = (screenshotArr[0]).indexOf('screenshot') > -1;
                }
            }
            idList.push({'id': val.accountId});
            if (val) {
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                    //是否是公司用款
                    if (!isHideAccount)
                        tr += '<td><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                    else  tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                }
                tr += '<td>' + _checkObj(val.amount) + '</td>';
                if (val.taskStatus == 5) {
                    //已匹配流水的记录 浅绿色背景标识
                    tr += '<td style="background-color: lightgreen">' + _showTaskStatus(val.taskStatus) + '</td>';
                } else {
                    tr += '<td>' + _showTaskStatus(val.taskStatus) + '</td>';
                }
                tr += '<td>' + _checkObj(val.operator) + '</td><td>'+_checkObj(val.taskHolder)+'</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccount)) {
                    outAccount += "|" + hideAccountAll(val.outAccount);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountOwner)) {
                    outAccount += "|" + hideName(val.outAccountOwner);
                } else {
                    outAccount += '无';
                }
                if (!outAccount || outAccount == '无无无') {
                    outAccount = '';
                }
                if (outAccount) {
                    tr += '<td>' +
                        "<a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                        + outAccount +
                        "</a>" +
                        '</td>';
                } else {
                    tr += '<td></td>';
                }
                tr += '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + _checkObj(val.asignTime) + '" data-original-title="认领时间">' + _checkObj(val.asignTime).substring(11, 19) + '</a></td>';

                if (val.failedOutTime5) {
                    tr += '<td style="background-color: indianred;color: white">' + _checkObj(val.timeConsume) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.timeConsume) + '</td>';
                }
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td>'
                            + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td>'
                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark)
                            + '</a>'
                            + '</td>';

                    }
                } else {
                    tr += '<td></td>';
                }
                if (!isHideImg&&_checkObj(val.successPhotoUrl)) {
                    if (hasScreenshot) {
                        tr += '<td>' +
                            '<a  href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\''  + _checkObj(val.successPhotoUrl) + '\')">查看</a>' +
                            '</td>';
                    } else {
                        tr += '<td>' +
                            '<a   href="javascript:void(0);" id="taskPhoto" onclick="_taskTotalPhoto(\'' + _checkObj(val.successPhotoUrl) + '\')">回执</a>' +
                            '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                if (hasScreenshot && UploadReceipt && _checkObj(val.member) && NewGenerationTaskflag) {
                    tr += '<td style="width: 254px;text-align: center" class="no-padding-right">';
                } else {
                    tr += '<td style="width: 210px;text-align: center" >';
                }
                if (_checkObj(val.member) && NewGenerationTaskflag) {
                    tr += '<button onclick="_distribution(' + val.id + ',' + '\'3\');" type="button"  class=" btn btn-xs btn-white btn-warning  btn-bold">' +
                        '<i class="ace-icon fa fa-reply  bigger-100 red"></i>重新生成任务</button>';
                }
                if (Noteflag) {
                    tr += '<button type="button" onclick="SCustomerserviceRemark(' + val.id + ');"  class="btn btn-xs btn-white btn-info btn-bold ">' +
                        '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button>';
                }
                if (FinishTask) {
                    tr += '<button type="button"  onclick="_beforeTotalTurntoFinished(' + val.id + ');"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                        '<i class="ace-icon fa fa-check bigger-100 blue"></i>完成</button>';
                }
                if (hasScreenshot && UploadReceipt) {
                    tr += '<button type="button"  onclick="_uploadReceiptPhoto(' + val.id + ');"  class="btn btn-xs btn-white btn-success btn-bold ">' +
                        '<i class="fa fa-arrow-circle-up bigger-100 green"></i>回执</button>';
                }
                tr += '</td>';
                amount += val.amount;
            }
        });
        if(outwardCurrentPageSum){
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="4">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: auto;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="15"></td></tr>' ;
        }else{
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="15">小计：统计中..</td></tr>' ;
        }
        if(outwardAllRecordSum){
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="4">总共：统计中..</td>' +
                '<td id="outwardTaskTotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width: auto;">统计中..</td>' +
                '<td colspan="15"></td></tr>';
        }else{
            trs +='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="15">总共：统计中..</td></tr>';
        }
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);$("[data-toggle='popover']").popover();
    if(!isHideAccount){
        accountPopOver(idList);
    }
}
function _fillDataForOutDrawing(data, subPageType) {
    var idList = [];
    var tr = '';
    var trs = '';$('#tbody_' + subPageType).empty();
    if (data) {
        var amount = 0;
        $(data).each(function (i, val) {
            if (val) {
                idList.push({'id': val.accountId});
                idList.push({'id': val.accountId,type:'transInfoHover'});
                amount += val.amount;
                tr += '<tr><td>' + _showHandicapNameByIdOrCode(val.handicap) + '</td>' +
                    '<td>' + _checkObj(val.level) + '</td>' +
                    '<td>' + _checkObj(val.member) + '</td>';
                if (val.timeGap) {
                    //是否超时
                    if (_checkObj(val.member) && _checkObj(val.member) != '公司用款') {
                        //是否是公司用款
                        if (!isHideAccount)
                            tr += '<td><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                        else tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    } else {
                        tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    }
                } else {
                    if (_checkObj(val.member)) {
                        //是否是公司用款
                        if (!isHideAccount)
                            tr += '<td><a  href="javascript:_showOrderDetail(' + val.outwardRequestId + ',\'' + val.orderNo + '\');">' + _checkObj(val.orderNo) + '</a></td>';
                        else tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    } else {
                        tr += '<td>' + _checkObj(val.orderNo) + '</td>';
                    }
                }
                tr += '<td>' + _checkObj(val.operator) + '</td>';
                var outAccount = '';
                if (_checkObj(val.outAccountAlias)) {
                    outAccount += _checkObj(val.outAccountAlias);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccount)) {
                    outAccount += "|" + hideAccountAll(val.outAccount);
                } else {
                    outAccount += '无';
                }
                if (_checkObj(val.outAccountOwner)) {
                    outAccount += "|" + hideName(val.outAccountOwner);
                } else {
                    outAccount += '无';
                }
                if (!outAccount || outAccount == '无无无') {
                    outAccount = '';
                }
                if (outAccount) {
                    tr += '<td>' +
                        "<a  class='bind_hover_card breakByWord' data-toggle='accountInfoHover" + val.accountId + "' data-placement='auto left' data-trigger='hover'  >"
                        + outAccount +
                        "</a>" +
                        '</td>';
                } else {
                    tr += '<td></td>';
                }
                if(outAccount){
                    tr += '<td><a  class="bind_hover_card breakByWord" data-toggle="transInfoHover' + val.accountId + '" data-placement="auto left" data-trigger="hover">' + _checkObj(val.amount) + '</a></td>';
                }else{
                    tr += '<td>' + _checkObj(val.amount) + '</td>';
                }
                tr += '<td><a  class="bind_hover_card breakByWord" title="" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + _checkObj(val.asignTime) + '" data-original-title="认领时间">' + _checkObj(val.asignTime).substring(11, 19) + '</a></td>';
                if (val.timeGap3to10) {
                    tr += '<td style="color: white;background-color: limegreen">' + _checkObj(val.timeUsed) + '</td>';
                } else if (val.timeGapMore10) {
                    tr += '<td style="color: white;background-color: indianred">' + _checkObj(val.timeUsed) + '</td>';
                } else {
                    tr += '<td>' + _checkObj(val.timeUsed) + '</td>';
                }
                if (_checkObj(val.remark)) {
                    if (_checkObj(val.remark).length > 23) {
                        tr += '<td>'
                            + '<a  class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + _divideRemarks(val.remark) + '">'
                            + _checkObj(val.remark).replace(/<br>/g, "").substring(0, 4)
                            + '</a>'
                            + '</td>';

                    } else {
                        tr += '<td>'
                            + '<a class="bind_hover_card breakByWord"  title="备注信息"'
                            + 'data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left"'
                            + ' data-content="' + val.remark + '">'
                            + _checkObj(val.remark)
                            + '</a>'
                            + '</td>';
                    }
                } else {
                    tr += '<td></td>';
                }
                if (Noteflag) {
                    tr += '<td style="width: 55px;text-align: center;"><button type="button"  onclick="SCustomerserviceRemark(' + val.id + ');"  class="btn btn-xs btn-white btn-warning btn-bold ">' +
                        '<i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button></td>';
                } else {
                    tr += '<td></td>';
                }

            }
        });
        if(outwardCurrentPageSum){
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="6">小计：统计中..</td>' +
                '<td bgcolor="#579EC8" style="color:white;width: 100px;">' + parseFloat(amount).toFixed(3) + '</td>' +
                '<td colspan="6"></td></tr>' ;
        }else{
            trs +='<tr><td id="outwardTaskTotalCurrentCount' + subPageType + '" colspan="15">小计：统计中..</td></tr>' ;
        }
        if(outwardAllRecordSum){
            trs +='<tr>' +
                '<td id="outwardTaskTotalAllCount' + subPageType + '" colspan="6">总共：统计中..</td>' +
                '<td id="outwardTaskTotalSum' + subPageType + '" bgcolor="#D6487E" style="color:white;width: 100px;">统计中..</td><td colspan="6"></td></tr>';
        }else{
            trs+='<tr><td id="outwardTaskTotalAllCount' + subPageType + '" colspan="15">总共：统计中..</td></tr>';
        }
    }
    $('#tbody_' + subPageType).empty().html(tr).append(trs);
    $("[data-toggle='popover']").popover();
    if(!isHideAccount){
        accountPopOver(idList);
    }
}
function _handicapTypeChange4BankCard() {
    _search4BankCard();
}

function _radioClick(obj) {
    var checked_val = $(obj).attr('checked_val');
    if(checked_val==1){
        var currentVal = $(obj).val();
        $('input[name="level_'+searchPageType +'"]').each(function (i) {
            if(this.value!=currentVal){
                $(this).attr('checked_val',1);$(this).prop('checked','');
            }
        });
        $(obj).attr('checked_val',2);$(obj).prop('checked','checked');
    }else{
        $(obj).attr('checked_val',1);$(obj).prop('checked','');
    }
    _search4BankCard();
}
function _resetValuesForTaskTotal4BankCard(type) {
    initPaging($('#' + type + '_footPage'), pageInitial);//重置后页面页脚也恢复初始值
    _initialSelectChosen(searchPageType);
    if(type=='outDrawing'|| type=='failedOut'){
        $('#level_all_'+type).prop('checked','checked');
        $('#level_all_'+type).attr('checked_val',2);
        $('#level_in_'+type).prop('checked','');
        $('#level_in_'+type).attr('checked_val',1);
        $('#level_out_'+type).prop('checked','');
        $('#level_out_'+type).attr('checked_val',1);
        $('#level_middle_'+type).prop('checked','');
        $('#level_middle_'+type).attr('checked_val',1);
        if(type=='toOutDraw'){
            $('#maintain_toOutDraw').prop('checked','');$('#maintain_toOutDraw').attr('checked_val',1);
        }
    }
    $('#account_' + type).val('');
    var operator = $('#operator_' + type).val();
    if (operator && operator != 'undefined') {
        $('#operator_' + type).val('');
    }
    var orderNoR = $('#orderNo_' + type).val();
    if (orderNoR) {
        $('#orderNo_' + type).val('');
    }
    var memberR = $('#member_' + type).val();
    if (memberR) {
        $('#member_' + type).val('');
    }
    var amount = $('#amount_' + type).val();
    if (amount) {
        $('#amount_' + type).val('');
    }
    if (type == 'failedOut') {
        $('#timeScope_failedOut').val('');
        $('#timeScope_masterOut').val('');
    } else {
        _dateP4BankCard();
    }
    if ($('#fromMoney_' + type).val()) {
        $('#fromMoney_' + type).val('');
    }
    if ($('#toMoney_' + type).val()) {
        $('#toMoney_' + type).val('');
    }
    if ($('#robot_' + type).prop('checked')) {
        $('#robot_' + type).prop('checked', false);
    }
    if ($('#manual_' + type).prop('checked')) {
        $('#manual_' + type).prop('checked', false);
    }
    if ($('#phone_' + type).prop('checked')) {
        $('#phone_' + type).prop('checked', false);
    }
    _search4BankCard();
}
var Noteflag = false,FinishTask=false,UploadReceipt=false,NewGenerationTaskflag=false;
//=========================================正在出款 待排查 end  =========================
