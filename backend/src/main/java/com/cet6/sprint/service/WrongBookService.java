package com.cet6.sprint.service;

import com.cet6.sprint.vo.WrongWordVO;

import java.util.List;

/**
 * 错词本服务
 */
public interface WrongBookService {

    /** 错词列表，mastered 传 null 表示全部 */
    List<WrongWordVO> list(Integer mastered);

    /** 标记攻克 / 取消攻克 */
    void markMastered(Long wrongId, boolean mastered);

    /** 移出错词本 */
    void remove(Long wrongId);
}
