import { ChevronLeft, ChevronRight } from "lucide-react";

interface PaginationProps {
  page: number; // base zero
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (page: number) => void;
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onPageChange,
}: PaginationProps) {
  if (totalElements === 0) return null;

  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <div className="mt-4 flex items-center justify-between text-sm text-content-secondary">
      <span>
        Mostrando <span className="text-content-primary">{from}</span>–
        <span className="text-content-primary">{to}</span> de{" "}
        <span className="text-content-primary">{totalElements}</span>
      </span>

      <div className="flex items-center gap-2">
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className="flex items-center gap-1 rounded-btn border border-border px-3 py-1.5 transition-colors hover:bg-bg-surface-hover disabled:cursor-not-allowed disabled:opacity-40"
        >
          <ChevronLeft className="h-4 w-4" />
          Anterior
        </button>
        <span className="px-2 text-content-primary">
          {page + 1} / {totalPages}
        </span>
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className="flex items-center gap-1 rounded-btn border border-border px-3 py-1.5 transition-colors hover:bg-bg-surface-hover disabled:cursor-not-allowed disabled:opacity-40"
        >
          Próxima
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
