package cn.com.v2.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.com.v2.common.domain.AjaxResult;

/**
 * 柱状图动态数据示例：与前端 {@code GET http://127.0.0.1:8083/test} 配置对应，
 * 返回 ECharts dataset 结构（dimensions + source），字段与 BarCommon 默认数据一致。
 */
@RestController
public class ChartDataTestController {

	@GetMapping("/test")
	public AjaxResult testBarDataset() {
		Map<String, Object> dataset = new LinkedHashMap<>();
		dataset.put("dimensions", Arrays.asList("product", "data1", "data2"));

		List<Map<String, Object>> source = new ArrayList<>();
		addRow(source, "Mon", 120, 130);
		addRow(source, "Tue", 200, 130);
		addRow(source, "Wed", 150, 312);
		addRow(source, "Thu", 80, 268);
		addRow(source, "Fri", 70, 155);
		addRow(source, "Sat", 110, 1);
		addRow(source, "Sun", 130, 16);
		dataset.put("source", source);

		return AjaxResult.successData(200, dataset);
	}

	private static void addRow(List<Map<String, Object>> source, String product, int data1, int data2) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("product", product);
		row.put("data1", data1);
		row.put("data2", data2);
		source.add(row);
	}
}
