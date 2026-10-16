<template>
  <n-config-provider :theme="lightTheme" :theme-overrides="themeOverrides">
    <n-message-provider><n-dialog-provider><router-view v-slot="{ Component }"><transition name="page-fade" mode="out-in"><component :is="Component" /></transition></router-view></n-dialog-provider></n-message-provider>
  </n-config-provider>
</template>
<script setup>
import { lightTheme } from 'naive-ui'
const themeOverrides={common:{primaryColor:'#d92d27',primaryColorHover:'#aa201d',primaryColorPressed:'#8f1916',primaryColorSuppl:'#d92d27',borderRadius:'3px',borderRadiusSmall:'3px',fontFamily:'Inter, Microsoft YaHei UI, Microsoft YaHei, system-ui, sans-serif'},Button:{borderRadiusMedium:'3px',borderRadiusSmall:'3px',borderRadiusLarge:'3px'},Input:{borderRadius:'3px'},Card:{borderRadius:'4px'},Dialog:{borderRadius:'4px'},Message:{borderRadius:'4px'}}
</script>
<style>.page-fade-enter-active,.page-fade-leave-active{transition:opacity .16s ease}.page-fade-enter-from,.page-fade-leave-to{opacity:0}@media(prefers-reduced-motion:reduce){.page-fade-enter-active,.page-fade-leave-active{transition:none}}</style>
