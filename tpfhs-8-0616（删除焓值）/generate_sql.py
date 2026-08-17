import openpyxl

wb = openpyxl.load_workbook('碳排放因子表.xlsx')
ws = wb['电力碳排放因子库']

data = []
for row in ws.iter_rows(values_only=True):
    data.append(row)

header = data[2]
print("表头:", header)

sql_lines = []
for row in data[3:]:
    if row[0] is None:
        continue
    factor_name = row[0]
    factor_value = row[1]
    unit = row[2]
    description = row[3] if row[3] else ''
    
    factor_name_escaped = str(factor_name).replace("'", "''")
    description_escaped = str(description).replace("'", "''")
    
    sql = f"INSERT INTO electricity_carbon_emission_factor (factor_name, factor_value, unit, description) VALUES ('{factor_name_escaped}', {factor_value}, '{unit}', '{description_escaped}');"
    sql_lines.append(sql)

with open('electricity_factor_data.sql', 'w', encoding='utf-8') as f:
    f.write('\n'.join(sql_lines))

print(f"Generated {len(sql_lines)} INSERT statements")