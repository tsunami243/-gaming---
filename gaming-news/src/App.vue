<template>
  <n-config-provider :theme="lightTheme">
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { lightTheme } from 'naive-ui'
</script>
