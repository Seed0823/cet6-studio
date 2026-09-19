package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.Writing;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 写作练习 Mapper
 */
public interface WritingMapper extends BaseMapper<Writing> {

    @Select("SELECT * FROM t_writing ORDER BY RAND() LIMIT #{limit}")
    List<Writing> random(@Param("limit") int limit);
}
