<template>
  <section class="comments" aria-label="已有评论">
    <h2>已有评论</h2><p>评论提交暂未开放。</p>
    <p v-if="error" role="alert">{{ error }} <button @click="load(page)">重试</button></p>
    <p v-if="loading">加载中…</p>
    <p v-else-if="!error && !items.length">暂无评论</p>
    <ul v-else-if="!loading"><li v-for="item in items" :key="item.id"><strong>{{ item.displayName || '用户' }}</strong><span v-if="item.parentId">（回复）</span><p>{{ item.content }}</p></li></ul>
    <nav aria-label="评论分页"><button :disabled="loading || page<=1" @click="load(page-1)">上一页</button><span>第 {{ page }} 页</span><button :disabled="loading || page*20>=total" @click="load(page+1)">下一页</button></nav>
  </section>
</template>
<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import request from '../utils/request'
const props=defineProps({articleId:{type:Number,required:true}})
const items=ref([]),total=ref(0),page=ref(1),loading=ref(false),error=ref('')
let generation=0
onBeforeUnmount(()=>{generation+=1})
async function load(wanted=1){
  const current=++generation;loading.value=true;error.value=''
  try{
    const response=await request({url:`/v1/articles/${props.articleId}/comments`,params:{pageNum:wanted,pageSize:20}})
    if(current!==generation)return
    if(response.code!==0)throw new Error(response.message||'评论读取失败')
    items.value=response.data.items;total.value=response.data.total;page.value=wanted
  }catch(cause){if(current===generation)error.value=cause.userMessage||'评论读取失败，请重试'}
  finally{if(current===generation)loading.value=false}
}
watch(()=>props.articleId,()=>{items.value=[];total.value=0;page.value=1;load()},{immediate:true})
</script>
<style scoped>
.comments{margin-top:20px;border-top:2px solid var(--gb-dark);padding-top:12px}h2{font-size:17px}ul{padding-left:20px}li{padding:8px 0;overflow-wrap:anywhere}button{padding:6px;margin:6px;background:var(--gb-lightest);border:1px solid var(--gb-dark)}
</style>
