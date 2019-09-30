currentPageLocation = window.location.href;
var _statisticCompanyExpenditureForOne = function (handicapId, startTime, endTime) {
    if (handicapId) {
        $.ajax({
            type: 'get',
            url: '/r/out/statisticsCompany',
            dataType: 'json',
            async: false,
            data: {"handicap": handicapId, "startTime": startTime, "endTime": endTime},
            success: function (res) {
                if (res) {
                    $('#pieDataDiv' + handicapId).find('#piechart-placeholder'+handicapId).empty();
                    if (res.status == 1) {
                        if (res.data && res.data.length > 0) {
                            var handicapIdArray = [];
                            var dataArray = new Map();
                            $.each(res.data[0], function (i, val) {
                                var keyStr = i.toString().split('-');//i:盘口id-盘口名称
                                var dataArr = val;
                                var handicapId = keyStr[0];
                                handicapIdArray.push(handicapId);
                                dataArray.set(handicapId, dataArr);
                            });
                            $.each(handicapIdArray, function (i, v) {
                                //渲染饼图
                                initialEveryPieCart(v, dataArray.get(v));
                                $('#piechart-placeholder'+handicapId).attr('style','width: 90%; min-height: 150px; padding: 0px; position: relative;');
                                $('#piechart-placeholder'+handicapId).find('table').attr('style','position:absolute;top:0px;right:-30px;;font-size:smaller;color:#545454;display:block');
                            });
                        } else {
                            initialEveryPieCart(handicapId,null);
                            $('#pieDataDiv' + handicapId).find('#piechart-placeholder' + handicapId).empty().html('<span style="text-align: center;"><h3>' + res.message + '</h3></span>');
                            $('#piechart-placeholder'+handicapId).attr('style','width: 90%; min-height: 120px; padding: 0px; position: relative;');
                            $('#piechart-placeholder'+handicapId).find('.legend').attr('style','display:none');
                        }
                    }else {
                        initialEveryPieCart(handicapId,null);
                        $('#pieDataDiv' + handicapId).find('#piechart-placeholder' + handicapId).empty().html('<span style="text-align: center;"><h3>' + res.message + '</h3></span>');
                        $('#piechart-placeholder'+handicapId).attr('style','width: 90%; min-height: 120px; padding: 0px; position: relative;');
                        $('#piechart-placeholder'+handicapId).find('.legend').attr('style','display:none');
                    }
                }
            }
        });
    }
}
var _statisticCompanyExpenditure = function (handicapId, startTime, endTime) {
    var handicap = ''
    if (!handicapId) {
        handicap = $('#handicapNameSearch').val();
    } else {
        var herfForDetail = $('#detailHrefFor' + handicapId).attr('href');
        if (herfForDetail) {
            herfForDetail += '&startTime=' + startTime + '&endTime=' + endTime;
            $('#detailHrefFor' + handicapId).attr('href', herfForDetail);
        }
        _statisticCompanyExpenditureForOne(handicapId, startTime, endTime);
        return;
    }
    $.ajax({
        type: 'get',
        url: '/r/out/statisticsCompany',
        dataType: 'json',
        data: {"handicap": handicap, "startTime": startTime, "endTime": endTime},
        success: function (res) {
            if (res) {
                if (res.status == 1) {
                    $('#statisticDiv').empty();
                    if (res.data && res.data.length > 0) {
                        var handicapIdArray = [];
                        var dataArray = new Map();
                        var divStr = '';
                        $.each(res.data[0], function (i, val) {
                            var keyStr = i.toString().split('-');
                            var dataArr = val;
                            var handicapId = keyStr[0];
                            handicapIdArray.push(handicapId);
                            dataArray.set(handicapId, dataArr);
                            var handicapName = keyStr[1];
                            divStr +=
                                '<div id="pieDataDiv' + handicapId + '" class="col-sm-4">' +
                                '<div class="widget-box">' +
                                '<div class="widget-header widget-header-flat widget-header-small">' +
                                '<h5 class="widget-title">' +
                                '<i class="ace-icon fa fa-signal"></i>' + handicapName + '</h5>' +
                                '<a id="detailHrefFor' + handicapId + '" href="#/financeCompanyExpenditureDetail:*?handicapId=' + handicapId + '&handicapName=' + handicapName + '"><button type="button" class="badge badge-pink  statisticDetail" onclick="">' +
                                '<i class="icon-only ace-icon fa fa-align-justify"></i>明细' +
                                '</button></a>' +
                                '<div class="widget-toolbar no-border" >' +
                                '<div class="inline dropdown-hover" >' +
                                '<button id="timeShow' + handicapId + '" class="btn btn-minier btn-primary">全部' +
                                '<i class="ace-icon fa fa-angle-down icon-on-right bigger-110"></i>' +
                                '</button>' +
                                '<ul class="dropdown-menu dropdown-menu-right dropdown-125 dropdown-lighter dropdown-close dropdown-caret">' +
                                '<li ><a href="javascript:void(0);" onmouseup="_liSelectTimeEvent(this,' + handicapId + ');" >' +
                                '<i class="ace-icon fa fa-caret-right bigger-110 invisible">&nbsp;</i>全部</a>' +
                                '</li>' +
                                '<li ><a href="javascript:void(0);" onmouseup="_liSelectTimeEvent(this,' + handicapId + ');" >' +
                                '<i class="ace-icon fa fa-caret-right bigger-110 invisible">&nbsp;</i>本周</a>' +
                                '</li>' +
                                '<li ><a href="javascript:void(0);" onmouseup="_liSelectTimeEvent(this,' + handicapId + ');" >' +
                                '<i class="ace-icon fa fa-caret-right bigger-110 invisible">&nbsp;</i>上周</a>' +
                                '</li>' +
                                '<li><a href="javascript:void(0);" onmouseup="_liSelectTimeEvent(this,' + handicapId + ');" >' +
                                '<i class="ace-icon fa fa-caret-right bigger-110 invisible">&nbsp;</i>本月</a>' +
                                '</li>' +
                                '<li><a href="javascript:void(0);" onmouseup="_liSelectTimeEvent(this,' + handicapId + ');" >' +
                                '<i class="ace-icon fa fa-caret-right bigger-110 invisible">&nbsp;</i>上月</a>' +
                                '</li>' +
                                '</li>' +
                                '<li><a href="javascript:void(0);" onmouseup="_liSelectTimeEvent(this,' + handicapId + ');" >' +
                                '<i class="ace-icon fa fa-caret-right bigger-110 invisible">&nbsp;</i>本季</a>' +
                                '</li>' +
                                '</li>' +
                                '<li><a href="javascript:void(0);" onmouseup="_liSelectTimeEvent(this,' + handicapId + ');" >' +
                                '<i class="ace-icon fa fa-caret-right bigger-110 invisible">&nbsp;</i>上季</a>' +
                                '</li>' +
                                '</ul></div></div></div>' +
                                '<div class="widget-body">' +
                                '<div class="widget-main statisticPiesHolder">' +
                                '<div id="piechart-placeholder' + handicapId + '"></div></div></div>' +
                                '</div></div>';
                        });
                        $('#statisticDiv').append(divStr).show();
                        $.each(handicapIdArray, function (i, v) {
                            //渲染饼图 i 是盘口id v 是对应数据
                            initialEveryPieCart(v, dataArray.get(v));
                        });

                    } else {
                        $('#statisticDiv').empty().html('<span style="text-align: center;"><h3>' + res.message + '</h3></span>');
                    }
                } else {
                    $('#statisticDiv').empty().html('<span style="text-align: center;"><h3>' + res.message + '</h3></span>');
                }
            } else {
                $('#statisticDiv').empty().html('<span style="text-align: center;"><h3>' + res.message + '</h3></span>');
            }
        }
    });
}
var initialEveryPieCart = function (id, dataArr) {
    var placeholder = $('#piechart-placeholder' + id).css({'width': '90%', 'min-height': '150px'});
    var data = [];
    var color = ["#68BC31", "#2091CF", "#AF4E96", "#DA5430", "#FEE074", "#68BC31", "#2091CF", "#AF4E96", "#DA5430", "#FEE074"];
    for (var i in dataArr) {
        var dataArray = dataArr[i];
        // dataArray[0]来源  dataArray[1]百分比 dataArray[4] 总金额
        data.push({
            label: dataArray[0] + '(' + dataArray[1] + '笔,金额:' + dataArray[4] + ')',
            data: dataArray[1],
            color: color[i]
        });
    }
    function drawPieChart(placeholder, data, position) {
        $.plot(placeholder, data, {
            series: {
                pie: {
                    show: true,
                    tilt: 0.9,
                    highlight: {
                        opacity: 0.50
                    },
                    stroke: {
                        color: '#fff',
                        width: 1
                    },
                    startAngle: 1
                }
            },
            legend: {
                show: true,
                position: position || 'ne',
                labelBoxBorderColor: null,
                margin: [-30, 0]
            }
            ,
            grid: {
                hoverable: true,
                clickable: true
            }
        })
    }

    drawPieChart(placeholder, data);

    /**
     we saved the drawing function and the data to redraw with different position later when switching to RTL mode dynamically
     so that's not needed actually.
     */
    placeholder.data('chart', data);
    placeholder.data('draw', drawPieChart);

    //pie chart tooltip example
    var $tooltip = $("<div class='tooltip top in'><div class='tooltip-inner'></div></div>").hide().appendTo('body');
    var previousPoint = null;

    placeholder.on('plothover', function (event, pos, item) {
        if (item) {
            if (previousPoint != item.seriesIndex) {
                previousPoint = item.seriesIndex;
                var tip = item.series['label'] + " : " + (item.series['percent']).toFixed(1) + '%';
                $tooltip.show().children(0).text(tip);
            }
            $tooltip.css({top: pos.pageY, left: pos.pageX});
        } else {
            $tooltip.hide();
            previousPoint = null;
        }

    });
};
/**重置*/
function _statisticCompanyExpenditureReset() {
    $('#handicapNameSearch').val('');
    _statisticCompanyExpenditure('', '', '');
}
/**选择时间*/
function _liSelectTimeEvent(obj, handicapId) {
    var startTime = '';
    var endTime = '';
    if (obj) {
        if ($(obj).parent().prop('class') == 'active') {
            $(obj).parent().prop('class', '');
            $(obj).prop('class', '');
        } else {
            $(obj).parent().prop('class', 'active');
            $(obj).prop('class', "blue");
            $(obj).parent().siblings().prop('class', '');
            $(obj).parent().siblings().find('a').prop('class', "");
            var timeArray = [];
            if ($(obj).html().indexOf('本周') > -1) {
                timeArray = DateTimeUtil.getCurrentWeek();
                startTime = timeArray[0].Format("yyyy-MM-dd hh:mm:ss");
                endTime = timeArray[1].Format("yyyy-MM-dd hh:mm:ss");
                _statisticCompanyExpenditure(handicapId, startTime, endTime);
                $('#timeShow' + handicapId).empty().html('本周<i class="ace-icon fa fa-angle-down icon-on-right bigger-110 visible"></i>');
            }
            else if ($(obj).html().indexOf('上周') > -1) {
                timeArray = DateTimeUtil.getPreviousWeek();
                startTime = timeArray[0].Format("yyyy-MM-dd hh:mm:ss");
                endTime = timeArray[1].Format("yyyy-MM-dd hh:mm:ss");
                _statisticCompanyExpenditure(handicapId, startTime, endTime);
                $('#timeShow' + handicapId).empty().html('上周<i class="ace-icon fa fa-angle-down icon-on-right bigger-110 visible"></i>');
            }
            else if ($(obj).html().indexOf('本月') > -1) {
                timeArray = DateTimeUtil.getCurrentMonth();
                startTime = timeArray[0].Format("yyyy-MM-dd hh:mm:ss");
                endTime = timeArray[1].Format("yyyy-MM-dd hh:mm:ss");
                _statisticCompanyExpenditure(handicapId, startTime, endTime);
                $('#timeShow' + handicapId).empty().html('本月<i class="ace-icon fa fa-angle-down icon-on-right bigger-110"></i>');

            }
            else if ($(obj).html().indexOf('上月') > -1) {
                timeArray = DateTimeUtil.getPreviousMonth();
                startTime = timeArray[0].Format("yyyy-MM-dd hh:mm:ss");
                endTime = timeArray[1].Format("yyyy-MM-dd hh:mm:ss");
                _statisticCompanyExpenditure(handicapId, startTime, endTime);
                $('#timeShow' + handicapId).empty().html('上月<i class="ace-icon fa fa-angle-down icon-on-right bigger-110"></i>');

            }
            else if ($(obj).html().indexOf('本季') > -1) {
                timeArray = DateTimeUtil.getCurrentSeason();
                startTime = timeArray[0].Format("yyyy-MM-dd hh:mm:ss");
                endTime = timeArray[1].Format("yyyy-MM-dd hh:mm:ss");
                _statisticCompanyExpenditure(handicapId, startTime, endTime);
                $('#timeShow' + handicapId).empty().html('本季<i class="ace-icon fa fa-angle-down icon-on-right bigger-110"></i>');

            }
            else if ($(obj).html().indexOf('上季') > -1) {
                timeArray = DateTimeUtil.getPreviousSeason();
                startTime = timeArray[0].Format("yyyy-MM-dd hh:mm:ss");
                endTime = timeArray[1].Format("yyyy-MM-dd hh:mm:ss");
                _statisticCompanyExpenditure(handicapId, startTime, endTime);
                $('#timeShow' + handicapId).empty().html('上季<i class="ace-icon fa fa-angle-down icon-on-right bigger-110"></i>');
            } else {
                $('#timeShow' + handicapId).empty().html('全部<i class="ace-icon fa fa-angle-down icon-on-right bigger-110"></i>');
                _statisticCompanyExpenditure(handicapId, startTime, endTime);
            }
        }
    }
}
/** * 针对时间的工具类 */
var DateTimeUtil = {
    /***     * 获得当前时间     */
    getCurrentDate: function () {
        return new Date();
    },
    /***     * 获得本周起止时间     */
    getCurrentWeek: function () {
        //起止日期数组
        var startStop = new Array();
        //获取当前时间
        var currentDate = this.getCurrentDate();
        //返回date是一周中的某一天
        var week = currentDate.getDay();
        //返回date是一个月中的某一天
        var month = currentDate.getDate();
        //一天的毫秒数
        var millisecond = 1000 * 60 * 60 * 24;
        //减去的天数
        var minusDay = week != 0 ? week - 1 : 6;
        //alert(minusDay);
        //本周 周一
        var monday = new Date(currentDate.getTime() - (minusDay * millisecond));
        //本周 周日
        var sunday = new Date(monday.getTime() + (6 * millisecond));
        //添加本周时间
        startStop.push(monday); //本周起始时间
        //添加本周最后一天时间
        startStop.push(sunday); //本周终止时间
        //返回
        return startStop;
    },
    /**     * 获得上一周的起止日期     * **/
    getPreviousWeek: function () {
        //起止日期数组
        var startStop = new Array();
        //获取当前时间
        var currentDate = this.getCurrentDate();
        //返回date是一周中的某一天
        var week = currentDate.getDay();
        //返回date是一个月中的某一天
        var month = currentDate.getDate();
        //一天的毫秒数
        var millisecond = 1000 * 60 * 60 * 24;
        //减去的天数
        var minusDay = week != 0 ? week - 1 : 6;
        //获得当前周的第一天
        var currentWeekDayOne = new Date(currentDate.getTime() - (millisecond * minusDay));
        //上周最后一天即本周开始的前一天
        var priorWeekLastDay = new Date(currentWeekDayOne.getTime() - millisecond);
        //上周的第一天
        var priorWeekFirstDay = new Date(priorWeekLastDay.getTime() - (millisecond * 6));
        //添加至数组
        startStop.push(priorWeekFirstDay);
        startStop.push(priorWeekLastDay);
        return startStop;
    },
    /***     * 获得本月的起止时间     */
    getCurrentMonth: function () {
        //起止日期数组
        var startStop = new Array();
        //获取当前时间
        var currentDate = this.getCurrentDate();
        //获得当前月份0-11
        var currentMonth = currentDate.getMonth();
        //获得当前年份4位年
        var currentYear = currentDate.getFullYear();
        //求出本月第一天
        var firstDay = new Date(currentYear, currentMonth, 1);
        //当为12月的时候年份需要加1
        //月份需要更新为0 也就是下一年的第一个月
        if (currentMonth == 11) {
            currentYear++;
            currentMonth = 0; //就为
        } else {
            //否则只是月份增加,以便求的下一月的第一天
            currentMonth++;
        }
        //一天的毫秒数
        var millisecond = 1000 * 60 * 60 * 24;
        //下月的第一天
        var nextMonthDayOne = new Date(currentYear, currentMonth, 1);
        //求出上月的最后一天
        var lastDay = new Date(nextMonthDayOne.getTime() - millisecond);
        //添加至数组中返回
        startStop.push(firstDay);
        startStop.push(lastDay);
        //返回
        return startStop;
    },
    /**     * 获得上一月的起止日期     * ***/
    getPreviousMonth: function () {
        //起止日期数组
        var startStop = new Array();
        //获取当前时间
        var currentDate = this.getCurrentDate();
        //获得当前月份0-11
        var currentMonth = currentDate.getMonth();
        //获得当前年份4位年
        var currentYear = currentDate.getFullYear();
        //获得上一个月的第一天
        var priorMonthFirstDay = this.getPriorMonthFirstDay(currentYear, currentMonth);
        //获得上一月的最后一天
        var priorMonthLastDay = new Date(priorMonthFirstDay.getFullYear(), priorMonthFirstDay.getMonth(), this.getMonthDays(priorMonthFirstDay.getFullYear(), priorMonthFirstDay.getMonth()));
        //添加至数组
        startStop.push(priorMonthFirstDay);
        startStop.push(priorMonthLastDay);
        //返回
        return startStop;
    },
    /**     * 得到上季度的起止日期     * **/
    getPreviousSeason: function () {
        //起止日期数组
        var startStop = new Array();
        //获取当前时间
        var currentDate = this.getCurrentDate();
        //获得当前月份0-11
        var currentMonth = currentDate.getMonth();
        //获得当前年份4位年
        var currentYear = currentDate.getFullYear();
        //上季度的第一天
        var priorSeasonFirstDay = this.getPriorSeasonFirstDay(currentYear, currentMonth);
        //上季度的最后一天
        var priorSeasonLastDay = new Date(priorSeasonFirstDay.getFullYear(), priorSeasonFirstDay.getMonth() + 2, this.getMonthDays(priorSeasonFirstDay.getFullYear(), priorSeasonFirstDay.getMonth() + 2));
        //添加至数组
        startStop.push(priorSeasonFirstDay);
        startStop.push(priorSeasonLastDay);
        return startStop;
    },
    /**     * 获得本季度的起止日期     */
    getCurrentSeason: function () {
        //起止日期数组
        var startStop = new Array();
        //获取当前时间
        var currentDate = this.getCurrentDate();
        //获得当前月份0-11
        var currentMonth = currentDate.getMonth();
        //获得当前年份4位年
        var currentYear = currentDate.getFullYear();
        //获得本季度开始月份
        var quarterSeasonStartMonth = this.getQuarterSeasonStartMonth(currentMonth);
        //获得本季度结束月份
        var quarterSeasonEndMonth = quarterSeasonStartMonth + 2;
        //获得本季度开始的日期
        var quarterSeasonStartDate = new Date(currentYear, quarterSeasonStartMonth, 1);
        //获得本季度结束的日期
        var quarterSeasonEndDate = new Date(currentYear, quarterSeasonEndMonth, this.getMonthDays(currentYear, quarterSeasonEndMonth));
        //加入数组返回
        startStop.push(quarterSeasonStartDate);
        startStop.push(quarterSeasonEndDate);
        //返回
        return startStop;
    },
    /**     * 得到本季度开始的月份     * @param month 需要计算的月份     ***/
    getQuarterSeasonStartMonth: function (month) {
        var spring = 0; //春
        var summer = 3; //夏
        var fall = 6; //秋
        var winter = 9; //冬
        //月份从0-11
        if (month < 3) {
            return spring;
        }
        if (month < 6) {
            return summer;
        }
        if (month < 9) {
            return fall;
        }
        return winter;
    },
    /**     * 获得该月的天数     * @param year年份     * @param month月份     * */
    getMonthDays: function (year, month) {
        //本月第一天 1-31
        var relativeDate = new Date(year, month, 1);
        //获得当前月份0-11
        var relativeMonth = relativeDate.getMonth();
        //获得当前年份4位年
        var relativeYear = relativeDate.getFullYear();
        //当为12月的时候年份需要加1
        //月份需要更新为0 也就是下一年的第一个月
        if (relativeMonth == 11) {
            relativeYear++;
            relativeMonth = 0;
        } else {
            //否则只是月份增加,以便求的下一月的第一天
            relativeMonth++;
        }
        //一天的毫秒数
        var millisecond = 1000 * 60 * 60 * 24;
        //下月的第一天
        var nextMonthDayOne = new Date(relativeYear, relativeMonth, 1);
        //返回得到上月的最后一天,也就是本月总天数
        return new Date(nextMonthDayOne.getTime() - millisecond).getDate();
    },
    /**     * 返回上一个月的第一天Date类型     * @param year 年     * @param month 月     **/
    getPriorMonthFirstDay: function (year, month) {
        //年份为0代表,是本年的第一月,所以不能减
        if (month == 0) {
            month = 11;
            //月份为上年的最后月份
            year--; //年份减1
            return new Date(year, month, 1);
        }
        //否则,只减去月份
        month--;
        return new Date(year, month, 1);
        ;
    },
    /**     * 得到上季度的起始日期     * year 这个年应该是运算后得到的当前本季度的年份     * month 这个应该是运算后得到的当前季度的开始月份     * */
    getPriorSeasonFirstDay: function (year, month) {
        var spring = 0; //春
        var summer = 3; //夏
        var fall = 6; //秋
        var winter = 9; //冬
        //月份从0-11
        switch (month) { //季度的其实月份
            case 0:
            case 1:
            case 2:
                //如果是第一季度则应该到去年的冬季
                year--;
                month = winter;
                break;
            case 3:
            case 4:
            case 5:
                month = spring;
                break;
            case 6:
            case 7:
            case 8:
                month = summer;
                break;
            case 9:
            case 10:
            case 11:
                month = fall;
                break;
        }
        ;
        return new Date(year, month, 1);
    }
};
Date.prototype.Format = function (fmt) {
    var o = {
        "M+": this.getMonth() + 1, //月份
        "d+": this.getDate(), //日
        "h+": this.getHours(), //小时
        "m+": this.getMinutes(), //分
        "s+": this.getSeconds(), //秒
        "q+": Math.floor((this.getMonth() + 3) / 3), //季度
        "S": this.getMilliseconds() //毫秒
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}
_statisticCompanyExpenditure("", "", "");