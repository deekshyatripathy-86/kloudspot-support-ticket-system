import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Activity, AlertCircle, ArrowDownUp, Check, ChevronDown, CircleHelp, Clock3, Filter, Headphones, LogOut, MessageSquare, Plus, RefreshCw, Search, Send, ShieldCheck, Ticket, UserRound, X } from 'lucide-react';
import './styles.css';

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';
const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

async function request(path, { token, method = 'GET', body } = {}) {
  const response = await fetch(`${API}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {})
  });
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!response.ok) throw new Error(data?.message || data?.detail || `Request failed (${response.status})`);
  return data;
}

function App() {
  const [token, setToken] = useState(() => localStorage.getItem('ks_token') || '');
  const [user, setUser] = useState(() => { try { return JSON.parse(localStorage.getItem('ks_user') || 'null'); } catch { return null; } });
  const [email, setEmail] = useState('agent@demo.com');
  const [password, setPassword] = useState('ChangeMe123!');
  const [organizationId, setOrganizationId] = useState('1');
  const [loginBusy, setLoginBusy] = useState(false);
  const [error, setError] = useState('');
  const [tickets, setTickets] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0, totalElements: 0 });
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [assignedAgentId, setAssignedAgentId] = useState('');
  const [agents, setAgents] = useState([]);
  const [sortDir, setSortDir] = useState('DESC');
  const [selected, setSelected] = useState(null);
  const [comments, setComments] = useState([]);
  const [history, setHistory] = useState([]);
  const [commentText, setCommentText] = useState('');
  const [internal, setInternal] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [newTicket, setNewTicket] = useState({ subject: '', description: '', priority: 'MEDIUM' });
  const [editingTicket, setEditingTicket] = useState(false);
  const [ticketChanges, setTicketChanges] = useState({ subject: '', description: '', priority: 'MEDIUM' });
  const [selectedAgentId, setSelectedAgentId] = useState('');
  const [showUserCreate, setShowUserCreate] = useState(false);
  const [newUser, setNewUser] = useState({ name: '', email: '', password: '', role: 'AGENT' });
  const [busy, setBusy] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const initials = useMemo(() => (user?.name || user?.email || 'U').split(/[ @]/).filter(Boolean).slice(0, 2).map(s => s[0].toUpperCase()).join(''), [user]);

  async function login(e) {
    e.preventDefault(); setError(''); setLoginBusy(true);
    try {
      const data = await request('/auth/login', { method: 'POST', body: { organizationId: String(organizationId), email, password } });
      localStorage.setItem('ks_token', data.accessToken); localStorage.setItem('ks_user', JSON.stringify(data.user));
      setToken(data.accessToken); setUser(data.user); setSelected(null);
    } catch (err) { setError(err.message); }
    finally { setLoginBusy(false); }
  }
  function logout() { localStorage.removeItem('ks_token'); localStorage.removeItem('ks_user'); setToken(''); setUser(null); setTickets([]); setSelected(null); }

  async function loadTickets() {
    if (!token) return;
    setError('');
    try {
      const params = new URLSearchParams({ page: String(page), size: '10', sortBy: 'createdAt', sortDir });
      if (search.trim()) params.set('search', search.trim()); if (status) params.set('status', status); if (priority) params.set('priority', priority);
      if (assignedAgentId) params.set('assignedAgentId', assignedAgentId);
      const data = await request(`/tickets?${params}`, { token });
      setTickets(data.content || []); setPageInfo({ number: data.number || 0, totalPages: data.totalPages || 0, totalElements: data.totalElements || 0 });
    } catch (err) { setError(err.message); }
  }
  useEffect(() => { if (token) loadTickets(); }, [token, page, status, priority, assignedAgentId, sortDir, refreshKey]);
  useEffect(() => {
    if (!token || user?.role === 'CUSTOMER') { setAgents([]); return; }
    request('/users/agents', { token }).then(setAgents).catch(err => setError(err.message));
  }, [token, user?.role]);
  useEffect(() => { if (!token) return; const timer = setTimeout(() => { setPage(0); loadTickets(); }, 250); return () => clearTimeout(timer); }, [search]);

  async function openTicket(ticket) {
    setSelected(ticket); setSelectedAgentId(ticket.assignedAgentId ? String(ticket.assignedAgentId) : ''); setEditingTicket(false); setError('');
    try {
      const [detail, commentData, historyData] = await Promise.all([
        request(`/tickets/${ticket.id}`, { token }), request(`/tickets/${ticket.id}/comments`, { token }), request(`/tickets/${ticket.id}/history`, { token })
      ]);
      setSelected(detail); setComments(commentData || []); setHistory(historyData || []);
    } catch (err) { setError(err.message); }
  }
  async function createTicket(e) {
    e.preventDefault(); setBusy(true); setError('');
    try {
      await request('/tickets', { token, method: 'POST', body: newTicket });
      setShowCreate(false); setNewTicket({ subject: '', description: '', priority: 'MEDIUM' }); setPage(0); setRefreshKey(k => k + 1);
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }
  async function saveTicket(e) {
    e.preventDefault();
    if (!selected) return;
    setBusy(true); setError('');
    try {
      const updated = await request(`/tickets/${selected.id}`, { token, method: 'PATCH', body: ticketChanges });
      setSelected(updated); setEditingTicket(false); setRefreshKey(k => k + 1);
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }
  async function assignTicket() {
    if (!selected || !selectedAgentId) return;
    setBusy(true); setError('');
    try {
      const updated = await request(`/tickets/${selected.id}/assign`, { token, method: 'PATCH', body: { agentId: Number(selectedAgentId) } });
      setSelected(updated); setRefreshKey(k => k + 1);
      const historyData = await request(`/tickets/${selected.id}/history`, { token });
      setHistory(historyData || []);
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }
  async function createUser(e) {
    e.preventDefault(); setBusy(true); setError('');
    try {
      await request('/users', { token, method: 'POST', body: newUser });
      setNewUser({ name: '', email: '', password: '', role: 'AGENT' });
      setShowUserCreate(false);
      setAgents(await request('/users/agents', { token }));
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }
  async function updateStatus(nextStatus) {
    if (!selected) return; setBusy(true);
    try { const updated = await request(`/tickets/${selected.id}/status`, { token, method: 'PATCH', body: { status: nextStatus } }); setSelected(updated); setRefreshKey(k => k + 1); await openTicket(updated); }
    catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  async function closeTicket() {
    if (!selected) return; setBusy(true);
    try { const updated = await request(`/tickets/${selected.id}/close`, { token, method: 'PATCH' }); setSelected(updated); setRefreshKey(k => k + 1); await openTicket(updated); }
    catch (err) { setError(err.message); } finally { setBusy(false); }
  }
  async function addComment(e) {
    e.preventDefault(); if (!selected || !commentText.trim()) return; setBusy(true);
    try { await request(`/tickets/${selected.id}/comments`, { token, method: 'POST', body: { content: commentText.trim(), internal: user?.role !== 'CUSTOMER' && internal } }); setCommentText(''); setInternal(false); await openTicket(selected); }
    catch (err) { setError(err.message); } finally { setBusy(false); }
  }

  if (!token || !user) return <div className="login-page"><div className="login-brand"><div className="brand-mark"><Headphones size={27} /></div><span>kloudspot<span className="brand-light">. support</span></span></div><div className="login-card"><div className="eyebrow">SUPPORT OPERATIONS</div><h1>Welcome back.</h1><p className="muted">Sign in to manage your support workspace.</p>{error && <div className="error-box"><AlertCircle size={17} />{error}</div>}<form onSubmit={login} className="stack-form"><label>Organization ID<input type="number" min="1" step="1" value={organizationId} onChange={e => setOrganizationId(e.target.value)} required /></label><label>Email address<input type="email" value={email} onChange={e => setEmail(e.target.value)} required /></label><label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label><button className="primary full" disabled={loginBusy}>{loginBusy ? 'Signing in…' : 'Sign in'}<span>→</span></button></form><div className="demo-note"><ShieldCheck size={16} /><div><strong>Demo access</strong><p>Use the prefilled agent account, or try admin@demo.com / customer@demo.com with the same password.</p></div></div></div><div className="login-footer">Secure workspace · Multi-tenant support desk</div></div>;

  const openCount = tickets.filter(t => t.status === 'OPEN' || t.status === 'IN_PROGRESS').length;
  const urgentCount = tickets.filter(t => t.priority === 'URGENT').length;
  return <div className="app-shell">
    <aside className="sidebar"><div className="side-brand"><div className="brand-mark small"><Headphones size={21} /></div><span>kloudspot<span className="brand-light">.</span></span></div><div className="workspace-label">WORKSPACE</div><button className="nav-item active"><Ticket size={18} />Tickets<span className="nav-count">{pageInfo.totalElements}</span></button><div className="sidebar-bottom"><div className="side-help"><CircleHelp size={18} /><div><strong>Need a hand?</strong><span>Check ticket activity and history.</span></div></div><div className="user-card"><div className="avatar">{initials}</div><div className="user-meta"><strong>{user.name || user.email}</strong><span>{user.role} · Org {user.organizationId}</span></div><button className="icon-button logout" onClick={logout} title="Sign out"><LogOut size={17} /></button></div></div></aside>
    <main className="main-area"><header className="topbar"><div className="breadcrumb">Support desk <span>/</span> <strong>Tickets</strong></div><div className="top-actions"><span className="secure-pill"><span className="green-dot" /> Workspace secure</span><button className="icon-button" title="Refresh tickets" onClick={() => setRefreshKey(k => k + 1)}><RefreshCw size={17} /></button></div></header>
      <section className="content"><div className="page-heading"><div><div className="eyebrow">YOUR WORKSPACE</div><h1>Support tickets</h1><p className="muted">Track issues, collaborate, and keep every customer moving.</p></div><div className="heading-actions">{user.role === 'ADMIN' && <button className="secondary" onClick={() => { setError(''); setShowUserCreate(true); }}><UserRound size={16} /> Add user</button>}<button className="primary" onClick={() => setShowCreate(true)}><Plus size={17} /> New ticket</button></div></div>
      {error && <div className="error-box global-error"><AlertCircle size={17} />{error}<button className="icon-button" onClick={() => setError('')}><X size={16} /></button></div>}
      <div className="stats-grid"><div className="stat-card"><div className="stat-icon purple"><Ticket size={18} /></div><div><span>Total tickets</span><strong>{pageInfo.totalElements}</strong><small>In your workspace view</small></div></div><div className="stat-card"><div className="stat-icon blue"><Activity size={18} /></div><div><span>Active on this page</span><strong>{openCount}</strong><small>Open or in progress</small></div></div><div className="stat-card"><div className="stat-icon orange"><AlertCircle size={18} /></div><div><span>Urgent on this page</span><strong>{urgentCount}</strong><small>Needs close attention</small></div></div></div>
      <div className="tickets-panel"><div className="panel-head"><div><h2>All tickets</h2><p>Review and manage incoming requests</p></div><div className="result-count">{pageInfo.totalElements} results <button type="button" className="secondary" aria-label={`Sort by creation date, ${sortDir === 'DESC' ? 'oldest first' : 'newest first'}`} onClick={() => { setSortDir(dir => dir === 'DESC' ? 'ASC' : 'DESC'); setPage(0); }}><ArrowDownUp size={14} />{sortDir === 'DESC' ? 'Newest first' : 'Oldest first'}</button></div></div><div className="filters"><div className="search-box"><Search size={17} /><input value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} placeholder="Search tickets…" /></div><div className="filter-control"><Filter size={15} /><select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}><option value="">All statuses</option>{STATUSES.map(s => <option key={s}>{s}</option>)}</select><ChevronDown size={14} /></div><div className="filter-control"><select value={priority} onChange={e => { setPriority(e.target.value); setPage(0); }}><option value="">All priorities</option>{PRIORITIES.map(p => <option key={p}>{p}</option>)}</select><ChevronDown size={14} /></div>{user.role !== 'CUSTOMER' && <div className="filter-control"><select value={assignedAgentId} onChange={e => { setAssignedAgentId(e.target.value); setPage(0); }}><option value="">All agents</option>{agents.map(agent => <option key={agent.id} value={agent.id}>{agent.name}</option>)}</select><ChevronDown size={14} /></div>}</div>
      <div className="table-wrap"><table><thead><tr><th>Ticket</th><th>Priority</th><th>Status</th><th>Assigned to</th><th>Created</th><th></th></tr></thead><tbody>{tickets.map(t => <tr key={t.id} onClick={() => openTicket(t)} className={selected?.id === t.id ? 'selected-row' : ''}><td><div className="ticket-title"><span className="ticket-id">#{t.id}</span><strong>{t.subject}</strong><small>{(t.description || '').replace(/\s+/g, ' ').slice(0, 76)}{t.description?.length > 76 ? '…' : ''}</small></div></td><td><span className={`priority ${t.priority?.toLowerCase()}`}><span />{t.priority}</span></td><td><span className={`status ${t.status?.toLowerCase()}`}>{t.status?.replace('_', ' ')}</span></td><td><div className="assignee"><div className="mini-avatar">{t.assignedAgentName ? t.assignedAgentName.slice(0, 1).toUpperCase() : '—'}</div><span>{t.assignedAgentName || 'Unassigned'}</span></div></td><td><span className="date-text">{formatDate(t.createdAt)}</span></td><td><button className="row-open" title="Open ticket">↗</button></td></tr>)}{tickets.length === 0 && <tr><td colSpan="6"><div className="empty-state"><div className="empty-icon"><Ticket size={22} /></div><strong>No tickets found</strong><span>Try changing your filters or search terms.</span>{user.role === 'CUSTOMER' && <button className="secondary" onClick={e => { e.stopPropagation(); setShowCreate(true); }}>Create a ticket</button>}</div></td></tr>}</tbody></table></div><div className="table-footer"><span>Page {pageInfo.totalPages ? page + 1 : 0} of {pageInfo.totalPages || 0}</span><div><button className="secondary" disabled={page <= 0} onClick={() => setPage(p => Math.max(0, p - 1))}>Previous</button><button className="secondary" disabled={page + 1 >= pageInfo.totalPages} onClick={() => setPage(p => p + 1)}>Next</button></div></div></div>
      </section>
    </main>
    {selected && <div className="detail-backdrop" onClick={() => setSelected(null)}><section className="detail-drawer" onClick={e => e.stopPropagation()}><div className="drawer-head"><div><span className="ticket-id">TICKET #{selected.id}</span><h2>{selected.subject}</h2></div><button className="icon-button" onClick={() => setSelected(null)}><X size={19} /></button></div><div className="drawer-scroll"><div className="detail-badges"><span className={`status ${selected.status?.toLowerCase()}`}>{selected.status?.replace('_', ' ')}</span><span className={`priority ${selected.priority?.toLowerCase()}`}><span />{selected.priority}</span></div><p className="detail-description">{selected.description}</p><div className="detail-meta"><div><span>Requester</span><strong>{selected.requesterName || '—'}</strong></div><div><span>Assigned agent</span><strong>{selected.assignedAgentName || 'Unassigned'}</strong></div><div><span>Created</span><strong>{formatDate(selected.createdAt)}</strong></div></div>{user.role !== 'CUSTOMER' && <div className="status-actions"><label>Assigned agent</label><div className="assignment-controls"><select value={selectedAgentId} onChange={e => setSelectedAgentId(e.target.value)}><option value="">Choose an agent</option>{agents.map(agent => <option key={agent.id} value={agent.id}>{agent.name}</option>)}</select><button className="secondary" disabled={busy || !selectedAgentId || selectedAgentId === String(selected.assignedAgentId || '')} onClick={assignTicket}>Assign</button></div><button className="secondary edit-ticket-button" disabled={busy} onClick={() => { setTicketChanges({ subject: selected.subject, description: selected.description, priority: selected.priority }); setEditingTicket(value => !value); }}>{editingTicket ? 'Cancel edit' : 'Edit ticket details'}</button>{editingTicket && <form className="ticket-edit-form" onSubmit={saveTicket}><label>Subject<input maxLength="255" value={ticketChanges.subject} onChange={e => setTicketChanges({ ...ticketChanges, subject: e.target.value })} required /></label><label>Description<textarea maxLength="10000" rows="4" value={ticketChanges.description} onChange={e => setTicketChanges({ ...ticketChanges, description: e.target.value })} required /></label><label>Priority<select value={ticketChanges.priority} onChange={e => setTicketChanges({ ...ticketChanges, priority: e.target.value })}>{PRIORITIES.map(item => <option key={item}>{item}</option>)}</select></label><button className="primary" disabled={busy}>{busy ? 'Saving…' : 'Save changes'}</button></form>}{selected.status !== 'CLOSED' && <><label>Update status</label><div className="status-buttons">{STATUSES.filter(s => s !== selected.status).map(s => <button key={s} className="secondary" disabled={busy} onClick={() => updateStatus(s)}>{s.replace('_', ' ')}</button>)}</div><button className="danger-button" disabled={busy} onClick={closeTicket}>Close ticket</button></>}</div>}
      <div className="drawer-section"><div className="section-title"><MessageSquare size={17} /><h3>Conversation</h3><span>{comments.length}</span></div><div className="comments-list">{comments.map(c => <div className="comment" key={c.id}><div className="comment-avatar">{(c.authorName || 'U').slice(0, 1).toUpperCase()}</div><div className="comment-body"><div className="comment-top"><strong>{c.authorName || 'User'}</strong><span>{formatDate(c.createdAt)}</span></div>{c.internal && <span className="internal-badge">INTERNAL NOTE</span>}<p>{c.content}</p></div></div>)}{comments.length === 0 && <div className="subtle-empty">No comments yet. Start the conversation below.</div>}</div><form className="comment-form" onSubmit={addComment}><textarea value={commentText} onChange={e => setCommentText(e.target.value)} placeholder="Write a reply or add context…" rows="3" required />{user.role !== 'CUSTOMER' && <label className="checkbox-label"><input type="checkbox" checked={internal} onChange={e => setInternal(e.target.checked)} /> Add as internal note</label>}<button className="primary" disabled={busy || !commentText.trim()}><Send size={15} /> Send comment</button></form></div>
      <div className="drawer-section history-section"><div className="section-title"><Clock3 size={17} /><h3>Activity history</h3></div>{history.length ? history.map(h => <div className="history-item" key={h.id}><span className="history-dot" /><div><strong>{h.fieldName?.replace(/([A-Z])/g, ' $1') || 'Updated'}</strong><p>{h.oldValue || '—'} <span>→</span> {h.newValue || '—'}</p><small>{formatDate(h.changedAt)}</small></div></div>) : <div className="subtle-empty">No activity recorded yet.</div>}</div></div></section></div>}
    {showCreate && <div className="modal-backdrop" onClick={() => setShowCreate(false)}><section className="create-modal" onClick={e => e.stopPropagation()}><div className="modal-head"><div><div className="eyebrow">NEW REQUEST</div><h2>Create a support ticket</h2><p className="muted">Describe what you need help with. Our team will take it from here.</p></div><button className="icon-button" onClick={() => setShowCreate(false)}><X size={19} /></button></div><form className="stack-form" onSubmit={createTicket}><label>Subject<input maxLength="255" value={newTicket.subject} onChange={e => setNewTicket({ ...newTicket, subject: e.target.value })} placeholder="A short summary of the issue" required /></label><label>Description<textarea maxLength="10000" rows="5" value={newTicket.description} onChange={e => setNewTicket({ ...newTicket, description: e.target.value })} placeholder="Include details that will help us resolve this…" required /></label><label>Priority<select value={newTicket.priority} onChange={e => setNewTicket({ ...newTicket, priority: e.target.value })}>{PRIORITIES.map(p => <option key={p}>{p}</option>)}</select></label><div className="modal-actions"><button type="button" className="secondary" onClick={() => setShowCreate(false)}>Cancel</button><button className="primary" disabled={busy}><Plus size={16} />{busy ? 'Creating…' : 'Create ticket'}</button></div></form></section></div>}
    {showUserCreate && <div className="modal-backdrop" onClick={() => setShowUserCreate(false)}><section className="create-modal" onClick={e => e.stopPropagation()}><div className="modal-head"><div><div className="eyebrow">ORGANIZATION USERS</div><h2>Add user</h2><p className="muted">The new user will belong to your organization.</p></div><button className="icon-button" onClick={() => setShowUserCreate(false)}><X size={19} /></button></div>{error && <div className="error-box"><AlertCircle size={17} />{error}</div>}<form className="stack-form" onSubmit={createUser}><label>Name<input maxLength="150" value={newUser.name} onChange={e => setNewUser({ ...newUser, name: e.target.value })} required /></label><label>Email address<input type="email" maxLength="255" value={newUser.email} onChange={e => setNewUser({ ...newUser, email: e.target.value })} required /></label><label>Temporary password<input type="password" minLength="8" maxLength="72" value={newUser.password} onChange={e => setNewUser({ ...newUser, password: e.target.value })} required /></label><label>Role<select value={newUser.role} onChange={e => setNewUser({ ...newUser, role: e.target.value })}><option value="AGENT">Support agent</option><option value="CUSTOMER">Customer</option><option value="ADMIN">Organization admin</option></select></label><div className="modal-actions"><button type="button" className="secondary" onClick={() => setShowUserCreate(false)}>Cancel</button><button className="primary" disabled={busy}>{busy ? 'Creating…' : 'Create user'}</button></div></form></section></div>}
  </div>;
}
function formatDate(value) { if (!value) return '—'; const d = new Date(value); return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }); }

createRoot(document.getElementById('root')).render(<React.StrictMode><App /></React.StrictMode>);
