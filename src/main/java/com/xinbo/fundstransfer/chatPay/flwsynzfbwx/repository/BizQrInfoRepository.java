package com.xinbo.fundstransfer.chatPay.flwsynzfbwx.repository;

import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BizQrInfoRepository extends BaseRepository<BizQrInfo, Long> {


    /**
     * 二维码验证工具获取任务-1.返利网兼职，新二维码
     */
     @Query(nativeQuery = true, value = "SELECT * FROM biz_qr_info WHERE qr_from=0  and val_qr_status=0 ORDER BY create_time asc LIMIT 1")
     BizQrInfo findFlwNewQr();


    /**
     * 二维码验证工具获取任务-1.返利网兼职，5分钟还每返回结果的验证任务
     */
     @Query(nativeQuery = true, value = "SELECT * FROM biz_qr_info WHERE qr_from=0  and val_qr_status=1 and val_qr_job_time <= CURRENT_TIMESTAMP - INTERVAL 5 MINUTE   ORDER BY create_time asc LIMIT 1")
     BizQrInfo findFlwJobQr();


    /**
     * 二维码验证工具获取任务-3.平台会员，新二维码
     */
    @Query(nativeQuery = true, value = "SELECT * FROM biz_qr_info WHERE qr_from=1  and val_qr_status=0 ORDER BY create_time asc LIMIT 1")
    BizQrInfo findPtNewQr();


    /**
     * 二维码验证工具获取任务-4.平台会员，2分钟还每返回结果的验证任务
     */
    @Query(nativeQuery = true, value = "SELECT * FROM biz_qr_info WHERE qr_from=1  and val_qr_status=1 and val_qr_job_time <= CURRENT_TIMESTAMP - INTERVAL 2 MINUTE   ORDER BY create_time asc LIMIT 1")
    BizQrInfo findPtJobQr();


    /**
     * 二维码id获取二维码信息
     */
    BizQrInfo findByQrId(String qrId);


    /**
     * 二维码查询
     */
    List<BizQrInfo> findAllByUidAndQrStatusAndValQrStatusAndQrContentIn(String uid,int qrStatus,int valQrStatus,String[] qrContent);



}