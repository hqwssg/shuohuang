import openpyxl

wb = openpyxl.load_workbook('C:\\myfile\\MyProg\\SOLO\\proj-1\\tpfhs-8\\碳排放因子表-new.xlsx')
print("Worksheets in workbook:")
for sheet_name in wb.sheetnames:
    print(f"- {sheet_name}")
