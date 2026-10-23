<script setup>
import { onMounted, ref } from 'vue'
import { deleteDocument, fetchDocuments, fetchHealth, fetchOrders, sendChat, uploadDocument } from './api'

const health = ref({ aiConfigured: false, model: 'glm-4.6v' })
const orders = ref([])
const documents = ref([])
const messages = ref([])
const input = ref('')
const sending = ref(false)
const error = ref('')
const sessionId = ref(localStorage.getItem('assistant-session') || '')

const prompts = [
  '查询订单 ORD20260925001',
  '张三有哪些订单',
  '哪些订单已发货',
  '北京今天天气怎么样',
  '未满 99 元怎么收运费',
  '会员积分怎么抵扣'
]

onMounted(async () => {
  try {
    health.value = await fetchHealth()
    orders.value = await fetchOrders()
    documents.value = await fetchDocuments()
  } catch (e) {
    error.value = e.message
  }
})

async function ask(text) {
  const message = (text ?? input.value).trim()
  if (!message || sending.value) return
  error.value = ''
  input.value = ''
  messages.value.push({ role: 'user', content: message })
  sending.value = true
  try {
    const data = await sendChat(sessionId.value, message)
    sessionId.value = data.sessionId
    localStorage.setItem('assistant-session', data.sessionId)
    messages.value.push({
      role: 'assistant',
      content: data.answer,
      tools: data.tools || [],
      sources: data.sources || []
    })
  } catch (e) {
    error.value = e.message
  } finally {
    sending.value = false
  }
}

async function onUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''
  try {
    await uploadDocument(file)
    documents.value = await fetchDocuments()
  } catch (e) {
    error.value = e.message
  }
}

async function removeDoc(id) {
  error.value = ''
  try {
    await deleteDocument(id)
    documents.value = await fetchDocuments()
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <div class="app">
    <header>
      <div>
        <h1>购物助手</h1>
        <p class="sub">查订单、查天气，并依据知识库回答店铺政策。</p>
      </div>
      <div class="badge" :class="{ warn: !health.aiConfigured }">
        {{ health.model }} · {{ health.aiConfigured ? '已配置 API Key' : '未设置 AI_API_KEY' }}
      </div>
    </header>

    <div class="layout">
      <aside>
        <section class="panel">
          <h2>示例订单</h2>
          <p v-if="!orders.length" class="empty">还没有订单。请先执行 sql/schema.sql。</p>
          <article v-for="order in orders" :key="order.orderNo" class="order">
            <strong>{{ order.orderNo }}</strong>
            <div class="meta">{{ order.userName }} · {{ order.statusLabel }} · ¥{{ order.totalAmount }}</div>
          </article>
        </section>
        <section class="panel">
          <h2>知识库</h2>
          <label class="upload">
            <input type="file" accept=".txt,.md" @change="onUpload" />
          </label>
          <p v-if="!documents.length" class="empty">启动并配置 Key 后会写入退换货、运费、会员三篇文档。</p>
          <article v-for="doc in documents" :key="doc.id" class="doc">
            <strong>{{ doc.title }}</strong>
            <div class="meta">{{ doc.chunkCount }} 个片段</div>
            <button class="ghost" @click="removeDoc(doc.id)">删除</button>
          </article>
        </section>
      </aside>

      <section class="chat">
        <div class="chat-head">对话</div>
        <div class="messages">
          <p v-if="!messages.length" class="hint">试试下面的问题。订单问题会调用数据库工具，天气会请求 Open-Meteo，政策问题走知识库检索。</p>
          <div v-for="(item, index) in messages" :key="index" class="bubble" :class="item.role">
            <div>{{ item.content }}</div>
            <div v-if="item.tools?.length" class="tags">
              <span v-for="tool in item.tools" :key="tool" class="tag">{{ tool }}</span>
            </div>
            <div v-if="item.sources?.length" class="sources">
              <div v-for="(source, sourceIndex) in item.sources" :key="sourceIndex" class="source">
                {{ source.title }}：{{ source.content }}
              </div>
            </div>
          </div>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <div class="chips">
          <button v-for="prompt in prompts" :key="prompt" class="chip" @click="ask(prompt)">{{ prompt }}</button>
        </div>
        <form class="composer" @submit.prevent="ask()">
          <input v-model="input" placeholder="例如：查一下张三的订单，或者上海天气" :disabled="sending" />
          <button type="submit" :disabled="sending">{{ sending ? '思考中' : '发送' }}</button>
        </form>
      </section>
    </div>
  </div>
</template>
