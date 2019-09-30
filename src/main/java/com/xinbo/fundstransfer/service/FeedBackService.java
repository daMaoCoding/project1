package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.ali4enterprise.inputdto.CommonInputDTO;
import com.xinbo.fundstransfer.ali4enterprise.outputdto.PageOutPutDTO;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.pojo.*;
import org.springframework.data.domain.PageRequest;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface FeedBackService {
    Map<String, Object> findFeedBack(String untreatedFind, String fristTime, String lastTime, String type, String level,
                                     PageRequest pageRequest) throws Exception;

    void saveFeedBack(String level, String describe, String userId, String imgs) throws Exception;

    String findOldRemark(String id) throws Exception;

    void saveRemark(String id, String remark) throws Exception;

    void dealWith(String id, String uid, String remark) throws Exception;

    void finish(String id) throws Exception;

    void deleteFeedBack(String id) throws Exception;

    List<Object> showFeedBackDetails(String id) throws Exception;

    void deleteFeedBackImgs(String imgs) throws Exception;

    ResponseDataNewPay addForCrk(AddForCrkInputDTO inputDTO);

    ResponseDataNewPay upload(List<File> files);

    ResponseDataNewPay upload2(List<File> files);

    ResponseDataNewPay<PageOutPutDTO<FindForOidOutputDTO>> findForOid(FindForOidInputDTO inputDTO);

    ResponseDataNewPay<PageOutPutDTO<FindContentOutputDTO>> findContent(CommonInputDTO inputDTO);

    ResponseDataNewPay<PageOutPutDTO<FindForOfbOutputDTO>> findForOfb(FindForOfbInputDTO inputDTO);

    ResponseDataNewPay<PageOutPutDTO<FindForOfb4DemandOutputDTO>> findForOfb2(FindForOfb4DemandInputDTO inputDTO);

    ResponseDataNewPay solve(CommonInputDTO inputDTO);

}
