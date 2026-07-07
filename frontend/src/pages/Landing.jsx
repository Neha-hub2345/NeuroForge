import { useAuth } from '../context/AuthContext'
import { Navigate } from 'react-router-dom'

export default function Landing() {
  const { isAuthenticated, login } = useAuth()

  // If the user is already logged in, send them straight to the app
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

  return (
    <div className="landing-container" style={styles.container}>
      <div className="hero-content" style={styles.content}>
        
        {/* You can replace this with an actual logo image later */}
        <div style={styles.logoBadge}>NF</div> 
        
        <h1 style={styles.title}>NeuroForge Nexus</h1>
        <p style={styles.subtitle}>
          Cloud-Native Software Development Lifecycle Management Platform
        </p>
        
        <div style={styles.features}>
          <span style={styles.chip}>Project Tracking</span>
          <span style={styles.chip}>Agile Sprints</span>
          <span style={styles.chip}>CI/CD Pipelines</span>
          <span style={styles.chip}>DevOps Monitoring</span>
        </div>

        <button className="btn-primary" style={styles.loginBtn} onClick={login}>
          Login / Register
        </button>
      </div>
    </div>
  )
}

// Basic inline styles to make it look great immediately. 
// Feel free to move these to your main CSS file!
const styles = {
  container: {
    height: '100vh',
    width: '100vw',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#0f172a', // Matches your dark sidebar
    color: 'white',
    fontFamily: 'system-ui, sans-serif'
  },
  content: {
    textAlign: 'center',
    maxWidth: '600px',
    padding: '2rem'
  },
  logoBadge: {
    background: '#3b82f6',
    color: 'white',
    width: '64px',
    height: '64px',
    borderRadius: '12px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '24px',
    fontWeight: 'bold',
    margin: '0 auto 1.5rem auto'
  },
  title: {
    fontSize: '2.5rem',
    fontWeight: '800',
    marginBottom: '1rem',
    letterSpacing: '-0.025em'
  },
  subtitle: {
    fontSize: '1.125rem',
    color: '#94a3b8',
    marginBottom: '2rem',
    lineHeight: '1.5'
  },
  features: {
    display: 'flex',
    gap: '0.5rem',
    justifyContent: 'center',
    flexWrap: 'wrap',
    marginBottom: '3rem'
  },
  chip: {
    background: '#1e293b',
    padding: '0.5rem 1rem',
    borderRadius: '999px',
    fontSize: '0.875rem',
    color: '#cbd5e1'
  },
  loginBtn: {
    padding: '0.75rem 2rem',
    fontSize: '1.125rem',
    cursor: 'pointer'
  }
}