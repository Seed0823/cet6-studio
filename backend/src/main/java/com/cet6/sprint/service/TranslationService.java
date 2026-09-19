package com.cet6.sprint.service;

import com.cet6.sprint.dto.TranslationSubmitDTO;
import com.cet6.sprint.vo.TranslationItemVO;
import com.cet6.sprint.vo.TranslationResultVO;

import java.util.List;

public interface TranslationService {

    /** 随机抽取练习题目（不含答案） */
    List<TranslationItemVO> random(int count);

    /** 提交作答，返回参考译文与关键点覆盖率自测 */
    TranslationResultVO submit(TranslationSubmitDTO dto);
}
