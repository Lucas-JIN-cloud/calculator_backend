package com.example.caculate;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Date;
import java.util.List;

@Service
public class CalculationService {

    @Autowired
    private CalculationHistoryMapper historyMapper;

    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    public Double calculate(String expression) {
        try {
            Object evalResult = engine.eval(expression);
            double result = Double.parseDouble(evalResult.toString());
            CalculationHistory history = new CalculationHistory();
            history.setExpression(expression);
            history.setResult(result);
            history.setCreateTime(new Date());
            historyMapper.insert(history);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("The expression is illegal and the calculation failed.");
        }
    }

    public List<CalculationHistory> getAllHistory() {
        return historyMapper.selectAll();
    }

    public void deleteHistory(Long id) {
        int rows = historyMapper.deleteById(id);
        if (rows == 0) {
            throw new RuntimeException("Record does not exist. Deletion failed.");
        }
    }
}
