import { v4 as uuidv4 } from 'uuid';
import { BuildRecord, BuildStatus, Artifact } from '../../types.js';
import { projectService } from '../projects/projectService.js';

export interface IBuildService {
  requestBuild(projectId: string, variant?: string): Promise<BuildRecord>;
  getBuild(buildId: string): Promise<BuildRecord | null>;
  cancelBuild(buildId: string): Promise<boolean>;
  getArtifact(artifactId: string): Promise<Artifact | null>;
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
      logs: `> Configure project :app\n[Mock Cloud Worker] Initializing container sandbox...\n[Mock Cloud Worker] JDK 17 (Eclipse Temurin) detected.\n[Mock Cloud Worker] Android SDK Platform 35 initialized.\n[Mock Cloud Worker] Target variant: ${variant}\n`,
      workerType: 'mock_simulation'
    };

    this.builds.set(buildId, record);

    // Run simulated build asynchronous pipeline
    this.executeBuildPipeline(buildId, projectId, files);

    return record;
  }

  private async executeBuildPipeline(
    buildId: string,
    projectId: string,
    files: Array<{ path: string; content: string }>
  ) {
    const record = this.builds.get(buildId);
    if (!record) return;

    // Check for obvious syntax or missing essential files
    const hasManifest = files.some(f => f.path.includes('AndroidManifest.xml'));
    const hasGradle = files.some(f => f.path.includes('build.gradle'));
    const hasKotlin = files.some(f => f.path.endsWith('.kt'));

    // Check for intentional syntax error injected or unclosed bracket in kotlin files
    let detectedError: string | null = null;
    for (const f of files) {
      if (f.path.endsWith('.kt')) {
        const openBraces = (f.content.match(/\{/g) || []).length;
        const closeBraces = (f.content.match(/\}/g) || []).length;
        if (openBraces !== closeBraces) {
          detectedError = `e: ${f.path}: (${f.content.split('\n').length}, 1): Unresolved reference / syntax error: Mismatched curly braces (expected ${openBraces}, found ${closeBraces})`;
          break;
        }
        if (f.content.includes('SYNTAX_ERROR_TRIGGER')) {
          detectedError = `e: ${f.path}: (42, 12): Unresolved reference: 'SYNTAX_ERROR_TRIGGER' cannot be found in current scope.`;
          break;
        }
      }
    }

    if (!hasManifest) {
      detectedError = `FATAL: AndroidManifest.xml is missing from the project structure.`;
    } else if (!hasGradle) {
      detectedError = `FATAL: No build.gradle or build.gradle.kts detected in app module.`;
    } else if (!hasKotlin) {
      detectedError = `FATAL: No Kotlin source files found in app/src/main/java/.`;
    }

    // Step 1: Queued -> Running
    await new Promise(r => setTimeout(r, 600));
    record.status = 'RUNNING';
    record.progress = 20;
    record.logs += `> Task :app:preBuild UP-TO-DATE\n> Task :app:generateDebugBuildConfig\n> Task :app:compileDebugAidl NO-SOURCE\n> Task :app:compileDebugRenderscript NO-SOURCE\n`;

    // Step 2: Compiling Kotlin
    await new Promise(r => setTimeout(r, 800));
    record.progress = 50;
    record.logs += `> Task :app:kspDebugKotlin UP-TO-DATE\n> Task :app:compileDebugKotlin\n[kotlinc] Compiling ${files.filter(f => f.path.endsWith('.kt')).length} Kotlin source files with -jvm-target 17\n`;

    if (detectedError) {
      // Failure branch
      await new Promise(r => setTimeout(r, 700));
      record.status = 'FAILED';
      record.finishedAt = Date.now();
      record.durationMs = record.finishedAt - record.startedAt;
      record.progress = 55;
      record.errorSummary = detectedError;
      record.logs += `\n[BUILD FAILED]\n${detectedError}\n\nFAILURE: Build failed with an exception.\n* What went wrong:\nExecution failed for task ':app:compileDebugKotlin'.\n> Compilation error occurred.\n`;
      return;
    }

    // Step 3: Resource merging & D8/R8
    await new Promise(r => setTimeout(r, 700));
    record.progress = 80;
    record.logs += `> Task :app:mergeDebugResources\n> Task :app:processDebugManifest\n> Task :app:dexBuilderDebug\n> Task :app:mergeProjectDexDebug\n> Task :app:packageDebug\n`;

    // Step 4: Signing & Artifact creation
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
      expiresAt: Date.now() + 86400000 * 3 // 3 days
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
    rec.durationMs = rec.finishedAt - rec.startedAt;
    rec.logs += `\n[BUILD CANCELLED BY USER]\n`;
    return true;
  }

  async getArtifact(artifactId: string): Promise<Artifact | null> {
    return this.artifacts.get(artifactId) || null;
  }
}

export const buildService = new MockBuildService();
