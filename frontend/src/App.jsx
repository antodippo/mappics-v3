import { BrowserRouter, Routes, Route } from 'react-router-dom'
import MapPage from './pages/MapPage.jsx'
import HeatmapPage from './pages/HeatmapPage.jsx'
import GalleryPage from './pages/GalleryPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MapPage />} />
        <Route path="/heatmap" element={<HeatmapPage />} />
        <Route path="/gallery/:id" element={<GalleryPage />} />
      </Routes>
    </BrowserRouter>
  )
}
