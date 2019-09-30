package com.xinbo.fundstransfer;

public class SqlConstants {

	/**
	 * countQuery sql
	 */
	public final static String SEARCH_QUEYACCOUNTSTATISTICS_COUNTQUERY = "select count(1) from ( select A.id account_id,A.account,A.alias,A.owner,A.bank_type,B.taAmount,B.count_,A.handicap_id from biz_account A left join "
			+ " (select ta.account_id,SUM(ta.amount)taAmount,count(1)count_ from "
			+ "biz_outward_request re,biz_outward_task ta "
			+ " where re.id=ta.outward_request_id and re.status in(5,6) and ta.status=5 "
			+ " and re.update_time BETWEEN ?2 and ?3"
			+ " GROUP BY ta.account_id)B on A.id=B.account_id where A.type=5 and (A.status in (1,4,5) or (A.status=3 and date_add(A.update_time, interval 2 day)>now()))"
			+ " and (?1 is null or A.account like concat('%',?1,'%'))"
			+ " and (?4 is null or A.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or A.bank_type like concat('%',?5,'%')) and (?6=0 or A.handicap_id=?6)) as total";

	public final static String findWechatMatched = "select count(1) from (select A.id "
			+ " from biz_wechat_request A,biz_wechat_log B,biz_account C"
			+ " where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
			+ " and (?8 is null or B.depositor like concat('%',?8,'%')) and (?9 is null or C.account like concat('%',?9,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
			+ " and A.wechat_log_id=B.id and A.wechatid=C.id and A.status=1 and B.status=1) as total";

	public final static String findAliMatched = "select count(1) from (select A.id "
			+ " from biz_alipay_request A,biz_alipay_log B,biz_account C"
			+ " where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
			+ " and (?8 is null or B.depositor like concat('%',?8,'%')) and (?9 is null or C.account like concat('%',?9,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
			+ " and A.alipay_log_id=B.id and A.alipayid=C.id and A.status=1 and B.status=1) as total";

	public final static String findMBAndInvoice = "select count(1) from (select id " + " from biz_wechat_request "
			+ " where wechatid=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or member_name like concat('%',?4,'%')) and (?5 is null or order_no =?5)"
			+ " and (?6=0 or amount>=?6) and (?7=0 or amount<=?7)) as total";

	public final static String findAliMBAndInvoice = "select count(1) from (select id " + " from biz_alipay_request "
			+ " where alipayid=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or member_name like concat('%',?4,'%')) and (?5 is null or order_no =?5)"
			+ " and (?6=0 or amount>=?6) and (?7=0 or amount<=?7)) as total";

	public final static String findBankLogMatch = "select count(1) from (select id " + " from biz_wechat_log"
			+ " where from_account=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or depositor like concat('%',?4,'%')) and (?5=0 or amount>=?5) and (?6=0 or amount<=?6)) as total";

	public final static String findAliBankLogMatch = "select count(1) from (select id " + " from biz_alipay_log"
			+ " where from_account=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or depositor like concat('%',?4,'%')) and (?5=0 or amount>=?5) and (?6=0 or amount<=?6)) as total";

	public final static String findWechatCanceled = "select count(1) from (select id,from_account,status,date_format(trading_time,'%Y-%m-%d %H:%i:%s'),amount,balance,remark,summary,depositor,date_format(create_time,'%Y-%m-%d %H:%i:%s') "
			+ " from biz_wechat_log"
			+ " where abs(TIMESTAMPDIFF(hour,now(),create_time)) >=24 and abs(TIMESTAMPDIFF(hour,now(),create_time))<=48 and status=0) as total";

	public final static String findAliCanceled = "select count(1) from (select id,from_account,status,date_format(trading_time,'%Y-%m-%d %H:%i:%s'),amount,balance,remark,summary,depositor,date_format(create_time,'%Y-%m-%d %H:%i:%s') "
			+ " from biz_alipay_log"
			+ " where abs(TIMESTAMPDIFF(hour,now(),create_time)) >=24 and abs(TIMESTAMPDIFF(hour,now(),create_time))<=48 and status=0) as total";

	public final static String findWechatUnClaim = "select count(1) from (select A.id "
			+ " from biz_wechat_log A,biz_account B " + " where (?2 is null or A.create_time between ?2 and ?3)"
			+ " and (abs(TIMESTAMPDIFF(hour,now(),A.create_time))) >=24 and (abs(TIMESTAMPDIFF(hour,now(),A.create_time)))<=48 and A.status=0"
			+ " and (?1=0 or B.handicap_id=?1) and (?4 is null or A.depositor like concat('%',?4,'%')) and (?5 is null or B.account like concat('%',?5,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and A.from_account=B.id ) as total";

	public final static String findAliUnClaim = "select count(1) from (select A.id "
			+ " from biz_alipay_log A,biz_account B " + " where (?2 is null or A.create_time between ?2 and ?3)"
			+ " and (abs(TIMESTAMPDIFF(hour,now(),A.create_time))) >=24 and (abs(TIMESTAMPDIFF(hour,now(),A.create_time)))<=48 and A.status=0"
			+ " and (?1=0 or B.handicap_id=?1) and (?4 is null or A.depositor like concat('%',?4,'%')) and (?5 is null or B.account like concat('%',?5,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and A.from_account=B.id ) as total";

	public final static String statisticalWechatLog = "select count(1) from (select A.from_account,count(1),B.handicap_id,B.account from biz_wechat_log A,biz_account B "
			+ " where A.from_account=B.id and"
			+ " A.status=0 and A.create_time between ?3 and ?4 and (?1=0 or B.handicap_id=4) and (?2=null or B.account like concat('%',?2,'%')) group by from_account) as total";

	public final static String statisticalAliLog = "select count(1) from (select A.from_account,count(1),B.handicap_id,B.account from biz_alipay_log A,biz_account B "
			+ " where A.from_account=B.id and"
			+ " A.status=0 and A.create_time between ?3 and ?4 and (?1=0 or B.handicap_id=4) and (?2=null or B.account like concat('%',?2,'%')) group by from_account) as total";

	public final static String SEARCH_QUEYACCOUNTSTATISTICS_COUNTQUERY_CLEARDATE = "select count(1) from (select A.account "
			+ " from biz_account A,biz_report B"
			+ " where B.time between ?2 and ?3 and (A.type in (5,14) or (A.type=1 and ROUND(B.outward_sys)=B.outward_sys)) and A.id=B.account_id "
			+ " and (?1 is null or A.account like concat('%',?1,'%')) and (?4 is null or A.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or A.bank_type like concat('%',?5,'%'))"
			+ " and B.account_handicap in (?6) and (?7 is null or A.type=?7)) as total";

	public final static String SEARCH_SHOWREBATE_USER = " select count(1) from ( select * from biz_account A left join "
			+ " (select C.uid,C.user_name,B.moible,B.margin from biz_account_more B,biz_rebate_user C where B.uid=C.uid) D "
			+ " on A.mobile=D.moible where flag=2 "
			+ " and (?1 is null or A.handicap_id=?1) and (?2 is null or A.bank_type like concat('%',?2,'%'))"
			+ " and A.type in (?3) and (?12 is null or A.sub_type=?12) "
			+ " and (?4 is null or A.alias=?4) and (?5 is null or A.account like concat('%',?5,'%')) and (?6 is null or A.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or A.curr_sys_level=?7) and A.status in (?8) and (?9 is null or D.user_name like concat('%',?9,'%'))"
			+ " order by margin desc,uid desc) N where  (?10 is null or N.margin>=?10) and (?11 is null or N.margin<=?11)";

	public final static String SEARCH_SHOWREBATE_STATISTICS = " select count(1) from (select * from biz_rebate_statistics brs where brs.statistics_date >= '?1' and brs.statistics_date <= '?2') as total";

	public final static String SEARCH_QUEYACCOUNTSTATISTICS_COUNTQUERYS = "select count(1) from (select lo.from_account accountid,ac.owner,ac.bank_type,SUM(ABS(amount))bkAmount,SUM(0.0)bkFee,count(1)count_ from "
			+ " biz_account ac,biz_bank_log lo" + " where lo.from_account=ac.id "
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) "
			+ " and (?2 is null or lo.trading_time between ?2 and ?3) and lo.amount<0"
			+ " and lo.status=1  and ac.type=5 " + " and (?4 is null or ac.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or ac.bank_type like concat('%',?5,'%'))" + " GROUP BY lo.from_account) as total";

	public final static String SEARCH_QUEYFINDFINOUTSTATSYS_COUNTQUERY = "select count(1) from (select "
			+ "(select name from biz_handicap where id=C.handicap) handicapname,"
			+ "(select name from biz_level where id=C.level and status=1) levelname," + "C.member,"
			+ "(select account from biz_account where id=B.account_id) accountname," + "C.to_account," + "B.amount,"
			+ "ifnull(0.0,0)fee," + "(select username from sys_user where id=B.operator) operatorname,"
			+ "date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s') asigntime"
			+ " from " + "biz_outward_task B,biz_outward_request C"
			+ " where B.outward_request_id=C.id and (?8=9999 or B.status=?8) and (?7=9999 or C.status=?7) and B.account_id=?1 and (?2 is null or C.member like concat('%',?2,'%'))  "
			+ " and C.update_time between ?3 and ?4 and (?5=0 or (B.amount>=?5 and B.amount<=?6))) as total";

	public final static String SEARCH_QUEYFINDFINOUTSTATFLOW_COUNTQUERY = "select count(1) from (select" + " B.id"
			+ " from " + "biz_bank_log B"
			+ " where (?6=0 or (abs(B.amount)>=?6 and abs(B.amount)<=?7)) and (?8=9999 or B.status=?8) and (?9=0 or ((?9=-1 or  B.amount>0) and (?9=1 or  B.amount<0))) and B.from_account=?1 and (?2 is null or B.to_account_owner like concat('%',?2,'%')) and (?3 is null or B.to_account like concat('%',?3,'%')) "
			+ " and unix_timestamp(B.create_time) between unix_timestamp(?4) and unix_timestamp(?5)) as total";

	public final static String SEARCH_QUEYFINDFINOUTSTATFLOWDETAILS_COUNTQUERY = "select count(1) from (select "
			+ "(select name from biz_handicap where id=D.handicap) handicapname,"
			+ "(select name from biz_level where id=D.level and status=1) levelname," + "D.member,"
			+ "D.to_account toaccount," + "D.to_account_name toaccountname," + "D.to_account_owner toaccountowner,"
			+ "A.to_account atoaccount," + "abs(A.amount) amount,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') tradingtime," + "B.type," + "D.amount damount,"
			+ "B.fee bfee,"
			+ "date_format((date_add(C.asign_time, interval C.time_consuming second)),'%Y-%m-%d %H:%i:%s') asigntime,"
			+ "(select username from sys_user where id=C.operator) operatorname,"
			+ "(select username from sys_user where id=B.confirmor) confirmor,"
			+ "(select account from biz_account where id=C.account_id)accountname,"
			+ "(select bank_name from biz_account where id=C.account_id)bankname,"
			+ "(select owner from biz_account where id=C.account_id)owner" + " from "
			+ "biz_bank_log A,biz_transaction_log B,biz_outward_task C,biz_outward_request D"
			+ " where A.id=B.from_banklog_id and B.order_id=C.id and C.outward_request_id=D.id "
			+ " and A.status=1 and A.amount<0 and B.type=0 and C.status=5 and D.status in (5,6) and A.id=?1) as total";

	public final static String SEARCH_QUEYACCOUNTSTATISTICSHANDICAP_COUNTQUERY = "select count(1) from (select "
			+ "A.handicap" + " from " + " biz_outward_request A,biz_outward_task B "
			+ " where A.id=B.outward_request_id and B.status=5 and A.status in (5,6) and (?1 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?1) and unix_timestamp(?2))"
			+ " and (?3 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ "group by A.handicap) as total";

	public final static String SEARCH_QUEYACCOUNTSTATISTICSHANDICAP_COUNTQUERY_FROMCLEARDATE = "select count(1) from (select B.account_handicap,sum(B.outward_sys),sum(fee),sum(outward_sys_count)"
			+ " from biz_account A,biz_report B "
			+ " where A.id=B.account_id and (A.type in (5,14) or (A.type=1 and ROUND(B.outward_sys)=B.outward_sys)) and B.time between ?2 and ?3 and B.account_handicap in (?1)"
			+ " group by B.account_handicap) as total";

	public final static String SEARCH_QUEYACCOUNTSTATISTICSBYHANDICAPANDLEVEL_COUNTQUERY = "select count(1) from (select "
			+ "(select name from biz_handicap where id=A.handicap)handicapname," + "A.handicap handicappno,"
			+ "(select name from biz_level where id=A.level and status=1) levelname," + "A.level levelno," + "B.amount,"
			+ "ifnull(0.0,0)fee," + "A.id" + " from " + " biz_outward_request A,biz_outward_task B"
			+ " where A.id=B.outward_request_id and A.status in (5,6) and (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?3 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?5) and unix_timestamp(?6))) as total";

	public final static String SEARCH_QUEYACCOUNTMATCHBYHANDICAP_COUNTQUERY = "select count(1) from (select "
			+ " B.outward_request_id " + " from " + " biz_outward_request A,biz_outward_task B"
			+ " where (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?6=0 or (A.amount>=?6 and A.amount<=?7)) and (?3 is null or A.member like concat('%',?3,'%'))"
			+ " and (?8=0 or A.handicap=?8) and A.handicap in (?12) and (?9=0 or  A.id=?9)  and (?10=9999 or A.status=?10) and (?11=9999 or B.status=?11) and "
			+ " (Case When A.update_time is null then A.create_time else A.update_time end) between ?4 and ?5 and A.id=B.outward_request_id) as total";

	public final static String SEARCH_QUEYFININSTATISTICS_COUNTQUERY = "select count(1) from (select * from (select ifnull(D.count_,0)count_,C.* "
			+ "from (select " + "A.id,B.name,"
			+ "A.account,ifnull(A.bank_balance,0),ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+ " from biz_account A,biz_handicap B "
			+ "where A.handicap_id=B.id and A.type in (?8) and A.handicap_id in (?1) and (?3 is null or A.account like concat('%',?3,'%'))"
			+ " and (?11 is null or A.owner like concat('%',?11,'%')) and (?12 is null or A.bank_type like concat('%',?12,'%'))) C"
			+ " left join " + " (select " + " C.to_id," + "count(1) count_ " + " from " + " biz_income_request C "
			+ " where C.type=?10 and (?2=0 or C.level=?2)" + " and (?4 is null or C.update_time between ?4 and ?5) "
			+ " and (?6 is null or C.update_time between ?6 and ?7) group by C.to_id) D on C.id=D.to_id) A "
			+ " where (0 = 0 or name=?9) and A.count_!=0) A";

	public final static String SEARCH_QUEYFININSTATISTICS_COUNTQUERY_FROM_CLEAR_DATE = "select count(1) from(select distinct * from ( select B.account_handicap,A.account,A.alias,"
			+ " A.owner,A.bank_type,B.balance,B.income,B.income_count,B.income_persons,B.income_sys,B.income_sys_count"
			+ " from biz_account A,biz_report B "
			+ " where B.time between ?3 and ?4 and A.type in (?5) and A.id=B.account_id  and B.account_handicap in (?1)"
			+ " and (?2 is null or A.account like concat('%',?2,'%'))"
			+ " and (?6 is null or A.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or A.bank_type like concat('%',?7,'%'))"
			+ "and (?8!='Bankcard' or ROUND(B.income)!=B.income))h)A";

	public final static String SEARCH_QUEYFININSTATISTICSSENDCARD_COUNTQUERY = "select count(1) from (select * from (select ifnull(D.amounts,0)amounts,ifnull(D.fees,0)fees,ifnull(D.count_,0)count_,C.* "
			+ "from (select " + "A.id,"
			+ "A.account,ifnull(A.bank_balance,0),ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+ " from biz_account A "
			+ "where A.status in (1,3,4,5) and A.type in (?6) and (?1 is null or A.account like concat('%',?1,'%'))"
			+ "and (?8 is null or A.owner like concat('%',?8,'%')) and (?9 is null or A.bank_type like concat('%',?9,'%'))) C"
			+ " left join " + " (select " + " B.to_account," + "sum(B.amount)amounts," + "sum(B.fee)fees,"
			+ "count(1) count_ " + " from " + " biz_transaction_log B,biz_income_request C "
			+ " where B.order_id=C.id and B.type=?7 " + " and (?2 is null or C.update_time between ?2 and ?3) "
			+ " and (?4 is null or C.update_time between ?4 and ?5) group by B.to_account) D on C.id=D.to_account) A "
			+ " where A.count_!=0) A";

	public final static String SEARCH_QUEYFINTHIRDINSTATISTICS_COUNTQUERY = "select count(1) from (select C.*,ifnull(D.amounts,0)amounts,ifnull(D.fees,0)fees,ifnull(D.count_,0)count_ "
			+ "from (select " + "A.id,B.name,B.id handicap_id,"
			+ " ifnull((select GROUP_CONCAT(name,'|') from biz_level where id in (select level_id from biz_account_level where account_id=A.id)  and (?2=0 or (handicap_id in (?1) and id=?2))),'无') levelname,"
			+ " A.account,ifnull(A.bank_balance,0)" + " from biz_account A,biz_handicap B "
			+ " where A.handicap_id=B.id and A.status in (1,3,4,5) and A.type=?8 and A.handicap_id in (?1) and (?3 is null or A.account like concat('%',?3,'%'))) C"
			+ " left join " + " (select C.handicap," + " C.to_account," + "sum(C.amount)amounts," + "sum(C.fee)fees,"
			+ "count(1) count_ " + " from " + " biz_third_request C "
			+ " where C.handicap in (?1) and (?2=0 or C.level=?2)"
			+ " and (?4 is null or C.ack_time between ?4 and ?5) "
			+ " and (?6 is null or C.ack_time between ?6 and ?7) and C.amount>0 group by C.to_account,C.handicap) D on C.account=D.to_account and C.handicap_id=D.handicap) A where A.amounts>0";

	public final static String SEARCH_QUEYINCOMETHIRD_COUNTQUERY = "select count(1)" + " from biz_income_request A "
			+ " where "
			+ " (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?3 is null or A.member_user_name like concat('%',?3,'%'))"
			+ " and (?7 is null or A.to_account like concat('%',?7,'%')) and (?8=0 or (A.amount>=?8 and A.amount<=?9))"
			+ " and (?4 is null or exists (select id from biz_account where id=A.to_id and bank_name like concat('%',?4,'%')))"
			+ " and A.status=1 and A.type=4 "
			+ " and unix_timestamp(A.update_time) between unix_timestamp(?5) and unix_timestamp(?6)";

	public final static String FINDHISTORY = "select count(1) from (select id,account_id,remark,date_format(time,'%Y-%m-%d %H:%i:%s')time,operator from biz_account_extra where account_id=?1) as total";

	public final static String SEARCH_FROSTLESS_COUNTQUERY = "select count(1) from (select " + "A.alias,"
			+ "A.handicap_id," + "(select name from biz_handicap where id=A.handicap_id)handicapname,"
			+ "(select GROUP_CONCAT(name,'||') from biz_level where handicap_id=A.handicap_id and status=1) levelname,"
			+ "A.type," + "A.account," + "A.bank_name," + "A.bank_balance" + " from " + " biz_account A"
			+ " where A.status=3" + " and A.handicap_id in (?1)"
			+ " and (?2 is null or A.account like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.update_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5 is null or unix_timestamp(A.update_time) between unix_timestamp(?5) and unix_timestamp(?6))"
			+ " and (?8 is null or A.type in (?7))) as total";

	public final static String SEARCH_FINDREBATE_COUNTQUERY = "select count(1) from ( SELECT A.id ," + "A.uid ,"
			+ "A.tid ," + "A.account_id ," + "A.to_account ," + "A.to_holder ," + "A.to_account_type ,"
			+ "A.to_account_info ," + "A.amount ," + "A.balance ," + "A.status ,"
			+ "date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time,"
			+ "date_format(A.update_time,'%Y-%m-%d %H:%i:%s') update_time," + "A.remark,A.handicap, "
			+ "date_format(A.asign_time,'%Y-%m-%d %H:%i:%s') asign_time,A.operator,A.screenshot, "
			+ " TIMESTAMPDIFF(SECOND,A.asign_time,now()) differenceMinutes,A.type,B.user_name "
			+ "FROM  biz_account_rebate A,biz_rebate_user B where A.uid=B.uid and A.status in (?1) and ((?2='rebate' or ?2='check' or ?2='canceled') or A.account_id is null) and (?2!='rebate' or A.account_id is not null)"
			+ " and (?3 is null or A.tid like concat('%',?3,'%')) and (?10 is null or B.user_name like concat('%',?10,'%')) and (?4 is null or A.amount>=?4) and (?5 is null or A.amount<=?5)"
			+ " and (?6 is null or A.update_time between ?6 and ?7) and (?8=0 or A.handicap=?8) and A.handicap in (?9)"
			+ " and (?11 is null or (?11='2' or (A.type is null or A.type=1)) and (?11='1' or A.type=2)))A";

	public final static String SEARCH_FINDREBATE_COMMISSION = "select count(1) from (select A.calc_time,sum(A.total_amount)total_amount,sum(A.amount)amount,count(distinct B.mobile)counts,A.status"
			+ " from fundsTransfer.biz_account_return_summary A,fundsTransfer.biz_account B where A.account=B.id and (?1 is null or calc_time between ?1 and ?2) and (?3 is null or A.status=?3) group by calc_time,A.status) as total";

	public final static String SEARCH_FINDDERATING_COUNTQUERY = "select count(1) from ( SELECT A.id ," + "A.uid ,"
			+ "A.tid ," + "A.account_id ," + "A.to_account ," + "A.to_holder ," + "A.to_account_type ,"
			+ "A.to_account_info ," + "A.amount ," + "A.balance ," + "A.status ,"
			+ "date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time,"
			+ "date_format(A.update_time,'%Y-%m-%d %H:%i:%s') update_time," + "A.remark,A.handicap, "
			+ "date_format(A.asign_time,'%Y-%m-%d %H:%i:%s') asign_time,A.operator,A.screenshot, "
			+ " TIMESTAMPDIFF(SECOND,A.asign_time,now()) differenceMinutes,A.type,B.user_name "
			+ "FROM  biz_account_rebate A,biz_rebate_user B where A.uid=B.uid and A.status in (?1) "
			+ " and (?2 is null or A.tid like concat('%',?2,'%')) and A.type=2 and (?7 is null or B.user_name like concat('%',?7,'%')) and (?3 is null or A.amount>=?3) and (?4 is null or A.amount<=?4)"
			+ " and (?5=0 or A.handicap=?5) and A.handicap in (?6))A";

	public final static String SEARCH_FINDREBATE_COMPLETE = "select count(1) from (select A.calc_time,ifnull(B.counts,0),ifnull(B.amounts,0),(A.rebateAmounts-ifnull(B.amounts,0)),A.remark from "
			+ " (select date_format(A.calc_time, '%Y-%m-%d')calc_time,sum(A.balance)rebateAmounts,A.remark"
			+ " from fundsTransfer.biz_account_return_summary A,fundsTransfer.biz_account B where A.account=B.id and A.status=1 group by date_format(calc_time, '%Y-%m-%d'),remark)A"
			+ " left join (select date_format(create_time, '%Y-%m-%d' )create_time,count(distinct uid)counts,sum(amount)amounts from "
			+ " fundsTransfer.biz_account_rebate where (status=1 or status=5) group by date_format(create_time, '%Y-%m-%d')) B on A.calc_time=B.create_time where (?1 is null or A.calc_time between ?1 and ?2)) as total";

	public final static String SEARCH_FINDREBATE_DEAL = "select count(1) from (select A.id,ifnull(B.balance,0),ifnull(B.sys_balance,0),A.status,date_format(B.time, '%Y-%m-%d') from biz_account A,biz_report B "
			+ " where A.id=B.account_id and A.type in (?5) and (?1 is null or A.account like concat('%',?1,'%')) and (?2 is null or A.bank_type=?2) and A.status in (?6) and (?3=0 or A.handicap_id=?3)  and date_format(B.time, '%Y-%m-%d') = ?4) as total";

	public final static String SEARCH_FINDREBATE_DRTAIL = "select count(1) from (select A.*,D.user_name from ( select account,sum(total_amount),sum(amount)amounts "
			+ " from fundsTransfer.biz_account_return_summary where calc_time=?3 group by account ) A" + " left join"
			+ " ( select A.id,C.user_name,A.bank_type from biz_account A,biz_account_more B,biz_rebate_user C"
			+ " where A.mobile=B.moible and B.uid=C.uid) D on A.account=D.id where (?1 is null or D.user_name like CONCAT('%',?1,'%')) and (?2 is null or D.bank_type=?2) and (?4=0 or A.amounts>=?4) and (?5=0 or A.amounts<=?5) and A.amounts>0) as total";

	public final static String SEARCH_FINDREBATE_COMPlETEDRTAIL = "select count(1) from (select V.user_name,V.amount,ifnull(N.amounts,0),(V.amount- (ifnull(N.amounts,0)))from(select T.uid,T.user_name,sum(T.amount)amount from (select A.*,D.user_name,D.uid from (select account,sum(amount) amount"
			+ " from fundsTransfer.biz_account_return_summary where calc_time=?2 group by account ) A" + " left join"
			+ " (select A.id,C.user_name,C.uid,A.bank_type from fundsTransfer.biz_account A,fundsTransfer.biz_account_more B,fundsTransfer.biz_rebate_user C"
			+ " where A.mobile=B.moible and B.uid=C.uid ) D on A.account=D.id order by D.user_name,D.uid desc) T group by T.user_name,T.uid)V"
			+ " left join" + " (select sum(amount)amounts,uid from "
			+ " fundsTransfer.biz_account_rebate where (status=1 or status=5) and date_format(create_time, '%Y-%m-%d')=?2 group by uid) N on V.uid=N.uid where (?1 is null or V.user_name like CONCAT('%',?1,'%'))) as total";

	public final static String SEARCH_FINDDELETEACCOUNT_COUNTQUERY = "select  count(1) "
			+ " from biz_account where (?3='goldener' or status=-2) and (?3='fin' or status in (3,4)) and (?3='fin' or  (update_time<date_add(now(),interval -30 day)))"
			+ " and (?1=0 or handicap_id=?1) and (?2 is null or alias like concat('%',?2,'%'))"
			+ " and (?4 is null or flag=?4) and (?5 is null or status=?5)";

	public final static String SEARCH_PENDING_COUNTQUERY = "select count(1) from (select " + "ifnull(A.alias,'无')alias,"
			+ "A.handicap_id," + "(select name from biz_handicap where id=A.handicap_id)handicapname,"
			+ "(select GROUP_CONCAT(name,'|') from biz_level where id in (select level_id from biz_account_level where account_id=A.id)  and (?2=0 or (handicap_id in (?1) and id=?2))) levelname,"
			+ "A.type," + "A.account," + "A.bank_name,"
			+ "B.bal,A.id,ifnull(A.owner,'无'),ifnull(A.bank_type,'无'),A.remark,A.curr_sys_level,A.balance,"
			+ "date_format(B.create_time,'%Y-%m-%d %H:%i:%s')time,B.operator,B.remark re,B.status pendStatus,B.id pendingId,A.status,B.amount,B.defrost_type"
			+ " from " + " biz_account A,biz_account_trace B"
			+ " where A.id=B.account_id and B.status in (?9) and (Case When ?10=6 then (A.owner like concat('%','3天未启用','%'))  else (?10 is null or B.status=?10) end) and (?11 is null or B.defrost_type=?11)"
			+ " and A.handicap_id in (?1)" + " and (?2 is null or A.account like concat('%',?2,'%'))"
			+ " and (?12 is null or A.alias like concat('%',?12,'%'))" + " and (?13 is null or A.flag=?13)"
			+ " and (?3 is null or B.create_time between ?3 and ?4)"
			+ " and (?5 is null or B.create_time between ?5 and ?6)"
			+ " and (?8 is null or A.type in (?7)) order by time desc) as total";

	public final static String SEARCH_FINDFININSTATMATCH_COUNTQUERY = "select count(1) from (select "
			+ " C.member_user_name " + " from " + " biz_account A,biz_income_request C "
			+ " where (?1 is null or C.member_user_name like concat('%',?1,'%')) "
			+ " and (?2 is null or C.update_time between ?2 and ?3) "
			+ " and (?4=0 or (C.amount>=?4 and C.amount<=?5)) and C.status=1 and A.id=C.to_id and A.id=?6 and C.type=?7) as total";

	public final static String SEARCH_FINDFININSTATMATCHBANK_COUNTQUERY = "select count(1) from (select * from biz_bank_log where create_time between ?1 and ?2"
			+ " and (?3=0 or (abs(amount)>=?3 and abs(amount)<=?4)) and (?7=0 or ((?7=-1 or amount>0) and (?7=1 or amount<0))) and from_account=?5 and (?6=9999 or status=?6)) as total";

	public final static String SEARCH_FINDSENDCARDMATCH_COUNTQUERY = "select count(1) from (select * from ( select "
			+ " C.from_account," + "(select bank_name from biz_account where id=C.from_id)bankname," + "C.type,"
			+ "C.order_no," + "C.amount," + "IFNULL(B.fee,0)fee,"
			+ "date_format(C.create_time,'%Y-%m-%d %H:%i:%s')create_time," + "C.remark,"
			+ "C.id,C.from_id,(select handicap_id from biz_account where id=C.from_id)handicap_id" + " from "
			+ " biz_account A,biz_transaction_log B,biz_income_request C "
			+ " where A.id=B.to_account and B.order_id=C.id and A.id=?6 "
			+ " and (?2 is null or C.create_time between ?2 and ?3) "
			+ " and (?4=0 or (C.amount>=?4 and C.amount<=?5)) and C.status in (1,3,4,5) and B.type=?7 and C.type=?7) A where (?1 is null or A.bankname like concat('%',?1,'%')) and (?8=0 or A.handicap_id=?8)) as total";

	public final static String SEARCH_FINDFININTHIRDSTATMATCH_COUNTQUERY = "select count(1) from (select "
			+ " C.member_user_name " + " from " + " biz_account A,biz_third_request C "
			+ " where A.account=C.to_account and A.id=?6 and (?1 is null or C.member_user_name like concat('%',?1,'%')) "
			+ " and (?2 is null or C.ack_time between ?2 and ?3) "
			+ " and (?4=0 or (C.amount>=?4 and C.amount<=?5))) as total";

	public final static String SEARCH_FINDINCOMEBYACCOUNT_COUNTQUERY = "select count(1) " + " FROM "
			+ " biz_income_request A,biz_handicap B,biz_level C,biz_account D"
			+ " where A.handicap=B.id and A.level=C.id and A.to_id=D.id and A.type=3 "
			+ " and A.create_time between ?3 and ?4 and A.to_id=?2 and A.member_user_name=?1";

	public final static String SEARCH_QUEYFINDFINTRANSSTAT_COUNTQUERY = "select count(1) from(select A.account,A.type,A.accountid,A.alias,A.owner,A.bank_type,ifnull(A.amounts,0) inAmounts,ifnull(A.fees,0) inFees,ifnull(A.count_,0) inCounts,abs(ifnull(B.amounts,0)),ifnull(B.fees,0),ifnull(B.count_,0) from ("
			+ " select"
			+ " ac.account,ac.alias,ac.owner,ac.bank_type,ac.type,re.from_id accountid,SUM(re.amount)amounts,SUM(re.fee)fees,count(1)count_"
			+ " from" + " biz_income_request re,biz_account ac"
			+ " where re.status=1 and ac.type in (?4) and re.from_id=ac.id and re.type in (?5) and re.update_time BETWEEN ?2 and ?3"
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) and (?6 is null or ac.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or ac.bank_type like concat('%',?7,'%'))" + " and (?8=0 or ac.handicap_id=?8)"
			+ " GROUP BY re.from_id)A" + " left join" + " (select"
			+ " lo.from_account accountid,SUM(lo.amount)amounts,SUM(0.0)fees,count(1)count_" + " from"
			+ " biz_bank_log lo,biz_account ac"
			+ " where lo.status=1 and lo.from_account=ac.id and lo.amount<0 and ac.type in (?4) and lo.trading_time BETWEEN ?2 and ?3"
			+ " GROUP BY lo.from_account)B on A.accountid=B.accountid) as total";

	public final static String SEARCH_QUEYFINCARDLIQUIDATION_COUNTQUERY = "select count(1) from(select A.handicap_id,A.account,A.type,A.id,A.alias, "
			+ " A.owner,A.bank_type, B.*,A.status from biz_account A,(select B.account_id"
			+ " from fundsTransfer.biz_report B where B.time between ?2 and ?3 group by B.account_id)B where A.id=B.account_id"
			+ " and (?1 is null or A.account like concat('%',?1,'%')) "
			+ " and (?4 is null or A.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or A.bank_type like concat('%',?5,'%')) and (?8!=3 or account_id in (?9))"
			+ " and A.handicap_id in (?6)" + " and A.type in (?7)" + " and (?8=0 or A.status=?8)) as total";

	public final static String SEARCH_QUEYFINDFINTRANSSTAT_COUNTQUERY_FROM_CLEAR_DATE = "select count(1) from(select B.account_handicap,A.account,A.id,A.alias,"
			+ "A.owner,A.bank_type,B.balance,B.outward,B.fee,B.outward_count,B.outward_sys,B.outward_sys_count"
			+ " from biz_account A,biz_report B"
			+ " where B.time between ?2 and ?3 and (?1 is null or A.account like concat('%',?1,'%')) "
			+ " and (?5 is null or A.owner like concat('%',?5,'%'))"
			+ " and (?6 is null or A.bank_type like concat('%',?6,'%'))"
			+ " and (?8!='Bankcard' or ROUND(B.outward_sys)!=B.outward_sys)" + " and B.account_handicap in (?7)"
			+ " and A.type in (?4) and A.id=B.account_id) as total";

	public final static String SEARCH_QUEYFINDTHIRD_COUNTQUERY = "select count(1) from(select A.account,A.type,A.accountid,A.alias,A.owner,A.bank_type,ifnull(A.amounts,0) inAmounts,ifnull(A.fees,0) inFees,ifnull(A.count_,0) inCounts,ifnull(A.handicap_id,0) from ("
			+ " select"
			+ " ac.handicap_id,ac.account,ac.alias,ac.owner,ac.bank_type,ac.type,re.from_id accountid,SUM(re.amount)amounts,SUM(re.fee)fees,count(1)count_"
			+ " from" + " biz_income_request re,biz_account ac"
			+ " where ac.type in (?4) and re.from_id=ac.id and re.type in (?5) and re.create_time BETWEEN ?2 and ?3"
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) and (?6 is null or ac.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or ac.bank_type like concat('%',?7,'%'))"
			+ " and (?8=0 or ac.handicap_id=?8)  and re.status=1" + " GROUP BY re.from_id)A) as total";

	public final static String SEARCH_SCREENING_COUNTQUERY = "select count(1) from(select A.* from ( select id,date_format(create_time,'%Y-%m-%d %H:%i:%s')create_time,date_format(update_time,'%Y-%m-%d %H:%i:%s')update_time from biz_income_request where "
			+ " create_time between ?1 and ?2"
			+ " and (update_time>?2 or status=0 or update_time>(select concat(if((date_format(create_time,'%Y-%m-%d')=date_format(update_time,'%Y-%m-%d')),date_format(date_add(create_time,interval 1 day),'%Y-%m-%d'),date_format(update_time,'%Y-%m-%d')), ' 06:59:59')))"
			+ " and type in (106,107))A where ((date_format(A.create_time,'%Y-%m-%d')!=date_format(A.update_time,'%Y-%m-%d')) or A.update_time is null)) as total";

	public final static String SEARCH_QUEYFINDMORESTAT_COUNTQUERY = "select * from biz_handicap where status=1";

	public final static String SEARCH_QUEYFINDMORESTAT_COUNTQUERYY = "select count(1) from biz_report where handicap is not null and time between ?1 and ?2";

	public final static String SEARCH_QUEYFINDMORELEVELSTAT_COUNTQUERY = "select count(1) "
			+ " from biz_income_request A,biz_level B,biz_handicap C"
			+ " where A.type>=1 and type<=100 and A.level=B.id and A.handicap=C.id and A.status!=3 and A.handicap=?1 and (?2=0 or A.level=?2) "
			+ " and (?3 is null or A.create_time between ?3 and ?4) "
			+ " and (?5 is null or A.create_time between ?5 and ?6)" + " GROUP BY level ";

	public final static String SEARCH_OUTPERSON_COUNTQUERY = "select count(1) from (select distinct member,handicap from fundsTransfer.biz_outward_request where create_time between ?1 and ?2)A group by handicap";

	public final static String SEARCH_FINTRANSSTATMATCHBANK_COUNTQUERY = "select count(1) from (select " + "C.order_no,"
			+ "A.from_account," + "A.to_account,"
			+ "(select account from biz_account where id=C.from_id)from_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(0.0,0)fee," + "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s')trading_time," + "C.remark,"
			+ "date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time" + " from "
			+ " biz_bank_log A,biz_transaction_log B,biz_income_request C " + " where C.status=1" + " and  A.status=1"
			+ " and (?1 is null or C.order_no =?1) " + " and C.update_time between ?2 and ?3"
			+ " and A.amount>0 and A.status=1 and A.id=B.to_banklog_id and B.order_id=C.id and C.from_id=?6"
			+ " and (?4=0 or (A.amount>=?4 and A.amount<=?5))) as total";

	public final static String SEARCH_FINTRANSSTATMATCHSYS_COUNTQUERY = "select count(1) from (select " + "B.order_no,"
			+ "B.from_id," + "B.from_account from_accountname," + "A.account," + "IFNULL(B.amount,0)amount,"
			+ "IFNULL(B.fee,0)fee," + "date_format(B.update_time,'%Y-%m-%d %H:%i:%s')create_time,"
			+ "B.remark,B.to_id,B.status,A.handicap_id " + " from " + " biz_income_request B,biz_account A "
			+ " where (?8=9999 or B.status=?8) and (?6=0 or (B.from_id=?6)) and (?6!=0 or (B.type in (?11))) "
			+ " and (?1 is null or B.order_no =?1) "
			+ " and (?7=103 or (?2 is null or B.update_time between ?2 and ?3)) and (?7!=103 or (?2 is null or B.update_time between ?2 and ?3))"
			+ " and (?4=0 or (B.amount>=?4 and B.amount<=?5)) and (?9=0 or A.handicap_id=?9) and A.handicap_id in (?10) and B.to_id=A.id) as totoal";

	public final static String SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERY = "select count(1) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + "biz_account A"
			+ " where type=1 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)) as total";

	public final static String SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERYcompany = "select count(1) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + " biz_account A"
			+ " where type in (1,3,4) and status=?2 and (?1 is null or A.account like concat('%',?1,'%'))) as total";

	public final static String SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERYNotissued = "select count(1) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + " biz_account A"
			+ " where type=2 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)) as total";

	public final static String SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERYOUT = "select count(1) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ ",ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')" + " from " + "biz_account A"
			+ " where type=5 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)) as total";

	public final static String SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERYReserveBank = "select count(1) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + " biz_account A"
			+ " where type in (?1) and status in (?3) and (?2 is null or A.account like concat('%',?2,'%')) and (?4 is null or A.bank_type=?4)) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERY = "select count(1)from (select B.username operatorname  from ( select "
			+ "A.operator" + " from " + " biz_transaction_log A,biz_income_request B,biz_account C"
			+ " where A.order_id=B.id and C.id=A.to_account and A.type in(1,2,3,4)  and B.type in (1,2,3,4) and B.status in (1,4,5)"
			+ " and (?1 is null or B.member_real_name like concat('%',?1,'%')) and (?2 is null or account like concat('%',?2,'%'))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and (?3 is null or A.create_time between ?3 and ?4) and (?7=0 or A.to_account=?7)) A"
			+ " LEFT JOIN sys_user B on A.operator=B.id) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCEOUTSYS_COUNTQUERY = "select count(1)from (select * from ( select "
			+ "A.id," + "A.to_account," + "(select account from biz_account where id=A.from_account) from_accountname,"
			+ "A.from_account," + "C.to_account to_accountname," + "IFNULL(A.amount,0)amount," + "IFNULL(A.fee,0)fee,"
			+ "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark," + "A.operator,"
			+ "(select username from sys_user where id=A.operator)operatorname" + " from "
			+ " biz_transaction_log A,biz_outward_task B,biz_outward_request C"
			+ " where A.order_id=B.id and B.outward_request_id=C.id and A.type=0  and B.status=5  and C.status in (5,6) "
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.from_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERYCashBank = "select count(1)from (select * from ( select "
			+ "A.id," + "A.to_account," + "B.member_real_name from_accountname," + "A.from_account,"
			+ "(select account from  biz_account where id=A.to_account)to_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(A.fee,0)fee," + "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark,"
			+ "A.operator," + "(select username from  sys_user where id=A.operator)operatorname" + " from "
			+ "  biz_transaction_log A, biz_income_request B" + " where A.order_id=B.id and B.status in (1,4,5)"
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERYCompany = "select count(1)from (select * from ( select "
			+ "A.id," + "A.to_account," + "B.member_real_name from_accountname," + "A.from_account,"
			+ "(select account from  biz_account where id=A.to_account)to_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(A.fee,0)fee," + "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark,"
			+ "A.operator," + "(select username from  sys_user where id=A.operator)operatorname" + " from "
			+ "  biz_transaction_log A, biz_income_request B"
			+ " where A.order_id=B.id and A.type in (1,2,3) and B.type in (1,2,3) and B.status in (1,4,5)"
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERYThird = "select count(1)from (select A.id,A.to_account,A.from_accountname,A.from_account,A.to_accountname,A.amount,"
			+ "A.fee,A.create_time,A.remark,A.operator,ifnull(B.username,'') from ( select * from ( select " + "B.id,"
			+ "B.to_account," + "B.member_user_name from_accountname," + "B.from_account,"
			+ "B.to_account to_accountname," + "IFNULL(B.amount,0)amount," + "IFNULL(B.fee,0)fee,"
			+ "date_format(B.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark," + "333 operator" + " from "
			+ " biz_third_request B" + " where "
			+ " (?3 is null or unix_timestamp(B.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (B.amount>=?5 and B.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%')))A"
			+ " LEFT JOIN sys_user B on A.operator=B.id) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERYNotissued = "select count(1)from (select * from ( select "
			+ "A.id," + "A.to_account," + "B.member_real_name from_accountname," + "A.from_account,"
			+ "(select account from  biz_account where id=A.to_account)to_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(A.fee,0)fee," + "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark,"
			+ "A.operator," + "(select username from  sys_user where id=A.operator)operatorname" + " from "
			+ "  biz_transaction_log A, biz_income_request B"
			+ " where A.order_id=B.id and A.type in (1,2,3,4) and B.type in (1,2,3,4) and B.status in (1,4,5)"
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCEPettycash_COUNTQUERY = "select count(1)from (select * from ( select * from ( select "
			+ "A.id," + "A.to_account," + "B.member_real_name from_accountname," + "A.from_account,"
			+ "(select account from biz_account where id=A.to_account)to_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(A.fee,0)fee," + "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark,"
			+ "A.operator," + "(select username from sys_user where id=A.operator)operatorname" + " from "
			+ " biz_transaction_log A,biz_income_request B" + " where A.order_id=B.id and B.status in (1,4,5)"
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))"
			+ " union " + "select * from ( select " + "C.id," + "C.to_account,"
			+ "(select account from biz_account where id=C.from_account) from_accountname," + "C.from_account,"
			+ "A.to_account to_accountname," + "IFNULL(C.amount,0)amount," + "IFNULL(C.fee,0)fee,"
			+ "date_format(C.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark," + "C.operator,"
			+ "(select username from sys_user where id=C.operator)operatorname" + " from "
			+ " biz_transaction_log C,biz_outward_task B,biz_outward_request A"
			+ " where C.order_id=B.id and B.outward_request_id=A.id and C.type=0  and B.status=5  and A.status in (5,6) "
			+ " and (?3 is null or unix_timestamp(C.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (C.amount>=?5 and C.amount<=?6))) C where (?7=0 or C.from_account=?7) and (?1 is null or C.from_accountname like concat('%',?1,'%')) and (?2 is null or C.to_accountname like concat('%',?2,'%'))) as A) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCEBANK_COUNTQUERY = "select count(1)from(select * from (select "
			+ "A.id" + " from " + "  biz_bank_log A where A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCECompanyBANK_COUNTQUERY = "select count(1)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time" + " from "
			+ "  biz_bank_log A where  A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCEThirdBANK_COUNTQUERY = "select count(1)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(A.fee,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time" + " from "
			+ "  biz_bank_log A where A.type =4 and A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCENotissuedBANK_COUNTQUERY = "select count(1)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(A.fee,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time" + " from "
			+ "  biz_bank_log A where A.type in (1,2,3,4) and A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String ClearAccountDate = "select count(1)from(select D.account from (select C.account from (select "
			+ " B.account" + " from biz_income_request A,biz_account B where A.to_id=B.id and A.status in (0,2) "
			+ " and (?1 is null or unix_timestamp(A.create_time)<=unix_timestamp(?1))" + " union all " + " select "
			+ " C.account "
			+ " from biz_outward_request A,biz_outward_task B,biz_account C where B.account_id=C.id  and A.id=B.outward_request_id and A.status in (0,1,3)"
			+ " and (?1 is null or unix_timestamp(A.create_time)<=unix_timestamp(?1))) C group by C.account)D,biz_account C "
			+ " where D.account=C.account ) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCECashBANK_COUNTQUERY = "select count(1)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time" + " from "
			+ "  biz_bank_log A where A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCEOUTBANK_COUNTQUERY = "select count(1)from(select * from (select "
			+ "A.id," + "A.from_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname," + "A.to_account,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time " + " from "
			+ "  biz_bank_log A where  A.status=1 and A.amount<0) A "
			+ " where (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_account like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String SEARCH_QUEYFINTRANSBALANCEBANKReservePettycash_COUNTQUERY = "select count(1)from(select * from (select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time " + " from "
			+ "  biz_bank_log A where A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7" + " union "
			+ "select * from (select " + "A.id," + "A.from_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname," + "A.to_account,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time " + " from "
			+ "  biz_bank_log A where A.status=1 and A.amount<0) A "
			+ " where (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_account like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as A) as total";

	public final static String totalfinBalanceStat = "select count(1) from(select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 1 id,'入款卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=1 and status in (1,2,3,4,5) group by status)A group by id"
			+ " union"
			+ " select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 2 id,'出款卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=5 and status in (1,2,3,4,5) group by status)A group by id"
			+ " union"
			+ " select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 3 id,'下发卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=13 and status in (1,2,3,4,5) group by status)A group by id"
			+ " union"
			+ " select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 4 id,'备用卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=8 and status in (1,2,3,4,5) group by status)A group by id"
			+ " union"
			+ " select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 5 id,'现金卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=9 and status in (1,2,3,4,5) group by status)A group by id"
			// +" union"
			// +" select
			// id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0)
			// from (select 6 id,'公司入款余额' type,(case when status in (1,5) then
			// IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then
			// IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then
			// IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then
			// IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then
			// IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then
			// IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type in (1,3,4)
			// and status in (1,2,3,4,5) group by status)A group by id"
			+ " union"
			+ " select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 7 id,'第三方入款余额' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=2 and status in (1,2,3,4,5) group by status)A group by id) as total";

	public final static String totalAccountSystem = "select ifnull(sum(taAmount),0) taAmount from (select SUM(ta.amount)taAmount from"
			+ " biz_outward_request re,biz_outward_task ta,biz_account ac"
			+ " where re.id=ta.outward_request_id and re.status in(5,6) and ta.status=5 and ac.id=ta.account_id"
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) "
			+ " and (?2 is null or re.update_time between ?2 and ?3) "
			+ " and (?4 is null or ac.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or ac.bank_type like concat('%',?5,'%'))" + " GROUP BY ta.account_id ) as total";

	public final static String totalAccountBank = "select ifnull(sum(bkAmount),0),ifnull(sum(bkFee),0),ifnull(sum(taAmount),0) from (select A.taAmount taAmount,B.bkAmount bkAmount,B.bkFee bkFee from "
			+ "( select ta.account_id,SUM(ta.amount)taAmount from "
			+ " biz_account ac,biz_outward_request re,biz_outward_task ta "
			+ " where re.id=ta.outward_request_id and re.status in(5,6) and ta.status=5 and ac.id=ta.account_id "
			+ " and re.update_time BETWEEN ?2 and ?3 and (?1 is null or ac.account like concat('%',?1,'%'))"
			+ " and (?4 is null or ac.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or ac.bank_type like concat('%',?5,'%')) and (?6=0 or ac.handicap_id=?6)"
			+ "GROUP BY ta.account_id ORDER BY ac.id) A LEFT JOIN "
			+ "(select lo.from_account,SUM(ABS(amount))bkAmount,SUM(0.0)bkFee from " + "biz_bank_log lo "
			+ "where lo.trading_time BETWEEN ?2 and ?3 and lo.amount<0 " + "and lo.status=1 "
			+ "GROUP BY from_account)B on A.account_id=B.from_account) as total";

	public final static String totalFindWechatMatched = "select sum(A.amount)"
			+ " from biz_wechat_request A,biz_wechat_log B,biz_account C"
			+ " where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
			+ " and (?8 is null or B.depositor like concat('%',?8,'%')) and (?9 is null or C.account like concat('%',?9,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
			+ " and A.wechat_log_id=B.id and A.wechatid=C.id and A.status=1 and B.status=1";

	public final static String totalFindAliMatched = "select sum(A.amount)"
			+ " from biz_alipay_request A,biz_alipay_log B,biz_account C"
			+ " where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
			+ " and (?8 is null or B.depositor like concat('%',?8,'%')) and (?9 is null or C.account like concat('%',?9,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
			+ " and A.alipay_log_id=B.id and A.alipayid=C.id and A.status=1 and B.status=1";

	public final static String totalfindMBAndInvoice = "select sum(amount) " + " from biz_wechat_request "
			+ " where wechatid=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or member_name like concat('%',?4,'%')) and (?5 is null or order_no =?5)"
			+ " and (?6=0 or amount>=?6) and (?7=0 or amount<=?7) ";

	public final static String totalfindAliMBAndInvoice = "select sum(amount) " + " from biz_alipay_request "
			+ " where alipayid=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or member_name like concat('%',?4,'%')) and (?5 is null or order_no =?5)"
			+ " and (?6=0 or amount>=?6) and (?7=0 or amount<=?7) ";

	public final static String totalFindBankLogMatch = "select sum(amount) " + " from biz_wechat_log"
			+ " where from_account=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or depositor like concat('%',?4,'%')) and (?5=0 or amount>=?5) and (?6=0 or amount<=?6)";

	public final static String totalFindAliBankLogMatch = "select sum(amount) " + " from biz_alipay_log"
			+ " where from_account=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or depositor like concat('%',?4,'%')) and (?5=0 or amount>=?5) and (?6=0 or amount<=?6)";

	public final static String totalFindWechatCanceled = "select sum(A.amount)" + " from biz_wechat_request A "
			+ " where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)" + " and A.status=3";

	public final static String totalFindAliCanceled = "select sum(A.amount)" + " from biz_alipay_request A "
			+ " where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)" + " and A.status=3";

	public final static String totalFindWechatUnClaim = "select sum(A.amount) "
			+ " from biz_wechat_log A,biz_account B " + " where (?2 is null or A.create_time between ?2 and ?3)"
			+ " and (abs(TIMESTAMPDIFF(hour,now(),A.create_time))) >=24 and (abs(TIMESTAMPDIFF(hour,now(),A.create_time)))<=48 and A.status=0"
			+ " and (?1=0 or B.handicap_id=?1) and (?4 is null or A.depositor like concat('%',?4,'%')) and (?5 is null or B.account like concat('%',?5,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and A.from_account=B.id ";

	public final static String totalFindAliUnClaim = "select sum(A.amount) " + " from biz_alipay_log A,biz_account B "
			+ " where (?2 is null or A.create_time between ?2 and ?3)"
			+ " and (abs(TIMESTAMPDIFF(hour,now(),A.create_time))) >=24 and (abs(TIMESTAMPDIFF(hour,now(),A.create_time)))<=48 and A.status=0"
			+ " and (?1=0 or B.handicap_id=?1) and (?4 is null or A.depositor like concat('%',?4,'%')) and (?5 is null or B.account like concat('%',?5,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and A.from_account=B.id ";

	public final static String totalAccountBankFromClearDate = "select ifnull(sum(outward),0),ifnull(sum(fee),0),ifnull(sum(outward_sys),0) from (select "
			+ " B.outward, B.fee,B.balance," + " B.outward_sys" + " from biz_account A,biz_report B"
			+ " where B.time between ?2 and ?3 and (A.type in (5,14) or (A.type=1 and ROUND(B.outward_sys)=B.outward_sys)) and A.id=B.account_id "
			+ " and (?1 is null or A.account like concat('%',?1,'%')) and (?4 is null or A.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or A.bank_type like concat('%',?5,'%'))"
			+ " and B.account_handicap in (?6) and (?7 is null or A.type=?7)) as total";

	public final static String totalqueyfindFinOutStatSys = "select sum(amount)amount,sum(fee)fee from (select "
			+ "(select name from biz_handicap where id=C.handicap) handicapname,"
			+ "(select name from biz_level where id=C.level and status=1) levelname," + "C.member,"
			+ "(select account from biz_account where id=B.account_id) accountname," + "C.to_account," + "B.amount,"
			+ "ifnull(0.0,0)fee," + "(select username from sys_user where id=B.operator) operatorname,"
			+ "date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s') asigntime"
			+ " from " + "biz_outward_task B,biz_outward_request C"
			+ " where B.outward_request_id=C.id and (?8=9999 or B.status=?8) and (?7=9999 or C.status=?7) and B.account_id=?1 and (?2 is null or C.member like concat('%',?2,'%'))  "
			+ " and C.update_time between ?3 and ?4 and (?5=0 or (B.amount>=?5 and B.amount<=?6))) as total ";

	public final static String totalqueyfindFinOutStatFlow = "select sum(amount)amount,sum(fee) from (select"
			+ " abs(B.amount) amount,ifnull(0.0,0)fee" + " from " + "biz_bank_log B"
			+ " where (?6=0 or (abs(B.amount)>=?6 and abs(B.amount)<=?7)) and (?8=9999 or B.status=?8) and (?9=0 or ((?9=-1 or  B.amount>0) and (?9=1 or  B.amount<0))) and B.from_account=?1 and (?2 is null or B.to_account_owner like concat('%',?2,'%')) and (?3 is null or B.to_account like concat('%',?3,'%')) "
			+ " and unix_timestamp(B.create_time) between unix_timestamp(?4) and unix_timestamp(?5)) as total";

	public final static String totalqueyAccountStatisticsHandicap = "select sum(amount)amount,sum(fee)fee from (select "
			+ "sum(B.amount) amount," + "ifnull(0.0,0)fee" + " from " + " biz_outward_request A,biz_outward_task B "
			+ " where A.id=B.outward_request_id and B.status=5 and A.status in (5,6) and (?1 is null or unix_timestamp(A.update_time) between unix_timestamp(?1) and unix_timestamp(?2))"
			+ " and (?3 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ "group by A.handicap) as total";

	public final static String totalqueyAccountStatisticsHandicapFromClearDate = "select sum(amount)amount,sum(fee)fee from (select A.handicap_id,sum(B.outward_sys)amount,sum(fee)fee,sum(outward_sys_count)"
			+ " from biz_account A,biz_report B "
			+ " where A.id=B.account_id and (A.type in (5,14) or (A.type=1 and ROUND(B.outward_sys)=B.outward_sys)) and B.time between ?2 and ?3 and B.account_handicap in (?1)"
			+ " group by B.account_handicap) as total";

	public final static String queyAccountStatisticsByHandicapAndLevel = "select sum(amount)amount,sum(fee)fee from (select "
			+ "B.amount," + "ifnull(0.0,0)fee," + "A.id" + " from " + " biz_outward_request A ,biz_outward_task B"
			+ " where A.id=B.outward_request_id and B.status=5 and A.status in (5,6) and (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?3 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?5) and unix_timestamp(?6))) as total";

	public final static String totalqueyAccountMatchByhandicap = "select sum(amounts)amounts from ( " + "select "
			+ " distinct B.outward_request_id," + " A.amount amounts" + " from "
			+ " biz_outward_request A,biz_outward_task B"
			+ " where (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?6=0 or (A.amount>=?6 and A.amount<=?7)) and (?3 is null or A.member like concat('%',?3,'%'))"
			+ " and (?8=0 or A.handicap=?8) and (?9=0 or  A.id=?9)  and (?10=9999 or A.status=?10) and (?11=9999 or B.status=?11) "
			+ " and A.update_time IS NULL and A.create_time between ?4 and ?5 " + " and A.id=B.outward_request_id "
			+ " UNION " + "select " + " distinct B.outward_request_id," + " A.amount amounts" + " from "
			+ " biz_outward_request A,biz_outward_task B"
			+ " where (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?6=0 or (A.amount>=?6 and A.amount<=?7)) and (?3 is null or A.member like concat('%',?3,'%'))"
			+ " and (?8=0 or A.handicap=?8) and (?9=0 or  A.id=?9)  and (?10=9999 or A.status=?10) and (?11=9999 or B.status=?11) "
			+ " and A.update_time between ?4 and ?5 " + " and A.id=B.outward_request_id " + ") as total";

	public final static String totalqueyAccountMatchByhandicap1 = "select sum(A.amount)amount,sum(A.fee)fee,SUM(IF(B.from_banklog_id!='',A.amount,0))  from (select "
			+ "B.amount,B.id," + "0.00 fee" + " from " + " biz_outward_request A,biz_outward_task B"
			+ " where (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?6=0 or (A.amount>=?6 and A.amount<=?7)) and (?3 is null or A.member like concat('%',?3,'%'))"
			+ " and (?8=0 or A.handicap=?8) and (?9=0 or  A.id=?9)  and (?10=9999 or A.status=?10) and (?11=9999 or B.status=?11) and "
			+ " A.update_time between ?4 and ?5 and A.id=B.outward_request_id)A  left join "
			+ " (select from_banklog_id,order_id from biz_transaction_log where create_time between ?4 and ?5 and type=0 ) B on A.id= B.order_id";

	public final static String totalFinInStatistics = "select sum(amounts)amount,sum(fees)fee from (select * from (select C.*,ifnull(D.amounts,0)amounts,ifnull(D.fees,0)fees,ifnull(D.count_,0)count_ "
			+ "from (select " + "A.id,B.name,1 levelname," + " A.account,ifnull(A.bank_balance,0)"
			+ " from biz_account A,biz_handicap B "
			+ " where A.handicap_id=B.id and A.type in (?8) and A.handicap_id in (?1) and (?3 is null or A.account like concat('%',?3,'%'))"
			+ " and (?11 is null or A.owner like concat('%',?11,'%')) and (?12 is null or A.bank_type like concat('%',?12,'%'))) C"
			+ " left join " + " (select " + " C.to_id," + "sum(C.amount)amounts," + "sum(0)fees," + "count(1) count_ "
			+ " from " + " biz_income_request C " + " where  C.type=?10 and (?2=0 or C.level=?2)"
			+ " and (?4 is null or C.update_time between ?4 and ?5) "
			+ " and (?6 is null or C.update_time between ?6 and ?7) group by C.to_id) D on C.id=D.to_id) A "
			+ " where (0 = 0 or levelname=?9)  ) as total";

	public final static String totalFinInStatisticsFromClearDate = "select sum(income_sys)amount,sum(balance)fee from (select distinct * from ( select B.account_handicap,A.account,A.alias,"
			+ " A.owner,A.bank_type,B.balance,B.income,B.income_count,B.income_persons,B.income_sys,B.income_sys_count"
			+ " from biz_account A,biz_report B "
			+ " where B.time between ?3 and ?4 and A.type in (?5) and A.id=B.account_id  and B.account_handicap in (?1)"
			+ " and (?2 is null or A.account like concat('%',?2,'%'))"
			+ " and (?6 is null or A.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or A.bank_type like concat('%',?7,'%'))"
			+ "and (?8!='Bankcard' or ROUND(B.income)!=B.income))h) as total";

	public final static String totalFinInStatisticsSendCard = "select sum(amounts)amount,sum(fees)fee from (select * from (select ifnull(D.amounts,0)amounts,ifnull(D.fees,0)fees,ifnull(D.count_,0)count_,C.* "
			+ "from (select " + "A.id,"
			+ "A.account,ifnull(A.bank_balance,0),ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+ " from biz_account A "
			+ "where A.status in (1,3,4,5) and A.type in (?6) and (?1 is null or A.account like concat('%',?1,'%'))"
			+ " and (?8 is null or A.owner like concat('%',?8,'%')) and (?9 is null or A.bank_type like concat('%',?9,'%'))) C"
			+ " left join " + " (select " + " B.to_account," + "sum(B.amount)amounts," + "sum(B.fee)fees,"
			+ "count(1) count_ " + " from " + " biz_transaction_log B,biz_income_request C "
			+ " where B.order_id=C.id and B.type=?7 " + " and (?2 is null or C.update_time between ?2 and ?3) "
			+ " and (?4 is null or C.update_time between ?4 and ?5) group by B.to_account) D on C.id=D.to_account) A "
			+ " where A.count_!=0) as total";

	public final static String totalFinInThirdStatistics = "select sum(amounts)amount,sum(fees)fee from (select * from (select C.*,ifnull(D.amounts,0)amounts,ifnull(D.fees,0)fees,ifnull(D.count_,0)count_ "
			+ "from (select " + "A.id,B.name,"
			+ "(select GROUP_CONCAT(name,'|') from biz_level where id in (select level_id from biz_account_level where account_id=A.id)  and (?2=0 or (handicap_id in (?1) and id=?2))) levelname,"
			+ "A.account,ifnull(A.bank_balance,0)" + " from biz_account A,biz_handicap B "
			+ "where A.handicap_id=B.id and A.status in (1,3,4,5) and A.type=?8 and A.handicap_id in (?1) and (?3 is null or A.account like concat('%',?3,'%'))) C"
			+ " left join " + " (select " + " C.to_account," + "sum(C.amount)amounts," + "sum(C.fee)fees,"
			+ "count(1) count_ " + " from " + " biz_third_request C "
			+ " where C.handicap in (?1) and (?2=0 or C.level=?2)"
			+ " and (?4 is null or C.ack_time between ?4 and ?5) "
			+ " and (?6 is null or C.ack_time between ?6 and ?7) and C.amount>0 group by C.to_account) D on C.account=D.to_account) A) as total";

	public final static String totalqueyIncomeThird = "select sum(A.amount)" + " from biz_income_request A " + " where "
			+ " (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?3 is null or A.member_user_name like concat('%',?3,'%'))"
			+ " and (?7 is null or A.to_account like concat('%',?7,'%')) and (?8=0 or (A.amount>=?8 and A.amount<=?9))"
			+ " and (?4 is null or exists (select id from biz_account where id=A.to_id and bank_name like concat('%',?4,'%')))"
			+ " and A.status=1 and A.type=4 "
			+ " and unix_timestamp(A.update_time) between unix_timestamp(?5) and unix_timestamp(?6)";

	public final static String totalFinFrostlessStatistics = "select sum(bank_balance)amount,sum(balance),sum(bank_balance-balance) from (select "
			+ "A.alias," + "A.handicap_id," + "(select name from biz_handicap where id=A.handicap_id)handicapname,"
			+ "(select GROUP_CONCAT(name,'||') from biz_level where handicap_id=A.handicap_id and status=1) levelname,"
			+ "A.type," + "A.account," + "A.bank_name," + "A.bank_balance,A.balance" + " from " + " biz_account A"
			+ " where A.status=3" + " and A.handicap_id in (?1)"
			+ " and (?2 is null or A.account like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.update_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5 is null or unix_timestamp(A.update_time) between unix_timestamp(?5) and unix_timestamp(?6))"
			+ " and (?8 is null or A.type in (?7))) as total";

	public final static String totalFindRebate = "select truncate(sum(amount),2)amoun from (SELECT A.id ," + "A.amount "
			+ "FROM  biz_account_rebate A,biz_rebate_user B where A.uid=B.uid and A.status in (?1) and ((?2='rebate' or ?2='check'  or ?2='canceled') or A.account_id is null) and (?2!='rebate' or A.account_id is not null)"
			+ "  and (?3 is null or A.tid like concat('%',?3,'%')) and (?10 is null or B.user_name like concat('%',?10,'%')) and (?4 is null or A.amount>=?4) and (?5 is null or A.amount<=?5)"
			+ "  and (?6 is null or A.update_time between ?6 and ?7)  and (?8=0 or A.handicap=?8)  and A.handicap in (?9)"
			+ " and (?11 is null or (?11='2' or (A.type is null or A.type=1)) and (?11='1' or A.type=2))) as total";

	public final static String totalFindAuditCommission = "select truncate(sum(total_amount),2)total_amount,truncate(sum(amount),2)amount from (select A.calc_time,sum(A.total_amount)total_amount,(sum(A.amount) + sum(ifnull(A.activity_amount,0))++sum(ifnull(A.agent_amount,0)))amount,count(distinct B.mobile)counts,A.status"
			+ " from fundsTransfer.biz_account_return_summary A,fundsTransfer.biz_account B where A.account=B.id and (?1 is null or calc_time between ?1 and ?2) and (?3 is null or A.status=?3) group by calc_time,A.status) as total";

	public final static String totalFindDerating = "select truncate(sum(amount),2)amoun from ( SELECT A.id ,"
			+ "A.amount " + "FROM  biz_account_rebate A,biz_rebate_user B where A.uid=B.uid and A.status in (?1) "
			+ " and (?2 is null or A.tid like concat('%',?2,'%')) and A.type=2 and (?7 is null or B.user_name like concat('%',?7,'%')) and (?3 is null or A.amount>=?3) and (?4 is null or A.amount<=?4)"
			+ " and (?5=0 or A.handicap=?5) and A.handicap in (?6)) as total";

	public final static String totalFindAuditComplete = "select truncate(sum(amounts),2)total_amount,truncate(sum(am),2)amount from (select A.calc_time,ifnull(B.amounts,0)amounts,A.rebateAmounts am,A.remark from "
			+ " (select date_format(A.calc_time, '%Y-%m-%d')calc_time,sum(ifnull(A.balance,0))rebateAmounts,A.remark"
			+ " from fundsTransfer.biz_account_return_summary A,fundsTransfer.biz_account B where A.account=B.id and A.status=1 group by date_format(calc_time, '%Y-%m-%d'),remark)A"
			+ " left join (select date_format(create_time, '%Y-%m-%d' )create_time,count(distinct uid)counts,sum(amount)amounts from "
			+ " fundsTransfer.biz_account_rebate where (status=1 or status=5) group by date_format(create_time, '%Y-%m-%d')) B on A.calc_time=B.create_time where (?1 is null or A.calc_time between ?1 and ?2)) as total";

	public final static String totalFindDetail = "select sum(balance)total_amount,sum(sys_balance)amount from (select A.id,ifnull(B.balance,0)balance,ifnull(B.sys_balance,0)sys_balance,A.status,date_format(B.time, '%Y-%m-%d') from biz_account A,biz_report B "
			+ " where A.id=B.account_id and A.type in (?5) and (?1 is null or A.account like concat('%',?1,'%')) and (?2 is null or A.bank_type=?2) and A.status in (?6) and (?3=0 or A.handicap_id=?3)  and date_format(B.time, '%Y-%m-%d') = ?4) as total";

	public final static String totalFindAuditDETAIL = "select truncate(sum(total_amount),2)total_amount,truncate(sum(amount),2)amount from (select A.*,D.user_name from ( select account,sum(total_amount)total_amount,(sum(amount)+sum(ifnull(activity_amount,0))+sum(ifnull(agent_amount,0)))amount "
			+ " from fundsTransfer.biz_account_return_summary where calc_time=?3 group by account ) A" + " left join"
			+ " ( select A.id,C.user_name,A.bank_type from biz_account A,biz_account_more B,biz_rebate_user C"
			+ " where A.mobile=B.moible and B.uid=C.uid) D on A.account=D.id where (?1 is null or D.user_name like CONCAT('%',?1,'%')) and (?2 is null or D.bank_type=?2) and (?4=0 or A.amount>=?4) and (?5=0 or A.amount<=?5) and A.amount>0) as total";

	public final static String totalFindCompleteDetail = "select truncate(sum(amount),2)amounts,truncate(sum(ab),2)ab,truncate(sum(ba),2)ba from (select V.user_name,V.amount,ifnull(N.amounts,0)ab,V.amount ba from(select T.uid,T.user_name,sum(T.amount)amount from (select A.*,D.user_name,D.uid from (select account,sum(ifnull(balance,0)) amount"
			+ " from fundsTransfer.biz_account_return_summary where calc_time=?2 group by account ) A" + " left join"
			+ " (select A.id,C.user_name,C.uid,A.bank_type from fundsTransfer.biz_account A,fundsTransfer.biz_account_more B,fundsTransfer.biz_rebate_user C"
			+ " where A.mobile=B.moible and B.uid=C.uid ) D on A.account=D.id order by D.user_name,D.uid desc) T group by T.user_name,T.uid)V"
			+ " left join" + " (select sum(amount)amounts,uid from "
			+ " fundsTransfer.biz_account_rebate where (status=1 or status=5) and date_format(create_time, '%Y-%m-%d')=?2 group by uid) N on V.uid=N.uid where (?1 is null or V.user_name like CONCAT('%',?1,'%'))) as total";

	public final static String totalFindDeleteAccount = "select  sum(bank_balance) "
			+ " from biz_account where (?3='goldener' or status=-2) and (?3='fin' or status in (3,4)) and (?3='fin' or  (update_time<date_add(now(),interval -30 day)))"
			+ " and (?1=0 or handicap_id=?1) and (?2 is null or alias like concat('%',?2,'%'))"
			+ " and (?4 is null or flag=?4) and (?5 is null or status=?5)";

	public final static String totalFinFrostlessPending = "select sum(bank_balance)amount,sum(balance),sum(bank_balance-balance),sum(amount),sum(bank_balance-ifnull(amount,0)) from (select "
			+ "A.alias," + "A.handicap_id," + "A.type," + "A.account," + "A.bank_name,"
			+ "B.bal bank_balance,A.balance,B.amount" + " from " + " biz_account A,biz_account_trace B"
			+ " where A.id=B.account_id and B.status in (?9) and (Case When ?10=6 then (A.owner like concat('%','3天未启用','%'))  else (?10 is null or B.status=?10) end) and (?11 is null or B.defrost_type=?11)"
			+ " and A.handicap_id in (?1)" + " and (?2 is null or A.account like concat('%',?2,'%'))"
			+ " and (?12 is null or A.alias like concat('%',?12,'%'))" + " and (?13 is null or A.flag=?13)"
			+ " and (?3 is null or B.create_time between ?3 and ?4)"
			+ " and (?5 is null or B.create_time between ?5 and ?6)" + " and (?8 is null or A.type in (?7))) as total";

	public final static String totalfindFinInStatMatch = "select sum(amount)amount,sum(fee)fee from (select "
			+ " C.amount," + "IFNULL(0.00,0)fee" + " from " + " biz_account A,biz_income_request C "
			+ " where (?1 is null or C.member_user_name like concat('%',?1,'%')) "
			+ " and (?2 is null or C.update_time between ?2 and ?3) "
			+ " and (?4=0 or (C.amount>=?4 and C.amount<=?5)) and C.status=1 and A.id=C.to_id and A.id=?6 and C.type=?7) as total";

	public final static String totalfindFinInStatMatchBank = "select sum(abs(amount))amount from (select * from biz_bank_log where create_time between ?1 and ?2"
			+ " and (?3=0 or (abs(amount)>=?3 and abs(amount)<=?4)) and (?7=0 or ((?7=-1 or amount>0) and (?7=1 or amount<0))) and from_account=?5 and (?6=9999 or status=?6)) as total";

	public final static String totalfindSendCardMatch = "select sum(amount)amount,sum(fee)fee from (select * from ( select "
			+ " C.from_account," + "(select bank_name from biz_account where id=C.from_id)bankname," + "C.type,"
			+ "C.order_no," + "C.amount," + "IFNULL(B.fee,0)fee,"
			+ "date_format(C.create_time,'%Y-%m-%d %H:%i:%s')create_time," + "C.remark,"
			+ "C.id,C.from_id,(select handicap_id from biz_account where id=C.from_id)handicap_id" + " from "
			+ " biz_account A,biz_transaction_log B,biz_income_request C "
			+ " where A.id=B.to_account and B.order_id=C.id and A.id=?6 "
			+ " and (?2 is null or C.create_time between ?2 and ?3) "
			+ " and (?4=0 or (C.amount>=?4 and C.amount<=?5)) and C.status in (1,3,4,5) and B.type=?7 and C.type=?7) A where (?1 is null or A.bankname like concat('%',?1,'%')) and (?8=0 or A.handicap_id=?8)) as total";

	public final static String totalfindFinInThirdStatMatch = "select sum(amount)amount,sum(fee)fee from (select "
			+ "C.amount," + "IFNULL(C.fee,0)fee " + " from " + " biz_account A,biz_third_request C "
			+ " where A.account=C.to_account and A.id=?6 and (?1 is null or C.member_user_name like concat('%',?1,'%')) "
			+ " and (?2 is null or C.ack_time between ?2 and ?3) "
			+ " and (?4=0 or (C.amount>=?4 and C.amount<=?5))) as total";

	public final static String findIncomeByAccount = "select SUM(A.amount) " + " FROM "
			+ " biz_income_request A,biz_handicap B,biz_level C,biz_account D"
			+ " where A.handicap=B.id and A.level=C.id and A.to_id=D.id and A.type=3 "
			+ " and A.create_time between ?3 and ?4 and A.to_id=?2 and A.member_user_name=?1";

	public final static String totalfindFinTransStat = "select sum(bankamount)bankamount,sum(bankfee)bankfee,sum(sysamount)sysamount,sum(sysfee)sysfee from (select A.account,A.type,A.accountid,A.alias,A.owner,A.bank_type,ifnull(A.amounts,0) sysamount,ifnull(A.fees,0) sysfee,A.count_ inCounts,abs(ifnull(B.amounts,0)) bankamount,ifnull(B.fees,0) bankfee,B.count_ from ("
			+ " select"
			+ " ac.account,ac.alias,ac.owner,ac.bank_type,ac.type,re.from_id accountid,SUM(re.amount)amounts,SUM(re.fee)fees,count(1)count_"
			+ " from" + " biz_income_request re,biz_account ac"
			+ " where re.status=1 and ac.type in (?4) and re.from_id=ac.id and re.type in (?5) and re.update_time BETWEEN ?2 and ?3"
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) and (?6 is null or ac.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or ac.bank_type like concat('%',?7,'%'))" + " and (?8=0 or ac.handicap_id=?8)"
			+ " GROUP BY re.from_id)A" + " left join" + " (select"
			+ " lo.from_account accountid,SUM(lo.amount)amounts,SUM(0.0)fees,count(1)count_" + " from"
			+ " biz_bank_log lo,biz_account ac"
			+ " where lo.status=1 and lo.from_account=ac.id and lo.amount<0 and ac.type in (?4) and lo.trading_time BETWEEN ?2 and ?3"
			+ " GROUP BY lo.from_account)B on A.accountid=B.accountid) as total";

	public final static String totalqueyFinCardLiquidation = "select sum(income),sum(outward),sum(fee),sum(balance),sum(loss),sum(income_sys),sum(outward_sys) from (select A.*,C.balance from (select A.handicap_id,A.account,A.type,A.id,A.alias, "
			+ " A.owner,A.bank_type, B.*,A.status from biz_account A,(select sum(B.income)income,sum(B.outward)outward,sum(B.fee)fee,sum(B.income_count)income_count,sum(B.outward_count)outward_count,sum(B.outward_persons)outward_persons "
			+ " ,sum(B.income_persons)income_persons,sum(B.fee_count)fee_count,sum(B.loss)loss,sum(B.loss_count)loss_count,sum(B.income_sys)income_sys,sum(B.income_sys_count)income_sys_count,sum(B.outward_sys)outward_sys,sum(B.outward_sys_count)outward_sys_count,B.account_id"
			+ " from fundsTransfer.biz_report B where B.time between ?2 and ?3 group by B.account_id)B where A.id=B.account_id"
			+ " and (?1 is null or A.account like concat('%',?1,'%')) "
			+ " and (?4 is null or A.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or A.bank_type like concat('%',?5,'%')) and (?8!=3 or account_id in (?9))"
			+ " and A.handicap_id in (?6)" + " and A.type in (?7)"
			+ " and (?8=0 or A.status=?8))A left join (select account_id,sum(balance)balance from biz_report where time=(select DATE_ADD(DATE_SUB(CURDATE(),INTERVAL 1 DAY),INTERVAL 7 HOUR) from dual) group by account_id) C on A.account_id=C.account_id order by C.balance desc) as total";

	public final static String minusDate = "select sum(balance),sum(income),sum(income_sys) from (select A.*,C.balance from (select A.handicap_id,A.account,A.type,A.id,A.alias, "
			+ " A.owner,A.bank_type, B.*,A.status from biz_account A,(select sum(B.income)income,sum(B.outward)outward,sum(B.fee)fee,sum(B.income_count)income_count,sum(B.outward_count)outward_count,sum(B.outward_persons)outward_persons "
			+ " ,sum(B.income_persons)income_persons,sum(B.fee_count)fee_count,sum(B.loss)loss,sum(B.loss_count)loss_count,sum(B.income_sys)income_sys,sum(B.income_sys_count)income_sys_count,sum(B.outward_sys)outward_sys,sum(B.outward_sys_count)outward_sys_count,B.account_id"
			+ " from fundsTransfer.biz_report B where B.time between ?2 and ?3 group by B.account_id)B where A.id=B.account_id"
			+ " and (?1 is null or A.account like concat('%',?1,'%')) "
			+ " and (?4 is null or A.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or A.bank_type like concat('%',?5,'%'))" + " and A.handicap_id in (?6)"
			+ " and A.type in (?7)" + " and A.type=8"
			+ " and (?8=0 or A.status=?8))A left join (select account_id,sum(balance)balance from biz_report where time=(select DATE_ADD(DATE_SUB(CURDATE(),INTERVAL 1 DAY),INTERVAL 7 HOUR) from dual) group by account_id) C on A.account_id=C.account_id order by C.balance desc) as total";

	public final static String totalfindFinTransStatFromClearDate = "select SUM(cast(outward as DECIMAL(18,2)))bankamount,sum(fee)bankfee,SUM(cast(outward_sys as DECIMAL(18,2)))sysamount from (select B.account_handicap,A.account,A.id,A.alias,"
			+ "A.owner,A.bank_type,B.balance,B.outward,B.fee,B.outward_count,B.outward_sys,B.outward_sys_count"
			+ " from biz_account A,biz_report B"
			+ " where B.time between ?2 and ?3 and (?1 is null or A.account like concat('%',?1,'%')) "
			+ " and (?5 is null or A.owner like concat('%',?5,'%'))"
			+ " and (?6 is null or A.bank_type like concat('%',?6,'%'))" + " and B.account_handicap in (?7)"
			+ " and (?8!='Bankcard' or ROUND(B.outward_sys)!=B.outward_sys)"
			+ " and A.type in (?4) and A.id=B.account_id) as total";

	public final static String totalfindthird = "select sum(inAmounts)sysamount,sum(inFees)sysfee from (select A.account,A.type,A.accountid,A.alias,A.owner,A.bank_type,ifnull(A.amounts,0) inAmounts,ifnull(A.fees,0) inFees,ifnull(A.count_,0) inCounts,ifnull(A.handicap_id,0) from ("
			+ " select"
			+ " ac.handicap_id,ac.account,ac.alias,ac.owner,ac.bank_type,ac.type,re.from_id accountid,SUM(re.amount)amounts,SUM(re.fee)fees,count(1)count_"
			+ " from" + " biz_income_request re,biz_account ac"
			+ " where ac.type in (?4) and re.from_id=ac.id and re.type in (?5) and re.create_time BETWEEN ?2 and ?3"
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) and (?6 is null or ac.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or ac.bank_type like concat('%',?7,'%'))"
			+ " and (?8=0 or ac.handicap_id=?8)  and re.status=1" + " GROUP BY re.from_id)A) as total";

	public final static String totalScreening = "select sum(amount) from ( select amount,date_format(create_time,'%Y-%m-%d %H:%i:%s')create_time,date_format(update_time,'%Y-%m-%d %H:%i:%s')update_time from biz_income_request where "
			+ " create_time between ?1 and ?2"
			+ " and (update_time>?2 or status=0 or update_time>(select concat(if((date_format(create_time,'%Y-%m-%d')=date_format(update_time,'%Y-%m-%d')),date_format(date_add(create_time,interval 1 day),'%Y-%m-%d'),date_format(update_time,'%Y-%m-%d')), ' 06:59:59')))"
			+ " and type in (106,107))A where ((date_format(A.create_time,'%Y-%m-%d')!=date_format(A.update_time,'%Y-%m-%d')) or A.update_time is null)";

	public final static String totalfinTransStatMatchBank = "select sum(amount)amount,IFNULL(sum(fee),0)fee from (select "
			+ "C.order_no," + "A.from_account," + "A.to_account,"
			+ "(select account from biz_account where id=C.from_id)from_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(0.0,0)fee," + "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s')trading_time," + "C.remark,"
			+ "date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time" + " from "
			+ " biz_bank_log A,biz_transaction_log B,biz_income_request C " + " where C.status=1" + " and  A.status=1"
			+ " and (?1 is null or C.order_no =?1) " + " and C.update_time between ?2 and ?3"
			+ " and A.amount>0 and A.status=1 and A.id=B.to_banklog_id and B.order_id=C.id and C.from_id=?6"
			+ " and (?4=0 or (A.amount>=?4 and A.amount<=?5))) as total";

	public final static String totalfinTransStatMatchSys = "select sum(amount)amount,IFNULL(sum(fee),0)fee from (select "
			+ "B.order_no," + "B.from_id," + "B.from_account from_accountname," + "A.account,"
			+ "IFNULL(B.amount,0)amount," + "IFNULL(B.fee,0)fee,"
			+ "date_format(B.create_time,'%Y-%m-%d %H:%i:%s')create_time," + "B.remark,B.to_id,B.status,A.handicap_id "
			+ " from " + " biz_income_request B,biz_account A " + " where (?8=9999 or B.status=?8) and B.from_id=?6"
			+ " and (?1 is null or B.order_no =?1) "
			+ " and (?7=103 or (?2 is null or B.update_time between ?2 and ?3)) and (?7!=103 or (?2 is null or B.update_time between ?2 and ?3))"
			+ " and (?4=0 or (B.amount>=?4 and B.amount<=?5))  and (?9=0 or A.handicap_id=?9) and B.to_id=A.id) as total";

	public final static String totalfindMoreStat = "select sum(amount_in_balance),sum(amount_in_actualamount),sum(count_out_fee),sum(amount_out_balance),sum(amount_out_actualamount),sum(profit) from (select "
			+ " IFNULL(A.amount_in_balance,0)amount_in_balance,IFNULL(A.amount_in_actualamount,0)amount_in_actualamount,"
			+ " IFNULL(B.count_out_fee,0)count_out_fee,IFNULL(B.amount_out_balance,0)amount_out_balance,IFNULL(B.amount_out_actualamount,0)amount_out_actualamount,"
			+ " (IFNULL(A.amount_in_actualamount,0)-IFNULL(B.amount_out_actualamount,0)-IFNULL(B.count_out_fee,0))profit "
			+ " from ( select D.handicap,D.name,D.amount_in_balance,E.amount_in_actualamount" + " from "
			+ " (select A.handicap,B.name,sum(count_in)count_in,sum(amount_in_balance)amount_in_balance,sum(countinps) countinps from (select A.handicap,count(1)count_in,sum(A.amount)amount_in_balance,count(DISTINCT A.member_code) countinps "
			+ " from biz_income_request A" + " where A.type>=1 and type<=100 and A.status!=3 and (?1=0 or handicap=?1) "
			+ " and (?2 is null or A.create_time between ?2 and ?3) "
			+ " and (?4 is null or A.create_time between ?4 and ?5)"
			+ " GROUP BY handicap union select A.handicap,count(1)count_in,sum(A.amount)amount_in_balance,count(DISTINCT A.member_code) countinps "
			+ " from biz_third_request A" + " where (?1=0 or handicap=?1) "
			+ " and (?2 is null or A.ack_time between ?2 and ?3) " + " and (?4 is null or A.ack_time between ?4 and ?5)"
			+ " GROUP BY handicap)A,biz_handicap B where A.handicap=B.id and B.status=1 group by handicap) D "
			+ " LEFT JOIN "
			+ " (select sum(A.amount_in_actualamount)amount_in_actualamount,A.handicap from(select IFNULL(sum(B.amount),0)amount_in_actualamount,B.handicap "
			+ " from biz_income_request B,biz_transaction_log C "
			+ " where B.id=C.order_id and B.status in (1,4,5) and C.type>=1 and C.type<=100 and (?1=0 or B.handicap=?1) "
			+ " and (?2 is null or B.update_time between ?2 and ?3) "
			+ " and (?4 is null or B.update_time between ?4 and ?5)"
			+ " GROUP BY B.handicap union select IFNULL(sum(B.amount),0)amount_in_actualamount,B.handicap"
			+ " from biz_third_request B " + "   where (?1=0 or B.handicap=?1) and "
			+ "  (?2 is null or B.ack_time between ?2 and ?3) and (?4 is null or B.ack_time between ?4 and ?5)"
			+ "  GROUP BY B.handicap)A group by A.handicap)E on D.handicap=E.handicap) A" + " LEFT JOIN"
			+ " (select B.handicap,C.count_out_fee,B.amount_out_balance,C.amount_out_actualamount" + " from "
			+ " (select IFNULL(SUM(A.amount),0)amount_out_balance,A.handicap " + " from biz_outward_request A"
			+ " where A.status!=4 and (?1=0 or A.handicap=?1) "
			+ " and (?2 is null or A.update_time between ?2 and ?3) "
			+ " and (?4 is null or A.update_time between ?4 and ?5)" + " GROUP BY A.handicap) B " + " LEFT JOIN"
			+ " (select IFNULL(sum(C.amount),0)amount_out_actualamount,B.handicap,IFNULL(sum(D.fee),0)count_out_fee "
			+ " from biz_outward_request B,biz_outward_task C,biz_transaction_log D"
			+ " where B.id=C.outward_request_id and C.id=D.order_id and D.type=0 and B.status in (5,6) and C.status=5 and (?1=0 or B.handicap=?1) "
			+ " and (?2 is null or B.update_time between ?2 and ?3) "
			+ " and (?4 is null or B.update_time between ?4 and ?5)"
			+ " GROUP BY B.handicap)C ON B.handicap=C.handicap) B" + " ON A.handicap=B.handicap) as total";

	public final static String totalfindMoreLevelStat = "select sum(amount_in_balance),sum(amount_in_actualamount),sum(count_out_fee),sum(amount_out_balance),sum(amount_out_actualamount),sum(profit) from (select "
			+ " IFNULL(A.amount_in_balance,0)amount_in_balance,IFNULL(A.amount_in_actualamount,0)amount_in_actualamount,"
			+ " IFNULL(B.count_out_fee,0)count_out_fee,IFNULL(B.amount_out_balance,0)amount_out_balance,IFNULL(B.amount_out_actualamount,0)amount_out_actualamount,"
			+ " IFNULL((IFNULL(A.amount_in_actualamount,0)-IFNULL(B.amount_out_actualamount,0)-IFNULL(B.count_out_fee,0)),0)profit"
			+ " from (select D.handicap,D.handicapname,D.level,D.levalname,D.amount_in_balance,E.amount_in_actualamount"
			+ " from "
			+ " (select A.handicap,C.name handicapname,A.level,B.name levalname,sum(count_in)count_in,sum(A.amount_in_balance)amount_in_balance,sum(A.countinps) countinps from (select A.handicap,A.level,count(1)count_in,sum(A.amount)amount_in_balance,count(DISTINCT A.member_code) countinps "
			+ " from biz_income_request A"
			+ " where A.type>=1 and type<=100 and A.status!=3 and A.handicap=?1 and (?2=0 or A.level=?2) "
			+ " and (?3 is null or A.create_time between ?3 and ?4) "
			+ " and (?5 is null or A.create_time between ?5 and ?6)"
			+ " GROUP BY level union select A.handicap,A.level,count(1)count_in,sum(A.amount)amount_in_balance,count(DISTINCT A.member_code) countinps "
			+ " from biz_third_request A" + " where A.handicap=?1 and (?2=0 or A.level=?2) "
			+ " and (?3 is null or A.create_time between ?3 and ?4) "
			+ " and (?5 is null or A.create_time between ?5 and ?6)"
			+ " GROUP BY level)A,biz_level B,biz_handicap C where A.level=B.id and A.handicap=C.id and A.handicap=?1 GROUP BY A.level)D"
			+ " LEFT JOIN"
			+ " (select sum(A.amount_in_actualamount)amount_in_actualamount,A.level from (select IFNULL(sum(B.amount),0)amount_in_actualamount,B.level"
			+ " from biz_income_request B,biz_transaction_log C"
			+ " where B.id=C.order_id and B.status in (1,4,5) and C.type>=1 and C.type<=100 and B.handicap=?1 and (?2=0 or B.level=?2)"
			+ " and (?3 is null or B.update_time between ?3 and ?4) "
			+ " and (?5 is null or B.update_time between ?5 and ?6)"
			+ " GROUP BY B.level union select IFNULL(sum(B.amount),0)amount_in_actualamount,B.level"
			+ " from biz_third_request B" + " where B.handicap=?1 and (?2=0 or B.level=?2)"
			+ " and (?3 is null or B.ack_time between ?3 and ?4) " + " and (?5 is null or B.ack_time between ?5 and ?6)"
			+ " GROUP BY B.level) A group by A.level)E on D.level=E.level)A" + " LEFT JOIN"
			+ " (select B.level,C.count_out_fee,B.amount_out_balance,C.amount_out_actualamount" + " from"
			+ " (select IFNULL(SUM(A.amount),0)amount_out_balance,A.level" + " from biz_outward_request A"
			+ " where A.status!=4 and A.handicap=?1 and (?2=0 or A.level=?2) "
			+ " and (?3 is null or A.create_time between ?3 and ?4) "
			+ " and (?5 is null or A.create_time between ?5 and ?6)" + " GROUP BY A.level) B" + " LEFT JOIN"
			+ " (select IFNULL(sum(C.amount),0)amount_out_actualamount,B.level,IFNULL(sum(D.fee),0)count_out_fee"
			+ " from biz_outward_request B,biz_outward_task C,biz_transaction_log D"
			+ " where B.id=C.outward_request_id and C.id=D.order_id and D.type=0 and B.status in (5,6) and C.status=5 and B.handicap=?1 and (?2=0 or B.level=?2)"
			+ " and (?3 is null or B.update_time between ?3 and ?4) "
			+ " and (?5 is null or B.update_time between ?5 and ?6)" + " GROUP BY B.level)C ON B.level=C.level) B"
			+ " ON A.level=B.level) as total";

	public final static String totalfinBalanceStatCard = "select sum(balance),sum(bank_balance) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + "biz_account A"
			+ " where type=1 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)) as total";

	public final static String totalfinBalanceStatCardcompany = "select sum(balance),sum(bank_balance) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + " biz_account A"
			+ " where type in (1,3,4) and status=?2 and (?1 is null or A.account like concat('%',?1,'%'))) as total";

	public final static String totalfinBalanceStatCardNotissued = "select sum(balance),sum(bank_balance) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + " biz_account A"
			+ " where type=2 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)) as total";

	public final static String totalfinBalanceStatCardOUT = "select sum(balance),sum(bank_balance) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + " biz_account A"
			+ " where type=5 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)) as total";

	public final static String totalfinBalanceStatCardReserveBank = "select sum(balance),sum(bank_balance) from(select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+ " from " + " biz_account A"
			+ " where type in (?1) and status in (?3) and (?2 is null or A.account like concat('%',?2,'%')) and (?4 is null or A.bank_type=?4)) as total";

	public final static String totalfinTransBalanceSys = "select sum(amount),sum(fee)from( select "
			+ " IFNULL(A.amount,0)amount," + "IFNULL(A.fee,0)fee" + " from "
			+ " biz_transaction_log A,biz_income_request B,biz_account C"
			+ " where A.order_id=B.id and C.id=A.to_account and A.type in(1,2,3,4)  and B.type in (1,2,3,4) and B.status in (1,4,5)"
			+ " and (?1 is null or B.member_real_name like concat('%',?1,'%')) and (?2 is null or account like concat('%',?2,'%'))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and (?3 is null or A.create_time between ?3 and ?4) and (?7=0 or A.to_account=?7))as total";

	public final static String totalfinTransBalanceOutSys = "select sum(amount),sum(fee)from(select * from ( select "
			+ "A.id," + "A.to_account," + "(select account from biz_account where id=A.from_account) from_accountname,"
			+ "A.from_account," + "C.to_account to_accountname," + "IFNULL(A.amount,0)amount," + "IFNULL(A.fee,0)fee,"
			+ "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark," + "A.operator,"
			+ "(select username from sys_user where id=A.operator)operatorname" + " from "
			+ " biz_transaction_log A,biz_outward_task B,biz_outward_request C"
			+ " where A.order_id=B.id and B.outward_request_id=C.id and A.type=0  and B.status=5  and C.status in (5,6) "
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.from_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%')))as total";

	public final static String totalfinTransBalanceSysCashBank = "select sum(amount),sum(fee)from(select * from ( select "
			+ "A.id," + "A.to_account," + "B.member_real_name from_accountname," + "A.from_account,"
			+ "(select account from  biz_account where id=A.to_account)to_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(A.fee,0)fee," + "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark,"
			+ "A.operator," + "(select username from  sys_user where id=A.operator)operatorname" + " from "
			+ "  biz_transaction_log A, biz_income_request B" + " where A.order_id=B.id and B.status in (1,4,5)"
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%')))as total";

	public final static String totalfinTransBalanceSysCompany = "select sum(amount),sum(fee)from(select * from ( select "
			+ "A.id," + "A.to_account," + "B.member_real_name from_accountname," + "A.from_account,"
			+ "(select account from  biz_account where id=A.to_account)to_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(A.fee,0)fee," + "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark,"
			+ "A.operator," + "(select username from  sys_user where id=A.operator)operatorname" + " from "
			+ "  biz_transaction_log A, biz_income_request B"
			+ " where A.order_id=B.id and A.type in (1,2,3) and B.type in (1,2,3) and B.status in (1,4,5)"
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%')))as total";

	public final static String totalfinTransBalanceSysThird = "select sum(amount),sum(fee)from(select A.id,A.to_account,A.from_accountname,A.from_account,A.to_accountname,A.amount,"
			+ "A.fee,A.create_time,A.remark,A.operator,ifnull(B.username,'') from ( select * from ( select " + "B.id,"
			+ "B.to_account," + "B.member_user_name from_accountname," + "B.from_account,"
			+ "B.to_account to_accountname," + "IFNULL(B.amount,0)amount," + "IFNULL(B.fee,0)fee,"
			+ "date_format(B.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark," + "333 operator" + " from "
			+ " biz_third_request B" + " where "
			+ " (?3 is null or unix_timestamp(B.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (B.amount>=?5 and B.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%')))A"
			+ " LEFT JOIN sys_user B on A.operator=B.id)as total";

	public final static String totalfinTransBalanceSysNotissued = "select sum(amount),sum(fee)from(select * from ( select "
			+ "A.id," + "A.to_account," + "B.member_real_name from_accountname," + "A.from_account,"
			+ "(select account from  biz_account where id=A.to_account)to_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(A.fee,0)fee," + "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark,"
			+ "A.operator," + "(select username from  sys_user where id=A.operator)operatorname" + " from "
			+ "  biz_transaction_log A, biz_income_request B"
			+ " where A.order_id=B.id and A.type in (1,2,3,4) and B.type in (1,2,3,4) and B.status in (1,4,5)"
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%')))as total";

	public final static String totalfinTransBalancePettycash = "select sum(amount),sum(fee)from(select * from ( select * from ( select "
			+ "A.id," + "A.to_account," + "B.member_real_name from_accountname," + "A.from_account,"
			+ "(select account from biz_account where id=A.to_account)to_accountname," + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(A.fee,0)fee," + "date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark,"
			+ "A.operator," + "(select username from sys_user where id=A.operator)operatorname" + " from "
			+ " biz_transaction_log A,biz_income_request B" + " where A.order_id=B.id and B.status in (1,4,5)"
			+ " and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))"
			+ " union " + "select * from ( select " + "C.id," + "C.to_account,"
			+ "(select account from biz_account where id=C.from_account) from_accountname," + "C.from_account,"
			+ "A.to_account to_accountname," + "IFNULL(C.amount,0)amount," + "IFNULL(C.fee,0)fee,"
			+ "date_format(C.create_time,'%Y-%m-%d %H:%i:%s') create_time," + "B.remark," + "C.operator,"
			+ "(select username from sys_user where id=C.operator)operatorname" + " from "
			+ " biz_transaction_log C,biz_outward_task B,biz_outward_request A"
			+ " where C.order_id=B.id and B.outward_request_id=A.id and C.type=0  and B.status=5  and A.status in (5,6) "
			+ " and (?3 is null or unix_timestamp(C.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (C.amount>=?5 and C.amount<=?6))) C where (?7=0 or C.from_account=?7) and (?1 is null or C.from_accountname like concat('%',?1,'%')) and (?2 is null or C.to_accountname like concat('%',?2,'%'))) as A)as total";

	public final static String totalfinTransBalanceBank = "select sum(amount),sum(fee)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time " + " from "
			+ "  biz_bank_log A where  A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_account like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String totalfinTransBalanceCompanyBank = "select sum(amount),sum(fee)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time" + " from "
			+ "  biz_bank_log A where A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String totalfinTransBalanceThirdBank = "select sum(amount),sum(fee)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(A.fee,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time" + " from "
			+ "  biz_bank_log A where A.type =4 and A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String totalfinTransBalanceNotissuedBank = "select sum(amount),sum(fee)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(A.fee,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time" + " from "
			+ "  biz_bank_log A where A.type in (1,2,3,4) and A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String totalfinTransBalanceCashBank = "select sum(amount),sum(fee)from(select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time" + " from "
			+ "  biz_bank_log A where A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_account like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String totalfinTransBalanceOUTBank = "select sum(amount),sum(fee)from(select * from (select "
			+ "A.id," + "A.from_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname," + "A.to_account,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time " + " from "
			+ "  biz_bank_log A where A.status=1 and A.amount<0) A "
			+ " where (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_account like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as total";

	public final static String totalfinTransBalanceReservePettycashBank = "select sum(amount),sum(fee)from(select * from (select * from (select "
			+ "A.id," + "A.from_account," + "A.to_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time " + " from "
			+ "  biz_bank_log A where A.status=1 and A.amount>0) A "
			+ " where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7" + " union "
			+ "select * from (select " + "A.id," + "A.from_account,"
			+ "(select account from  biz_account where id=A.from_account)from_accountname," + "A.to_account,"
			+ "IFNULL(A.amount,0)amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time " + " from "
			+ "  biz_bank_log A where A.status=1 and A.amount<0) A "
			+ " where (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_account like concat('%',?2,'%'))"
			+ " and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as A) as total";

	public final static String queryCountByToAccount = "select \r\n"
			+ "(select count(id) from biz_income_request where  status=0 and to_id = ?1) as mapping,\r\n"
			+ "(select count(id) from biz_income_request where  status=1 and to_id =  ?1) as mapped,\r\n"
			+ "(select count(id) from biz_income_request where  status=2 and to_id = ?1) as notMap,\r\n"
			+ "(select count(id) from biz_income_request where  status=3 and to_id = ?1) as cancel \r\n" + "from dual";

	public final static String queryCountByFromAccount = "select \r\n"
			+ "(select count(id) from biz_income_request where  status=0 and from_id = ?1) as mapping,\r\n"
			+ "(select count(id) from biz_income_request where  status=1 and from_id = ?1) as mapped,\r\n"
			+ "(select count(id) from biz_income_request where  status=2 and from_id = ?1) as notMap,\r\n"
			+ "(select count(id) from biz_income_request where  status=3 and from_id = ?1) as cancel \r\n"
			+ "from dual";

	public final static String queryCountByFromAccountAndToAccount = "select \r\n"
			+ "(select count(id) from biz_income_request where  status=0 and from_id = ?1 and to_id = ?2) as mapping,\r\n"
			+ "(select count(id) from biz_income_request where  status=1 and from_id = ?1 and to_id = ?2) as mapped,\r\n"
			+ "(select count(id) from biz_income_request where  status=2 and from_id = ?1 and to_id = ?2) as notMap,\r\n"
			+ "(select count(id) from biz_income_request where  status=3 and from_id = ?1 and to_id = ?2) as cancel \r\n"
			+ "from dual";

	public final static String queryCountByAccount4Outward = "select \r\n"
			+ "(select count(id) from biz_outward_task where  status=1 and account_id = ?1) as mapping,\r\n"
			+ "(select count(id) from biz_outward_task where  status=5 and account_id = ?1) as mapped,\r\n"
			+ "(select count(id) from biz_outward_task where  status=6 and account_id = ?1) as cancel \r\n"
			+ "from dual";

	public final static String querySumAmountCondition = "select sum(amount) as amountTotal from biz_bank_log \r\n"
			+ "where   status=0 and  amount>0 and from_account = ?1 and timestampdiff(hour,trading_time,NOW())>=?2 ";

	public final static String queryIncomeTotal = "select sum(amount) as amountTotal from biz_bank_log \r\n"
			+ "where amount>0 and from_account = ?1 ";

	public final static String queryOutTotal = "select sum(amount) as amountTotal from biz_bank_log \r\n"
			+ "where amount<0 and from_account = ?1 ";

	/**
	 * 已匹配查询条件公用部分
	 */
	public final static String queryFilter_incomeMacthed = " from " + " biz_income_request r "
			+ " LEFT JOIN sys_user u  ON u.id=r.operator " + " where 1=1 " + " and r.handicap in (:handicapList) "
			+ " and (:memberUsername is null or r.member_user_name =:memberUsername) "
			+ " and (:startamount=0 or r.amount>=:startamount) " + " and (:endamount=0 or r.amount<=:endamount) "
			+ " and (:toAccount is null or r.to_account like concat('%',:toAccount,'%')) "
			+ " and (:operatorUid is null or u.uid like concat('%',:operatorUid,'%')) "
			+ " and (:manual is null or r.operator is not null) " + " and (:robot is null or r.operator is null) "
			+ " and (:orderNo is null or r.order_no =:orderNo) "
			+ " and (:fristTime is null or r.create_time>=:fristTime) "
			+ " and (:lastTime is null or r.create_time<=:lastTime) " + " and r.type =3 " + " and r.status =1 ";

	public final static String queryFilter_bankLog = " FROM " + " fundsTransfer.biz_bank_log " + " WHERE 1=1 "
			+ " AND (:minAmount is null or amount >=:minAmount ) "
			+ " AND (:maxAmount is null or amount <=:maxAmount ) "
			+ " AND (:startTime is null or create_time>=:startTime) "
			+ " AND (:endTime is null or create_time<=:endTime) "
			+ " AND (:toAccount is null or to_account like  concat('%',:toAccount,'%') ) "
			+ " AND (:toAccountOwner is null or to_account_owner like  concat('%',:toAccountOwner,'%') ) "
			+ " AND (:fromAccount is null or from_Account = :fromAccount ) ";

	public final static String queryFilter_noOwner4Income = " FROM "
			+ " fundsTransfer.biz_account a,fundsTransfer.biz_bank_log log " + " WHERE a.id=log.from_account "
			+ " AND (:remark is null or log.remark like  concat('%',:remark,'%') ) "
			+ " AND (:minAmount is null or log.amount >=:minAmount ) "
			+ " AND (:maxAmount is null or log.amount <=:maxAmount ) "
			+ " AND (:startTime is null or log.create_time>=:startTime) "
			+ " AND (:endTime is null or log.create_time<=:endTime) "
			+ " AND log.create_time <= NOW() - INTERVAL 24 HOUR "
			+ " AND (:toAccount is null or log.to_account like  concat('%',:toAccount,'%') ) "
			+ " AND (:toAccountOwner is null or log.to_account_owner like  concat('%',:toAccountOwner,'%') ) "
			+ " AND (:fromAccountNO is null or a.account like  concat('%',:fromAccountNO,'%') ) " + " AND log.amount>0 "
			+ " AND log.status=0 " + " AND a.type=1 " + " AND a.handicap_id in(:handicapIdToList) "
			+ " ORDER BY log.trading_time,log.create_time,log.id DESC ";

	public final static String SEARCH_FINDISSUEDTHIRD_COUNTQUERY = "select count(1) from biz_income_request where create_time between ?2 and ?3 and from_id=?1";

	public final static String SEARCH_FINDENCASHTHIRD_COUNTQUERY = "select count(1) from biz_third_out where create_time between ?2 and ?3 and handicap is null and from_account=?1";

	public final static String SEARCH_FINDMEMBERSTHIRD_COUNTQUERY = "select count(1) from biz_third_out where create_time between ?2 and ?3 and handicap is not null and from_account=?1 and (?4 is null or handicap=?4)";

	public final static String SEARCH_FINDPERPLESBYROLEID_COUNTQUERY = "select count(1)"
			+ " from sys_user A,sys_user_role B"
			+ " where (?2=0 or B.role_id=?2) and A.id=B.user_id and (?2!=0 or A.status=0) and (?1 is null or A.username=?1)";

	public final static String SEARCH_FINDUSERS_COUNTQUERY = "select count(1)" + " from sys_user A"
			+ " where A.status=0 and A.id not in(select user_id from sys_user_role where role_id=?2) and (?1 is null or A.username=?1)";

	public final static String queryFilter_findIncomeAccountOrderByBankLog = " FROM fundsTransfer.biz_account A "
			+ " LEFT JOIN biz_handicap H ON H.id = A.handicap_id  " + " LEFT JOIN "
			+ " (SELECT COUNT(1) counts, B.from_account  FROM biz_bank_log B WHERE B.STATUS = 0 AND B.create_time > NOW() - INTERVAL 24 HOUR AND B.amount > 0 GROUP BY B.from_account) B ON A.id = B.from_account "
			+ " WHERE  " + " A.status IN (:accountStatusList) " + " AND A.flag IN (:search_IN_flag) "
			+ " AND A.handicap_id IN (:handicapList) " + " AND A.type=1 "
			+ " AND (:owner IS NULL OR A.owner LIKE concat('%',:owner,'%') ) "
			+ " AND (:bankType IS NULL OR A.bank_type =:bankType ) " + " AND (:alias IS NULL OR A.alias =:alias ) "
			+ " AND (:account IS NULL OR A.account LIKE concat('%',:account,'%') ) ";

	public final static String SEARCH_FEEDBACK_COUNTQUERY = "select count(1)"
			+ " from biz_feedback where  status in (?4) and (?1 is null or issue like concat('%',?1,'%')) and (?2 is null or (?6='untreatedFind' or update_time between ?2 and ?3))"
			+ " and (?2 is null or (?6='processedFind' or create_time between ?2 and ?3)) and (?5=0 or level=?5)";

	public final static String totalAmountAliOutToMatch = "SELECT\r\n" + "	sum(bor.amount)\r\n" + "FROM\r\n"
			+ "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "WHERE\r\n" + "	1 = 1\r\n" + "AND bor.handicap = h.id\r\n"
			+ "AND bor. level = l.id\r\n" + "AND bor. STATUS = 7\r\n" + "AND bor. type = 1\r\n"
			+ "AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + "AND (:level IS NULL OR :level=0 OR bor. LEVEL =:level)\r\n" + "AND (\r\n"
			+ "	:member IS NULL\r\n" + "	OR bor.member LIKE concat('%' ,:member, '%')\r\n" + ")\r\n" + "AND (\r\n"
			+ "	:orderNo IS NULL\r\n" + "	OR bor.order_no LIKE concat('%' ,:orderNo, '%')\r\n" + ")\r\n"
			+ "AND bor.create_time >= :timeStart\r\n" + "AND bor.create_time <= :timeEnd";

	public final static String totalAmountAliIncomeToMatch = "select sum(ir.amount) " + "    FROM\r\n"
			+ "        fundsTransfer.biz_income_request ir,\r\n" + "        fundsTransfer.biz_handicap h,\r\n"
			+ "        fundsTransfer.biz_level l  \r\n" + "    WHERE\r\n" + "        1 = 1  \r\n"
			+ "        AND ir.handicap = h.id  \r\n" + "        AND ir.level = l.id and ir. STATUS = 0 AND ir. type = 1 \r\n"
			+ "        and (?1 is null or ir.handicap =?1)" + "      and (?2 is null or ?2 = 0 or ir.level =?2)"
			+ "      and (?3 ='' or ir.member_user_name like concat('%',?3,'%'))"
			+ "      and (?4 ='' or ir.order_no like concat('%',?4,'%')) "
			+ "      and ir.create_time between ?5 and ?6 \r\n";

	public final static String totalAmountAliIncomeFail = "select sum(ir.amount) " + "    FROM\r\n"
			+ "        fundsTransfer.biz_income_request ir,\r\n" + "        fundsTransfer.biz_handicap h,\r\n"
			+ "        fundsTransfer.biz_level l  \r\n" + "    WHERE\r\n" + "        1 = 1  \r\n"
			+ "        AND ir.handicap = h.id  \r\n"
			+ "        AND ir.level = l.id AND (ir.STATUS = 3 or ir.STATUS = 5) AND ir. type = 1 \r\n"
			+ "        and (?1 is null or ir.handicap =?1)" + "      and (?2 is null or ?2 = 0 or ir.level =?2)"
			+ "      and (?3 ='' or ir.member_user_name like concat('%',?3,'%'))"
			+ "      and (?4 ='' or ir.order_no like concat('%',?4,'%')) "
			+ "      and ir.create_time between ?5 and ?6 \r\n";

	public final static String totalAmountAliOutFail = "SELECT\r\n" + "	sum(bor.amount)\r\n" + "FROM\r\n"
			+ "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "WHERE\r\n" + "	1 = 1\r\n" + "AND bor.handicap = h.id\r\n"
			+ "AND bor. level = l.id\r\n" + "AND bor. STATUS = 10\r\n" + "AND bor. type = 1\r\n"
			+ "AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + "AND (:level IS NULL OR :level=0 OR bor. LEVEL =level)\r\n" + "AND (\r\n"
			+ "	:member IS NULL\r\n" + "	OR bor.member LIKE concat('%' ,:member, '%')\r\n" + ")\r\n" + "AND (\r\n"
			+ "	:orderNo IS NULL\r\n" + "	OR bor.order_no LIKE concat('%' ,:orderNo, '%')\r\n" + ")\r\n"
			+ "AND bor.create_time >= :timeStart\r\n" + "AND bor.create_time <= :timeEnd";

	public final static String aliIncomeToMatch = "SELECT\r\n" + "	count(1)\r\n" + "FROM\r\n" + "	(\r\n"
			+ "		SELECT\r\n" + "	ir.id\r\n" + "FROM\r\n" + "	fundsTransfer.biz_income_request ir,\r\n"
			+ "	fundsTransfer.biz_handicap h,\r\n" + "	fundsTransfer.biz_level l\r\n" + "WHERE\r\n" + "	1 = 1\r\n"
			+ "AND ir.handicap = h.id\r\n" + "AND ir. LEVEL = l.id\r\n" + "AND ir. STATUS = 0 AND ir. type = 1 \r\n"
			+ "AND ir. type = 1\r\n" + "AND (? 1 IS NULL OR ir.handicap =? 1)\r\n"
			+ "AND (? 2 IS NULL OR ir. LEVEL =? 2)\r\n" + "AND (\r\n" + "	? 3 IS NULL\r\n"
			+ "	OR ir.member_user_name LIKE concat('%' ,? 3, '%')\r\n" + ")\r\n" + "AND (\r\n" + "	? 4 IS NULL\r\n"
			+ "	OR ir.order_no LIKE concat('%' ,? 4, '%')\r\n" + ")\r\n" + "AND ir.create_time BETWEEN ? 5\r\n"
			+ "AND ? 6\r\n" + "	) AS total";

	public final static String aliOutToMatch = "SELECT\r\n" + "	count(1)\r\n" + "FROM\r\n" + "	(\r\n" + "	SELECT\r\n"
			+ "	bor.id\r\n" + "FROM\r\n" + "	fundsTransfer.biz_outward_request bor,\r\n"
			+ "	fundsTransfer.biz_handicap h,\r\n" + "	fundsTransfer.biz_level l\r\n" + "WHERE\r\n" + "	1 = 1\r\n"
			+ "AND bor.handicap = h.id\r\n" + "AND bor. level = l.id\r\n" + "AND bor. STATUS = 7\r\n"
			+ "AND bor. type = 1\r\n" + "AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level=0 OR bor. LEVEL =:level)\r\n" + "AND (\r\n" + "	:member IS NULL\r\n"
			+ "	OR bor.member LIKE concat('%' ,:member, '%')\r\n" + ")\r\n" + "AND (\r\n" + "	:orderNo IS NULL\r\n"
			+ "	OR bor.order_no LIKE concat('%' ,:orderNo, '%')\r\n" + ")\r\n" + "AND bor.create_time >= :timeStart\r\n"
			+ "AND  bor.create_time <= :timeEnd\r\n" + "	) AS total";

	public final static String aliIncomeFail = "select count(1) from ( select\r\n" + "        ir.id,\r\n"
			+ "    FROM\r\n" + "        fundsTransfer.biz_income_request ir,\r\n"
			+ "        fundsTransfer.biz_handicap h,\r\n" + "        fundsTransfer.biz_level l  \r\n" + "    WHERE\r\n"
			+ "        1 = 1  \r\n" + "        AND ir.handicap = h.id  \r\n"
			+ "        AND ir.level = l.id AND (ir.STATUS = 3 or ir.STATUS = 5) AND ir. type = 1 \r\n"
			+ "          and (?1 is null or ir.handicap =?1)" + "        and (?2 is null or ?2 = 0 or ir.level =?2) "
			+ "     and (?3 ='' or ir.member_user_name like concat('%',?3,'%'))"
			+ "       and (?4 ='' or ir.order_no like concat('%',?4,'%'))"
			+ "     and ir.create_time between ?5 and ?6\r\n" + "        )  as total ";

	public final static String aliOutFail = "SELECT\r\n" + "	count(1)\r\n" + "FROM\r\n" + "	(\r\n" + "	SELECT\r\n"
			+ "	bor.id\r\n" + "FROM\r\n" + "	fundsTransfer.biz_outward_request bor,\r\n"
			+ "	fundsTransfer.biz_handicap h,\r\n" + "	fundsTransfer.biz_level l\r\n" + "WHERE\r\n" + "	1 = 1\r\n"
			+ "AND bor.handicap = h.id\r\n" + "AND bor. level = l.id\r\n" + "AND bor. STATUS = 10\r\n"
			+ "AND bor. type = 1\r\n" + "AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level=0 OR bor. LEVEL =:level)\r\n" + "AND (\r\n" + "	:member IS NULL\r\n"
			+ "	OR bor.member LIKE concat('%' ,:member, '%')\r\n" + ")\r\n" + "AND (\r\n" + "	:orderNo IS NULL\r\n"
			+ "	OR bor.order_no LIKE concat('%' ,:orderNo, '%')\r\n" + ")\r\n" + "AND bor.create_time >= :timeStart\r\n"
			+ "AND bor.create_time <= :timeEnd\r\n" + "	) AS total";

	public final static String aliIncomeMatched = "SELECT\r\n" + "	count(1)\r\n" + "FROM\r\n" + "	(\r\n"
			+ "		SELECT\r\n" + "	id from\r\n" + " (\r\n" + "SELECT\r\n" + "	ir.id\r\n" + "FROM\r\n"
			+ "	fundsTransfer.biz_income_request ir,\r\n" + "  fundsTransfer.biz_chatpay_log cl,\r\n"
			+ "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l,\r\n" + "	fundsTransfer.biz_handicap th,\r\n"
			+ "	fundsTransfer.biz_level tl\r\n" + "WHERE\r\n" + "1 = 1\r\n" + "AND ir.order_no = cl.income_order_no\r\n"
			+ "AND cl.outward_order_no = bor.order_no\r\n" + "AND bor.handicap = th.id\r\n"
			+ "AND bor.LEVEL = tl.id \r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 1\r\n" + "AND ir. type = 1 AND cl. status = 0\r\n" + "and bor.member LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + "union \r\n" + "\r\n"
			+ "SELECT\r\n" + "	ir.id\r\n" + "FROM\r\n" + "	fundsTransfer.biz_income_request ir,\r\n"
			+ "  fundsTransfer.biz_chatpay_log cl,\r\n" + "  fundsTransfer.biz_account_translog bat,\r\n"
			+ "  fundsTransfer.biz_account a,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "\r\n" + "WHERE\r\n" + "1 = 1\r\n"
			+ "AND ir.order_no = cl.income_order_no\r\n" + "AND cl.outward_order_no = bat.id\r\n"
			+ "AND bat.account_id = a.id\r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 1\r\n" + "AND ir. type = 1 AND cl. status = 0 \r\n" + "and a.account LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bat.code LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + ") orderAll\r\n" + "\r\n"
			+ "\r\n" + "\r\n" + "\r\n" + "	) AS total";
	
	public final static String aliOutMatched = "SELECT\r\n" + 
			"	count(1)\r\n" + 
			"FROM (\r\n" + 
			"SELECT id\r\n" + 
			"from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			" bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  ir.member_user_name as toMember,\r\n" + 
			"  ir.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  ir.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = ir.order_no\r\n" + 
			"AND ir.handicap = th.id\r\n" + 
			"AND ir.LEVEL = tl.id \r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 8\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:toMember, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			"  bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 8\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND ( :handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"AND bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			") orderAll GROUP BY id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount\r\n" + 
			"	\r\n" + 
			") AS total";

	public final static String aliIncomeSuccess = "SELECT\r\n" + "	count(1)\r\n" + "FROM\r\n" + "	(\r\n"
			+ "		SELECT\r\n" + "	id from\r\n" + " (\r\n" + "SELECT\r\n" + "	ir.id\r\n" + "FROM\r\n"
			+ "	fundsTransfer.biz_income_request ir,\r\n" + "  fundsTransfer.biz_chatpay_log cl,\r\n"
			+ "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l,\r\n" + "	fundsTransfer.biz_handicap th,\r\n"
			+ "	fundsTransfer.biz_level tl\r\n" + "WHERE\r\n" + "1 = 1\r\n" + "AND ir.order_no = cl.income_order_no\r\n"
			+ "AND cl.outward_order_no = bor.order_no\r\n" + "AND bor.handicap = th.id\r\n"
			+ "AND bor.LEVEL = tl.id \r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 4\r\n" + "AND ir. type = 1 AND cl. status = 1 \r\n" + "and bor.member LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + "union \r\n" + "\r\n"
			+ "SELECT\r\n" + "	ir.id\r\n" + "FROM\r\n" + "	fundsTransfer.biz_income_request ir,\r\n"
			+ "  fundsTransfer.biz_chatpay_log cl,\r\n" + "  fundsTransfer.biz_account_translog bat,\r\n"
			+ "  fundsTransfer.biz_account a,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "\r\n" + "WHERE\r\n" + "1 = 1\r\n"
			+ "AND ir.order_no = cl.income_order_no\r\n" + "AND cl.outward_order_no = bat.id\r\n"
			+ "AND bat.account_id = a.id\r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 4\r\n" + "AND ir. type = 1 AND cl. status = 1 \r\n" + "and a.account LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bat.code LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + ") orderAll\r\n"
			+ ") AS total";
	
	public final static String aliOutSuccess = "SELECT\r\n" + 
			"	count(1)\r\n" + 
			"FROM (\r\n" + 
			"SELECT id\r\n" + 
			"from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			" bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  ir.member_user_name as toMember,\r\n" + 
			"  ir.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  ir.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = ir.order_no\r\n" + 
			"AND ir.handicap = th.id\r\n" + 
			"AND ir.LEVEL = tl.id \r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 9\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:toMember, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			"  bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 9\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND ( :handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"AND bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			") orderAll GROUP BY id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount\r\n" + 
			"	\r\n" + 
			") AS total";

	public final static String totalAmountAliIncomeMatched = "SELECT\r\n" + "	sum(\r\n" + "  amount\r\n" + "  )\r\n"
			+ "FROM\r\n" + "\r\n" + " (\r\n" + "SELECT\r\n" + "	ir.amount\r\n" + "FROM\r\n"
			+ "	fundsTransfer.biz_income_request ir,\r\n" + "  fundsTransfer.biz_chatpay_log cl,\r\n"
			+ "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l,\r\n" + "	fundsTransfer.biz_handicap th,\r\n"
			+ "	fundsTransfer.biz_level tl\r\n" + "WHERE\r\n" + "1 = 1\r\n" + "AND ir.order_no = cl.income_order_no\r\n"
			+ "AND cl.outward_order_no = bor.order_no\r\n" + "AND bor.handicap = th.id\r\n"
			+ "AND bor.LEVEL = tl.id \r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 1\r\n" + "AND ir. type = 1\r\n AND cl. status = 0 " + "and bor.member LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + "union \r\n" + "\r\n"
			+ "SELECT\r\n" + "	ir.amount\r\n" + "FROM\r\n" + "	fundsTransfer.biz_income_request ir,\r\n"
			+ "  fundsTransfer.biz_chatpay_log cl,\r\n" + "  fundsTransfer.biz_account_translog bat,\r\n"
			+ "  fundsTransfer.biz_account a,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "\r\n" + "WHERE\r\n" + "1 = 1\r\n"
			+ "AND ir.order_no = cl.income_order_no\r\n" + "AND cl.outward_order_no = bat.id\r\n"
			+ "AND bat.account_id = a.id\r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 1\r\n" + "AND ir. type = 1 AND cl. status = 0 \r\n" + "and a.account LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bat.code LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + ") orderAll\r\n" + "	";

	public final static String totalAmountAliOutMatched ="SELECT\r\n" + 
			"	sum(\r\n" + 
			"  amount\r\n" + 
			"  )\r\n" + 
			"FROM\r\n" + 
			"\r\n" + 
			" (\r\n" + 
			"SELECT amount\r\n" + 
			"from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			" bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  ir.member_user_name as toMember,\r\n" + 
			"  ir.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  ir.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = ir.order_no\r\n" + 
			"AND ir.handicap = th.id\r\n" + 
			"AND ir.LEVEL = tl.id \r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 8\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:toMember, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			"  bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 8\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND ( :handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"AND bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			") orderAll GROUP BY id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount\r\n" + 
			") orderAll2\r\n" + 
			"	";
			
	public final static String totalToAmountAliIncomeMatched = "SELECT\r\n" + "	sum(\r\n" + "toAmount\r\n" + "  ) \r\n"
			+ "FROM\r\n" + "	 (\r\n" + "SELECT\r\n" + "  bor.amount as toAmount\r\n" + "FROM\r\n"
			+ "	fundsTransfer.biz_income_request ir,\r\n" + "  fundsTransfer.biz_chatpay_log cl,\r\n"
			+ "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l,\r\n" + "	fundsTransfer.biz_handicap th,\r\n"
			+ "	fundsTransfer.biz_level tl\r\n" + "WHERE\r\n" + "1 = 1\r\n" + "AND ir.order_no = cl.income_order_no\r\n"
			+ "AND cl.outward_order_no = bor.order_no\r\n" + "AND bor.handicap = th.id\r\n"
			+ "AND bor.LEVEL = tl.id \r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 1\r\n" + "AND ir. type = 1 AND cl. status = 0 \r\n" + "and bor.member LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + "union \r\n" + "\r\n"
			+ "SELECT\r\n" + "  bat.money as toAmount\r\n" + "FROM\r\n" + "	fundsTransfer.biz_income_request ir,\r\n"
			+ "  fundsTransfer.biz_chatpay_log cl,\r\n" + "  fundsTransfer.biz_account_translog bat,\r\n"
			+ "  fundsTransfer.biz_account a,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "\r\n" + "WHERE\r\n" + "1 = 1\r\n"
			+ "AND ir.order_no = cl.income_order_no\r\n" + "AND cl.outward_order_no = bat.id\r\n"
			+ "AND bat.account_id = a.id\r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 1\r\n" + "AND ir. type = 1 AND cl. status = 0 \r\n" + "and a.account LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bat.code LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + ") orderAll";

	public final static String totalToAmountAliOutMatched = "SELECT\r\n" + 
			"	sum(\r\n" + 
			"  toAmount\r\n" + 
			"  )\r\n" + 
			"FROM\r\n" + 
			"\r\n" + 
			" (\r\n" + 
			"SELECT sum(toAmount) as toAmount\r\n" + 
			"from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			" bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  ir.member_user_name as toMember,\r\n" + 
			"  ir.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  ir.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = ir.order_no\r\n" + 
			"AND ir.handicap = th.id\r\n" + 
			"AND ir.LEVEL = tl.id \r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 8\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:toMember, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			"  bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 8\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND ( :handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"AND bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			") orderAll GROUP BY id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount\r\n" + 
			") orderAll2\r\n" + 
			"	";
	
	public final static String totalAmountAliIncomeSuccess = "SELECT\r\n" + "	sum(\r\n" + "  amount\r\n" + "  )\r\n"
			+ "FROM\r\n" + "\r\n" + " (\r\n" + "SELECT\r\n" + "	\r\n" + "	ir.amount\r\n" + "  \r\n" + "FROM\r\n"
			+ "	fundsTransfer.biz_income_request ir,\r\n" + "  fundsTransfer.biz_chatpay_log cl,\r\n"
			+ "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l,\r\n" + "	fundsTransfer.biz_handicap th,\r\n"
			+ "	fundsTransfer.biz_level tl\r\n" + "WHERE\r\n" + "1 = 1\r\n" + "AND ir.order_no = cl.income_order_no\r\n"
			+ "AND cl.outward_order_no = bor.order_no\r\n" + "AND bor.handicap = th.id\r\n"
			+ "AND bor.LEVEL = tl.id \r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 4\r\n" + "AND ir. type = 1 AND cl. status = 1 \r\n" + "and bor.member LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + "union \r\n" + "\r\n"
			+ "SELECT\r\n" +

			"	ir.amount\r\n" +

			"FROM\r\n" + "	fundsTransfer.biz_income_request ir,\r\n" + "  fundsTransfer.biz_chatpay_log cl,\r\n"
			+ "  fundsTransfer.biz_account_translog bat,\r\n" + "  fundsTransfer.biz_account a,\r\n"
			+ "	fundsTransfer.biz_handicap h,\r\n" + "	fundsTransfer.biz_level l\r\n" + "\r\n" + "WHERE\r\n"
			+ "1 = 1\r\n" + "AND ir.order_no = cl.income_order_no\r\n" + "AND cl.outward_order_no = bat.id\r\n"
			+ "AND bat.account_id = a.id\r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 4\r\n" + "AND ir. type = 1 AND cl. status = 1 \r\n" + "and a.account LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bat.code LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + ") orderAll\r\n" + "	";

	public final static String totalAmountAliOutSuccess = "SELECT\r\n" + 
			"	sum(\r\n" + 
			"  amount\r\n" + 
			"  )\r\n" + 
			"FROM\r\n" + 
			"\r\n" + 
			" (\r\n" + 
			"SELECT amount\r\n" + 
			"from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			" bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  ir.member_user_name as toMember,\r\n" + 
			"  ir.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  ir.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = ir.order_no\r\n" + 
			"AND ir.handicap = th.id\r\n" + 
			"AND ir.LEVEL = tl.id \r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 9\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:toMember, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			"  bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 9\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND ( :handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"AND bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			") orderAll GROUP BY id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount\r\n" + 
			") orderAll2\r\n" + 
			"	";
	
	public final static String totalToAmountAliIncomeSuccess = "SELECT\r\n" + "	sum(\r\n" + "toAmount\r\n" + "  ) \r\n"
			+ "FROM\r\n" + "	 (\r\n" + "SELECT\r\n" + "	\r\n" + "  bor.amount as toAmount\r\n" + "  \r\n"
			+ "FROM\r\n" + "	fundsTransfer.biz_income_request ir,\r\n" + "  fundsTransfer.biz_chatpay_log cl,\r\n"
			+ "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l,\r\n" + "	fundsTransfer.biz_handicap th,\r\n"
			+ "	fundsTransfer.biz_level tl\r\n" + "WHERE\r\n" + "1 = 1\r\n" + "AND ir.order_no = cl.income_order_no\r\n"
			+ "AND cl.outward_order_no = bor.order_no\r\n" + "AND bor.handicap = th.id\r\n"
			+ "AND bor.LEVEL = tl.id \r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 4\r\n" + "AND ir. type = 1 AND cl. status = 1 \r\n" + "and bor.member LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + "union \r\n" + "\r\n"
			+ "SELECT\r\n" + "  bat.money as toAmount\r\n" + "FROM\r\n" + "	fundsTransfer.biz_income_request ir,\r\n"
			+ "  fundsTransfer.biz_chatpay_log cl,\r\n" + "  fundsTransfer.biz_account_translog bat,\r\n"
			+ "  fundsTransfer.biz_account a,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "\r\n" + "WHERE\r\n" + "1 = 1\r\n"
			+ "AND ir.order_no = cl.income_order_no\r\n" + "AND cl.outward_order_no = bat.id\r\n"
			+ "AND bat.account_id = a.id\r\n" + "AND ir.handicap = h.id\r\n" + "AND ir.LEVEL = l.id\r\n"
			+ "AND ir. STATUS = 4\r\n" + "AND ir. type = 1 AND cl. status = 1 \r\n" + "and a.account LIKE concat('%' ,:toMember, '%')\r\n"
			+ "and bat.code LIKE concat('%' ,:outOrderNo, '%')\r\n" + "AND ( ir.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n"
			+ "AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n"
			+ "AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n"
			+ "AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n"
			+ "AND ir.create_time >= :timeStart\r\n" + "AND ir.create_time <= :timeEnd\r\n" + ") orderAll";
	
	public final static String totalToAmountAliOutSuccess = "SELECT\r\n" + 
			"	sum(\r\n" + 
			"  toAmount\r\n" + 
			"  )\r\n" + 
			"FROM\r\n" + 
			"\r\n" + 
			" (\r\n" + 
			"SELECT sum(toAmount) as toAmount\r\n" + 
			"from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			" bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  ir.member_user_name as toMember,\r\n" + 
			"  ir.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  ir.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = ir.order_no\r\n" + 
			"AND ir.handicap = th.id\r\n" + 
			"AND ir.LEVEL = tl.id \r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 9\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:toMember, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			"  bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 9\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND ( :handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"AND bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			") orderAll GROUP BY id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount\r\n" + 
			") orderAll2\r\n" + 
			"	";
}
