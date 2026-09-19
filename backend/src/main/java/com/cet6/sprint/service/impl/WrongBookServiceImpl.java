package com.cet6.sprint.service.impl;

import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.entity.WrongWord;
import com.cet6.sprint.mapper.WrongWordMapper;
import com.cet6.sprint.service.WrongBookService;
import com.cet6.sprint.vo.WrongWordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 错词本服务实现
 */
@Service
@RequiredArgsConstructor
public class WrongBookServiceImpl implements WrongBookService {

    private final WrongWordMapper wrongWordMapper;

    @Override
    public List<WrongWordVO> list(Integer mastered) {
        return wrongWordMapper.listWithWord(UserContext.getUserId(), mastered);
    }

    @Override
    public void markMastered(Long wrongId, boolean mastered) {
        WrongWord db = getOwned(wrongId);
        db.setMastered(mastered ? 1 : 0);
        wrongWordMapper.updateById(db);
    }

    @Override
    public void remove(Long wrongId) {
        WrongWord db = getOwned(wrongId);
        wrongWordMapper.deleteById(db.getId());
    }

    /** 越权校验：只能操作自己的数据 */
    private WrongWord getOwned(Long wrongId) {
        WrongWord db = wrongWordMapper.selectById(wrongId);
        if (db == null) {
            throw new BusinessException("记录不存在");
        }
        if (!db.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "无权操作该记录");
        }
        return db;
    }
}
