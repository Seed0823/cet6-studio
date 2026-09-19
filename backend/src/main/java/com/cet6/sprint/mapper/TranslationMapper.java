package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.Translation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 翻译练习 Mapper
 */
public interface TranslationMapper extends BaseMapper<Translation> {

    @Select("SELECT * FROM t_translation ORDER BY RAND() LIMIT #{limit}")
    List<Translation> random(@Param("limit") int limit);
}
