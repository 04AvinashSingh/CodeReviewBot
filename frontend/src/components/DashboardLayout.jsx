import React, { useContext } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { Bot, LogOut, GitMerge, Activity } from 'lucide-react';
import './Dashboard.css';

const DashboardLayout = () => {
  const { logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="dashboard-layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <img src="/logo.png" alt="Logo" width={24} height={24} style={{ borderRadius: '4px' }} />
          <span>ReviewBot</span>
        </div>
        
        <nav className="sidebar-nav">
          <Link 
            to="/dashboard" 
            className={`sidebar-link ${location.pathname === '/dashboard' ? 'active' : ''}`}
          >
            <GitMerge size={18} />
            Repositories
          </Link>
          <Link 
            to="/dashboard/usage" 
            className={`sidebar-link ${location.pathname === '/dashboard/usage' ? 'active' : ''}`}
          >
            <Activity size={18} />
            Usage & Limits
          </Link>
        </nav>

        <div className="sidebar-footer">
          <button className="sidebar-link text-muted" onClick={handleLogout}>
            <LogOut size={18} />
            Sign Out
          </button>
        </div>
      </aside>
      
      <main className="dashboard-content">
        <Outlet />
      </main>
    </div>
  );
};

export default DashboardLayout;
