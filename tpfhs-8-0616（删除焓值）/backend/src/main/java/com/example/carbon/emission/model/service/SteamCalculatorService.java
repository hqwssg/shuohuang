package com.example.carbon.emission.model.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 蒸汽热力学计算服务
 * 调用Python脚本使用IAPWS热力学公式计算饱和蒸汽和过热蒸汽的物理参数
 *
 * @author System
 * @version 1.0
 */
@Service
public class SteamCalculatorService {

    private static final Logger logger = LoggerFactory.getLogger(SteamCalculatorService.class);

    /**
     * Python解释器路径，根据实际情况配置
     * Windows环境下可能需要完整路径如 "python" 或 "py"
     */
    private static final String PYTHON_PATH =
            System.getenv().getOrDefault("PYTHON_PATH", "python3");

    /**
     * Python脚本路径
     */
    private static final String SCRIPT_PATH = System.getenv().getOrDefault(
            "STEAM_CALCULATOR_SCRIPT",
            System.getProperty("user.dir") + "/steam_calculator.py");

    /**
     * 计算给定温度下的饱和蒸汽性质（压力和焓值）
     *
     * @param temperature 温度，单位摄氏度（°C）
     * @return 包含压力的饱和蒸汽性质，格式：{success: true/false, pressure: MPa, enthalpy: kJ/kg}
     */
    public Map<String, Object> calculateSaturationProperties(double temperature) {
        Map<String, Object> result = new HashMap<>();

        try {
            String command = String.format("%s \"%s\" saturation %.6f", PYTHON_PATH, SCRIPT_PATH, temperature);
            logger.info("执行命令: {}", command);

            Process process = Runtime.getRuntime().exec(command);
            String output = readProcessOutput(process);
            String errorOutput = readProcessError(process);
            int exitCode = process.waitFor();

            logger.info("Python脚本退出码: {}, 输出: {}, 错误输出: {}", exitCode, output, errorOutput);

            if (exitCode == 0) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> pythonResult = objectMapper.readValue(output, Map.class);

                    if (Boolean.TRUE.equals(pythonResult.get("success"))) {
                        result.put("success", true);
                        result.put("pressure", pythonResult.get("pressure"));
                        result.put("enthalpy", pythonResult.get("enthalpy"));
                        result.put("unit", pythonResult.get("unit"));
                    } else {
                        result.put("success", false);
                        result.put("error", pythonResult.get("error"));
                    }
                } catch (Exception e) {
                    logger.error("解析Python脚本输出失败", e);
                    result.put("success", false);
                    result.put("error", "解析输出失败: " + e.getMessage() + ", 原始输出: " + output);
                }
            } else {
                result.put("success", false);
                result.put("error", "Python脚本执行失败，退出码: " + exitCode + ", 错误信息: " + errorOutput);
            }
        } catch (Exception e) {
            logger.error("计算饱和蒸汽性质失败", e);
            result.put("success", false);
            result.put("error", "计算失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 计算过热蒸汽性质（焓值）
     * 在计算前会验证输入的压力和温度是否满足过热水蒸汽的形成条件
     *
     * @param temperature 温度，单位摄氏度（°C）
     * @param pressure 压力，单位MPa
     * @return 包含焓值的过热蒸汽性质，格式：{success: true/false, enthalpy: kJ/kg, error: "错误信息"}
     */
    public Map<String, Object> calculateSuperheatedProperties(double temperature, double pressure) {
        Map<String, Object> result = new HashMap<>();

        try {
            String command = String.format("%s \"%s\" superheated %.6f %.6f",
                    PYTHON_PATH, SCRIPT_PATH, temperature, pressure);
            logger.info("执行命令: {}", command);

            Process process = Runtime.getRuntime().exec(command);
            String output = readProcessOutput(process);
            String errorOutput = readProcessError(process);
            int exitCode = process.waitFor();

            logger.info("Python脚本退出码: {}, 输出: {}, 错误输出: {}", exitCode, output, errorOutput);

            if (exitCode == 0) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> pythonResult = objectMapper.readValue(output, Map.class);

                    if (Boolean.TRUE.equals(pythonResult.get("success"))) {
                        result.put("success", true);
                        result.put("enthalpy", pythonResult.get("enthalpy"));
                        if (pythonResult.containsKey("saturation_pressure")) {
                            result.put("saturation_pressure", pythonResult.get("saturation_pressure"));
                        }
                    } else {
                        result.put("success", false);
                        result.put("error", pythonResult.get("error"));
                        if (pythonResult.containsKey("saturation_pressure")) {
                            result.put("saturation_pressure", pythonResult.get("saturation_pressure"));
                        }
                    }
                } catch (Exception e) {
                    logger.error("解析Python脚本输出失败", e);
                    result.put("success", false);
                    result.put("error", "解析输出失败: " + e.getMessage() + ", 原始输出: " + output);
                }
            } else {
                result.put("success", false);
                result.put("error", "Python脚本执行失败，退出码: " + exitCode + ", 错误信息: " + errorOutput);
            }
        } catch (Exception e) {
            logger.error("计算过热蒸汽性质失败", e);
            result.put("success", false);
            result.put("error", "计算失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 读取进程输出
     *
     * @param process 进程对象
     * @return 进程的标准输出内容
     * @throws Exception 读取过程中的异常
     */
    private String readProcessOutput(Process process) throws Exception {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        return output.toString();
    }
    
    private String readProcessError(Process process) throws Exception {
        StringBuilder error = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }

        return error.toString();
    }
}
