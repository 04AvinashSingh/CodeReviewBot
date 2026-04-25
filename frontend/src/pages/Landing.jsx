import React from 'react';
import { Link } from 'react-router-dom';
import { GitMerge, Zap, Shield, Code2 } from 'lucide-react';
import './Landing.css';

const Landing = () => {
  return (
    <div className="landing-page animate-fade-in">
      <nav className="navbar container">
        <div className="logo">
          <img src="/logo.png" alt="CodeReviewBot Logo" width={24} height={24} style={{ borderRadius: '4px' }} />
          <span>CodeReviewBot</span>
        </div>
        <div className="nav-links">
          <Link to="/login" className="nav-link">Sign In</Link>
          <Link to="/register" className="btn btn-secondary">Get Started</Link>
        </div>
      </nav>

      <main className="hero container">
        <div className="hero-content">
          <div className="badge">Gemini 2.5 Integration</div>
          <h1 className="hero-title">
            Ship Perfect Code. <br/>
            Zero Setup.
          </h1>
          <p className="hero-subtitle">
            Instantly review your team's pull requests for bugs, vulnerabilities, and bad practices using advanced AI.
          </p>
          <div className="hero-cta">
            <Link to="/register" className="btn btn-primary btn-lg">
              Start Free Trial
            </Link>
            <a href="https://github.com" className="btn btn-secondary btn-lg" target="_blank" rel="noreferrer">
              <GitMerge size={18} />
              View Demo PR
            </a>
          </div>
        </div>

        <div className="features-grid">
          <div className="feature-card">
            <Zap className="feature-icon" color="var(--text-primary)" />
            <h3>Lightning Fast</h3>
            <p>Get comprehensive code reviews in seconds. Never wait for manual reviews to unblock your deployment.</p>
          </div>
          <div className="feature-card">
            <Shield className="feature-icon" color="var(--text-primary)" />
            <h3>Catch Vulnerabilities</h3>
            <p>Automatically detect security flaws and secret leaks before they get merged into production.</p>
          </div>
          <div className="feature-card">
            <Code2 className="feature-icon" color="var(--text-primary)" />
            <h3>Actionable Feedback</h3>
            <p>Receives inline comments directly on your GitHub diffs, exactly where the issues are found.</p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Landing;
