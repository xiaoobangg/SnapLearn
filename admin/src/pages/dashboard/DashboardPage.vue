<template>
  <div class="dashboard">
    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <div class="stat-card stat-card-blue">
          <div class="stat-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ overview.total_users || 0 }}</div>
            <div class="stat-label">总用户数</div>
            <div class="stat-sub">今日新增 +{{ overview.today_new_users || 0 }}</div>
          </div>
          <div class="stat-bg"></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-purple">
          <div class="stat-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ overview.total_cards || 0 }}</div>
            <div class="stat-label">总卡片数</div>
            <div class="stat-sub">{{ overview.total_groups || 0 }} 个卡片组</div>
          </div>
          <div class="stat-bg"></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-green">
          <div class="stat-icon">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ overview.today_reviews || 0 }}</div>
            <div class="stat-label">今日复习(打卡)</div>
            <div class="stat-sub">今日打卡 {{ overview.today_checkins || 0 }} 人</div>
          </div>
          <div class="stat-bg"></div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card stat-card-yellow">
          <div class="stat-icon">
            <el-icon><ArrowUp /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ overview.mastered_rate || 0 }}%</div>
            <div class="stat-label">掌握率</div>
            <div class="stat-sub">{{ overview.mastered_count || 0 }} 已掌握</div>
          </div>
          <div class="stat-bg"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px">
      <el-col :span="12">
        <el-card shadow="none" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">近 7 天学习活跃趋势</span>
              <span class="card-sub">用户增长与学习数据</span>
            </div>
          </template>
          <v-chart :option="reviewChartOption" style="height: 340px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="none" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">卡片组状态分布</span>
              <span class="card-sub">各阶段学习进度</span>
            </div>
          </template>
          <v-chart :option="groupStatusChartOption" style="height: 340px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px">
      <el-col :span="12">
        <el-card shadow="none" class="info-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">系统信息</span>
            </div>
          </template>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">活跃用户(7日)</span>
              <span class="info-value">{{ overview.active_users_7d || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">今日新卡片</span>
              <span class="info-value">{{ overview.today_new_cards || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">单词池总量</span>
              <span class="info-value">{{ overview.total_pool || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">已掌握单词</span>
              <span class="info-value">{{ overview.mastered_count || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">待复习单词</span>
              <span class="info-value">{{ overview.today_review_due || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="none" class="info-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">打卡概况</span>
            </div>
          </template>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">今日打卡人数</span>
              <span class="info-value">{{ overview.today_checkins || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">今日复习(打卡)</span>
              <span class="info-value">{{ overview.today_reviews || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">待学习组</span>
              <span class="info-value">{{ overview.groups_pending || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">学习中组</span>
              <span class="info-value">{{ overview.groups_learning || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">已学完组</span>
              <span class="info-value">{{ overview.groups_learn_done || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">测试中组</span>
              <span class="info-value">{{ overview.groups_testing || 0 }}</span>
            </div>
            <div class="info-item info-item-highlight">
              <span class="info-label">已通关组</span>
              <span class="info-value info-value-green">{{ overview.groups_test_done || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { dashboardApi } from "@/api";
import VChart from "vue-echarts";
import { use } from "echarts/core";
import { CanvasRenderer } from "echarts/renderers";
import { LineChart, BarChart, PieChart } from "echarts/charts";
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from "echarts/components";
import { User, Document, Calendar, ArrowUp } from "@element-plus/icons-vue";

use([CanvasRenderer, LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent]);

const overview = ref<Record<string, any>>({});
const dailyStats = ref<any[]>([]);

const reviewChartOption = computed(() => ({
  tooltip: { 
    trigger: "axis",
    backgroundColor: "rgba(30, 41, 59, 0.95)",
    borderColor: "rgba(255, 255, 255, 0.1)",
    borderWidth: 1,
    textStyle: { color: "#F8FAFC" }
  },
  legend: { 
    data: ["复习数", "新用户", "新卡片组", "打卡人数"],
    textStyle: { color: "#94A3B8" },
    top: 0
  },
  grid: { left: "3%", right: "4%", bottom: "3%", top: "15%", containLabel: true },
  xAxis: { 
    type: "category", 
    data: dailyStats.value.map((d: any) => d.date.slice(5)),
    axisLine: { lineStyle: { color: "rgba(255,255,255,0.1)" } },
    axisLabel: { color: "#94A3B8" },
    axisTick: { show: false }
  },
  yAxis: { 
    type: "value",
    axisLine: { show: false },
    axisLabel: { color: "#94A3B8" },
    splitLine: { lineStyle: { color: "rgba(255,255,255,0.05)" } }
  },
  series: [
    {
      name: "复习数",
      type: "bar",
      data: dailyStats.value.map((d: any) => d.reviews),
      itemStyle: { 
        color: {
          type: "linear",
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: "#6366F1" },
            { offset: 1, color: "#4F46E5" }
          ]
        },
        borderRadius: [6, 6, 0, 0]
      },
      barWidth: "40%"
    },
    {
      name: "新用户",
      type: "line",
      data: dailyStats.value.map((d: any) => d.new_users),
      smooth: true,
      lineStyle: { color: "#10B981", width: 3 },
      itemStyle: { color: "#10B981" },
      symbol: "circle",
      symbolSize: 8
    },
    {
      name: "新卡片组",
      type: "line",
      data: dailyStats.value.map((d: any) => d.new_groups),
      smooth: true,
      lineStyle: { color: "#F59E0B", width: 3 },
      itemStyle: { color: "#F59E0B" },
      symbol: "circle",
      symbolSize: 8
    },
    {
      name: "打卡人数",
      type: "line",
      data: dailyStats.value.map((d: any) => d.checkins || 0),
      smooth: true,
      lineStyle: { color: "#06B6D4", width: 3 },
      itemStyle: { color: "#06B6D4" },
      symbol: "circle",
      symbolSize: 8
    },
  ],
}));

const groupStatusChartOption = computed(() => ({
  tooltip: { 
    trigger: "item", 
    formatter: "{b}: {c} ({d}%)",
    backgroundColor: "rgba(30, 41, 59, 0.95)",
    borderColor: "rgba(255, 255, 255, 0.1)",
    textStyle: { color: "#F8FAFC" }
  },
  legend: { 
    bottom: 0,
    textStyle: { color: "#94A3B8" }
  },
  series: [{
    type: "pie",
    radius: ["50%", "75%"],
    avoidLabelOverlap: false,
    padAngle: 4,
    itemStyle: { borderRadius: 12, borderColor: "#1E293B", borderWidth: 3 },
    label: { 
      show: true, 
      formatter: "{b}\n{c}组",
      color: "#F8FAFC",
      fontSize: 13,
      fontWeight: 500
    },
    data: [
      { value: overview.value.groups_pending || 0, name: "待学习", itemStyle: { color: "#64748B" } },
      { value: overview.value.groups_learning || 0, name: "学习中", itemStyle: { color: "#F59E0B" } },
      { value: overview.value.groups_learn_done || 0, name: "已学完", itemStyle: { color: "#6366F1" } },
      { value: overview.value.groups_testing || 0, name: "测试中", itemStyle: { color: "#F97316" } },
      { value: overview.value.groups_test_done || 0, name: "已通关", itemStyle: { color: "#10B981" } },
    ],
  }],
}));

onMounted(async () => {
  try {
    overview.value = (await dashboardApi.overview()).data;
  } catch { /* handled */ }
  try {
    dailyStats.value = (await dashboardApi.dailyStats(7)).data;
  } catch { /* handled */ }
});
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 0;
}

.stats-row {
  .stat-card {
    position: relative;
    border-radius: 18px;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 18px;
    overflow: hidden;
    transition: all 0.3s ease;
    border: 1px solid rgba(255, 255, 255, 0.06);
    
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
      border-color: rgba(255, 255, 255, 0.1);
    }
    
    &.stat-card-blue {
      background: linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(79, 70, 229, 0.1));
      
      .stat-icon {
        background: linear-gradient(135deg, #6366F1, #4F46E5);
      }
      
      .stat-num { color: #818CF8; }
      .stat-bg { background: radial-gradient(circle at 80% -20%, rgba(99, 102, 241, 0.3), transparent 60%); }
    }
    
    &.stat-card-purple {
      background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(126, 34, 206, 0.1));
      
      .stat-icon {
        background: linear-gradient(135deg, #A78BFA, #8B5CF6);
      }
      
      .stat-num { color: #A78BFA; }
      .stat-bg { background: radial-gradient(circle at 80% -20%, rgba(139, 92, 246, 0.3), transparent 60%); }
    }
    
    &.stat-card-green {
      background: linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(5, 150, 105, 0.1));
      
      .stat-icon {
        background: linear-gradient(135deg, #34D399, #10B981);
      }
      
      .stat-num { color: #34D399; }
      .stat-bg { background: radial-gradient(circle at 80% -20%, rgba(16, 185, 129, 0.3), transparent 60%); }
    }
    
    &.stat-card-yellow {
      background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(217, 119, 6, 0.1));
      
      .stat-icon {
        background: linear-gradient(135deg, #FBBF24, #F59E0B);
      }
      
      .stat-num { color: #FBBF24; }
      .stat-bg { background: radial-gradient(circle at 80% -20%, rgba(245, 158, 11, 0.3), transparent 60%); }
    }
    
    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
      
      .el-icon {
        font-size: 26px;
        color: #fff;
      }
    }
    
    .stat-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    
    .stat-num {
      font-size: 36px;
      font-weight: 700;
      letter-spacing: -1px;
    }
    
    .stat-label {
      font-size: 14px;
      font-weight: 600;
      color: #F8FAFC;
    }
    
    .stat-sub {
      font-size: 12px;
      color: #64748B;
      margin-top: 2px;
    }
    
    .stat-bg {
      position: absolute;
      top: 0;
      right: 0;
      width: 150px;
      height: 150px;
      opacity: 0.6;
    }
  }
}

.chart-card {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.06);
  
  .card-header {
    display: flex;
    flex-direction: column;
    gap: 4px;
    
    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #F8FAFC;
    }
    
    .card-sub {
      font-size: 12px;
      color: #64748B;
    }
  }
}

.info-card {
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.06);
  
  .card-header {
    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #F8FAFC;
    }
  }
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
  background: rgba(15, 23, 42, 0.4);
  border-radius: 12px;
  transition: all 0.2s ease;
  
  &:hover {
    background: rgba(79, 70, 229, 0.1);
  }
  
  &.info-item-highlight {
    grid-column: span 2;
    background: rgba(16, 185, 129, 0.08);
    border: 1px solid rgba(16, 185, 129, 0.2);
  }
  
  .info-label {
    font-size: 13px;
    color: #64748B;
    font-weight: 500;
  }
  
  .info-value {
    font-size: 22px;
    font-weight: 700;
    color: #F8FAFC;
    letter-spacing: -0.5px;
    
    &.info-value-green {
      color: #34D399;
    }
  }
}
</style>