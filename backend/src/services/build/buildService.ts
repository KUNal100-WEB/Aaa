import { v4 as uuidv4 } from 'uuid';
import { BuildRecord, Artifact } from '../../models/types.js';
import { projectService } from '../projects/projectService.js';

export interface IBuildService {
  requestBuild(projectId: string, variant?: string): Promise<BuildRecord>;
  getBuild(buildId: string): Promise<BuildRecord | null>;
  cancelBuild(buildId: string): Promise<boolean>;
  getArtifact(artifactId: string): Promise<Artifact | null>;
  getAllArtifacts(): Promise<Array<Artifact & { projectName: string; packageName: string; completedAt: number; variant: string }>>;
}

export class MockBuildService implements IBuildService {
  private builds: Map<string, BuildRecord> = new Map();
  private artifacts: Map<string, Artifact> = new Map();

  constructor() {
    // Seed initial ready-to-run demo built app
    const demoArtifactId = 'apk_notes_demo';
    this.artifacts.set(demoArtifactId, {
      id: demoArtifactId,
      buildId: 'build_initial_notes',
      fileName: 'modern-notes-app-debug.apk',
      size: 14859200, // 14.1 MB
      downloadUrl: `/api/artifacts/${demoArtifactId}/download`,
      expiresAt: Date.now() + 86400000 * 30
    });
  }

  async getAllArtifacts(): Promise<Array<Artifact & { projectName: string; packageName: string; completedAt: number; variant: string }>> {
    const list: Array<Artifact & { projectName: string; packageName: string; completedAt: number; variant: string }> = [];
    for (const artifact of this.artifacts.values()) {
      const build = this.builds.get(artifact.buildId);
      const project = build ? await projectService.getProject(build.projectId) : await projectService.getProject('proj_notes_app_demo');
      list.push({
        ...artifact,
        projectName: project?.name || 'Modern Notes App',
        packageName: project?.packageName || 'com.example.notesapp',
        completedAt: build?.finishedAt || Date.now() - 3600000 * 2,
        variant: 'debug'
      });
    }
    return list.reverse();
  }

  async requestBuild(projectId: string, variant: string = 'debug'): Promise<BuildRecord> {
    const project = await projectService.getProject(projectId);
    if (!project) {
      throw new Error(`Project ${projectId} not found.`);
    }

    const files = await projectService.getFiles(projectId);
    const buildId = 'build_' + uuidv4().slice(0, 8);

    const record: BuildRecord = {
      id: buildId,
      projectId,
      status: 'QUEUED',
      startedAt: Date.now(),
      progress: 0,
      logs: `> Task :app:preBuild UP-TO-DATE\n> Task :app:preDebugBuild UP-TO-DATE\n> Checking environment: Java 17, Gradle 9.3.1, AGP 9.1.1\n`
    };

    this.builds.set(buildId, record);

    // Run async simulation pipeline
    this.executeBuildPipeline(buildId, projectId, files);

    return record;
  }

  private async executeBuildPipeline(buildId: string, projectId: string, files: any[]) {
    const record = this.builds.get(buildId);
    if (!record) return;

    record.status = 'RUNNING';
    record.progress = 15;
    record.logs += `> Task :app:generateDebugBuildConfig UP-TO-DATE\n> Task :app:checkDebugAarMetadata UP-TO-DATE\n`;

    await new Promise(r => setTimeout(r, 600));
    if (record.status === 'CANCELLED') return;

    // Check syntax errors in Kotlin files
    const ktFiles = files.filter(f => f.path.endsWith('.kt'));
    let syntaxError = false;
    let errFile = '';

    for (const kf of ktFiles) {
      const openCount = (kf.content.match(/{/g) || []).length;
      const closeCount = (kf.content.match(/}/g) || []).length;
      if (openCount !== closeCount) {
        syntaxError = true;
        errFile = kf.path;
        break;
      }
    }

    record.progress = 45;
    record.logs += `> Task :app:compileDebugKotlin\n[Compiler] Analyzing ${ktFiles.length} Kotlin source files...\n`;

    await new Promise(r => setTimeout(r, 700));
    if (record.status === 'CANCELLED') return;

    if (syntaxError) {
      record.status = 'FAILED';
      record.finishedAt = Date.now();
      record.durationMs = record.finishedAt - record.startedAt;
      record.logs += `\ne: ${errFile}: Syntax error: Mismatched curly braces detected in Kotlin compilation.\n> Task :app:compileDebugKotlin FAILED\n\nFAILURE: Build failed with an exception.\n* What went wrong:\nExecution failed for task ':app:compileDebugKotlin'.\n> Compilation error in ${errFile}\n`;
      record.errorSummary = `Mismatched curly braces detected in ${errFile}. Tap 'Repair with AI' in AI Builder to auto-fix.`;
      return;
    }

    record.progress = 80;
    record.logs += `> Task :app:mergeDebugResources\n> Task :app:processDebugManifest\n> Task :app:packageDebug\n`;

    await new Promise(r => setTimeout(r, 600));
    const artifactId = 'apk_' + uuidv4().slice(0, 8);
    const proj = await projectService.getProject(projectId);
    const apkFileName = `${(proj?.name || 'app').toLowerCase().replace(/\s+/g, '-')}-debug.apk`;

    const artifact: Artifact = {
      id: artifactId,
      buildId,
      fileName: apkFileName,
      size: 14859200, // ~14.1 MB
      downloadUrl: `/api/artifacts/${artifactId}/download`,
      expiresAt: Date.now() + 86400000 * 30
    };

    this.artifacts.set(artifactId, artifact);

    record.status = 'SUCCEEDED';
    record.finishedAt = Date.now();
    record.durationMs = record.finishedAt - record.startedAt;
    record.progress = 100;
    record.artifactId = artifactId;
    record.logs += `> Task :app:createDebugApkListingFileRedirect\n[Mock Cloud Worker] APK signed successfully with debug keystore.\n[Artifact Worker] Generated: build/outputs/apk/debug/${apkFileName} (14.1 MB)\n\nBUILD SUCCESSFUL in ${((record.durationMs || 2500) / 1000).toFixed(1)}s\n42 actionable tasks: 38 executed, 4 up-to-date\n`;
  }

  async getBuild(buildId: string): Promise<BuildRecord | null> {
    return this.builds.get(buildId) || null;
  }

  async cancelBuild(buildId: string): Promise<boolean> {
    const rec = this.builds.get(buildId);
    if (!rec || rec.status === 'SUCCEEDED' || rec.status === 'FAILED') return false;
    rec.status = 'CANCELLED';
    rec.finishedAt = Date.now();
    rec.durationMs = rec.finishedAt - record.startedAt;
    rec.logs += `\n[BUILD CANCELLED BY USER]\n`;
    return true;
  }

  async getArtifact(artifactId: string): Promise<Artifact | null> {
    return this.artifacts.get(artifactId) || null;
  }
}

export const buildService = new MockBuildService();
