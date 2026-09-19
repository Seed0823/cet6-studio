package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.ListeningQuestion;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 听力选择题 Mapper
 */
public interface ListeningQuestionMapper extends BaseMapper<ListeningQuestion> {

    @Select("SELECT * FROM t_listening_question WHERE listening_id = #{listeningId} ORDER BY id ASC")
    List<ListeningQuestion> selectByListeningId(@Param("listeningId") Long listeningId);
}
