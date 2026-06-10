import { useParams } from 'react-router-dom'

export default function GalleryPage() {
  const { id } = useParams()
  return <div className="page-placeholder">GalleryPage for "{id}" — coming in step 13</div>
}
