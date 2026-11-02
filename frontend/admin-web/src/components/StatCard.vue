<template>
  <div class="stat-card">
    <div class="stat-icon" :style="{ background: iconBg, color: iconColor }">
      <el-icon><component :is="icon" /></el-icon>
    </div>
    <div>
      <div class="stat-label">{{ label }}</div>
      <div class="stat-value">
        {{ value }}<span v-if="suffix" class="stat-suffix">{{ suffix }}</span>
      </div>
      <div v-if="hint" class="stat-hint">{{ hint }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: '' },
  value: { type: [String, Number], default: 0 },
  suffix: { type: String, default: '' },
  hint: { type: String, default: '' },
  icon: { type: [String, Object], default: 'DataLine' },
  iconBg: { type: String, default: '#eaf1fe' },
  iconColor: { type: String, default: '#2563eb' }
})
</script>
