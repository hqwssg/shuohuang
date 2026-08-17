import openpyxl

wb = openpyxl.load_workbook('碳排放因子表.xlsx')
ws = wb['化石燃料排放因子库']

data = []
for row in ws.iter_rows(values_only=True):
    data.append(row)

print("表头:", data[0])

sql_lines = []
for row in data[1:]:
    if row[0] is None:
        continue
    fuel_type = row[0]
    source = row[1] if row[1] else ''
    unit = row[2]
    lower_heating_value = row[3] if row[3] else 'NULL'
    carbon_content = row[4] if row[4] else 'NULL'
    oxidation_rate = row[5] if row[5] else 'NULL'
    emission_factor = row[6] if row[6] else 'NULL'
    description = row[7] if row[7] else ''
    
    fuel_type_escaped = str(fuel_type).replace("'", "''")
    source_escaped = str(source).replace("'", "''")
    description_escaped = str(description).replace("'", "''")
    
    sql = f"INSERT INTO fossil_fuel_emission_factor (fuel_type, source, unit, lower_heating_value, carbon_content_per_unit_heat, fuel_oxidation_rate, emission_factor, description) VALUES ('{fuel_type_escaped}', '{source_escaped}', '{unit}', {lower_heating_value}, {carbon_content}, {oxidation_rate}, {emission_factor}, '{description_escaped}');"
    sql_lines.append(sql)

with open('fossil_fuel_data.sql', 'w', encoding='utf-8') as f:
    f.write('\n'.join(sql_lines))

print(f"Generated {len(sql_lines)} INSERT statements")