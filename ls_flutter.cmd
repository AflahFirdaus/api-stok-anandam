@echo off
echo ===STOCK_DIR===
dir /b /s "D:\Idos\Coding\Flutter\stok_anandam\lib\features\stock" 2>&1
echo ===MODELS_DIR===
dir /b /s "D:\Idos\Coding\Flutter\stok_anandam\lib\data\models" 2>&1
echo ===GREP_ISPPN===
findstr /s /i /n /c:"isPpn" /c:"is_ppn" /c:"parName" "D:\Idos\Coding\Flutter\stok_anandam\lib\*.dart" 2>&1
echo ===DECK===
dir /b /s "D:\Idos\Coding\Flutter\stok_anandam\lib\features\shared\widgets" 2>&1