package com.cet6.sprint.service.impl;

import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.dto.ListeningSubmitDTO;
import com.cet6.sprint.entity.Listening;
import com.cet6.sprint.entity.ListeningQuestion;
import com.cet6.sprint.mapper.ListeningMapper;
import com.cet6.sprint.mapper.ListeningQuestionMapper;
import com.cet6.sprint.service.ListeningService;
import com.cet6.sprint.service.StudyService;
import com.cet6.sprint.vo.ListeningPaperVO;
import com.cet6.sprint.vo.ListeningQuestionVO;
import com.cet6.sprint.vo.ListeningResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 听力练习服务实现
 * <p>
 * 选项以 JSON 数组存于 options_json，读取时用 Jackson 解析。答案不下发到前端，
 * 由 submit 在后端判分，避免偷看。
 */
@Service
@RequiredArgsConstructor
public class ListeningServiceImpl implements ListeningService {

    private static final ObjectMapper OM = new ObjectMapper();

    private final ListeningMapper listeningMapper;
    private final ListeningQuestionMapper questionMapper;
    private final StudyService studyService;

    @Override
    public List<ListeningPaperVO> randomPaper(int count) {
        int n = Math.max(1, Math.min(count, 10));
        List<Listening> list = listeningMapper.random(n);
        if (list.isEmpty()) {
            throw new BusinessException("题库暂无听力题目");
        }
        List<ListeningPaperVO> papers = new ArrayList<>();
        for (Listening l : list) {
            ListeningPaperVO paper = new ListeningPaperVO();
            paper.setId(l.getId());
            paper.setTitle(l.getTitle());
            paper.setCategory(l.getCategory());
            paper.setDifficulty(l.getDifficulty());
            paper.setScript(l.getScript());
            List<ListeningQuestion> qs = questionMapper.selectByListeningId(l.getId());
            List<ListeningQuestionVO> qv = new ArrayList<>();
            int idx = 1;
            for (ListeningQuestion q : qs) {
                ListeningQuestionVO vo = new ListeningQuestionVO();
                vo.setIndex(idx++);
                vo.setQuestionId(q.getId());
                vo.setQuestion(q.getQuestion());
                vo.setOptions(parseOptions(q.getOptionsJson()));
                qv.add(vo);
            }
            paper.setQuestions(qv);
            papers.add(paper);
        }
        return papers;
    }

    @Override
    public ListeningResultVO submit(ListeningSubmitDTO dto) {
        Long userId = UserContext.getUserId();
        Listening l = listeningMapper.selectById(dto.getId());
        if (l == null) {
            throw new BusinessException("听力题目不存在");
        }
        List<ListeningQuestion> qs = questionMapper.selectByListeningId(l.getId());
        Map<Long, ListeningQuestion> map = qs.stream()
                .collect(Collectors.toMap(ListeningQuestion::getId, q -> q, (a, b) -> a, LinkedHashMap::new));

        int correct = 0;
        List<ListeningResultVO.Item> details = new ArrayList<>();
        List<ListeningSubmitDTO.Item> answers = dto.getAnswers() == null ? new ArrayList<>() : dto.getAnswers();
        for (ListeningSubmitDTO.Item a : answers) {
            ListeningQuestion q = a == null ? null : map.get(a.getQuestionId());
            if (q == null) {
                continue;
            }
            int correctIndex = q.getAnswerIndex() == null ? -1 : q.getAnswerIndex();
            int chosen = a.getChosenIndex() == null ? -1 : a.getChosenIndex();
            boolean right = chosen >= 0 && chosen == correctIndex;
            if (right) {
                correct++;
            }
            ListeningResultVO.Item item = new ListeningResultVO.Item();
            item.setQuestionId(q.getId());
            item.setQuestion(q.getQuestion());
            item.setOptions(parseOptions(q.getOptionsJson()));
            item.setChosenIndex(chosen);
            item.setCorrectIndex(correctIndex);
            item.setExplain(q.getExplain());
            item.setRight(right);
            details.add(item);
        }

        ListeningResultVO vo = new ListeningResultVO();
        vo.setId(l.getId());
        vo.setTotal(details.size());
        vo.setCorrect(correct);
        vo.setDetails(details);

        studyService.record(userId, "listening", 0, details.size());
        studyService.checkinToday(userId);
        return vo;
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            String[] arr = OM.readValue(json, String[].class);
            return new ArrayList<>(Arrays.asList(arr));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
