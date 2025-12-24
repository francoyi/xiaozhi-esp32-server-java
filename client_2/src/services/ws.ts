import { env } from '../config/env'

export type WsStatus = 'idle' | 'connecting' | 'open' | 'closed' | 'error'

export type WsLogItem = {
  ts: number
  dir: 'in' | 'out' | 'sys'
  text: string
}

export type WsClientOptions = {
  wsUrl?: string
  deviceId: string
  authorization?: string
  extraQuery?: Record<string, string | number | boolean | undefined | null>
  onOpen?: () => void
  onClose?: (code?: number, reason?: string) => void
  onError?: (err: any) => void
  onMessage?: (data: string) => void
}

export class WsClient {
  private ws: WebSocket | null = null
  status: WsStatus = 'idle'
  private opts: WsClientOptions

  constructor(opts: WsClientOptions) {
    this.opts = opts
  }

  connect() {
    if (this.ws && (this.status === 'connecting' || this.status === 'open')) return
    const base = (this.opts.wsUrl || env.wsUrl).replace(/\/$/, '')
    const qs = new URLSearchParams()
    qs.set('device-id', this.opts.deviceId)
    if (this.opts.authorization) qs.set('Authorization', this.opts.authorization)

    if (this.opts.extraQuery) {
      for (const [k, v] of Object.entries(this.opts.extraQuery)) {
        if (v === undefined || v === null) continue
        qs.set(k, String(v))
      }
    }

    const url = `${base}?${qs.toString()}`
    this.status = 'connecting'
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.status = 'open'
      this.opts.onOpen?.()
    }
    this.ws.onclose = (evt) => {
      this.status = 'closed'
      this.opts.onClose?.(evt.code, evt.reason)
      this.ws = null
    }
    this.ws.onerror = (err) => {
      this.status = 'error'
      this.opts.onError?.(err)
    }
    this.ws.onmessage = (evt) => {
      const text = typeof evt.data === 'string' ? evt.data : '[binary]'
      this.opts.onMessage?.(text)
    }
  }

  sendText(text: string) {
    if (!this.ws || this.status !== 'open') throw new Error('WebSocket not open')
    this.ws.send(text)
  }

  close() {
    if (!this.ws) return
    this.ws.close()
    this.ws = null
    this.status = 'closed'
  }
}
