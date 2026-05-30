import { useEffect, useState } from 'react';
import { getRecommendations, searchInstruments } from './api';
import { AdvancedSearchForm } from './components/AdvancedSearchForm';
import { HowItWorks } from './components/HowItWorks';
import { ProfileForm } from './components/ProfileForm';
import { ResultsList } from './components/ResultsList';
import { RippleGridBackground } from './components/RippleGridBackground';
import { SearchResultsList } from './components/SearchResultsList';
import type { InstrumentSearchParams, InstrumentSearchResponse, Recommendation, RecommendationRequest } from './types';

const initialProfile: RecommendationRequest = {
  goal: 'CAPITAL_GROWTH',
  riskProfile: 'MEDIUM',
  horizon: 'LONG',
  budget: 100000,
  assetClasses: ['STOCK', 'BOND'],
  limit: 10,
};

const initialSearch: InstrumentSearchParams = {
  query: '',
  assetClass: '',
  page: 0,
  limit: 6,
  sortBy: 'ticker',
  sortDirection: 'asc',
};

function App() {
  const [mode, setMode] = useState<'beginner' | 'advanced'>('advanced');
  const [profile, setProfile] = useState<RecommendationRequest>(initialProfile);
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [recommendationError, setRecommendationError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useState<InstrumentSearchParams>(initialSearch);
  const [searchResponse, setSearchResponse] = useState<InstrumentSearchResponse | null>(null);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const submit = async () => {
    setRecommendationLoading(true);
    setRecommendationError(null);

    try {
      const response = await getRecommendations(profile);
      setRecommendations(response.recommendations);
    } catch (apiError) {
      setRecommendationError(apiError instanceof Error ? apiError.message : 'Не удалось загрузить инструменты');
    } finally {
      setRecommendationLoading(false);
    }
  };

  const runSearch = async (params = searchParams) => {
    setSearchLoading(true);
    setSearchError(null);

    try {
      const response = await searchInstruments(params);
      setSearchResponse(response);
    } catch (apiError) {
      setSearchError(apiError instanceof Error ? apiError.message : 'Не удалось выполнить поиск');
    } finally {
      setSearchLoading(false);
    }
  };

  const resetSearch = () => {
    const cleared = { ...initialSearch };
    setSearchParams(cleared);
    void runSearch(cleared);
  };

  useEffect(() => {
    void runSearch(initialSearch);
  }, []);

  return (
    <main className="app-shell">
      <RippleGridBackground />
      <header className="site-header">
        <div className="product-mark">MOEX Select</div>
        <div className="header-note">Delta team</div>
      </header>

      <section className="hero">
        <h1>Подбор финансовых инструментов</h1>
        <p>Найдите инструменты Московской Биржи под ваши цели, риск-профиль и горизонт инвестирования</p>
        <p className="disclaimer">
          Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем
          параметрах и открытых рыночных данных.
        </p>
      </section>

      <section className="mode-switcher" aria-label="Режим работы">
        <div>
          <h2>Режим работы</h2>
          <p>Переключайтесь между простой анкетой и расширенным поиском по инструментам</p>
        </div>
        <div className="segmented">
          <button className={mode === 'beginner' ? 'active' : ''} type="button" onClick={() => setMode('beginner')}>
            Новичок
          </button>
          <button className={mode === 'advanced' ? 'active' : ''} type="button" onClick={() => setMode('advanced')}>
            Опытный
          </button>
        </div>
      </section>

      <div className="layout">
        <aside className="sidebar">
          {mode === 'beginner' ? (
            <>
              <ProfileForm value={profile} loading={recommendationLoading} onChange={setProfile} onSubmit={submit} />
              <HowItWorks />
            </>
          ) : (
            <AdvancedSearchForm
              value={searchParams}
              loading={searchLoading}
              onChange={setSearchParams}
              onSubmit={() => {
                const nextParams = { ...searchParams, page: 0 };
                setSearchParams(nextParams);
                void runSearch(nextParams);
              }}
              onReset={resetSearch}
            />
          )}
        </aside>

        {mode === 'beginner' ? (
          <ResultsList recommendations={recommendations} loading={recommendationLoading} error={recommendationError} />
        ) : (
          <SearchResultsList
            response={searchResponse}
            loading={searchLoading}
            error={searchError}
            onPageChange={(page) => {
              const nextParams = { ...searchParams, page };
              setSearchParams(nextParams);
              void runSearch(nextParams);
            }}
          />
        )}
      </div>
    </main>
  );
}

export default App;
