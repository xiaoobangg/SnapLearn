#!/bin/bash
# ============================================
# SnapLearn 服务器端部署脚本
# 在服务器上执行：
#   git pull → mvn 打包 → docker build → compose up
# ============================================
set -e

# 开启命令日志：显示执行的每一条命令
set -x

# ---- 配置 ----
PROJECT_DIR="/home/SnapLearn"
BRANCH="main"

echo "=========================================="
echo " SnapLearn 服务器部署"
echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

cd "${PROJECT_DIR}"

# ====== Step 1: 拉取最新代码 ======
echo ""
echo "[1/5] 拉取最新代码 (${BRANCH})..."
git checkout "${BRANCH}"
git pull origin "${BRANCH}"
echo "当前 commit: $(git rev-parse --short HEAD)"

# ====== Step 2: Maven 打包后端 ======
echo ""
echo "[2/5] Maven 打包后端..."
cd "${PROJECT_DIR}/backend-java"
mvn clean package -DskipTests -q
ls -lh target/snaplearn.jar  # 确认 JAR 存在
echo "JAR 构建完成: target/snaplearn.jar"

# ====== Step 3: 备份当前版本（用于回滚） ======
echo ""
echo "[3/6] 保留上一版本用于回滚..."

# 如果当前存在 latest 镜像，将其标记为 previous
if docker image inspect snaplearn-backend:latest &>/dev/null; then
    docker tag snaplearn-backend:latest snaplearn-backend:previous
    echo "snaplearn-backend:latest → previous"
fi
if docker image inspect snaplearn-admin:latest &>/dev/null; then
    docker tag snaplearn-admin:latest snaplearn-admin:previous
    echo "snaplearn-admin:latest → previous"
fi

# ====== Step 4: Docker 构建镜像 ======
echo ""
echo "[4/6] Docker 构建镜像..."

# 后端：进入 backend-java 目录构建
cd "${PROJECT_DIR}/backend-java"
docker build -t snaplearn-backend:latest .

# 管理后台：进入 admin 目录构建
cd "${PROJECT_DIR}/admin"
docker build -t snaplearn-admin:latest .

echo "镜像构建完成"

# 清理 dangling 镜像（<none>），但保留 previous 标签
echo ""
echo "清理无用镜像..."
docker image prune -f

# ====== Step 4: 启动服务 ======
echo ""
echo "[5/6] 启动/更新服务..."
cd "${PROJECT_DIR}"

# 首次部署需要创建 .env.server
if [ ! -f .env.server ]; then
    echo "创建默认 .env.server（首次部署）..."
    cp .env.server.example .env.server
    echo ">>> 请编辑 .env.server 填入实际密码和 Key 后重新执行脚本 <<<"
    exit 0
fi

docker compose -f docker-compose.server.yml --env-file .env.server up -d --remove-orphans

# ====== Step 5: 健康检查 ======
echo ""
echo "[6/6] 健康检查..."
sleep 5

echo ""
echo "容器状态:"
docker compose -f docker-compose.server.yml ps

echo ""
echo "后端健康检查:"
curl -s http://localhost:8080/api/health && echo "" || echo "(后端仍在启动中，请稍候...)"

echo ""
echo "Prometheus 健康检查:"
curl -s http://localhost:9090/-/healthy && echo "" || echo "(Prometheus 仍在启动中...)"

echo ""
echo "Grafana 健康检查:"
curl -s http://localhost:3100/api/health && echo "" || echo "(Grafana 仍在启动中...)"

echo ""
echo "=========================================="
echo " 部署完成"
echo " 后端 API:      http://$(hostname -I 2>/dev/null | awk '{print $1}'):8080"
echo " 管理后台:      http://$(hostname -I 2>/dev/null | awk '{print $1}'):3001"
echo " Prometheus:    http://$(hostname -I 2>/dev/null | awk '{print $1}'):9090"
echo " Grafana:       http://$(hostname -I 2>/dev/null | awk '{print $1}'):3100  (首次登录 admin / admin)"
echo "=========================================="