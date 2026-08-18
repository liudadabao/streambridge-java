import request from '@/utils/request'

export function getMediaEngineStatus() {
  return request({
    url: '/media/engine/status',
    method: 'get'
  })
}

export function listMediaStreams() {
  return request({
    url: '/media/engine/list',
    method: 'get'
  })
}

export function listMediaOperations() {
  return request({ url: '/media/engine/operations', method: 'get' })
}

export function pullMediaStream(data) {
  return request({ url: '/media/engine/pull', method: 'post', data })
}

export function pushMediaStream(data) {
  return request({ url: '/media/engine/push', method: 'post', data })
}

export function stopMediaOperation(operationId) {
  return request({ url: `/media/engine/operations/${operationId}`, method: 'delete' })
}

export function closeMediaStream(data) {
  return request({ url: '/media/engine/stream', method: 'delete', data })
}

export function startMediaRecording(data) {
  return request({ url: '/media/engine/recording/start', method: 'post', data })
}

export function stopMediaRecording(data) {
  return request({ url: '/media/engine/recording/stop', method: 'post', data })
}

export function openRtpServer(data) {
  return request({ url: '/media/engine/rtp/open', method: 'post', data })
}

export function createWebRtcAnswer(data) {
  return request({ url: '/media/engine/webrtc/answer', method: 'post', data })
}
