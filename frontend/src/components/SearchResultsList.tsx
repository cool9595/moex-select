import type { Instrument, InstrumentSearchResponse } from '../types';
import { InstrumentCard } from './InstrumentCard';

interface SearchResultsListProps {
  response: InstrumentSearchResponse | null;
  loading: boolean;
  error: string | null;
  onPageChange: (page: number) => void;
}

export function SearchResultsList({ response, loading, error, onPageChange }: SearchResultsListProps) {
  if (loading) {
    return (
      <section className="panel results-panel">
        <div className="status-box">Загружаем инструменты...</div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="panel results-panel">
        <div className="status-box error">{error}</div>
      </section>
    );
  }

  if (!response || response.items.length === 0) {
    return (
      <section className="panel results-panel">
        <div className="status-box">По заданным фильтрам ничего не найдено. Измените диапазоны или поисковый запрос.</div>
      </section>
    );
  }

  return (
    <section className="results-panel" aria-label="Результаты поиска">
      <div className="results-heading">
        <div>
          <h2>Поиск инструментов</h2>
          <p>Найдено {response.total.toLocaleString('ru-RU')} инструментов</p>
        </div>
        <span>Страница {response.page + 1} из {Math.max(response.totalPages, 1)}</span>
      </div>

      {response.items.map((instrument: Instrument) => (
        <InstrumentCard key={`${instrument.assetClass}-${instrument.ticker}`} instrument={instrument} variant="search" />
      ))}

      <div className="pagination">
        <button type="button" onClick={() => onPageChange(response.page - 1)} disabled={response.page === 0}>
          Назад
        </button>
        <span>{response.page + 1} / {Math.max(response.totalPages, 1)}</span>
        <button type="button" onClick={() => onPageChange(response.page + 1)} disabled={response.page + 1 >= response.totalPages}>
          Вперед
        </button>
      </div>
    </section>
  );
}
