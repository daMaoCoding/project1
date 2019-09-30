/**
 *
 *find the start time and end tiem of  the current day.
 * @return
 * object[0]  the milliseconds of the start time;
 * object[1]  milliseconds of the end time ;
 */
var getStTmAndEdTm4Tdy = function () {
    return [moment().hours(0).minutes(0).seconds(0) + '', moment().hours(23).minutes(59).seconds(59) + ''];
};

/**
 *  find the mobile number that transfer from the parent page.
 * <p>
 *     return '' if the mobile number doesn't exist.
 * </p>
 */
var getReqMobile = function () {
    var req = getRequest();
    return req.mobile ? req.mobile : '';
};

/**
 *the mobile number that transfer from the parant page.
 */
var mobile = getReqMobile();

/**
 *the default tab ID loaded.
 */
var defaultLoadTabId = 'tabAlipayToday';

var loadTabId = null;

var fillContent = function (pageNo, theTabId) {
    if (!theTabId) {
        theTabId = loadTabId;
        if (!theTabId) {
            theTabId = defaultLoadTabId;
        }
    } else {
        loadTabId = theTabId;
    }
    var params = packBonusParams(pageNo, theTabId);
    var $tab = $('#' + theTabId);
    var url = '/r/bonus/cloud/findIncomeLogByAliAcc';
    if (theTabId == 'tabWechatToday' || theTabId == 'tabWechatHis') {
        url = '/r/bonus/cloud/findIncomeLogByWecAcc';
    } else if (theTabId == 'tabBonus') {
        url = '/r/bonus/cloud/bonus';
    }
    var html = '';
    $.ajax({
        type: "PUT", dataType: 'JSON', url: url, data: params, success: function (jsonObject) {
            if (jsonObject.status != 1) {
                bootbox.alert("请先绑定账号信息！");
            }
            if(jsonObject.data){
            	 $.each(jsonObject.data.content, function (i, record) {
                     record.remark = record.remark ? record.remark : '';
                     record.remarkShort = record.remark ? record.remark.slice(0, 10) : '';
                     record.remark = record.remark.replace(/\r\n/g, '</br>');
                     if (theTabId != 'tabBonus') {
                         html += '<tr>\
                         <td>' + record.account + '</td>\
                         <td>' + timeStamp2yyyyMMddHHmmss(record.tradingTime) + '</td>\
                         <td>' + timeStamp2yyyyMMddHHmmss(record.createTime) + '</td>\
                         <td>' + record.amount + '</td>\
                         <td>' + record.summary + '</td>\
                         <td><a class="breakByWord" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + record.remark + '" data-original-title="备注信息">' + record.remarkShort + '</a></td>\
                         </tr>';
                     } else {
                         html += '<tr>\
                         <td>' + record.fromAccount + '</td>\
                         <td>' + record.toAccount + '</td>\
                         <td>' + record.toBank + '</td>\
                         <td>' + record.toAccount + '</td>\
                         <td>' + record.amount + '</td>\
                         <td>' + record.status + '</td>\
                         <td>' + timeStamp2yyyyMMddHHmmss(record.createTime) + '</td>\
                         <td><a class="breakByWord" data-html="true" data-toggle="popover" data-trigger="hover" data-placement="left" data-content="' + record.remark + '" data-original-title="备注信息">' + record.remarkShort + '</a></td>\
                         </tr>';
                     }
                 });
                 $tab.find('table tbody').html(html);
                 $("[data-toggle='popover']").popover();
                 showPading(jsonObject.data, theTabId + 'Page', fillContent);
             }
            }
    });
};

/**
 *init tab content when click the tab navigation
 * such as Alipay Log Records Today, Wechat Log Records Today...
 * @param htmObj
 *          Html Element Object '<a></a>', of  the tab navigation clicked.
 *
 */
var initBonusTab = function (htmObj) {
    var href = $(htmObj).attr('href');
    var theTabId = href.slice(1);
    fillContent(0, theTabId);
};

/**
 *
 * pack the query params
 *
 * @param currPage
 *              the current page Number.
 *              <code>!currPage == true</code> if the param doesn't exist at present.
 * @param theTabId
 *              the current page tab's ID
 *              <code>theTabId == 'tabAlipayToday' </code>  if the param doesn't exist .
 */
var packBonusParams = function (currPage, theTabId) {
    theTabId || ( theTabId = 'tabAlipayToday');
    var $currPage = $('#' + theTabId + 'Page').find('.Current_Page');
    currPage = (currPage && currPage > 0 || currPage == 0) ? currPage : ($currPage.text() ? $currPage.text() - 1 : 0);
    currPage = currPage < 0 ? 0 : currPage;
    var pageSize = $.session.get('initPageSize');

    var data = {
        pageNo: currPage,
        pageSize: pageSize,
        mobile: mobile
    };

    if (theTabId == 'tabAlipayToday' || theTabId == 'tabWechatToday') {
        var stEd = getStTmAndEdTm4Tdy();
        data.startTime = stEd[0];
        data.endTime = stEd[1];
    } else {
        var $tab = $('#' + theTabId);
        var startAmt = $tab.find('input[name=startAmt]').val();
        var endAmt = $tab.find('input[name=endAmt]').val();
        if (startAmt) {
            data.startAmt = startAmt;
        }
        if (endAmt) {
            data.endAmt = endAmt;
        }
        var startAndEndTime = $tab.find('input[name=startAndEndTime]').val();
        if (startAndEndTime) {
            var stEdTm = startAndEndTime.split(' - ');
            data.startTime = new Date(stEdTm[0]).getTime() + '';
            data.endTime = new Date(stEdTm[1]).getTime() + '';
        }
    }
    return data;
};


/**
 * load  page content  when init .
 */
fillContent(null, defaultLoadTabId);
initTimePicker();