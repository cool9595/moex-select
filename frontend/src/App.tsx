import { useEffect, useState } from 'react';
import { getRecommendations } from './api';
import { HowItWorks } from './components/HowItWorks';
import { ProfileForm } from './components/ProfileForm';
import { ResultsList } from './components/ResultsList';
import type { Recommendation, RecommendationRequest } from './types';

const initialProfile: RecommendationRequest = {
  goal: 'CAPITAL_GROWTH',
  riskProfile: 'MEDIUM',
  horizon: 'LONG',
  budget: 100000,
  experience: 'BEGINNER',
  assetClasses: ['STOCK', 'BOND'],
  limit: 10,
};

function App() {
  const [profile, setProfile] = useState<RecommendationRequest>(initialProfile);
  const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await getRecommendations(profile);
      setRecommendations(response.recommendations);
    } catch (apiError) {
      setError(apiError instanceof Error ? apiError.message : 'Не удалось загрузить инструменты');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void submit();
  }, []);

  return (
    <main className="app-shell">
      <header className="site-header">
        <div className="product-mark">MOEX Select</div>
        <div className="header-note">Delta team | Данные рынка: MOEX ISS</div>
      </header>

      <section className="hero">
        <h1>Подбор финансовых инструментов</h1>
        <p>Найдите инструменты Московской Биржи под ваши цели, риск-профиль и горизонт инвестирования</p>
        <p className="disclaimer">
          Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем
          параметрах и открытых рыночных данных MOEX ISS.
        </p>
      </section>

      <div className="layout">
        <aside className="sidebar">
          <ProfileForm value={profile} loading={loading} onChange={setProfile} onSubmit={submit} />
          <HowItWorks />
        </aside>

        <ResultsList recommendations={recommendations} loading={loading} error={error} />
      </div>
    </main>
  );
}

export default App;
