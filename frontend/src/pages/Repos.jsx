import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { GitMerge, Plus, RefreshCw } from 'lucide-react';

const Repos = () => {
  const [repos, setRepos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [newRepo, setNewRepo] = useState('');
  const [adding, setAdding] = useState(false);
  const { token } = useContext(AuthContext);

  const fetchRepos = async () => {
    try {
      const API_URL = import.meta.env.VITE_API_URL || '';
      const res = await fetch(`${API_URL}/api/repos`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!res.ok) throw new Error('Failed to fetch repositories');
      const data = await res.json();
      setRepos(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRepos();
  }, [token]);

  const handleAddRepo = async (e) => {
    e.preventDefault();
    let formattedRepo = newRepo.trim();
    if (!formattedRepo) return;
    
    // Automatically strip full URLs if user pastes them
    if (formattedRepo.startsWith('https://github.com/')) {
      formattedRepo = formattedRepo.replace('https://github.com/', '');
    }
    if (formattedRepo.endsWith('/')) {
      formattedRepo = formattedRepo.slice(0, -1);
    }
    
    setAdding(true);
    try {
      const API_URL = import.meta.env.VITE_API_URL || '';
      const res = await fetch(`${API_URL}/api/repos/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ 
          repoFullName: formattedRepo,
          githubInstallationId: 126776605 // Using the global installation ID for now
        })
      });
      
      if (!res.ok) {
        const data = await res.json();
        throw new Error(data.message || 'Failed to add repository');
      }
      
      setNewRepo('');
      fetchRepos();
    } catch (err) {
      alert(err.message);
    } finally {
      setAdding(false);
    }
  };

  return (
    <div className="animate-fade-in">
      <div className="dashboard-header">
        <h1>Repositories</h1>
        <p>Manage the GitHub repositories connected to your AI Code Review Bot.</p>
      </div>

      <div className="glass-panel" style={{ padding: '24px', marginBottom: '40px' }}>
        <h3 style={{ marginBottom: '16px' }}>Add New Repository</h3>
        <form onSubmit={handleAddRepo} style={{ display: 'flex', gap: '16px' }}>
          <input 
            type="text" 
            className="form-input" 
            placeholder="owner/repo-name (e.g., octocat/Hello-World)"
            value={newRepo}
            onChange={(e) => setNewRepo(e.target.value)}
            style={{ flex: 1, margin: 0 }}
          />
          <button type="submit" className="btn btn-primary" disabled={adding}>
            {adding ? <RefreshCw className="animate-spin" size={18} /> : <Plus size={18} />}
            Register Repo
          </button>
        </form>
      </div>

      {loading ? (
        <p>Loading repositories...</p>
      ) : error ? (
        <div className="auth-error">{error}</div>
      ) : repos.length === 0 ? (
        <div className="glass-panel" style={{ padding: '48px', textAlign: 'center' }}>
          <GitMerge size={48} color="var(--text-muted)" style={{ margin: '0 auto 16px' }} />
          <h3>No repositories yet</h3>
          <p className="text-muted">Register your first repository above to get started.</p>
        </div>
      ) : (
        <div className="card-grid">
          {repos.map(repo => (
            <div key={repo.id} className="repo-card glass-panel animate-fade-in">
              <div className="repo-card-header">
                <div className="repo-name">
                  <GitMerge size={20} color="var(--text-secondary)" />
                  {repo.repoFullName}
                </div>
                <span className={`status-badge ${repo.active ? '' : 'inactive'}`}>
                  {repo.active ? 'Active' : 'Inactive'}
                </span>
              </div>
              <p className="text-muted" style={{ fontSize: '0.85rem' }}>
                ID: {repo.id}
              </p>
              <div style={{ marginTop: 'auto', paddingTop: '16px', borderTop: '1px solid var(--surface-border)' }}>
                <span className="text-secondary" style={{ fontSize: '0.85rem' }}>
                  Connected on {new Date(repo.createdAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Repos;
