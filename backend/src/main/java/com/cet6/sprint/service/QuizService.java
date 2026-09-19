package com.cet6.sprint.service;

import com.cet6.sprint.dto.QuizSubmitDTO;
import com.cet6.sprint.entity.QuizRecord;
import com.cet6.sprint.vo.QuizQuestionVO;
import com.cet6.sprint.vo.QuizResultVO;

import java.util.List;

/**
 * 单词自测服务
 */
public interface QuizService {

    /** 生成一套题（不含正确答案） */
    List<QuizQuestionVO> generate(int count);

    /** 提交答卷并判分 */
    QuizResultVO submit(QuizSubmitDTO dto);

    /** 历史测验记录 */
    List<QuizRecord> history(int limit);
}
