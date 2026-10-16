<template>
  <div class="admin-shell">
    <AppHeader title="GAMEBOI / 内容审核">
      <template #actions>
        <button :disabled="loading || busy" @click="load(page)">REFRESH</button>
      </template>
    </AppHeader>
    <main class="admin-page">
    <p v-if="error" role="alert" class="error">{{ error }}</p>
    <section>
      <div class="section-head"><h2>文章审核</h2><select v-model="status" :disabled="loading || busy" @change="load(1)"><option :value="ARTICLE_WORKFLOW_STATUS.PENDING_REVIEW">待审核</option><option :value="ARTICLE_WORKFLOW_STATUS.PUBLISHED">已发布</option><option :value="ARTICLE_WORKFLOW_STATUS.REJECTED">已拒绝</option><option :value="ARTICLE_WORKFLOW_STATUS.UNPUBLISHED">已下架</option></select></div>
      <textarea v-model="reason" maxlength="1000" placeholder="审核说明（可选）" :disabled="busy || loading" />
      <p v-if="loading">LOADING…</p><p v-else-if="!articles.length">NO ARTICLES</p>
      <article v-for="item in articles" :key="item.id" class="article">
        <strong>{{ item.title }}</strong><span>{{ item.gameCodes?.join(' / ') }} · {{ item.contentType }} · r{{ item.revision }}</span><small>{{ item.workflowReasonCode || item.sourceName || '待人工确认' }}</small>
        <a v-if="safeLink(item.originalUrl)" :href="safeLink(item.originalUrl)" target="_blank" rel="noopener noreferrer">ORIGINAL ↗</a>
        <div><button :disabled="auditLoading" @click="loadAudit(item, 1)">AUDIT</button><template v-if="item.workflowStatus===ARTICLE_WORKFLOW_STATUS.PENDING_REVIEW"><button :disabled="busy || loading || stale" @click="transition(approveArticle,item)">APPROVE</button><button :disabled="busy || loading || stale" @click="transition(rejectArticle,item)">REJECT</button></template>
        <button v-if="item.workflowStatus===ARTICLE_WORKFLOW_STATUS.PUBLISHED" :disabled="busy || loading || stale" @click="transition(unpublishArticle,item)">UNPUBLISH</button></div>
      </article>
      <nav aria-label="审核分页"><button :disabled="busy || loading || page<=1" @click="load(page-1)">上一页</button><span>第 {{ page }} 页 / {{ total }} 篇</span><button :disabled="busy || loading || page*20>=total" @click="load(page+1)">下一页</button></nav>
    </section>
    <section v-if="auditArticle" class="audit-section">
      <div class="section-head"><h2>审核记录 #{{ auditArticle.id }}</h2><button :disabled="auditLoading" @click="clearAudit">CLOSE</button></div>
      <p v-if="auditError" role="alert" class="error">{{ auditError }}</p><p v-else-if="auditLoading">LOADING 审核记录…</p><p v-else-if="!auditItems.length">NO 审核记录 RECORDS</p>
      <ol v-else><li v-for="entry in auditItems" :key="entry.id"><strong>{{ entry.action }}</strong> · r{{ entry.revision }} · {{ entry.beforeStatus || '—' }} → {{ entry.afterStatus }} · 管理员 #{{ entry.actorUserId ?? '历史未知' }}<small>{{ entry.reasonCode || entry.reasonDetail || '无额外说明' }} · {{ entry.createdAt }}</small></li></ol>
      <nav aria-label="审核记录分页"><button :disabled="auditLoading || auditPage<=1" @click="loadAudit(auditArticle, auditPage-1)">上一页</button><span>第 {{ auditPage }} 页 / {{ auditTotal }} 条</span><button :disabled="auditLoading || auditPage*20>=auditTotal" @click="loadAudit(auditArticle, auditPage+1)">下一页</button></nav>
    </section>
    <section><h2>来源状态</h2><article v-for="source in sources" :key="source.code" class="source"><strong>{{ source.name }}</strong><span>{{ source.verificationStatus }}</span><small>{{ source.pauseReason || source.lastSuccessAt || '未成功运行' }}</small><button :disabled="busy || loading" v-if="source.verificationStatus===SOURCE_VERIFICATION_STATUS.PAUSED" @click="sourceAction(source,true)">RESUME</button><button :disabled="busy || loading" v-else @click="sourceAction(source,false)">PAUSE</button></article></section>
    <section><h2>最近采集</h2><p v-if="!runs.length">NO RUNS</p><ul><li v-for="run in runs" :key="run.runId">{{ run.status }} · {{ run.processedCount }}/{{ run.discoveredCount }} · {{ run.startedAt }}</li></ul></section>
  </main>
  </div>
</template>
<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { approveArticle, getAdminSources, getIngestRuns, getModerationArticles, getModerationAudit, pauseSource, rejectArticle, resumeSource, unpublishArticle } from '../api/admin'
import { ARTICLE_WORKFLOW_STATUS, SOURCE_VERIFICATION_STATUS } from '../utils/constants'
import AppHeader from '../components/AppHeader.vue'
const articles=ref([]), sources=ref([]), runs=ref([]), loading=ref(false), busy=ref(false), error=ref(''), stale=ref(false)
const status=ref(ARTICLE_WORKFLOW_STATUS.PENDING_REVIEW), page=ref(1), total=ref(0), reason=ref('')
const auditArticle=ref(null), auditItems=ref([]), auditLoading=ref(false), auditError=ref(''), auditPage=ref(1), auditTotal=ref(0)
let generation=0, auditGeneration=0
onBeforeUnmount(()=>{generation+=1;auditGeneration+=1})
function safeLink(value){try{const u=new URL(value);return ['https:','http:'].includes(u.protocol)&&!u.username&&!u.password?u.href:null}catch{return null}}
function checked(res){if(!res || res.code!==0)throw new Error(res?.message||'操作失败');return res.data}
function displayError(cause, fallback){return cause.userMessage||cause.response?.data?.message||cause.message||fallback}
async function load(wanted=1){
  const current=++generation; loading.value=true; error.value=''; stale.value=true
  try{
    const [a,s,r]=await Promise.all([getModerationArticles({workflowStatus:status.value,pageNum:wanted,pageSize:20}),getAdminSources(),getIngestRuns()])
    if(current!==generation)return
    const result=checked(a), sourceData=checked(s), runData=checked(r)
    articles.value=result.items||[];total.value=result.total||0;page.value=wanted;sources.value=sourceData||[];runs.value=runData||[];stale.value=false
  }catch(cause){if(current===generation)error.value=displayError(cause,'加载失败')}
  finally{if(current===generation)loading.value=false}
}
async function loadAudit(item,wanted=1){
  const current=++auditGeneration;auditArticle.value=item;auditLoading.value=true;auditError.value=''
  try{
    const result=checked(await getModerationAudit(item.id,{pageNum:wanted,pageSize:20}))
    if(current!==auditGeneration)return
    auditItems.value=result.items||[];auditTotal.value=result.total||0;auditPage.value=wanted
  }catch(cause){if(current===auditGeneration){auditItems.value=[];auditTotal.value=0;auditError.value=displayError(cause,'审核记录加载失败')}}
  finally{if(current===auditGeneration)auditLoading.value=false}
}
function clearAudit(){auditGeneration+=1;auditArticle.value=null;auditItems.value=[];auditTotal.value=0;auditError.value='';auditPage.value=1}
async function transition(fn,item){
  if(busy.value||loading.value||stale.value)return
  const refreshAudit=auditArticle.value?.id===item.id
  busy.value=true;error.value=''
  try{checked(await fn(item.id,{revision:item.revision,reasonDetail:reason.value.trim()||'管理员审核'}));await load(page.value);if(refreshAudit)await loadAudit(item,1)}
  catch(cause){stale.value=true;error.value=cause.response?.status===409?'文章已被修改，请刷新列表后重新审核':displayError(cause,'审核操作未确认，请刷新')}
  finally{busy.value=false}
}
async function sourceAction(source,resume){
  if(busy.value||loading.value)return
  const text=resume?null:window.prompt('暂停原因')
  if(!resume&&!text?.trim())return
  busy.value=true;error.value=''
  try{checked(await (resume?resumeSource(source.code):pauseSource(source.code,text.trim())));await load(page.value)}
  catch(cause){error.value=displayError(cause,'来源操作失败')}
  finally{busy.value=false}
}
onMounted(()=>load())
</script>
<style scoped>
.admin-shell{min-height:100vh;background:var(--gb-canvas)}.admin-page{max-width:1220px;margin:auto;padding:32px 28px 80px;font-family:var(--font-body);color:#151619}.section-head{display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #dedfe2;padding-bottom:16px;gap:14px}.admin-page h1{font-size:clamp(1.8rem,4vw,3rem);letter-spacing:-.06em;margin:0}.admin-page h2{font-size:20px;letter-spacing:-.04em;margin:0}section{margin:34px 0;background:#fff;border:1px solid #dedfe2;padding:22px}.article,.source{display:grid;gap:8px;padding:18px 0;border-bottom:1px solid #ececef}.article strong{font-size:17px}.article span,.article small,.source small{color:#6f737b}.audit-section li{padding:12px 0;border-bottom:1px solid #dedfe2;display:grid;gap:4px}.audit-section ol{padding-left:22px}.source{grid-template-columns:2fr 1fr 3fr auto;align-items:center}button{background:#fff;border:1px solid #dedfe2;padding:8px 11px;font-size:13px;font-weight:700;cursor:pointer;margin-right:6px}button:hover{color:#d92d27;border-color:#d92d27}.error{border:1px solid #d92d27;padding:12px;color:#aa201d;background:#fff0ef}a{color:#d92d27;font-weight:700}textarea{width:100%;min-height:76px;margin:16px 0;box-sizing:border-box;border:1px solid #dedfe2;padding:12px;font:inherit}@media(max-width:700px){.source{grid-template-columns:1fr}.admin-page{padding:20px 16px}.section-head{align-items:flex-start;flex-direction:column}section{padding:16px}}
</style>
