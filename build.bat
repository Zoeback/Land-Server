@echo off
echo LandOtherPlugin 编译脚本
echo ========================

REM 检查是否安装了Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未安装Java或Java未添加到系统PATH
    echo 请先安装Java 17或更高版本
    pause
    exit /b 1
)

REM 检查是否安装了Maven
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未安装Maven或Maven未添加到系统PATH
    echo 请先安装Maven
    pause
    exit /b 1
)

echo Java和Maven检测通过，开始编译...

REM 清理并编译项目
echo 正在清理项目...
mvn clean

if %errorlevel% neq 0 (
    echo 清理失败!
    pause
    exit /b 1
)

echo 正在编译项目...
mvn package

if %errorlevel% neq 0 (
    echo 编译失败!
    pause
    exit /b 1
)

echo.
echo 编译成功!
echo.
echo 插件JAR文件位置: target\LandOtherPlugin-1.0.0.jar
echo.
echo 使用方法:
echo 1. 将 target\LandOtherPlugin-1.0.0.jar 复制到你的服务器的 plugins 文件夹
echo 2. 重启或启动服务器
echo 3. 使用命令 /publicitems 或 /pi 打开公用物品领取界面
echo.
pause