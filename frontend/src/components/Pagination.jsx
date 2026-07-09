export default function Pagination({ page, onPageChange }) {
  if (!page || page.totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button
        className="btn secondary small"
        disabled={page.number === 0}
        onClick={() => onPageChange(page.number - 1)}
      >
        ← Newer
      </button>
      <span>
        Page {page.number + 1} of {page.totalPages}
      </span>
      <button
        className="btn secondary small"
        disabled={page.number >= page.totalPages - 1}
        onClick={() => onPageChange(page.number + 1)}
      >
        Older →
      </button>
    </div>
  );
}
