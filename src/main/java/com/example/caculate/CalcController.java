package com.example.calculate;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CalcController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 表达式计算接口
     * 支持：+ - * / % ^ sin cos tan sqrt 括号 小数 负数
     * 计算成功才写入数据库，失败不存库
     */
    @PostMapping("/calculate")
    public Map<String, Object> calculate(@RequestBody Map<String, String> body) {
        String expression = body.get("expression");

        if (expression == null || expression.trim().isEmpty()) {
            return Map.of("success", false, "message", "Expression cannot be empty");
        }
        if (expression.length() > 255) {
            return Map.of("success", false, "message", "Expression length cannot exceed 255 characters");
        }

        try {
            // 界面符号转标准运算符
            String normalized = expression
                    .replace("×", "*")
                    .replace("÷", "/")
                    .replace("％", "%");

            Expression exp = new ExpressionBuilder(normalized).build();
            double result = exp.evaluate();

            // 计算成功存入数据库
            String sql = "INSERT INTO calculation_history (expression, result, created_at, is_favorite) VALUES (?, ?, ?, 0)";
            jdbcTemplate.update(sql, expression, result, LocalDateTime.now());

            return Map.of(
                    "success", true,
                    "expression", expression,
                    "result", result
            );

        } catch (ArithmeticException e) {
            return Map.of("success", false, "message", "Calculation error: division by zero");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Invalid expression. Please check operators and parentheses.");
        }
    }

    /**
     * 查询全部历史记录
     * 按时间倒序，返回收藏状态
     */
    @GetMapping("/history")
    public Map<String, Object> getHistory() {
        try {
            String sql = "SELECT id, expression, result, created_at AS createTime, is_favorite FROM calculation_history ORDER BY created_at DESC";
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            return Map.of("success", true, "data", list);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to load history");
        }
    }

    /**
     * 单条删除历史记录
     */
    @DeleteMapping("/history/{id}")
    public Map<String, Object> deleteHistory(@PathVariable Long id) {
        try {
            String sql = "DELETE FROM calculation_history WHERE id = ?";
            int rows = jdbcTemplate.update(sql, id);
            if (rows == 0) {
                return Map.of("success", false, "message", "Record not found");
            }
            return Map.of("success", true);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Delete failed");
        }
    }

    /**
     * 一键清空所有历史记录
     */
    @DeleteMapping("/history/all")
    public Map<String, Object> deleteAllHistory() {
        try {
            String sql = "DELETE FROM calculation_history";
            jdbcTemplate.update(sql);
            return Map.of("success", true);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Failed to clear history");
        }
    }

    /**
     * 切换历史记录收藏状态
     */
    @PostMapping("/history/{id}/favorite")
    public Map<String, Object> toggleFavorite(@PathVariable Long id) {
        try {
            String sql = "UPDATE calculation_history SET is_favorite = NOT is_favorite WHERE id = ?";
            jdbcTemplate.update(sql, id);
            return Map.of("success", true);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Toggle favorite failed");
        }
    }

    /**
     * 进制转换接口
     * 支持2-36进制互转
     */
    @PostMapping("/convert/base")
    public Map<String, Object> convertBase(@RequestBody Map<String, Object> body) {
        try {
            String value = body.get("value").toString();
            int fromBase = Integer.parseInt(body.get("fromBase").toString());
            int toBase = Integer.parseInt(body.get("toBase").toString());

            if (fromBase < 2 || fromBase > 36 || toBase < 2 || toBase > 36) {
                return Map.of("success", false, "message", "Base must be between 2 and 36");
            }

            BigInteger num = new BigInteger(value, fromBase);
            String result = num.toString(toBase).toUpperCase();

            return Map.of("success", true, "result", result);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Invalid value for given base");
        }
    }
}

