package com.blzcat.adapter.common;

import org.apache.calcite.linq4j.Enumerator;

public interface StoreEngine<T> {
    /**
     * 根据SQL查询获取结果
     *
     * @param sql sql
     * @return 结果
     */
    Enumerator<T> selectBySQL(String sql);
}
