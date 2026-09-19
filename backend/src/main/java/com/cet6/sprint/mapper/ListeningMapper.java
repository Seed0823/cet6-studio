package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.Listening;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 听力练习 Mapper
 */
public interface ListeningMapper extends BaseMapper<Listening> {

    @Select("SELECT * FROM t_listening ORDER BY RAND() LIMIT #{limit}")
    List<Listening> random(@Param("limit") int limit);
}
