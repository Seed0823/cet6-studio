package com.cet6.sprint.service;

import com.cet6.sprint.dto.ListeningSubmitDTO;
import com.cet6.sprint.vo.ListeningPaperVO;
import com.cet6.sprint.vo.ListeningResultVO;

import java.util.List;

public interface ListeningService {

    /** 随机抽取听力试卷（不含答案） */
    List<ListeningPaperVO> randomPaper(int count);

    /** 提交答卷并判分 */
    ListeningResultVO submit(ListeningSubmitDTO dto);
}
