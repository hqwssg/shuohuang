INSERT IGNORE INTO waste_incineration_factor (emission_factor_name, waste_type, ccw, fcf, ce, emission_factor, unit, source, description) VALUES 
('生活垃圾焚烧', '生活垃圾', 0.4, 0.4, 0.95, 0.30400000000000005, 'tCO2/t固体废物', '缺省值', '数据来源《省级温室气体清单编制指南（2025年版）》'),
('危险废物焚烧', '危险废物', 0.5, 0.9, 0.995, 0.8955, 'tCO2/t固体废物', '缺省值', '数据来源《省级温室气体清单编制指南（2025年版）》');