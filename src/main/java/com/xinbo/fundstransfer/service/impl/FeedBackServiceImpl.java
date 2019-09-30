package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.ali4enterprise.inputdto.CommonInputDTO;
import com.xinbo.fundstransfer.ali4enterprise.outputdto.PageOutPutDTO;
import com.xinbo.fundstransfer.component.net.http.newpay.HttpClient4NewPay;
import com.xinbo.fundstransfer.component.net.http.newpay.PlatformNewPayService;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.enums.FeedBackStatus;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.FeedBackRepository;
import com.xinbo.fundstransfer.newpay.RequestBodyNewPay;
import com.xinbo.fundstransfer.service.FeedBackService;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import rx.Observable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FeedBackServiceImpl implements FeedBackService {
    static final Logger log = LoggerFactory.getLogger(FeedBackServiceImpl.class);
    @Autowired
    private FeedBackRepository feedBackRepository;
    @Value("${funds.transfer.multipart.location}")
    private String deletePath;

    @Override
    public Map<String, Object> findFeedBack(String untreatedFind, String fristTime, String lastTime, String type,
                                            String level, PageRequest pageRequest) throws Exception {
        List<Integer> status = new ArrayList<>();
        if (type.equals("untreatedFind")) {
            status.add(FeedBackStatus.Untreated.getStatus());
            status.add(FeedBackStatus.Processing.getStatus());
        } else {
            status.add(FeedBackStatus.Treated.getStatus());
        }
        Page<Object> dataToPage = feedBackRepository.findFeedBack(untreatedFind, fristTime, lastTime, status, level,
                type, pageRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("Page", dataToPage);
        return map;
    }

    @Transactional
    @Override
    public void saveFeedBack(String level, String describe, String userId, String imgs) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
        String nowTime = df.format(new Date());
        feedBackRepository.saveFeedBack(nowTime, level, describe, userId, imgs);
    }

    @Override
    public String findOldRemark(String id) throws Exception {
        return feedBackRepository.findOldRemark(id);
    }

    @Transactional
    @Override
    public void saveRemark(String id, String remark) throws Exception {
        feedBackRepository.saveRemark(id, remark);
    }

    @Transactional
    @Override
    public void dealWith(String id, String uid, String remark) throws Exception {
        feedBackRepository.dealWith(id, uid, remark);
    }

    @Transactional
    @Override
    public void finish(String id) throws Exception {
        feedBackRepository.finish(id);
    }

    @Override
    public List<Object> showFeedBackDetails(String id) throws Exception {
        return feedBackRepository.showFeedBackDetails(id);
    }

    @Transactional
    @Override
    public void deleteFeedBack(String id) throws Exception {
        feedBackRepository.deleteFeedBack(id);
    }

    @Override
    public void deleteFeedBackImgs(String imgs) throws Exception {
        String[] img = imgs.split(",");
        for (int i = 0; i < img.length; i++) {
            deletedScreenshots(deletePath + img[i]);
        }
    }

    public static void deletedScreenshots(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        int tryCount = 0;
        while (tryCount++ < 3) {
            System.gc();
            file.delete();
        }
    }

    @Autowired
    private RequestBodyNewPay requestBodyNewPay;

    @Override
    public ResponseDataNewPay addForCrk(AddForCrkInputDTO inputDTO) {
        ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
        try {
            RequestBody requestBody = requestBodyNewPay.addForCrkRequestBody(inputDTO);
            PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
                    .getPlatformNewPayServiceApi(true);
            if (inputDTO.getBussinessType() == 1) {
                Observable<ResponseDataNewPay> res = platformNewPayService.addForCrk(requestBody);
                res.subscribe(ret -> {
                    log.info("FeedBackServiceImpl execute addForCrk success :code:{},msg:{}", ret.getCode(),
                            ret.getMsg());
                    threadLocal.set(ret);
                }, e -> log.error("FeedBackServiceImpl execute addForCrk  fail : ", e));
            } else if (inputDTO.getBussinessType() == 2) {
                Observable<ResponseDataNewPay> res = platformNewPayService.addForFlw(requestBody);
                res.subscribe(ret -> {
                    log.info("FeedBackServiceImpl execute addForFlw success :code:{},msg:{}", ret.getCode(),
                            ret.getMsg());
                    threadLocal.set(ret);
                }, e -> log.error("FeedBackServiceImpl execute addForFlw  fail : ", e));
            } else if (inputDTO.getBussinessType() == 3) {
                Observable<ResponseDataNewPay> res = platformNewPayService.addForFlwTool(requestBody);
                res.subscribe(ret -> {
                    log.info("FeedBackServiceImpl execute addForFlwTool success :code:{},msg:{}", ret.getCode(),
                            ret.getMsg());
                    threadLocal.set(ret);
                }, e -> log.error("FeedBackServiceImpl execute addForFlwTool  fail : ", e));
            } else if (inputDTO.getBussinessType() == 4) {
                Observable<ResponseDataNewPay> res = platformNewPayService.addForPCTool(requestBody);
                res.subscribe(ret -> {
                    log.info("FeedBackServiceImpl execute addForPCTool success :code:{},msg:{}", ret.getCode(),
                            ret.getMsg());
                    threadLocal.set(ret);
                }, e -> log.error("FeedBackServiceImpl execute addForPCTool  fail : ", e));
            } else {
                log.error("FeedBackServiceImpl execute fail :未指定业务类型 ");
            }

        } catch (Exception e) {
            log.error("FeedBackServiceImpl execute addForCrk fail : ", e);
        }
        return threadLocal.get();
    }

    @Override
    public ResponseDataNewPay upload(List<File> files) {
        if (CollectionUtils.isEmpty(files)) {
            return null;
        }
        ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
        try {
            PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
                    .getPlatformNewPayServiceApi(true);
            if (platformNewPayService == null) {
                return null;
            }
            List<MultipartBody.Part> list = new ArrayList<>();
            for (int i = 0, len = files.size(); i < len; i++) {
                RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), files.get(i));
                MultipartBody.Part requestImgPart = MultipartBody.Part.createFormData("file" + i,
                        files.get(i).getName(), requestBody);
                list.add(requestImgPart);
            }
            log.info("requestImgPart size:{}", list.size());
            Observable<ResponseDataNewPay> res = platformNewPayService.upload(list);
            res.subscribe(ret -> {
                log.info("FeedBackServiceImpl execute upload success :code:{},msg:{}", ret.getCode(), ret.getMsg());
                threadLocal.set(ret);
            }, e -> log.error("FeedBackServiceImpl execute upload error : ", e));
        } catch (Exception e) {
            log.error("execute upload error:", e);
        }
        return threadLocal.get();
    }

    @Override
    public ResponseDataNewPay upload2(List<File> files) {
        if (CollectionUtils.isEmpty(files)) {
            return null;
        }
        ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
        try {
            PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
                    .getPlatformNewPayServiceApi(true);
            if (platformNewPayService == null) {
                return null;
            }
            MultipartBody.Builder builder = new MultipartBody.Builder();
            for (int i = 0, len = files.size(); i < len; i++) {
                RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), files.get(i));
                builder.addFormDataPart("file" + i, files.get(i).getName(), requestBody);
            }
            builder.setType(MultipartBody.FORM);
            MultipartBody multipartBody = builder.build();
            Observable<ResponseDataNewPay> res = platformNewPayService.upload2(multipartBody);
            res.subscribe(ret -> {
                log.info("FeedBackServiceImpl execute upload success :code:{},msg:{}", ret.getCode(), ret.getMsg());
                threadLocal.set(ret);
            }, e -> log.error("FeedBackServiceImpl execute upload error : ", e));
        } catch (Exception e) {
            log.error("execute upload error:", e);
        }
        return threadLocal.get();
    }

    @Override
    public ResponseDataNewPay<PageOutPutDTO<FindForOidOutputDTO>> findForOid(FindForOidInputDTO inputDTO) {
        try {
            if (Objects.isNull(inputDTO)) {
                return null;
            }
            ThreadLocal<ResponseDataNewPay<PageOutPutDTO<FindForOidOutputDTO>>> threadLocal = new ThreadLocal<>();
            PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
                    .getPlatformNewPayServiceApi(true);
            if (platformNewPayService == null) {
                return null;
            }
            RequestBody requestBody = requestBodyNewPay.findForOidRequestBody(inputDTO);
            Observable<ResponseDataNewPay<PageOutPutDTO<FindForOidOutputDTO>>> res = platformNewPayService.findForOid(requestBody);
            res.subscribe(ret -> {
                log.info("FeedBackServiceImpl execute findForOid success :code:{},msg:{}", ret.getCode(), ret.getMsg());
                threadLocal.set(ret);
            }, e -> log.error("FeedBackServiceImpl execute findForOid error : ", e));
            return threadLocal.get();
        } catch (Exception e) {
            log.error("查询反馈列表失败:", e);
        }
        return null;
    }

    @Override
    public ResponseDataNewPay<PageOutPutDTO<FindContentOutputDTO>> findContent(CommonInputDTO inputDTO) {
        try {
            if (Objects.isNull(inputDTO)) {
                return null;
            }
            ThreadLocal<ResponseDataNewPay<PageOutPutDTO<FindContentOutputDTO>>> threadLocal = new ThreadLocal<>();
            PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
                    .getPlatformNewPayServiceApi(true);
            if (platformNewPayService == null) {
                return null;
            }
            RequestBody requestBody = requestBodyNewPay.findContentRequestBody(inputDTO);
            Observable<ResponseDataNewPay<PageOutPutDTO<FindContentOutputDTO>>> res = platformNewPayService.findContent(requestBody);
            res.subscribe(ret -> {
                log.info("FeedBackServiceImpl execute findContent success :code:{},msg:{}", ret.getCode(), ret.getMsg());
                threadLocal.set(ret);
            }, e -> log.error("FeedBackServiceImpl execute findContent error : ", e));
            return threadLocal.get();
        } catch (Exception e) {
            log.error("业主反馈 查看反馈内容失败:", e);
        }
        return null;
    }

    @Override
    public ResponseDataNewPay<PageOutPutDTO<FindForOfbOutputDTO>> findForOfb(FindForOfbInputDTO inputDTO) {
        try {
            if (Objects.isNull(inputDTO)) {
                return null;
            }
            ThreadLocal<ResponseDataNewPay<PageOutPutDTO<FindForOfbOutputDTO>>> threadLocal = new ThreadLocal<>();
            PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
                    .getPlatformNewPayServiceApi(true);
            if (platformNewPayService == null) {
                return null;
            }
            RequestBody requestBody = requestBodyNewPay.findForOfbRequestBody(inputDTO);
            Observable<ResponseDataNewPay<PageOutPutDTO<FindForOfbOutputDTO>>> res = platformNewPayService.findForOfb(requestBody);
            res.subscribe(ret -> {
                log.info("FeedBackServiceImpl execute findForOfb success :code:{},msg:{}", ret.getCode(), ret.getMsg());
                threadLocal.set(ret);
            }, e -> log.error("FeedBackServiceImpl execute findForOfb error : ", e));
            return threadLocal.get();
        } catch (Exception e) {
            log.error(" 查询回复记录失败:", e);
        }
        return null;
    }

    @Override
    public ResponseDataNewPay<PageOutPutDTO<FindForOfb4DemandOutputDTO>> findForOfb2(FindForOfb4DemandInputDTO inputDTO) {
        try {
            if (Objects.isNull(inputDTO)) {
                return null;
            }
            ThreadLocal<ResponseDataNewPay<PageOutPutDTO<FindForOfb4DemandOutputDTO>>> threadLocal = new ThreadLocal<>();
            PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
                    .getPlatformNewPayServiceApi(true);
            if (platformNewPayService == null) {
                return null;
            }
            RequestBody requestBody = requestBodyNewPay.findForOfb2RequestBody(inputDTO);
            Observable<ResponseDataNewPay<PageOutPutDTO<FindForOfb4DemandOutputDTO>>> res = platformNewPayService.findForOfb2(requestBody);
            res.subscribe(ret -> {
                log.info("FeedBackServiceImpl execute findForOfb2 success :code:{},msg:{}", ret.getCode(), ret.getMsg());
                threadLocal.set(ret);
            }, e -> log.error("FeedBackServiceImpl execute findForOfb2 error : ", e));
            return threadLocal.get();
        } catch (Exception e) {
            log.error(" 查询指定业主反馈的需求进度失败:", e);
        }
        return null;
    }

    @Override
    public ResponseDataNewPay solve(CommonInputDTO inputDTO) {
        try {
            if (Objects.isNull(inputDTO)) {
                return null;
            }
            ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
            PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
                    .getPlatformNewPayServiceApi(true);
            if (platformNewPayService == null) {
                return null;
            }
            RequestBody requestBody = requestBodyNewPay.solveRequestBody(inputDTO);
            Observable<ResponseDataNewPay> res = platformNewPayService.solve(requestBody);
            res.subscribe(ret -> {
                log.info("FeedBackServiceImpl execute solve success :code:{},msg:{}", ret.getCode(), ret.getMsg());
                threadLocal.set(ret);
            }, e -> log.error("FeedBackServiceImpl execute solve error : ", e));
            return threadLocal.get();
        } catch (Exception e) {
            log.error("标记反馈信息已解决失败:", e);
        }
        return null;
    }
}
