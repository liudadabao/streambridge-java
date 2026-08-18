<template>
  <div class="app-container">
    <el-row :gutter="16" class="status-row">
      <el-col :xs="24" :sm="8"><el-card shadow="never"><el-statistic title="媒体引擎" :value="status.engine || '-'" /></el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never"><el-statistic title="运行状态" :value="status.state || 'UNKNOWN'" /></el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never"><el-statistic title="在线流" :value="streams.length" /></el-card></el-col>
    </el-row>

    <el-card shadow="never" class="panel">
      <template #header>
        <div class="card-header">
          <span>流媒体资源</span>
          <div>
            <el-button v-hasPermi="['media:engine:edit']" type="primary" plain @click="openCommand('pull')">拉流代理</el-button>
            <el-button v-hasPermi="['media:engine:edit']" type="success" plain @click="openCommand('push')">推流代理</el-button>
            <el-button v-hasPermi="['media:engine:edit']" type="warning" plain @click="openCommand('rtp')">开启 RTP</el-button>
            <el-button :loading="loading" @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="loading" :data="streams" empty-text="暂无在线流">
        <el-table-column prop="virtualHost" label="VHost" min-width="150" />
        <el-table-column prop="application" label="应用" min-width="100" />
        <el-table-column prop="stream" label="流 ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sourceUri" label="来源" min-width="220" show-overflow-tooltip />
        <el-table-column prop="readerCount" label="观看数" width="90" align="center" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="record(scope.row, true)">开始录像</el-button>
            <el-button link type="warning" @click="record(scope.row, false)">停止录像</el-button>
            <el-button link type="danger" @click="closeStream(scope.row)">关流</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header><div class="card-header"><span>托管操作</span><el-button @click="loadOperations">刷新</el-button></div></template>
      <el-table :data="operations" empty-text="暂无托管操作">
        <el-table-column prop="operationId" label="操作 ID" min-width="260" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100" />
        <el-table-column prop="stream" label="流 ID" min-width="150" />
        <el-table-column prop="port" label="端口" width="90" />
        <el-table-column prop="open" label="状态" width="90"><template #default="s"><el-tag :type="s.row.open ? 'success' : 'info'">{{ s.row.open ? '运行中' : '已结束' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="s"><el-button link type="danger" @click="stopOperation(s.row.operationId)">停止</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="commandVisible" :title="commandTitles[commandType]" width="620px" append-to-body>
      <el-form label-width="110px">
        <el-form-item v-if="commandType === 'pull'" label="源地址"><el-input v-model="form.sourceUri" placeholder="rtsp://..." /></el-form-item>
        <el-form-item v-if="commandType === 'push'" label="目标地址"><el-input v-model="form.targetUri" placeholder="rtmp://..." /></el-form-item>
        <el-form-item label="VHost"><el-input v-model="form.virtualHost" /></el-form-item>
        <el-form-item label="应用"><el-input v-model="form.application" /></el-form-item>
        <el-form-item label="流 ID"><el-input v-model="form.stream" /></el-form-item>
        <el-form-item v-if="commandType === 'push'" label="源协议"><el-input v-model="form.sourceSchema" /></el-form-item>
        <el-form-item v-if="commandType === 'rtp'" label="监听端口"><el-input-number v-model="form.port" :min="0" :max="65535" /></el-form-item>
        <el-form-item v-if="commandType === 'rtp'" label="传输模式">
          <el-select v-model="form.transport"><el-option label="UDP" value="UDP" /><el-option label="TCP 主动" value="TCP_ACTIVE" /><el-option label="TCP 被动" value="TCP_PASSIVE" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="commandVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitCommand">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import {
  closeMediaStream, getMediaEngineStatus, listMediaOperations, listMediaStreams, openRtpServer,
  pullMediaStream, pushMediaStream, startMediaRecording, stopMediaOperation, stopMediaRecording
} from '@/api/media/engine'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const submitting = ref(false)
const status = ref({})
const streams = ref([])
const operations = ref([])
const commandVisible = ref(false)
const commandType = ref('pull')
const commandTitles = { pull: '创建拉流代理', push: '创建推流代理', rtp: '开启 RTP/GB28181 接收' }
const defaults = () => ({ sourceUri: '', targetUri: '', virtualHost: '__defaultVhost__', application: 'live', stream: '', sourceSchema: 'rtmp', port: 0, transport: 'UDP' })
const form = reactive(defaults())

function address(value = form) {
  return { virtualHost: value.virtualHost, application: value.application, stream: value.stream }
}

async function loadOperations() {
  const response = await listMediaOperations()
  operations.value = response.data || []
}

async function loadData() {
  loading.value = true
  try {
    const [statusResponse, streamResponse] = await Promise.all([getMediaEngineStatus(), listMediaStreams()])
    status.value = statusResponse.data || {}
    streams.value = streamResponse.data || []
    await loadOperations()
  } finally { loading.value = false }
}

function openCommand(type) {
  Object.assign(form, defaults())
  commandType.value = type
  commandVisible.value = true
}

async function submitCommand() {
  submitting.value = true
  try {
    if (commandType.value === 'pull') await pullMediaStream({ sourceUri: form.sourceUri, target: address(), options: {} })
    if (commandType.value === 'push') await pushMediaStream({ source: address(), sourceSchema: form.sourceSchema, targetUri: form.targetUri, options: {} })
    if (commandType.value === 'rtp') await openRtpServer({ target: address(), port: form.port, transport: form.transport, multiplexed: false })
    proxy.$modal.msgSuccess('操作已提交')
    commandVisible.value = false
    await loadData()
  } finally { submitting.value = false }
}

async function stopOperation(id) {
  await stopMediaOperation(id)
  proxy.$modal.msgSuccess('操作已停止')
  await loadData()
}

async function closeStream(row) {
  await closeMediaStream(address(row))
  proxy.$modal.msgSuccess('关流请求已提交')
  await loadData()
}

async function record(row, start) {
  const command = { stream: address(row), format: 'MP4', directory: null, segmentSeconds: null }
  if (start) await startMediaRecording(command)
  else await stopMediaRecording(command)
  proxy.$modal.msgSuccess(start ? '录像已开启' : '录像已停止')
}

loadData()
</script>

<style scoped>
.status-row,.panel { margin-bottom: 16px; }
.status-row .el-col { margin-bottom: 16px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
</style>
