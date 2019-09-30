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
var outNeedNormalAmt = 0,outNeedNormalNum = 0,outNeedStopTempAmt = 0,outNeedStopTempNum = 0;
var outTotalNormalAmt = 0,outTotalNormalNum = 0,outTotalStopTempAmt = 0,outTotalStopTempNum = 0;
var inTotalNormalAmt = 0,inTotalNormalNum = 0,inTotalStopTempAmt = 0,inTotalStopTempNum = 0;
var reTotalNormalAmt = 0,reTotalNormalNum = 0,reTotalStopTempAmt = 0,reTotalStopTempNum = 0;
var conTotalNormalAmt = 0,conTotalNormalNum = 0,conTotalStopTempAmt = 0,conTotalStopTempNum = 0;
var inThirdNormalAmt = 0,inThirdNormalNum = 0,inThirdStopTempAmt = 0,inThirdStopTempNum = 0;
var getMonitorStat=function () {
    $.ajax({
        dataType:'json',type:"PUT",async:false,url:API.r_accountMonitor_buildTrans,
        success:function(jsonObject){
            if(jsonObject.status == -1){
                showMessageForFail("操作失败："+jsonObject.message);
                return;
            }
            outNeedNormalAmt = 0,outNeedNormalNum = 0,outNeedStopTempAmt = 0,outNeedStopTempNum = 0;
        	outTotalNormalAmt = 0,outTotalNormalNum = 0,outTotalStopTempAmt = 0,outTotalStopTempNum = 0;
        	inTotalNormalAmt = 0,inTotalNormalNum = 0,inTotalStopTempAmt = 0,inTotalStopTempNum = 0;
        	reTotalNormalAmt = 0,reTotalNormalNum = 0,reTotalStopTempAmt = 0,reTotalStopTempNum = 0;
        	conTotalNormalAmt = 0,conTotalNormalNum = 0,conTotalStopTempAmt = 0,conTotalStopTempNum = 0;
        	inThirdNormalAmt = 0,inThirdNormalNum = 0,inThirdStopTempAmt = 0,inThirdStopTempNum = 0;
            outNeedAmt = 0,outNeedNum = 0,outTotalAmt = 0,outTotalNum = 0,inTotalAmt = 0,inTotalNum = 0,reTotalAmt = 0,reTotalNum = 0,conTotalAmt = 0,conTotalNum = 0;
//            drawCanvas( jsonObject.data.stat);
            echartsDataFilling(jsonObject.data.stat);
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
    var outNeedAmt = 0,outNeedNum = 0,outTotalAmt = 0,outTotalNum = 0,inTotalAmt = 0,inTotalNum = 0,reTotalAmt = 0,reTotalNum = 0,conTotalAmt = 0,conTotalNum = 0,inThirdAmt = 0,inThirdNum = 0;
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
        }else if(record.accType == accountTypeInThird){
        	inThirdNum = inThirdNum + record.totalNum;
        	inThirdAmt = inThirdAmt + record.totalBal / 10000;
        }
    });
    outNeedAmt = outNeedAmt.toFixed(2);
    outTotalAmt = outTotalAmt.toFixed(2);
    inTotalAmt = inTotalAmt.toFixed(2);
    reTotalAmt = reTotalAmt.toFixed(2);
    conTotalAmt = conTotalAmt.toFixed(2);
    inThirdAmt = inThirdAmt.toFixed(2);
    var d1 = [
        { label: "银行卡数", data: [[0,conTotalNum],[1,inThirdNum],[2,reTotalNum],[3,inTotalNum],[4,outNeedNum],[5,outTotalNum]],color: '#68BC31' },
        { label: "金额万元", data: [[0,conTotalAmt],[1,inThirdAmt],[2,reTotalAmt],[3,inTotalAmt],[4,outNeedAmt],[5,outTotalAmt]],color: '#2091CF'}
    ];

    var previousPoint = null;
    var $tooltip = $("<div class='tooltip top in'><div class='tooltip-inner'></div></div>").hide().appendTo('body');

    $('#transCanvas').plot(d1,{
        series: {  bars: { show: true }},
        bars: {align: "center", barWidth: 0.5},
        grid: { hoverable: true },
        xaxis: {
            show: true, tickSize: 3,  axisLabelUseCanvas: true, axisLabelFontSizePixels: 14,
            axisLabelFontFamily: 'Verdana, Arial', axisLabelPadding: 12,
            ticks: [ [0,'下发卡 ( &nbsp;'+conTotalAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+conTotalNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;银行卡数&nbsp;）'],
                     [1,'第三方 ( &nbsp;'+inThirdAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+inThirdNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;第三方数&nbsp;）'],
                     [2,'备用卡 ( &nbsp;'+reTotalAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+reTotalNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;银行卡数&nbsp;）'],
                     [3,'入款卡 ( &nbsp;'+inTotalAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+inTotalNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;银行卡数&nbsp;）'],
                     [4,'出款卡【待下发】 ( &nbsp;'+outNeedAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+outNeedNum+'&nbsp;张&nbsp;)</br>（&nbsp;当前所需金额&nbsp;/&nbsp;银行卡数&nbsp;）'],
                     [5,'出款卡 ( &nbsp;'+outTotalAmt+'&nbsp;万元&nbsp;/&nbsp;&nbsp;'+outTotalNum+'&nbsp;张&nbsp;)</br>（&nbsp;总金额&nbsp;/&nbsp;银行卡数&nbsp;）']
            ]
        }
    }).on('plothover', function (event, pos, item) {
        if(!item){
            $tooltip.hide();
            previousPoint = null;
            return
        }
        var dataIndex = item.dataIndex;
        debugger;
        var text = '';
        if(dataIndex == 1 ){
            text = '总金额&nbsp;:&nbsp;'+inThirdAmt+'&nbsp;万元</br>第三方数&nbsp;:&nbsp;'+inThirdNum+'&nbsp;张';
        }else if(dataIndex == 5 ){
            text = '总金额&nbsp;:&nbsp;'+outTotalAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+outTotalNum+'&nbsp;张';
        }else if(dataIndex == 4 ){
            text = '所需金额&nbsp;:&nbsp;'+outNeedAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+outNeedNum+'&nbsp;张';
        }else if(dataIndex == 3 ){
            text = '总金额&nbsp;:&nbsp;'+inTotalAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+inTotalNum+'&nbsp;张';
        }else if(dataIndex == 2 ){
            text = '总金额&nbsp;:&nbsp;'+reTotalAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+reTotalNum+'&nbsp;张';
        }else if(dataIndex == 0 ){
            text = '总金额&nbsp;:&nbsp;'+conTotalAmt+'&nbsp;万元</br>银行卡数&nbsp;:&nbsp;'+conTotalNum+'&nbsp;张';
        }
        if (previousPoint != item.seriesIndex) {
            previousPoint = item.seriesIndex;
            $tooltip.show().children(0).html(text);
        }
        $tooltip.css({top:pos.pageY + 12, left:pos.pageX + 12});
    });
};

var echartsDataFilling=function(msg){
	var outNeedNormalAmt = 0,outNeedNormalNum = 0,outNeedStopTempAmt = 0,outNeedStopTempNum = 0;
	var outTotalNormalAmt = 0,outTotalNormalNum = 0,outTotalStopTempAmt = 0,outTotalStopTempNum = 0;
	var inTotalNormalAmt = 0,inTotalNormalNum = 0,inTotalStopTempAmt = 0,inTotalStopTempNum = 0;
	var reTotalNormalAmt = 0,reTotalNormalNum = 0,reTotalStopTempAmt = 0,reTotalStopTempNmt = 0;
	var conTotalNormalAmt = 0,conTotalNormalNum = 0,conTotalStopTempAmt = 0,conTotalStopTempNum = 0;
	var inThirdNormalAmt = 0,inThirdNormalNum = 0,inThirdStopTempAmt = 0,inThirdStopTempNum = 0;
    $.each(msg,function(index,record){
    	var type = record.accTypeAndStatus.split("-")[0];
    	var status = record.accTypeAndStatus.split("-")[1];
    	if(type == accountTypeOutBank){
    		if(status == accountStatusNormal || status == accountStatusEnabled){
    			outNeedNormalNum = outNeedNormalNum + record.accNum;
    			outNeedNormalAmt = outNeedNormalAmt + Math.abs(record.amount) / 10000;
    			outTotalNormalNum = outTotalNormalNum + record.totalNum;
    			outTotalNormalAmt = outTotalNormalAmt + record.totalBal / 10000;
    		}else if(status == accountStatusStopTemp){
    			outNeedStopTempNum = outNeedStopTempNum + record.accNum;
    			outNeedStopTempAmt = outNeedStopTempAmt + Math.abs(record.amount) / 10000;
    			outTotalStopTempNum = outTotalStopTempNum + record.totalNum;
    			outTotalStopTempAmt = outTotalStopTempAmt + record.totalBal / 10000;
    		}
    	}else if(type == accountTypeInBank){
    		if(status == accountStatusNormal || status == accountStatusEnabled){
    			inTotalNormalNum = inTotalNormalNum + record.totalNum;
    			inTotalNormalAmt = inTotalNormalAmt + record.totalBal / 10000;
    		}else if(status == accountStatusStopTemp){
    			inTotalStopTempNum = inTotalStopTempNum + record.totalNum;
    			inTotalStopTempAmt = inTotalStopTempAmt + record.totalBal / 10000;
    		}
    	}else if(type == accountTypeReserveBank){
    		if(status == accountStatusNormal || status == accountStatusEnabled){
    			reTotalNormalNum = reTotalNormalNum + record.totalNum;
    			reTotalNormalAmt = reTotalNormalAmt + record.totalBal / 10000;
    		}else if(status == accountStatusStopTemp){
    			reTotalStopTempNum = reTotalStopTempNum + record.totalNum;
    			reTotalStopTempAmt = reTotalStopTempAmt + record.totalBal / 10000;
    		}
    	}else if(type == accountTypeBindWechat || type == accountTypeBindAli || type == accountTypeThirdCommon || type == accountTypeBindCommon){
    		if(status == accountStatusNormal || status == accountStatusEnabled){
    			conTotalNormalNum = conTotalNormalNum + record.totalNum;
    			conTotalNormalAmt = conTotalNormalAmt + record.totalBal / 10000;
    		}else if(status == accountStatusStopTemp){
    			conTotalStopTempNum = conTotalStopTempNum + record.totalNum;
    			conTotalStopTempAmt = conTotalStopTempAmt + record.totalBal / 10000;
    		}
    	}else if(type == accountTypeInThird){
    		if(status == accountStatusNormal || status == accountStatusEnabled){
    			inThirdNormalNum = inThirdNormalNum + record.totalNum;
    			inThirdNormalAmt = inThirdNormalAmt + record.totalBal / 10000;
    		}else if(status == accountStatusStopTemp){
    			inThirdStopTempNum = inThirdStopTempNum + record.totalNum;
    			inThirdStopTempAmt = inThirdStopTempAmt + record.totalBal / 10000;
    		}
    	}
    });
	outNeedNormalAmt = outNeedNormalAmt.toFixed(2);
	outNeedStopTempAmt = outNeedStopTempAmt.toFixed(2);
	outTotalNormalAmt = outTotalNormalAmt.toFixed(2);
	outTotalStopTempAmt = outTotalStopTempAmt.toFixed(2);
	inTotalNormalAmt = inTotalNormalAmt.toFixed(2);
	inTotalStopTempAmt = inTotalStopTempAmt.toFixed(2);
	reTotalNormalAmt = reTotalNormalAmt.toFixed(2);
	reTotalStopTempAmt = reTotalStopTempAmt.toFixed(2);
	conTotalNormalAmt = conTotalNormalAmt.toFixed(2);
	conTotalStopTempAmt = conTotalStopTempAmt.toFixed(2);
	inThirdNormalAmt = inThirdNormalAmt.toFixed(2);
	inThirdStopTempAmt = inThirdStopTempAmt.toFixed(2);
	var myChart = echarts.init(document.getElementById('transCanvas'));
    //显示数据，可修改
    var data1 = [conTotalStopTempAmt,inThirdStopTempAmt,reTotalStopTempAmt,inTotalStopTempAmt,outNeedStopTempAmt,outTotalStopTempAmt];//柱子下层的数据
    var data2 = [conTotalNormalAmt,inThirdNormalAmt,reTotalNormalAmt,inTotalNormalAmt,outNeedNormalAmt,outTotalNormalAmt];//柱子上层的数据
    var data4 = [conTotalStopTempNum,inThirdStopTempNum,reTotalStopTempNum,inTotalStopTempNum,outNeedStopTempNum,outTotalStopTempNum];//柱子下层的个数
    var data5 = [conTotalNormalNum,inThirdNormalNum,reTotalNormalNum,inTotalNormalNum,outNeedNormalNum,outTotalNormalNum];//柱子上层的个数
    //总计
    var data3 = function() {
        var datas = [];
        for (var i = 0; i < data1.length; i++) {
            var numStr = ((+data1[i]) + (+data2[i])).toFixed(2).toString();
			if(i==0){
				datas.push("下发卡"+"\n"+numStr + " 万 / " + (data4[i]+data5[i]) + " 张");
			}else if(i==1){
				datas.push("第三方"+"\n"+numStr + " 万 / " + (data4[i]+data5[i]) + " 张");
			}else if(i==2){
				datas.push("备用卡"+"\n"+numStr + " 万 / " + (data4[i]+data5[i]) + " 张");
			}else if(i==3){
				datas.push("入款卡"+"\n"+numStr + " 万 / " + (data4[i]+data5[i]) + " 张");
			}else if(i==4){
				datas.push("出款卡【待下发】"+"\n"+numStr + " 万 / " + (data4[i]+data5[i]) + " 张");
			}else if(i==5){
				datas.push("出款卡"+"\n"+numStr + " 万 / " + (data4[i]+data5[i]) + " 张");
			}
        }
        return datas;
    }();
    option = {
        title: {
            text: '',
            left: 'center',
            top: 'top',
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis : [
            {
                type : 'category',
                data : data3,
            }
        ],
        yAxis : [
            {
                type : 'value'
            }
        ],
        series : [
            {
                type:'bar',
                stack:'sum',
                itemStyle:{
                    normal:{
                        label: {
                            show: true,
                            formatter:function(num)  {  
                                var numStr = num.data.toString();
                                return numStr + " 万 / " + data4[num.dataIndex] + " 张"
                            }  
                        },
                        color:'#DD5A43'
                    }
                },
                data:data1
            },
            {
                type:'bar',
                stack:'sum',
                // barWidth: '20%',
                itemStyle:{
                    normal:{
                        label: {
                            show: true,
                            formatter:function(num)  {  
                                var numStr = num.data.toString();
                                return numStr + " 万 / " + data5[num.dataIndex] + " 张"
                            }  
                        },
                        color:'#82AF6F'
                    }
                },
                data:data2
            }
 
        ]
    };
    myChart.setOption(option);
}

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
    var accountType =  $.trim($tab.find('input[name=search_In_accountType1]:checked').val());
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
        trans.frTypeName = !trans.frType?'未知':(trans.frType == accountTypeInBank?'入款卡':(trans.frType == accountTypeReserveBank?'备用卡':(trans.frType == accountTypeBindCustomer?'客户绑定卡':(trans.frType == accountTypeInThird?'第三方':'下发卡'))));
        var uName = trans.username.toLowerCase()=='admin'?'系统':trans.username;
        html = html +'<tr>'
            + ' <td>'+ (1+parseInt(index)) +'</td>'
            + ' <td>'+ $.trim(trans.toHandiCapName) +'</td>'
            + ' <td>'+ trans.toCslName +'</td>'
            + ' <td>'+ trans.orderNo +'</td>'
            + ' <td>'+ trans.frTypeName +'</td>'
            + ' <td>'+ genAccHoverHtml(trans.frId,trans.frAlias,trans.frAcc,hideName(trans.frOwner),trans.frBankType,true) +'</td>'
            + ' <td>'+ genAccHoverHtml(trans.toId,trans.toAlias,trans.toAcc,hideName(trans.toOwner),trans.toBankType,true) +'</td>'
            + ' <td>'+ trans.transAmt +'</td>'
            + ' <td>'+ genAsignTimeHtml(trans.createTime) +'</td>'
            + ' <td style="'+genConsumeStype(currMillis,trans.createTime)+'">'+trans.timeConsume +'</td>'
            + ' <td>'+ uName +'</td>'
            + ' <td>'+ genRemarkHtml(trans.remark) +'</td>'
            + ' <td><button type="button" onclick="openRemarkModal(\''+$.trim(trans.orderNo)+'\')" class="btn btn-xs btn-white btn-warning btn-bold"><i class="ace-icon fa fa-list bigger-100 orange"></i>备注</button></td>'
            + '</tr>';
    }
    var $tbody = $tab.find('table tbody');
    $tbody.html(html);
    showSubAndTotalStatistics4Table($tbody,{column:13, subCount:dataList.length,8:{subTotal:subTotal}});
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
        trans.hasTask = trans.priority == 1 ? '第三方下发':'非第三方下发';
        trans.toCslName = !trans.toCsl?'未知':(trans.toCsl == 1?'外层':(trans.toCsl == 2?'内层':'指定层'));
        html = html + '<tr>'
            + ' <td>'+ (1+parseInt(index)) +'</td>'
            + ' <td>'+ $.trim(trans.toHandiCapName) +'</td>'
            + ' <td>'+ trans.toCslName+'</td>'
            + ' <td>'+ genAccHoverHtml(trans.toId,trans.toAlias,hideAccountAll(trans.toAcc),hideName(trans.toOwner),trans.toBankType,true) +'</td>'
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
    var $tab = $(tabId), csl = [],startTime=null,endTime=null,fType = [];
    var search_LIKE_bankType =$tab.find('select[name=search_LIKE_bankType]').val();
    var bankType = search_LIKE_bankType =='请选择'?'':search_LIKE_bankType;
    var status = tabId != '#tabTransferFailure'?[incomeRequestStatusMatching,incomeRequestStatusMatched]:[incomeRequestStatusCanceled];
    var accref = $tab.find("input[name='account_outDrawing']").val();
    $tab.find("input[name='"+(tabId=='#tabTransferFailure' ?"search_In_accountType3":"search_In_accountType4")+"']:checked").each(function(){
    	fType.push(this.value);
    });
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
    $.ajax({dataType:'json',url:API.r_accountMonitor_issueList,data:{pageSize:pageSize,pageNo:pageNo,status:status.toString(),startTime:startTime,endTime:endTime,level:csl.toString(),bankType:bankType,accRef:accref,fromType:fType.toString()},success:function(jsonObject){
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
    onchangeZoneSelect();
}

var onchangeZoneSelect = function(){
    var $out_need_widget=$("#AsginInfoModal");
    var zone = $out_need_widget.find("select[name='zone_sync']").val();
    zone = !zone?0:zone;
    $.ajax({
        type: "POST", url: '/r/set/findOutUpLim', dataType: 'JSON', async: false,data:{zone:zone},
        success: function (res) {
            if (res.status != 1) {
                showMessageForFail('云端配置初始化失败');
                return;
            }
            $.each($out_need_widget.find('input[name=out_need_type]'), function (index, result) {
                if ($(result).val() == res.data.on1stop0) {
                    $(result).prop('checked', 'checked');
                }
            });
            if (res.data.on1stop0 == 0) {
                $out_need_widget.find("[name=out_need_expire_time]").text('');//过期时间
                $out_need_widget.find("[name=out_need_trig_limit]").val('');
                $out_need_widget.find("[name=out_need_up_limit]").val('');
                $out_need_widget.find("[name=out_need_last_time]").val('');
            } else {
                $out_need_widget.find("[name=out_need_expire_time]").html('&nbsp;&nbsp;&nbsp;&nbsp;过期时间：' + res.data.expireTime);
                $out_need_widget.find("[name=out_need_trig_limit]").val(res.data.triglimit);
                $out_need_widget.find("[name=out_need_up_limit]").val(res.data.uplimit);
                $out_need_widget.find("[name=out_need_last_time]").val(res.data.lastTime);
            }
        }
    });
};

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

contentRight();
getMonitorStat();