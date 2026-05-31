import type { Recommendation } from '../types';
import { InstrumentCard } from './InstrumentCard';

interface ResultsListProps {
  recommendations: Recommendation[];
  profileSummary: string;
  loading: boolean;
  error: string | null;
}

export function ResultsList({ recommendations, profileSummary, loading, error }: ResultsListProps) {
  if (loading) {
    return (
      <section className="panel results-panel">
        <div className="status-box">Подбираем инструменты...</div>
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

  if (recommendations.length === 0) {
    return (
      <section className="panel results-panel">
        <div className="status-box">Пока нет результатов. Измените параметры анкеты и запустите подбор.</div>
      </section>
    );
  }

  return (
    <section className="results-panel" aria-label="Результаты подбора">
      <div className="results-heading">
        <div>
          <h2>Результаты подбора</h2>
          <p>Инструменты отсортированы с учетом выбранных параметров</p>
        </div>
        <span>{recommendations.length} инструментов</span>
      </div>

      {profileSummary && (
        <div className="profile-insight">
          <span>Инвестиционный профиль</span>
          <p>{profileSummary}</p>
        </div>
      )}

      {recommendations.map((instrument) => (
        <InstrumentCard key={`${instrument.assetClass}-${instrument.ticker}`} instrument={instrument} />
      ))}
    </section>
  );
}
