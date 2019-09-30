package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizHost;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface HostRepository extends BaseRepository<BizHost, Integer> {
	
	@Query(nativeQuery = true, 
			value = "SELECT IFNULL(SUM(1) ,0)  FROM fundsTransfer.biz_host UNION ALL " + 
					"SELECT IFNULL(SUM(1) ,0)  FROM fundsTransfer.biz_account where gps is not null UNION ALL " + 
					"SELECT IFNULL(SUM(1) ,0)  FROM fundsTransfer.biz_account where gps is not null AND TYPE=1 UNION ALL " + 
					"SELECT IFNULL(SUM(1) ,0)  FROM fundsTransfer.biz_account where gps is not null AND TYPE=5 UNION ALL " + 
					"SELECT IFNULL(SUM(1) ,0)  FROM fundsTransfer.biz_account where gps is not null AND TYPE IN (10,11,12,13) UNION ALL " + 
					"SELECT IFNULL(SUM(1) ,0)  FROM fundsTransfer.biz_account where gps is not null AND TYPE=8 ")
	String[] loadHostTotal();
	

	@Query(nativeQuery = true, 
			value = "SELECT * FROM fundsTransfer.biz_host WHERE host_info LIKE CONCAT('%',?1,',%')  OR ip =?1 LIMIT 1")
	BizHost findByIp(String ip);
	/**
	 * 搜索主机 不区分大小写  主机匹配后有个【,】且ip校验时为全匹配
	 * @param seachStr 完整匹配：编号/IP	模糊匹配：主机名/账号
	 * @return
	 */
	@Query(nativeQuery = true, 
			value = "SELECT DISTINCT b.* FROM fundsTransfer.biz_host b " + 
					" LEFT JOIN fundsTransfer.biz_account a " + 
					" ON (b.host_info LIKE CONCAT('%',a.gps,',%') OR b.ip =a.gps) " + 
					" WHERE  " + 
					" b.ip LIKE CONCAT('%',?1,'%') " + 
					" OR b.name LIKE CONCAT('%',?1,'%') " + 
					" OR b.host_info LIKE CONCAT('%',?1,'%') " + 
					" OR ( ( a.account LIKE CONCAT('%',?1,'%') OR a.alias=?1 ) AND a.gps IS NOT NULL ) " + 
					" ORDER BY b.x,b.y ")
	List<BizHost> findList(String seachStr);
}