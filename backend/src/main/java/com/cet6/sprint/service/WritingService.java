package com.cet6.sprint.service;

import com.cet6.sprint.dto.WritingSubmitDTO;
import com.cet6.sprint.vo.WritingItemVO;
import com.cet6.sprint.vo.WritingResultVO;

import java.util.List;

public interface WritingService {

    /** 随机抽取命题作文（不含参考范文） */
    List<WritingItemVO> random(int count);

    /** 提交作文，返回字数校验、关键词覆盖率自测与参考范文 */
    WritingResultVO submit(WritingSubmitDTO dto);
}
