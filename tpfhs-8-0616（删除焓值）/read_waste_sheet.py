import openpyxl

wb = openpyxl.load_workbook('C:\\myfile\\MyProg\\SOLO\\proj-1\\tpfhs-8\\碳排放因子表-new.xlsx')
ws = wb['固体废弃物焚烧排放因子']

print("固体废弃物焚烧排放因子数据:")
for i, row in enumerate(ws.iter_rows(values_only=True)):
    if i < 20:
        print(f"第{i}行: {row}")
