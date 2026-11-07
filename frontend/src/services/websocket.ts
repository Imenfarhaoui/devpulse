import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type StatusCheckCallback = (data: any) => void;
export type IncidentUpdateCallback = (data: any) => void;

class RealtimeService {
  private client: Client | null = null;
  private currentOrgId: string | null = null;
  private statusCallbacks: Set<StatusCheckCallback> = new Set();
  private incidentCallbacks: Set<IncidentUpdateCallback> = new Set();
  private isConnected = false;

  public connect(orgId: string) {
    if (this.currentOrgId === orgId && this.isConnected) {
      return;
    }

    this.disconnect();
    this.currentOrgId = orgId;

    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.isConnected = true;
        this.subscribeToTopics(orgId);
      },
      onDisconnect: () => {
        this.isConnected = false;
      },
      onStompError: (frame) => {
        console.warn('STOMP broker error:', frame.headers['message']);
      },
    });

    this.client.activate();
  }

  private subscribeToTopics(orgId: string) {
    if (!this.client || !this.client.connected) return;

    // Service status feed
    this.client.subscribe(`/topic/org.${orgId}.status`, (message) => {
      try {
        const payload = JSON.parse(message.body);
        this.statusCallbacks.forEach((cb) => cb(payload));
      } catch (err) {
        console.error('Failed to parse STOMP status message', err);
      }
    });

    // Incident updates feed
    this.client.subscribe(`/topic/org.${orgId}.incidents`, (message) => {
      try {
        const payload = JSON.parse(message.body);
        this.incidentCallbacks.forEach((cb) => cb(payload));
      } catch (err) {
        console.error('Failed to parse STOMP incident message', err);
      }
    });
  }

  public onServiceCheck(cb: StatusCheckCallback) {
    this.statusCallbacks.add(cb);
    return () => {
      this.statusCallbacks.delete(cb);
    };
  }

  public onIncidentUpdate(cb: IncidentUpdateCallback) {
    this.incidentCallbacks.add(cb);
    return () => {
      this.incidentCallbacks.delete(cb);
    };
  }

  public disconnect() {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch (e) {
        // ignore
      }
      this.client = null;
      this.isConnected = false;
    }
  }

  public getStatus() {
    return this.isConnected;
  }
}

export const realtimeService = new RealtimeService();

