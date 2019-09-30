

var NODATA = '<div style="margin-bottom:0px;font-size: 20px;width:100%;" class="alert alert-success center">无数据</div>';

function searchByFilter(){
    var req = {};
    var problemAccInv =  $("#problemAccInv");
    req.search_EQ_handicapId = problemAccInv.find("select[name='search_EQ_handicapId']").val();
    req.search_EQ_bankType = problemAccInv.find("select[name=search_EQ_bankType]").val();
    req.search_EQ_alias=problemAccInv.find("input[name=search_EQ_alias]").val();
    req.search_EQ_flag=problemAccInv.find("input[name='search_EQ_flag']:checked").val();
    var tbody =  problemAccInv.find("table#dynamic-table tbody");
    var tableFoot = problemAccInv.find("#accountPage");
    tableFoot.html('');
    $.ajax({ dataType:'json',type:"get",url:'/r/problem/accInv',data:req,success:function(jsonObject){
        if(jsonObject.status == 1){
            var data = jsonObject.data;
            if(!data||data.length==0){
                tbody.html('');
                tableFoot.html(NODATA);
                return;
            }
            var html = '';
            var idList=new Array();
            var nowTm = new Date().getTime();
            $.each(data,function(idx, obj) {
                idList.push({'id':obj.id});
                var flowTm = '<span class="flow label label-grey" style="display:block;width:99%;float:left;" title=""></span>';
                if(obj.offLine!=1){
                    var classInf = obj.tmBal ==0||(obj.tmBal > 0 && (nowTm-obj.tmBal)< 180000)?'label-success':'label-warning';
                    classInf = 'flow label '+(obj.tmBal && obj.tmBal > 0 &&((nowTm-obj.tmBal)>= 600000)?'label-danger':classInf);
                    flowTm = '<span class="'+classInf+'" style="display:block;width:99%;float:left;" title="">'+obj.tmBal >0?('余额:'+geeTime4Crawl(obj.tmBal)):''+'</span>';
                }
                var balTm = '<span class="flow label label-grey" style="display:block;width:99%;float:left;" title="">离线</span>';
                if(obj.offLine!=1){
                    var classInf = obj.tmLog ==0||(obj.tmLog > 0 && (nowTm-obj.tmLog)< 180000)?'label-success':'label-warning';
                    classInf = 'flow label '+(obj.tmLog && obj.tmLog > 0 &&((nowTm-obj.tmLog)>= 600000)?'label-danger':classInf);
                    balTm = '<span class="'+classInf+'" style="display:block;width:99%;float:left;" title="">'+obj.tmLog >0?('流水:'+geeTime4Crawl(obj.tmLog)):'已连接'+'</span>';
                }
                var bg = obj.tmGap == 0?'':(obj.tmGap == 1?'color:white;background-color:limegreen':'color:white;background-color:indianred');
                html = html + '<tr>';
                html = html +   '<td>'+obj.handicap+'</td>';
                html = html +   '<td>'+obj.typeName+'|'+obj.flagSim+'</td>';
                html = html +   '<td><a class="bind_hover_card" data-toggle="accountInfoHover'+obj.id+'" data-placement="auto right" data-trigger="hover"><span>'+obj.frAccSim+balTm+'</span></a></td>';
                html = html +   '<td>'+obj.bankBalance+flowTm+'</td>';
                html = html +   '<td>'+obj.balance+'</td>';
                html = html +   '<td>'+obj.alarmType+'</td>';
                html = html +   '<td>'+obj.tmFull+'</td>';
                html = html +   '<td style="'+bg+'">'+obj.tmUsed+'</td>';
                html = html +   '<td>';
                html = html +       '<button class="btn btn-xs btn-white btn-primary btn-bold" onclick="report_showInOutListModal('+obj.id+')"><i class="ace-icon fa fa-check bigger-100 green"></i><span>处理</span></button>';
                html = html +       '<button class="btn btn-xs btn-white btn-primary btn-bold orange" onclick="showInOutListModal('+obj.id+')"><i class="ace-icon fa fa-list bigger-100 orange"></i><span>明细</span></button>';
                html = html +   '</td>';
                html = html + '</tr>';
            });
            tbody.html(html);
            loadHover_accountInfoHover(idList);
        }else {
            tableFoot.html(NODATA);
            bootbox.alert(jsonObject.message);
        }
    },error:function(result){tableFoot.html(NODATA); ;bootbox.alert(result);}});
};

getHandicap_select($("select[name='search_EQ_handicapId']"),null,"全部");
getBankTyp_select($("select[name='search_EQ_bankType']"),null,"全部");

searchByFilter();

initRefreshSelect($("#accountFilter #refreshProblemAccInv"),$("#accountFilter #search-button"),75,"refresh_ProblemAccInv");
