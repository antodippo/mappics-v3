import { Link, NavLink } from 'react-router-dom'
import './AppHeader.css'

// Shared floating header used by the full-map pages. Top-left pill: a clickable
// "Mappics" title that returns home, plus a small nav menu. Designed to grow as
// more pages are added.
export default function AppHeader() {
  return (
    <header className="app-header">
      <Link to="/" className="app-title">Mappics</Link>
      <nav className="app-nav">
        <NavLink to="/" end className="app-nav-link">Galleries</NavLink>
        <NavLink to="/heatmap" className="app-nav-link">Heatmap</NavLink>
        <NavLink to="/stats" className="app-nav-link">Stats</NavLink>
      </nav>
    </header>
  )
}
