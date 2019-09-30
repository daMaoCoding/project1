currentPageLocation = window.location.href;
/**
 * 校验类 creater: Anastasia
 */
//输入校验提示
var showMessageForCheck=function(message,$element) {
	//聚焦
	if($element) $element.focus();

	$.gritter.add({
		title :'<img src="../images/tips24.png" />'+'输入提示：',
		text : message,
		stiscky : false,
		time : 1000,
		speed : 100,
        position: 'bottom-right',
		class_name: 'gritter-error'
	});
}

//非空校验
var validateEmpty=function($element,name){
	if($.trim($element.val())){
		return true;
	}else{
		showMessageForCheck(name+"不能为空",$element);
		return false;
	}
}

/**
 * 批量非空校验
 * [{ele:$amount,name:'金额'},{ele:$remark,name:'备注'}]
 */
var validateEmptyBatch=function(list){
	var isOK=true;
	$.each(list,function(index,result){
		if(!validateEmpty(result.ele,result.name)){
			isOK=false;
			//结束循环
			return false;
		}
	});
	return isOK;
}

/**
 * 批量输入校验
 * [{ele:$amount,name:'金额',type:'amount'},{ele:$remark,name:'备注',type:'remark'}]
 */
var validateInput=function(list){
	var isOK=true,okLength=true,exp,info;
	$.each(list,function(index,result){
		var str=$.trim(result.ele.val());
		if(str){
			//不可小于等于此值
			if((result.min||result.min==0)&&str*1<=result.min){
				okLength=false;
				info="值必须大于:"+result.min;
			}
			//不可以大于等于此值
			if((result.max||result.max==0)&&str*1>=result.max){
				okLength=false;
				info="值必须小于:"+result.max;
			}
			//最小值
			if((result.minEQ||result.minEQ==0)&&str*1<result.minEQ){
				okLength=false;
				info="值必须大于等于:"+result.minEQ;
			}
			//最大值
			if((result.maxEQ||result.maxEQ==0)&&str*1>result.maxEQ){
				okLength=false;
				info="值必须小于等于:"+result.maxEQ;
			}
			//字符串长度校验
			if(result.minLength&&str.length<result.minLength){
				okLength=false;
				info="最小长度为:"+result.minLength;
			}
			if(result.maxLength&&str.length>result.maxLength){
				okLength=false;
				info="最大长度为:"+result.maxLength;
			}
			if(result.eqLength&&str.length!=result.eqLength){
				okLength=false;
				info="长度应为:"+result.eqLength;
			}
			//有值才校验正则
			if(!okLength){
				showMessageForCheck(result.name+info,result.ele);
				isOK=false;
				return false;
			}
			if(result.type){
				if(!(result.type=='amountCanZero'&&str==0)){
					if(result.type=='amountPlus'||result.type=='amountCanZero'){
						//非0正数 小数点前8位，小数点后2位
						exp=/^([1-9][\d]{0,7}|0)(\.[\d]{1,3})?$/;
						info="值区间：0.001 ~ 99,999,999.999";
					}else if(result.type=='amount'){
						//正负数都可以 小数点前8位，小数点后2位
						exp=/^(-)?([1-9][\d]{0,7}|0)(\.[\d]{1,3})?$/;
						info="值区间：-99,999,999.999 ~ 99,999,999.999";
					}else if(result.type=='positiveInt'){
						//正整数
						exp=/^[0-9]+$/;
						info="必须是正整数";
					}
					//有值才校验正则
					if(str==0||(exp&&str&&!exp.test(str))){
						showMessageForCheck(result.name+info,result.ele);
						isOK=false;
						return false;
					}
				}
			}
		}
	});	
	return isOK;
}
/**
 * 根据用户名读取用户ID，如果存在，返回list 不存在 返回false
 */
var checkUserName=function($element,name){
		var userIdList=new Array();
		var authRequest = {"pageNo": 0,"userNameLike":$.trim($element.val())};
		//读取账号id，并返回id
		$.ajax({
			type: "POST", 
			url: "/r/user/findUserCategoryInfo", 
			data: authRequest, 
			async:false,
			dataType: 'JSON', 
			success: function (res) {
				//查询成功并返回用户集合
		        if (res.status == 1 && res.data.length>0) {
		        	userIdList.push()
		        }
			}
		});
}

