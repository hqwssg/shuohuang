import openpyxl

wb = openpyxl.load_workbook('C:\\myfile\\MyProg\\SOLO\\proj-1\\tpfhs-8\\碳排放因子表-new.xlsx')
ws = wb['热力排放因子库']

data = []
for row in ws.iter_rows(values_only=True):
    data.append(row)

print("表头:", data[0])

sql_lines = []
for row in data[1:]:
    if row[0] is None:
        continue
    name = str(row[0]) if row[0] else ''
    factor = str(row[1]) if row[1] else '0'
    unit = str(row[2]) if row[2] else ''
    source = str(row[3]) if row[3] else ''
    description = str(row[4]) if len(row) > 4 and row[4] else ''
    
    name = name.replace("'", "''")
    unit = unit.replace("'", "''")
    source = source.replace("'", "''")
    description = description.replace("'", "''")
    
    sql = f"('{name}', {factor}, '{unit}', '{source}', '{description}')"
    sql_lines.append(sql)

print(f"Generated {len(sql_lines)} data rows")
with open('thermal_data.sql', 'w', encoding='utf-8') as f:
    f.write("INSERT IGNORE INTO thermal_emission_factor (emission_factor_name, emission_factor, unit, source, description) VALUES \n")
    f.write(",\n".join(sql_lines) + ";")
