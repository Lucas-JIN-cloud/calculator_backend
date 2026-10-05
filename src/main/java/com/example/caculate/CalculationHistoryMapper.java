package com.example.caculate;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface CalculationHistoryMapper {
    List<CalculationHistory> selectAll();

    int insert(CalculationHistory history);

    int deleteById(Long id);
}