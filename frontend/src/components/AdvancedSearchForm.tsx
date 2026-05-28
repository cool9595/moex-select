import type { AssetClass, InstrumentSearchParams, SortBy, SortDirection } from '../types';

interface AdvancedSearchFormProps {
  value: InstrumentSearchParams;
  loading: boolean;
  onChange: (value: InstrumentSearchParams) => void;
  onSubmit: () => void;
}

const assetClasses: Array<{ value: AssetClass | ''; label: string }> = [
  { value: '', label: 'Все классы' },
  { value: 'STOCK', label: 'Акции' },
  { value: 'BOND', label: 'Облигации' },
  { value: 'FUTURE', label: 'Фьючерсы' },
  { value: 'OPTION', label: 'Опционы' },
];

const sortOptions: Array<{ value: SortBy; label: string }> = [
  { value: 'ticker', label: 'Тикер' },
  { value: 'name', label: 'Название' },
  { value: 'price', label: 'Цена' },
  { value: 'yield', label: 'Доходность' },
  { value: 'volume', label: 'Объем' },
  { value: 'turnover', label: 'Оборот / ликвидность' },
  { value: 'maturity', label: 'Срок погашения' },
  { value: 'volatility', label: 'Волатильность' },
  { value: 'marketCap', label: 'Капитализация' },
  { value: 'strikePrice', label: 'Strike price' },
];

function numberOrEmpty(value: string) {
  return value === '' ? '' : Number(value);
}

export function AdvancedSearchForm({ value, loading, onChange, onSubmit }: AdvancedSearchFormProps) {
  const update = <K extends keyof InstrumentSearchParams>(key: K, fieldValue: InstrumentSearchParams[K]) => {
    onChange({ ...value, page: key === 'page' ? Number(fieldValue) : 0, [key]: fieldValue });
  };

  return (
    <section className="panel profile-panel" aria-label="Расширенный поиск">
      <div className="panel-heading">
        <h2>Расширенный поиск</h2>
        <span>Фильтры по данным MOEX ISS для точного отбора инструментов</span>
      </div>

      <label className="field">
        <span>Поиск по тикеру или названию</span>
        <input value={value.query} onChange={(event) => update('query', event.target.value)} />
      </label>

      <div className="form-grid two-columns">
        <label className="field">
          <span>Класс инструмента</span>
          <select value={value.assetClass ?? ''} onChange={(event) => update('assetClass', event.target.value as AssetClass | '')}>
            {assetClasses.map((assetClass) => (
              <option key={assetClass.value || 'all'} value={assetClass.value}>
                {assetClass.label}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Сортировка</span>
          <select value={value.sortBy} onChange={(event) => update('sortBy', event.target.value as SortBy)}>
            {sortOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="segmented small">
        {(['asc', 'desc'] as SortDirection[]).map((direction) => (
          <button
            className={value.sortDirection === direction ? 'active' : ''}
            key={direction}
            type="button"
            onClick={() => update('sortDirection', direction)}
          >
            {direction === 'asc' ? 'По возрастанию' : 'По убыванию'}
          </button>
        ))}
      </div>

      <div className="filter-section">
        <span>Цена и доходность</span>
        <div className="form-grid two-columns">
          <label className="field compact">
            <span>Цена от</span>
            <input type="number" value={value.minPrice ?? ''} onChange={(event) => update('minPrice', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Цена до</span>
            <input type="number" value={value.maxPrice ?? ''} onChange={(event) => update('maxPrice', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Доходность от, %</span>
            <input type="number" value={value.minYield ?? ''} onChange={(event) => update('minYield', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Доходность до, %</span>
            <input type="number" value={value.maxYield ?? ''} onChange={(event) => update('maxYield', numberOrEmpty(event.target.value))} />
          </label>
        </div>
      </div>

      <div className="filter-section">
        <span>Ликвидность и риск</span>
        <div className="form-grid two-columns">
          <label className="field compact">
            <span>Объем от</span>
            <input type="number" value={value.minVolume ?? ''} onChange={(event) => update('minVolume', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Объем до</span>
            <input type="number" value={value.maxVolume ?? ''} onChange={(event) => update('maxVolume', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Оборот от</span>
            <input type="number" value={value.minTurnover ?? ''} onChange={(event) => update('minTurnover', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Оборот до</span>
            <input type="number" value={value.maxTurnover ?? ''} onChange={(event) => update('maxTurnover', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Волатильность от</span>
            <input type="number" value={value.minVolatility ?? ''} onChange={(event) => update('minVolatility', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Волатильность до</span>
            <input type="number" value={value.maxVolatility ?? ''} onChange={(event) => update('maxVolatility', numberOrEmpty(event.target.value))} />
          </label>
        </div>
      </div>

      <div className="filter-section">
        <span>Капитализация</span>
        <div className="form-grid two-columns">
          <label className="field compact">
            <span>Капитализация от</span>
            <input type="number" value={value.minMarketCap ?? ''} onChange={(event) => update('minMarketCap', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Капитализация до</span>
            <input type="number" value={value.maxMarketCap ?? ''} onChange={(event) => update('maxMarketCap', numberOrEmpty(event.target.value))} />
          </label>
        </div>
      </div>

      <div className="filter-section">
        <span>Сроки и производные</span>
        <div className="form-grid two-columns">
          <label className="field compact">
            <span>Погашение от</span>
            <input type="date" value={value.maturityFrom ?? ''} onChange={(event) => update('maturityFrom', event.target.value)} />
          </label>
          <label className="field compact">
            <span>Погашение до</span>
            <input type="date" value={value.maturityTo ?? ''} onChange={(event) => update('maturityTo', event.target.value)} />
          </label>
          <label className="field compact">
            <span>Тип опциона</span>
            <select value={value.optionType ?? ''} onChange={(event) => update('optionType', event.target.value)}>
              <option value="">Любой</option>
              <option value="CALL">Call</option>
              <option value="PUT">Put</option>
            </select>
          </label>
          <label className="field compact">
            <span>Strike от</span>
            <input type="number" value={value.minStrikePrice ?? ''} onChange={(event) => update('minStrikePrice', numberOrEmpty(event.target.value))} />
          </label>
          <label className="field compact">
            <span>Strike до</span>
            <input type="number" value={value.maxStrikePrice ?? ''} onChange={(event) => update('maxStrikePrice', numberOrEmpty(event.target.value))} />
          </label>
        </div>
      </div>

      <button className="primary-button" type="button" onClick={onSubmit} disabled={loading}>
        {loading ? 'Ищем...' : 'Найти инструменты'}
      </button>
    </section>
  );
}
