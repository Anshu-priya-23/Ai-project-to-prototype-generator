import { useCallback, useEffect, useMemo, useState } from 'react';
import axios from 'axios';

const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api' });
const statusLabels = {
  NOT_STARTED: 'Ready to generate', QUEUED: 'Queued', GENERATING: 'Generating',
  COMPLETED: 'Completed', FAILED: 'Failed'
};

function PasswordInput({ value, onChange }) {
  const [visible, setVisible] = useState(false);

  return <div className="password-field">
    <input
      type={visible ? 'text' : 'password'}
      placeholder="Password"
      value={value}
      onChange={onChange}
      required
      minLength="6"
    />
    <button
      type="button"
      className="password-toggle"
      onClick={() => setVisible(current => !current)}
      aria-label={visible ? 'Hide password' : 'Show password'}
      aria-pressed={visible}
    >
      {visible
        ? <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m3 3 18 18M10.6 10.7a2 2 0 0 0 2.7 2.7M9.9 4.2A10.5 10.5 0 0 1 12 4c5.5 0 9 5.5 9 5.5a15 15 0 0 1-2.1 2.7M6.6 6.6C4.4 8 3 9.5 3 9.5S6.5 15 12 15a9.8 9.8 0 0 0 3.4-.6"/></svg>
        : <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12s3.5-5.5 9-5.5 9 5.5 9 5.5-3.5 5.5-9 5.5S3 12 3 12Z"/><circle cx="12" cy="12" r="2.5"/></svg>}
    </button>
  </div>;
}

function Preview({ project }) {
  const [activeId, setActiveId] = useState(null);
  const [activeStack, setActiveStack] = useState('frontend');
  const spec = useMemo(() => {
    try { return JSON.parse(project.prototypeSpec); } catch { return null; }
  }, [project.prototypeSpec]);
  const firstScreenId = spec?.screens?.[0]?.id || null;
  useEffect(() => setActiveId(firstScreenId), [project.id, firstScreenId]);
  useEffect(() => setActiveStack('frontend'), [project.id, project.prototypeSpec]);
  if (!spec) return null;
  const screen = spec.screens.find(item => item.id === activeId) || spec.screens[0];
  const navigate = action => spec.screens.some(item => item.id === action) && setActiveId(action);
  const stack = spec.recommendedTechStack || {};
  const stackTabs = [
    { key: 'frontend', label: 'Frontend' },
    { key: 'backend', label: 'Backend' },
    { key: 'database', label: 'Database' },
    { key: 'aiIntegrations', label: 'AI / Integrations' },
    { key: 'toolsDeployment', label: 'Tools & Deployment' }
  ].filter(tab => Array.isArray(stack[tab.key]) && stack[tab.key].length > 0);
  const selectedStack = stack[activeStack] || stack[stackTabs[0]?.key] || [];

  return <div className="preview">
    <aside className="screen-nav">
      <span className="eyebrow">Prototype screens</span>
      {spec.screens.map(item => <button key={item.id} className={item.id === screen.id ? 'active' : ''}
        onClick={() => setActiveId(item.id)}>{item.title}</button>)}
    </aside>
    <section className="desktop-preview">
      <div className="preview-toolbar"><span>{screen.title}</span></div>
      <div className="canvas">
        <p className="purpose">{screen.purpose}</p>
        {screen.components?.map((component, index) => {
          const key = `${component.type}-${index}`;
          if (component.type === 'header') return <h3 key={key}>{component.label}</h3>;
          if (component.type === 'input') return <label key={key} className="field">{component.label}<input placeholder={component.content || component.label}/></label>;
          if (component.type === 'button') return <button key={key} className="primary" onClick={() => navigate(component.action)}>{component.label}</button>;
          if (component.type === 'stat') return <div key={key} className="stat"><strong>{component.content}</strong><span>{component.label}</span></div>;
          if (['card','list','table'].includes(component.type)) return <div key={key} className="component-card" onClick={() => navigate(component.action)}><strong>{component.label}</strong><p>{component.content}</p></div>;
          return <p key={key}>{component.content || component.label}</p>;
        })}
      </div>
      {stackTabs.length > 0 && <section className="tech-stack">
        <div className="tech-stack-heading"><span className="eyebrow">Recommended tech stack</span><p>Selected for this project’s features, users, and delivery needs.</p></div>
        <div className="stack-tabs" role="tablist" aria-label="Recommended technology stack">
          {stackTabs.map(tab => <button key={tab.key} role="tab" aria-selected={activeStack === tab.key}
            className={activeStack === tab.key ? 'active' : ''} onClick={() => setActiveStack(tab.key)}>{tab.label}</button>)}
        </div>
        <div className="stack-recommendations" role="tabpanel">
          {selectedStack.map(item => <article key={`${activeStack}-${item.name}`}><strong>{item.name}</strong><p>{item.reason}</p></article>)}
        </div>
      </section>}
    </section>
    <aside className="spec-summary">
      <span className="eyebrow">Product brief</span><p>{spec.overview}</p>
      <h4>Key features</h4><ul>{spec.keyFeatures?.map(item => <li key={item}>{item}</li>)}</ul>
      <h4>User roles</h4>{spec.userRoles?.map(role => <p key={role.name}><strong>{role.name}</strong><br/>{role.description}</p>)}
    </aside>
  </div>;
}

function App() {
  const [session, setSession] = useState(() => JSON.parse(localStorage.getItem('session') || 'null'));
  const [registering, setRegistering] = useState(false);
  const [auth, setAuth] = useState({ firstname: '', lastname: '', email: '', password: '' });
  const [projects, setProjects] = useState([]);
  const [draft, setDraft] = useState({ name: '', description: '' });
  const [error, setError] = useState('');
  const [expanded, setExpanded] = useState(null);

  useEffect(() => {
    if (session) api.defaults.headers.common.Authorization = `Bearer ${session.token}`;
    else delete api.defaults.headers.common.Authorization;
  }, [session]);

  const loadProjects = useCallback(async () => {
    if (!session) return;
    try {
      const { data } = await api.get('/v1/projects', { params: { ownerEmail: session.email } });
      setProjects(data);
    } catch { setError('Could not load your projects.'); }
  }, [session]);
  useEffect(() => { loadProjects(); }, [loadProjects]);
  useEffect(() => {
    if (!session || !projects.some(p => ['QUEUED','GENERATING'].includes(p.prototypeStatus))) return;
    const timer = setInterval(loadProjects, 2000); return () => clearInterval(timer);
  }, [session, projects, loadProjects]);

  const submitAuth = async event => {
    event.preventDefault(); setError('');
    try {
      const path = registering ? '/v1/auth/register' : '/v1/auth/authenticate';
      const { data } = await api.post(path, auth);
      const next = { token: data.token, email: data.email || auth.email };
      localStorage.setItem('session', JSON.stringify(next)); setSession(next);
    } catch (requestError) { setError(requestError.response?.data?.message || 'Please check your details and try again.'); }
  };
  const createProject = async event => {
    event.preventDefault(); setError('');
    try {
      await api.post('/v1/projects', { ...draft, ownerEmail: session.email });
      setDraft({ name: '', description: '' }); await loadProjects();
    } catch { setError('Could not save the project.'); }
  };
  const generate = async id => {
    setError(''); setExpanded(id);
    try { await api.post(`/v1/projects/${id}/prototype`); await loadProjects(); }
    catch { setError('Could not queue prototype generation.'); }
  };
  const logout = () => { localStorage.removeItem('session'); setSession(null); setProjects([]); };

  if (!session) return <main className="auth-page"><section className="auth-card">
    <div className="brand-mark">P</div><span className="eyebrow">Enterprise AI Platform</span>
    <h1>{registering ? 'Create your workspace' : 'Welcome back'}</h1>
    <p>Turn a detailed product idea into a prototype you can explore.</p>
    <form onSubmit={submitAuth}>
      {registering && <div className="two-col"><input placeholder="First name" value={auth.firstname} onChange={e => setAuth({...auth, firstname:e.target.value})} required/><input placeholder="Last name" value={auth.lastname} onChange={e => setAuth({...auth, lastname:e.target.value})} required/></div>}
      <input type="email" placeholder="Email address" value={auth.email} onChange={e => setAuth({...auth, email:e.target.value})} required/>
      <PasswordInput value={auth.password} onChange={e => setAuth({...auth, password:e.target.value})}/>
      <button className="primary">{registering ? 'Create account' : 'Sign in'}</button>
    </form>
    {error && <p className="error">{error}</p>}
    <button className="link" onClick={() => { setRegistering(!registering); setError(''); }}>{registering ? 'Already have an account? Sign in' : 'New here? Create an account'}</button>
  </section></main>;

  return <div><header><div><div className="brand-mark small">P</div><strong>Prototype Studio</strong></div><button className="secondary" onClick={logout}>Sign out</button></header>
    <main className="workspace">
      <section className="hero"><span className="eyebrow">AI project-to-prototype generator</span><h1>Shape an idea into something you can click.</h1><p>Describe the product and its users. We’ll map the experience, screens, content, and navigation.</p></section>
      {error && <div className="error banner">{error}</div>}
      <form className="composer" onSubmit={createProject}><span className="eyebrow">Start a project</span><h2>What are you building?</h2>
        <div className="composer-fields">
        <label>Project name<input value={draft.name} onChange={e => setDraft({...draft,name:e.target.value})} placeholder="e.g. Field service command center" required/></label>
        <label>Detailed description<textarea value={draft.description} onChange={e => setDraft({...draft,description:e.target.value})} placeholder="Who uses it, what they need to accomplish, important workflows, and any special constraints…" required minLength="20"/></label>
        <button className="primary">Save project</button>
        </div>
      </form>
      <section className="projects"><div className="section-title"><div><span className="eyebrow">Your workspace</span><h2>Saved projects</h2></div><button className="secondary" onClick={loadProjects}>Refresh</button></div>
          {!projects.length && <div className="empty">Your first idea is waiting. Save a project to begin.</div>}
          {projects.map(project => <article className="project" key={project.id}><div className="project-head"><div><h3>{project.name}</h3><p>{project.description}</p></div><span className={`status ${project.prototypeStatus.toLowerCase()}`}>{statusLabels[project.prototypeStatus]}</span></div>
            {project.prototypeStatus === 'FAILED' && <p className="error">{project.prototypeError || 'Generation failed. Please try again.'}</p>}
            <div className="actions"><button className="primary" disabled={['QUEUED','GENERATING'].includes(project.prototypeStatus)} onClick={() => generate(project.id)}>{project.prototypeStatus === 'COMPLETED' ? 'Regenerate prototype' : project.prototypeStatus === 'GENERATING' ? 'Building your prototype…' : project.prototypeStatus === 'QUEUED' ? 'Waiting to start…' : 'Generate prototype'}</button>
              {project.prototypeStatus === 'COMPLETED' && <button className="secondary" onClick={() => setExpanded(expanded === project.id ? null : project.id)}>{expanded === project.id ? 'Close preview' : 'Open preview'}</button>}</div>
            {expanded === project.id && project.prototypeStatus === 'COMPLETED' && <Preview project={project}/>}</article>)}
      </section>
    </main></div>;
}
export default App;
