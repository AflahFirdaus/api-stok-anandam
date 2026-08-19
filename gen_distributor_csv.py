import openpyxl, csv

wb = openpyxl.load_workbook("data_ppn_dan_non_ppn.xlsx", data_only=True)
ws = wb["Sheet1"]

def normalize(t):
    if t is None:
        return "NON PPN"
    v = str(t).strip().upper().replace("\u00a0", " ")
    if v.startswith("PPN"):
        return "PPN"
    return "NON PPN"

rows = []
for row in ws.iter_rows(min_row=2, values_only=True):
    if row is None or len(row) < 2:
        continue
    nama = row[0]
    tipe = row[1]
    if nama is None or str(nama).strip() == "":
        continue
    rows.append((str(nama).strip(), normalize(tipe)))

rows = sorted(set(rows), key=lambda x: x[0])
out = "src/main/resources/distributor.csv"
with open(out, "w", newline="", encoding="utf-8") as f:
    writer = csv.writer(f, lineterminator="\n")
    writer.writerow(["nama_distributor", "tipe_pajak"])
    for r in rows:
        writer.writerow(r)

from collections import Counter
print("TOTAL", len(rows))
print(Counter(r[1] for r in rows))
print("writing to", out)