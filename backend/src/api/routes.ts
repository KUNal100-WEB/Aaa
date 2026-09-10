import { Router, Request, Response } from 'express';
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const archiver = require('archiver');
import { projectService } from '../services/projects/projectService.js';
import { buildService } from '../services/build/buildService.js';
import { aiOrchestrator } from '../services/ai/aiOrchestrator.js';
import { authService } from '../services/auth/authService.js';

export const apiRouter = Router();

// --- Auth Routes ---
apiRouter.get('/auth/me', async (req: Request, res: Response) => {
  const user = await authService.getCurrentUser();
  res.json({ user, provider: 'Firebase Ready / Mock Active' });
});

apiRouter.post('/auth/login', async (req: Request, res: Response) => {
  const { email, password } = req.body;
  const user = await authService.login(email || 'developer@phone-ai-ide.internal');
  res.json({ user });
});

apiRouter.post('/auth/register', async (req: Request, res: Response) => {
  const { email } = req.body;
  const user = await authService.register(email || 'new-dev@phone-ai-ide.internal');
  res.json({ user });
});

apiRouter.post('/auth/logout', async (req: Request, res: Response) => {
  await authService.logout();
  res.json({ success: true });
});

// --- Projects Routes ---
apiRouter.get('/projects', async (req: Request, res: Response) => {
  const list = await projectService.listProjects();
  res.json({ projects: list });
});

apiRouter.post('/projects', async (req: Request, res: Response) => {
  const { name, packageName, description, theme, appType, firebaseRequired } = req.body;
  if (!name || !name.trim()) {
    return res.status(400).json({ error: 'Project name is required' });
  }
  const project = await projectService.createProject({
    name: name.trim(),
    packageName: packageName || `com.example.${name.toLowerCase().replace(/[^a-z0-9]/g, '')}`,
    description: description || 'Generated with Phone AI Android IDE',
    theme,
    appType,
    firebaseRequired
  });
  res.status(201).json({ project });
});

apiRouter.get('/projects/:projectId', async (req: Request, res: Response) => {
  const project = await projectService.getProject(req.params.projectId);
  if (!project) return res.status(404).json({ error: 'Project not found' });
  res.json({ project });
});

apiRouter.patch('/projects/:projectId', async (req: Request, res: Response) => {
  const { name } = req.body;
  const updated = await projectService.renameProject(req.params.projectId, name);
  if (!updated) return res.status(404).json({ error: 'Project not found' });
  res.json({ project: updated });
});

apiRouter.post('/projects/:projectId/duplicate', async (req: Request, res: Response) => {
  const duplicate = await projectService.duplicateProject(req.params.projectId);
  if (!duplicate) return res.status(404).json({ error: 'Project not found' });
  res.status(201).json({ project: duplicate });
});

apiRouter.delete('/projects/:projectId', async (req: Request, res: Response) => {
  const deleted = await projectService.deleteProject(req.params.projectId);
  res.json({ success: deleted });
});

// --- Files Routes ---
apiRouter.get('/projects/:projectId/files', async (req: Request, res: Response) => {
  const files = await projectService.getFiles(req.params.projectId);
  res.json({ files });
});

apiRouter.post('/projects/:projectId/files', async (req: Request, res: Response) => {
  const { path, content } = req.body;
  if (!path) return res.status(400).json({ error: 'File path required' });
  try {
    const file = await projectService.saveFile(req.params.projectId, path, content ?? '');
    res.json({ file });
  } catch (err: any) {
    res.status(400).json({ error: err.message });
  }
});

apiRouter.delete('/projects/:projectId/files', async (req: Request, res: Response) => {
  const path = req.query.path as string;
  if (!path) return res.status(400).json({ error: 'File path required' });
  const ok = await projectService.deleteFile(req.params.projectId, path);
  res.json({ success: ok });
});

// --- ZIP Export ---
apiRouter.post('/projects/:projectId/export', async (req: Request, res: Response) => {
  const project = await projectService.getProject(req.params.projectId);
  if (!project) return res.status(404).json({ error: 'Project not found' });

  const files = await projectService.getFiles(req.params.projectId);
  const zipFileName = `${project.name.toLowerCase().replace(/[^a-z0-9]/g, '_')}_project.zip`;

  res.setHeader('Content-Type', 'application/zip');
  res.setHeader('Content-Disposition', `attachment; filename="${zipFileName}"`);

  const archive = archiver('zip', { zlib: { level: 9 } });
  archive.pipe(res);

  for (const file of files) {
    archive.append(file.content, { name: file.path });
  }

  // Include a README.md if not already present
  if (!files.some(f => f.path.toLowerCase() === 'readme.md')) {
    archive.append(
      `# ${project.name}\n\nExported from Phone AI Android IDE.\nPackage: \`${project.packageName}\`\nLanguage: Kotlin / Jetpack Compose\nTarget SDK: 35\n`,
      { name: 'README.md' }
    );
  }

  await archive.finalize();
});

// --- AI Routes ---
apiRouter.get('/projects/:projectId/ai', async (req: Request, res: Response) => {
  const requests = await aiOrchestrator.getRequests(req.params.projectId);
  res.json({ requests });
});

apiRouter.post('/projects/:projectId/ai', async (req: Request, res: Response) => {
  const { prompt, model } = req.body;
  if (!prompt || !prompt.trim()) {
    return res.status(400).json({ error: 'Prompt is required' });
  }
  try {
    const aiReq = await aiOrchestrator.processUserPrompt(req.params.projectId, prompt, model);
    res.json({ request: aiReq });
  } catch (err: any) {
    res.status(500).json({ error: err.message });
  }
});

// --- Build Routes ---
apiRouter.post('/projects/:projectId/build', async (req: Request, res: Response) => {
  const { variant } = req.body;
  try {
    const build = await buildService.requestBuild(req.params.projectId, variant);
    res.status(202).json({
      buildId: build.id,
      status: build.status,
      message: 'Build task queued for isolated build worker.'
    });
  } catch (err: any) {
    res.status(400).json({ error: err.message });
  }
});

apiRouter.get('/builds/:buildId', async (req: Request, res: Response) => {
  const build = await buildService.getBuild(req.params.buildId);
  if (!build) return res.status(404).json({ error: 'Build not found' });
  res.json(build);
});

apiRouter.post('/builds/:buildId/cancel', async (req: Request, res: Response) => {
  const ok = await buildService.cancelBuild(req.params.buildId);
  res.json({ success: ok });
});

// --- Artifact Routes ---
apiRouter.get('/built-apps', async (req: Request, res: Response) => {
  const list = await buildService.getAllArtifacts();
  res.json({ builtApps: list });
});

apiRouter.get('/artifacts/:artifactId', async (req: Request, res: Response) => {
  const artifact = await buildService.getArtifact(req.params.artifactId);
  if (!artifact) return res.status(404).json({ error: 'Artifact not found' });
  res.json({ artifact });
});

apiRouter.get('/artifacts/:artifactId/download', async (req: Request, res: Response) => {
  const artifact = await buildService.getArtifact(req.params.artifactId);
  if (!artifact) return res.status(404).json({ error: 'Artifact not found' });

  // Stream a sample simulated APK file with honest disclaimer header
  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', `attachment; filename="${artifact.fileName}"`);

  const mockApkHeader = `PK\x03\x04Phone AI Android IDE Demo Artifact - Mock Build Pipeline Simulator\nArtifact ID: ${artifact.id}\nFile: ${artifact.fileName}\nNotice: Generated by isolated worker stub in simulation mode.\n`;
  res.send(Buffer.from(mockApkHeader + '0'.repeat(1024)));
});

// --- Settings Route ---
apiRouter.get('/settings', (req: Request, res: Response) => {
  res.json({
    aiModels: [
      { id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash (Recommended)', description: 'Fastest mobile coding model' },
      { id: 'gemini-1.5-pro', name: 'Gemini 1.5 Pro', description: 'Deep reasoning and complex refactors' },
      { id: 'gemini-2.0-flash', name: 'Gemini 2.0 Flash', description: 'Multimodal coding assistant' }
    ],
    selectedModel: 'gemini-2.5-flash',
    buildWorkerType: 'Mock Simulation Worker (Isolated Cloud Ready)',
    cloudBuildEnabled: false,
    theme: 'Dark First (Darcula M3)',
    hasGeminiKey: !!(process.env.GEMINI_API_KEY && !process.env.GEMINI_API_KEY.includes('MY_GEMINI_API_KEY')),
    storageUsage: '4.2 MB / 500 MB',
    appVersion: 'Phone AI Android IDE v1.0.0-mvp'
  });
});
