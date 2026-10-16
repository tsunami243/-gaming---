#!/usr/bin/env bash
# ============================================
# GameBoi 项目初始化 & 验证入口
# 用法: ./init.sh [--skip-tests] [--check-only]
#   --check-only  仅检查环境，宽松模式（warning 不终止）
#   --skip-tests  跳过单元测试
# ============================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SKIP_TESTS=false
CHECK_ONLY=false

for arg in "$@"; do
    case "$arg" in
        --skip-tests) SKIP_TESTS=true ;;
        --check-only) CHECK_ONLY=true ;;
        *) echo "未知参数: $arg"; exit 1 ;;
    esac
done

pass()  { echo -e "  ${GREEN}✓${NC} $1"; }
fail()  { echo -e "  ${RED}✗${NC} $1"; }
warn()  { echo -e "  ${YELLOW}⚠${NC} $1"; }

echo "============================================"
echo " GameBoi 环境检查"
echo "============================================"

# ---- 基础工具检查 ----
echo ""
echo "[1/5] 基础工具"

if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -1)
    pass "Java: $JAVA_VER"
else
    fail "Java 未安装 (需要 JDK 17+)"
fi

if command -v node &>/dev/null; then
    NODE_VER=$(node -v)
    pass "Node.js: $NODE_VER"
else
    fail "Node.js 未安装 (需要 18+)"
fi

# Python: 优先 python3，失败再试 python（兼容 conda / Windows 残留 PATH）
PY_VER=""
if command -v python3 &>/dev/null; then
    PY_VER=$(python3 --version 2>/dev/null) || true
fi
if [ -z "$PY_VER" ] && command -v python &>/dev/null; then
    PY_VER=$(python --version 2>/dev/null) || true
fi
if [ -n "$PY_VER" ]; then
    pass "Python: $PY_VER"
else
    warn "Python 未安装 (爬虫模块需要 3.11+)"
fi

# ---- 网络服务检查 ----
echo ""
echo "[2/5] 网络服务"

if command -v mysql &>/dev/null; then
    if mysql -u "${DB_USERNAME:?DB_USERNAME is required}" -p"${DB_PASSWORD:?DB_PASSWORD is required}" -e "SELECT 1" big_event &>/dev/null 2>&1; then
        pass "MySQL: 可连接, 数据库 big_event 存在"
    else
        warn "MySQL 可连接但数据库 big_event 可能不存在，请先执行:"
        warn "  CREATE DATABASE IF NOT EXISTS big_event DEFAULT CHARSET utf8mb4;"
        warn "  mysql -u root -p big_event < sql/schema.sql"
    fi
else
    warn "mysql CLI 未找到，无法自动检测数据库状态"
fi

if command -v redis-cli &>/dev/null; then
    if redis-cli ping &>/dev/null 2>&1; then
        pass "Redis: PONG"
    else
        fail "Redis 未启动 (端口 6379)"
    fi
else
    warn "redis-cli 未找到，无法自动检测 Redis 状态"
fi

# ---- Maven 依赖 ----
echo ""
echo "[3/5] 后端依赖"
if [ -f "pom.xml" ]; then
    pass "pom.xml 存在"
else
    fail "pom.xml 缺失"
fi

# ---- 前端依赖 ----
echo ""
echo "[4/5] 前端依赖"
if [ -f "gaming-news/package.json" ]; then
    if [ -d "gaming-news/node_modules" ]; then
        pass "node_modules 已安装"
    else
        warn "node_modules 不存在，运行: cd gaming-news && npm install"
    fi
else
    fail "gaming-news/package.json 缺失"
fi

# ---- 爬虫依赖 ----
if [ -f "ingest/requirements.txt" ]; then
    warn "爬虫依赖未检查，按需运行: cd ingest && pip install -r requirements.txt"
fi

# ---- 编译 & 测试 ----
echo ""
echo "[5/5] 后端编译与测试"

if [ "$CHECK_ONLY" = true ]; then
    echo "  (check-only 模式，跳过编译与测试)"
    echo ""
    echo "============================================"
    echo " 环境检查完成"
    echo "============================================"
    exit 0
fi

# ============================================================
# 严格模式 — 仅在编译 & 测试阶段启用
# ============================================================
set -euo pipefail

echo "  正在编译..."
if ./mvnw compile -q 2>&1 | tail -5; then
    pass "编译通过"
else
    fail "编译失败，请检查错误信息"
    exit 1
fi

if [ "$SKIP_TESTS" = true ]; then
    warn "跳过测试 (--skip-tests)"
else
    echo "  正在运行测试..."
    if ./mvnw test -q 2>&1 | tail -5; then
        pass "测试全部通过"
    else
        fail "测试失败，请在继续前修复"
        exit 1
    fi
fi

echo ""
echo "============================================"
echo " 初始化完成 — 可以开始开发"
echo "============================================"
