import openpyxl

wb = openpyxl.load_workbook('C:\\myfile\\MyProg\\SOLO\\proj-1\\tpfhs-8\\碳排放因子表-new.xlsx')
ws = wb['固体废弃物焚烧排放因子']

data = []
for row in ws.iter_rows(values_only=True):
    data.append(row)

print("表头:", data[0])

sql_lines = []
for row in data[1:]:
    if row[0] is None:
        continue
    name = str(row[0]) if row[0] else ''
    waste_type = str(row[1]) if row[1] else ''
    ccw = str(row[2]) if row[2] is not None else '0'
    fcf = str(row[3]) if row[3] is not None else '0'
    ce = str(row[4]) if row[4] is not None else '0'
    # 计算碳排放因子：CCW * FCF * CE * 44/22 = CCW * FCF * CE * 2
    try:
        ccw_val = float(ccw) if ccw else 0
        fcf_val = float(fcf) if fcf else 0
        ce_val = float(ce) if ce else 0
        emission_factor = str(ccw_val * fcf_val * ce_val * 2)
    except:
        emission_factor = '0'
    unit = str(row[6]) if len(row) > 6 and row[6] else ''
    source = str(row[7]) if len(row) > 7 and row[7] else ''
    description = str(row[8]) if len(row) > 8 and row[8] else ''
    
    name = name.replace("'", "''")
    waste_type = waste_type.replace("'", "''")
    unit = unit.replace("'", "''")
    source = source.replace("'", "''")
    description = description.replace("'", "''")
    
    sql = f"('{name}', '{waste_type}', {ccw}, {fcf}, {ce}, {emission_factor}, '{unit}', '{source}', '{description}')"
    sql_lines.append(sql)

print(f"Generated {len(sql_lines)} data rows")
with open('waste_data.sql', 'w', encoding='utf-8') as f:
    f.write("INSERT IGNORE INTO waste_incineration_factor (emission_factor_name, waste_type, ccw, fcf, ce, emission_factor, unit, source, description) VALUES \n")
    f.write(",\n".join(sql_lines) + ";")
