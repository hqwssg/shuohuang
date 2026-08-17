package cn.com.v2.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.com.v2.common.domain.AjaxResult;

/**
 * 朔黄铁路碳排放三维曲面数据接口。
 * 与前端 {@code GET /carbon/surface?granularity=month|quarter} 对应。
 */
@RestController
public class CarbonEmissionController {

	private static final int[] YEARS = { 2021, 2022, 2023, 2024 };

	@GetMapping("/carbon/surface")
	public AjaxResult surface(@RequestParam(value = "granularity", defaultValue = "month") String granularity) {
		if (!"month".equals(granularity) && !"quarter".equals(granularity)) {
			return AjaxResult.error(400, "granularity 仅支持 month 或 quarter");
		}

		List<String> periodLabels = buildPeriodLabels(granularity);
		int periodCount = periodLabels.size();
		List<List<Double>> matrix = new ArrayList<>();

		for (int yearIndex = 0; yearIndex < YEARS.length; yearIndex++) {
			int year = YEARS[yearIndex];
			List<Double> row = new ArrayList<>();
			for (int periodIndex = 0; periodIndex < periodCount; periodIndex++) {
				row.add(round2(mockEmission(year, yearIndex, periodIndex, periodCount, granularity)));
			}
			matrix.add(row);
		}

		Map<String, Object> dataset = new LinkedHashMap<>();
		dataset.put("scope", "overall");
		dataset.put("scopeName", "朔黄铁路全路");
		dataset.put("granularity", granularity);
		dataset.put("unit", "tCO2e");
		dataset.put("periodLabels", periodLabels);
		dataset.put("years", Arrays.asList(
				YEARS[0], YEARS[1], YEARS[2], YEARS[3]));
		dataset.put("matrix", matrix);

		return AjaxResult.successData(200, dataset);
	}

	private static List<String> buildPeriodLabels(String granularity) {
		if ("quarter".equals(granularity)) {
			return Arrays.asList("Q1", "Q2", "Q3", "Q4");
		}
		List<String> labels = new ArrayList<>();
		for (int m = 1; m <= 12; m++) {
			labels.add(m + "月");
		}
		return labels;
	}

	/**
	 * 模拟排放：基准约 1200 tCO2e/期，含季节波动与逐年略降。
	 */
	private static double mockEmission(int year, int yearIndex, int periodIndex, int periodCount, String granularity) {
		double base = 1200.0 - yearIndex * 45.0;
		double seasonal;
		if ("quarter".equals(granularity)) {
			// 冬春季略高、夏季略低
			seasonal = new double[] { 80, -40, -60, 20 }[periodIndex];
		} else {
			// 1-12 月正弦季节项
			seasonal = 90 * Math.sin((periodIndex + 1) * Math.PI / 6.0);
		}
		double yearNoise = (year % 7) * 3.5;
		double periodNoise = periodIndex * 2.3;
		return base + seasonal + yearNoise + periodNoise;
	}

	private static double round2(double v) {
		return Math.round(v * 100.0) / 100.0;
	}
}
