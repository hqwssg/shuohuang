package com.ruoyi.carbon.report.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReportUnitConverterTest
{
    private final ReportUnitConverter converter = new ReportUnitConverter();

    @Test
    void naturalGasStaysReportUnit()
    {
        ReportUnitConverter.Conversion conversion = converter.convert("FF_NG", new BigDecimal("12.5"), "10k_m3");
        assertTrue(conversion.ok);
        assertEquals(0, new BigDecimal("12.5").compareTo(conversion.normalizedValue));
        assertEquals("10k_m3", conversion.normalizedUnit);
    }

    @Test
    void dieselLitersConvertToTonnes()
    {
        ReportUnitConverter.Conversion conversion = converter.convert("FF_D", new BigDecimal("1000"), "L");
        assertTrue(conversion.ok);
        assertEquals(0, new BigDecimal("0.850000").compareTo(conversion.normalizedValue));
        assertEquals("t", conversion.normalizedUnit);
    }

    @Test
    void missingFactorIsGap()
    {
        ReportUnitConverter.Conversion conversion = converter.convert("FF_NG", new BigDecimal("1"), "kg");
        assertFalse(conversion.ok);
    }
}
