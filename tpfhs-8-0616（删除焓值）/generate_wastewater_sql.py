import openpyxl

wb = openpyxl.load_workbook('C:\\myfile\\MyProg\\SOLO\\proj-1\\tpfhs-8\\碳排放因子表-new.xlsx')
ws = wb['废水处理排放因子']

data = []
for row in ws.iter_rows(values_only=True):
    data.append(row)

print("表头:", data[0])

sql_lines = []
for row in data[1:]:
    if row[0] is None:
        continue
    name = str(row[0]) if row[0] else ''
    wastewater_type = str(row[1]) if row[1] else ''
    od = str(row[3]) if row[3] is not None else '0'
    bo = str(row[4]) if row[4] is not None else '0'
    mcf = str(row[5]) if row[5] is not None else '0'
    gwp = str(row[6]) if row[6] is not None else '28'
    # 计算碳排放因子：OD * Bo * MCF * GWP / 1000000
    try:
        od_val = float(od) if od else 0
        bo_val = float(bo) if bo else 0
        mcf_val = float(mcf) if mcf else 0
        gwp_val = float(gwp) if gwp else 28
        emission_factor = str(od_val * bo_val * mcf_val * gwp_val / 1000000)
    except:
        emission_factor = '0'
    unit = str(row[8]) if len(row) > 8 and row[8] else ''
    source = str(row[9]) if len(row) > 9 and row[9] else ''
    description = str(row[10]) if len(row) > 10 and row[10] else ''
    
    name = name.replace("'", "''")
    wastewater_type = wastewater_type.replace("'", "''")
    unit = unit.replace("'", "''")
    source = source.replace("'", "''")
    description = description.replace("'", "''")
    
    sql = f"('{name}', '{wastewater_type}', {od}, {bo}, {mcf}, {gwp}, {emission_factor}, '{unit}', '{source}', '{description}')"
    sql_lines.append(sql)

print(f"Generated {len(sql_lines)} data rows")
with open('wastewater_data.sql', 'w', encoding='utf-8') as f:
    f.write("INSERT IGNORE INTO wastewater_treatment_factor (emission_factor_name, wastewater_type, od, bo, mcf, gwp, emission_factor, unit, source, description) VALUES \n")
    f.write(",\n".join(sql_lines) + ";")
