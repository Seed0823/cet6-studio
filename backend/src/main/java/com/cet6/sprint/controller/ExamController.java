package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.dto.ExamFinishDTO;
import com.cet6.sprint.service.ListeningService;
import com.cet6.sprint.service.StudyService;
import com.cet6.sprint.service.TranslationService;
import com.cet6.sprint.service.WritingService;
import com.cet6.sprint.vo.ExamPrepareVO;
import com.cet6.sprint.vo.ListeningPaperVO;
import com.cet6.sprint.vo.TranslationItemVO;
import com.cet6.sprint.vo.WritingItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 真题模考接口
 * <p>
 * 组合「听力 + 翻译 + 写作」三节，前端分节计时、逐节调用各模块 submit 判分，
 * 本控制器只负责组卷与结束时的学习时长记录，不在后端重复实现评分逻辑。
 */
@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ListeningService listeningService;
    private final TranslationService translationService;
    private final WritingService writingService;
    private final StudyService studyService;

    /** 组卷：各模块随机抽一份 */
    @GetMapping("/prepare")
    public Result<ExamPrepareVO> prepare() {
        ExamPrepareVO vo = new ExamPrepareVO();
        List<ListeningPaperVO> lp = listeningService.randomPaper(1);
        vo.setListening(lp.isEmpty() ? null : lp.get(0));
        List<TranslationItemVO> tr = translationService.random(1);
        vo.setTranslation(tr.isEmpty() ? null : tr.get(0));
        List<WritingItemVO> wr = writingService.random(1);
        vo.setWriting(wr.isEmpty() ? null : wr.get(0));
        return Result.ok(vo);
    }

    /** 完成模考，记录学习时长并打卡 */
    @PostMapping("/finish")
    public Result<Void> finish(@RequestBody ExamFinishDTO dto) {
        Long userId = UserContext.getUserId();
        int seconds = dto.getDurationSecond() == null ? 0 : dto.getDurationSecond();
        int minutes = seconds <= 0 ? 1 : Math.max(1, seconds / 60);
        studyService.record(userId, "exam", minutes, 1);
        studyService.checkinToday(userId);
        return Result.ok();
    }
}
