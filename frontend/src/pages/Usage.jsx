import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { Activity, ShieldCheck, Zap } from 'lucide-react';

const Usage = () => {
  const [usage, setUsage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { token } = useContext(AuthContext);

  useEffect(() => {
    const fetchUsage = async () => {
      try {
        const res = await fetch('/api/usage', {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (!res.ok) throw new Error('Failed to fetch usage statistics');
        const data = await res.json();
        setUsage(data);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchUsage();
  }, [token]);

  if (loading) return <p>Loading usage data...</p>;
  if (error) return <div className="auth-error">{error}</div>;
  if (!usage) return null;

  const percentage = Math.min(100, Math.round((usage.reviewsUsedThisMonth / usage.reviewLimit) * 100));

  return (
    <div className="animate-fade-in">
      <div className="dashboard-header">
        <h1>Usage & Limits</h1>
        <p>Monitor your AI code review consumption for the current billing cycle.</p>
      </div>

      <div className="card-grid">
        <div className="glass-panel" style={{ padding: '32px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
            <Activity color="var(--accent-primary)" size={24} />
            <h2 style={{ fontSize: '1.5rem', margin: 0 }}>Monthly Reviews</h2>
          </div>
          
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: '8px' }}>
            <span style={{ fontSize: '2.5rem', fontWeight: 'bold', fontFamily: 'var(--font-display)' }}>
              {usage.reviewsUsedThisMonth}
            </span>
            <span className="text-secondary">/ {usage.reviewLimit} limit</span>
          </div>

          <div className="progress-container" style={{ height: '12px', marginTop: '16px' }}>
            <div 
              className="progress-bar" 
              style={{ 
                width: `${percentage}%`,
                background: percentage > 90 ? 'var(--danger)' : percentage > 75 ? 'var(--warning)' : 'var(--accent-gradient)'
              }}
            />
          </div>
          <p className="text-muted" style={{ marginTop: '12px', fontSize: '0.9rem' }}>
            {100 - percentage}% remaining this cycle
          </p>
        </div>

        <div className="glass-panel" style={{ padding: '32px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <ShieldCheck color="var(--success)" size={24} />
            <h2 style={{ fontSize: '1.25rem', margin: 0 }}>Current Plan</h2>
          </div>
          <p style={{ fontSize: '1.1rem', marginBottom: '8px' }}>
            {usage.planType} Tier
          </p>
          <p className="text-secondary" style={{ fontSize: '0.9rem', marginBottom: '24px' }}>
            Your tenant ID is <br/><span style={{fontFamily: 'monospace', color: 'var(--text-primary)'}}>{usage.tenantId}</span>
          </p>
          <button className="btn btn-secondary">
            <Zap size={16} /> Upgrade Plan
          </button>
        </div>
      </div>
    </div>
  );
};

export default Usage;
