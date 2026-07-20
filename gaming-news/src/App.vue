<template>
  <n-config-provider :theme="lightTheme" :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { lightTheme } from 'naive-ui'

const themeOverrides = {
  common: {
    primaryColor: '#306230',
    primaryColorHover: '#0F380F',
    primaryColorPressed: '#0F380F',
    primaryColorSuppl: '#306230',
    borderRadius: '0px',
    borderRadiusSmall: '0px',
    fontFamily: "'Courier New', monospace",
  },
  Button: {
    borderRadiusMedium: '0px',
    borderRadiusSmall: '0px',
    borderRadiusLarge: '0px',
  },
  Input: {
    borderRadius: '0px',
  },
  Card: {
    borderRadius: '0px',
  },
  Dialog: {
    borderRadius: '0px',
  },
  Message: {
    borderRadius: '0px',
  },
}
</script>

<style>
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.25s steps(6);
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>
