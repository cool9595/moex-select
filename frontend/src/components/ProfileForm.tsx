import type { AssetClass, Experience, Goal, Horizon, RecommendationRequest, RiskProfile } from '../types';

interface ProfileFormProps {
  value: RecommendationRequest;
  loading: boolean;
  onChange: (value: RecommendationRequest) => void;
  onSubmit: () => void;
}

const goals: Array<{ value: Goal; label: string }> = [
  { value: 'CAPITAL_PRESERVATION', label: 'Сохранить капитал' },
  { value: 'STABLE_INCOME', label: 'Получать стабильный доход' },
  { value: 'CAPITAL_GROWTH', label: 'Рост капитала' },
  { value: 'SPECULATION', label: 'Спекулятивные идеи' },
];

const risks: Array<{ value: RiskProfile; label: string }> = [
  { value: 'LOW', label: 'Низкий' },
  { value: 'MEDIUM', label: 'Средний' },
  { value: 'HIGH', label: 'Высокий' },
];

const horizons: Array<{ value: Horizon; label: string }> = [
  { value: 'SHORT', label: 'До 1 года' },
  { value: 'MEDIUM', label: '1-3 года' },
  { value: 'LONG', label: 'Более 3 лет' },
];

const experiences: Array<{ value: Experience; label: string }> = [
  { value: 'BEGINNER', label: 'Новичок' },
  { value: 'INTERMEDIATE', label: 'Уверенный пользователь' },
  { value: 'ADVANCED', label: 'Продвинутый пользователь' },
];

const assetClasses: Array<{ value: AssetClass; label: string }> = [
  { value: 'STOCK', label: 'Акции' },
  { value: 'BOND', label: 'Облигации' },
  { value: 'FUTURE', label: 'Фьючерсы' },
  { value: 'OPTION', label: 'Опционы' },
];

export function ProfileForm({ value, loading, onChange, onSubmit }: ProfileFormProps) {
  const update = <K extends keyof RecommendationRequest>(key: K, fieldValue: RecommendationRequest[K]) => {
    onChange({ ...value, [key]: fieldValue });
  };

  const toggleAssetClass = (assetClass: AssetClass) => {
    const selected = value.assetClasses.includes(assetClass)
      ? value.assetClasses.filter((item) => item !== assetClass)
      : [...value.assetClasses, assetClass];

    update('assetClasses', selected.length > 0 ? selected : value.assetClasses);
  };

  return (
    <section className="panel profile-panel" aria-label="Параметры подбора">
      <div className="panel-heading">
        <h2>Параметры подбора</h2>
        <span>Укажите ваши инвестиционные предпочтения</span>
      </div>

      <label className="field">
        <span>Цель инвестирования</span>
        <select value={value.goal} onChange={(event) => update('goal', event.target.value as Goal)}>
          {goals.map((goal) => (
            <option key={goal.value} value={goal.value}>
              {goal.label}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Уровень риска</span>
        <select value={value.riskProfile} onChange={(event) => update('riskProfile', event.target.value as RiskProfile)}>
          {risks.map((risk) => (
            <option key={risk.value} value={risk.value}>
              {risk.label}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Горизонт инвестирования</span>
        <select value={value.horizon} onChange={(event) => update('horizon', event.target.value as Horizon)}>
          {horizons.map((horizon) => (
            <option key={horizon.value} value={horizon.value}>
              {horizon.label}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Сумма, руб.</span>
        <input
          min={0}
          type="number"
          value={value.budget}
          onChange={(event) => update('budget', Number(event.target.value))}
        />
      </label>

      <label className="field">
        <span>Опыт</span>
        <select value={value.experience} onChange={(event) => update('experience', event.target.value as Experience)}>
          {experiences.map((experience) => (
            <option key={experience.value} value={experience.value}>
              {experience.label}
            </option>
          ))}
        </select>
      </label>

      <div className="field">
        <span>Классы инструментов</span>
        <div className="checkbox-grid">
          {assetClasses.map((assetClass) => (
            <label className="checkbox-field" key={assetClass.value}>
              <input
                type="checkbox"
                checked={value.assetClasses.includes(assetClass.value)}
                onChange={() => toggleAssetClass(assetClass.value)}
              />
              <span>{assetClass.label}</span>
            </label>
          ))}
        </div>
      </div>

      <button className="primary-button" type="button" onClick={onSubmit} disabled={loading}>
        {loading ? 'Подбираем...' : 'Подобрать инструменты'}
      </button>
    </section>
  );
}
