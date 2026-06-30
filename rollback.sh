#!/bin/bash
# ============================================
# SnapLearn 回滚脚本
# 将服务回滚到上一个版本（previous → latest）
# ============================================
set -e
set -x

PROJECT_DIR="/home/SnapLearn"

echo "=========================================="
echo " SnapLearn 回滚"
echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

cd "${PROJECT_DIR}"

# 检查 previous 镜像是否存在
if ! docker image inspect snaplearn-backend:previous &>/dev/null; then
    echo "错误: 没有可回滚的版本 (snaplearn-backend:previous 不存在)"
    exit 1
fi

echo ""
echo "[1/4] 确认回滚版本..."
echo "当前 latest 镜像:"
docker images snaplearn-backend:latest --format "  {{.ID}} {{.CreatedAt}}"
docker images snaplearn-admin:latest --format "  {{.ID}} {{.CreatedAt}}"
echo ""
echo "将回滚到 previous 镜像:"
docker images snaplearn-backend:previous --format "  {{.ID}} {{.CreatedAt}}"
docker images snaplearn-admin:previous --format "  {{.ID}} {{.CreatedAt}}"

echo ""
read -p "确认回滚? (y/N): " CONFIRM
if [ "${CONFIRM}" != "y" ] && [ "${CONFIRM}" != "Y" ]; then
    echo "已取消"
    exit 0
fi

echo ""
echo "[2/4] 回滚镜像标签..."

# 当前 latest 需要先移走，避免冲突
docker tag snaplearn-backend:latest snaplearn-backend:failed 2>/dev/null || true
docker tag snaplearn-admin:latest snaplearn-admin:failed 2>/dev/null || true

# previous → latest
docker tag snaplearn-backend:previous snaplearn-backend:latest
docker tag snaplearn-admin:previous snaplearn-admin:latest

echo "镜像标签已切换"

echo ""
echo "[3/4] 重启服务..."
docker compose -f docker-compose.server.yml --env-file .env.server up -d --remove-orphans

echo ""
echo "[4/4] 清理..."
# 删除标记为 failed 的旧镜像标签
docker rmi snaplearn-backend:failed 2>/dev/null || true
docker rmi snaplearn-admin:failed 2>/dev/null || true

# 清理 dangling 镜像
docker image prune -f

echo ""
echo "=========================================="
echo " 回滚完成"
echo " 后端 API:  http://$(hostname -I 2>/dev/null | awk '{print $1}'):8080"
echo "=========================================="
