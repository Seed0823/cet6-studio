package com.cet6.sprint.service;

import com.cet6.sprint.vo.DashboardVO;

import java.util.Map;

/**
 * 数据统计服务
 */
public interface StatsService {

    /** 今日学习页所需的全部数据 */
    DashboardVO dashboard();

    /** 学习统计页数据（趋势 / 累计） */
    Map<String, Object> overview(int days);
}
