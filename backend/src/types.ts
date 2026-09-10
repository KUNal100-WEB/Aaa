export interface User {
  id: string;
  email: string;
  createdAt: number;
}

export interface Project {
  id: string;
  userId: string;
  name: string;
  packageName: string;
  description: string;
  language: 'Kotlin';
  framework: 'Jetpack Compose';
  createdAt: number;
  updatedAt: number;
  theme?: string;
  appType?: string;
  firebaseRequired?: boolean;
}

export interface ProjectFile {
  id: string;
  projectId: string;
  path: string;
  content: string;
  updatedAt: number;
}

export type BuildStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'CANCELLED';

export interface BuildRecord {
  id: string;
  projectId: string;
  status: BuildStatus;
  startedAt: number;
  finishedAt?: number;
  durationMs?: number;
  progress: number;
  logs: string;
  errorSummary?: string;
  artifactId?: string;
  workerType: 'mock_simulation' | 'isolated_cloud_worker';
}

export interface Artifact {
  id: string;
  buildId: string;
  fileName: string;
  size: number;
  downloadUrl: string;
  expiresAt: number;
}

export interface AIAction {
  type: 'create_file' | 'update_file' | 'delete_file' | 'rename_file' | 'add_dependency' | 'request_build';
  path?: string;
  oldPath?: string;
  newPath?: string;
  content?: string;
  dependency?: string;
}

export interface AIRequest {
  id: string;
  projectId: string;
  userMessage: string;
  status: 'PENDING' | 'APPLIED' | 'FAILED';
  summary?: string;
  actions: AIAction[];
  repairAttempt?: number;
  createdAt: number;
}
