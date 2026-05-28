import type { AssetClass, DisplayLevel, Instrument, Recommendation } from '../types';

const assetClassLabels = {
  STOCK: 'Акция',
  BOND: 'Облигация',
  FUTURE: 'Фьючерс',
  OPTION: 'Опцион',
};

const riskLabels = {
  LOW: 'низкий',
  MEDIUM: 'средний',
  HIGH: 'высокий',
};

const liquidityLabels = {
  LOW: 'низкая',
  MEDIUM: 'средняя',
  HIGH: 'высокая',
};

interface InstrumentCardProps {
  instrument: Recommendation | Instrument;
  variant?: 'recommendation' | 'search';
}

function formatMaturityDate(value: string | null | undefined) {
  if (!value || value === '0000-00-00') {
    return null;
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toLocaleDateString('ru-RU');
}

function formatNumber(value: number | null | undefined, suffix = '') {
  if (value == null) {
    return '—';
  }
  return `${value.toLocaleString('ru-RU', { maximumFractionDigits: 2 })}${suffix}`.trim();
}

function impliedRisk(assetClass: AssetClass): DisplayLevel {
  if (assetClass === 'BOND') {
    return 'LOW';
  }
  return assetClass === 'STOCK' ? 'MEDIUM' : 'HIGH';
}

function liquidityLevel(instrument: Recommendation | Instrument): DisplayLevel {
  if ('liquidityLevel' in instrument) {
    return instrument.liquidityLevel;
  }
  const turnover = instrument.turnover ?? 0;
  const volume = instrument.volume ?? 0;
  if (turnover > 1_000_000_000 || volume > 1_000_000) {
    return 'HIGH';
  }
  if (turnover > 10_000_000 || volume > 50_000) {
    return 'MEDIUM';
  }
  return 'LOW';
}

function explanations(instrument: Recommendation | Instrument, variant: InstrumentCardProps['variant']) {
  if ('explanation' in instrument) {
    return instrument.explanation;
  }
  const result = [
    'Инструмент найден по открытым данным MOEX ISS.',
    `Класс инструмента: ${assetClassLabels[instrument.assetClass].toLowerCase()}.`,
  ];
  if (variant === 'search') {
    result.push('Параметры карточки приведены к единому виду для сравнения.');
  }
  return result;
}

function warnings(instrument: Recommendation | Instrument) {
  if ('warnings' in instrument) {
    return instrument.warnings;
  }
  return instrument.assetClass === 'FUTURE' || instrument.assetClass === 'OPTION'
    ? ['Инструмент относится к сложным финансовым инструментам и подходит только пользователям с соответствующим опытом.']
    : [];
}

export function InstrumentCard({ instrument, variant = 'recommendation' }: InstrumentCardProps) {
  const price = `${formatNumber(instrument.price)} ${instrument.currency ?? ''}`.trim();
  const maturity = formatMaturityDate(instrument.maturityDate);
  const riskLevel = 'riskLevel' in instrument ? instrument.riskLevel : impliedRisk(instrument.assetClass);
  const liquidity = liquidityLevel(instrument);
  const profileMatch = 'profileMatch' in instrument ? instrument.profileMatch : false;

  return (
    <article className="instrument-card">
      <div className="card-topline">
        <div>
          <div className="ticker-line">
            <h3>{instrument.ticker}</h3>
            <span className={`asset-badge ${instrument.assetClass.toLowerCase()}`}>{assetClassLabels[instrument.assetClass]}</span>
          </div>
          <p className="instrument-name">{instrument.name}</p>
        </div>
      </div>

      <div className="badge-row">
        <span className={`detail-badge risk-${riskLevel.toLowerCase()}`}>Риск: {riskLabels[riskLevel]}</span>
        <span className="detail-badge">Ликвидность: {liquidityLabels[liquidity]}</span>
        {profileMatch && <span className="fit-badge">Подходит под профиль</span>}
      </div>

      <dl className="facts-grid">
        <div>
          <dt>Цена</dt>
          <dd>{price}</dd>
        </div>
        <div>
          <dt>Расчетная доходность</dt>
          <dd>{formatNumber(instrument.yieldValue, '%')}</dd>
        </div>
        <div>
          <dt>Объем</dt>
          <dd>{formatNumber('volume' in instrument ? instrument.volume : null)}</dd>
        </div>
        <div>
          <dt>Оборот</dt>
          <dd>{formatNumber('turnover' in instrument ? instrument.turnover : null)}</dd>
        </div>
        <div>
          <dt>Волатильность</dt>
          <dd>{formatNumber('volatility' in instrument ? instrument.volatility : null, '%')}</dd>
        </div>
        <div>
          <dt>Погашение</dt>
          <dd>{maturity ?? '—'}</dd>
        </div>
        {'marketCap' in instrument && (
          <div>
            <dt>Капитализация</dt>
            <dd>{formatNumber(instrument.marketCap)}</dd>
          </div>
        )}
        {'strikePrice' in instrument && (
          <div>
            <dt>Strike price</dt>
            <dd>{formatNumber(instrument.strikePrice)}</dd>
          </div>
        )}
      </dl>

      <ul className="explain-list">
        {explanations(instrument, variant).map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>

      {warnings(instrument).length > 0 && (
        <div className="warning-list">
          {warnings(instrument).map((warning) => (
            <p key={warning}>{warning}</p>
          ))}
        </div>
      )}

      <a className="moex-link" href={instrument.moexUrl} target="_blank" rel="noreferrer">
        Открыть на MOEX
      </a>
    </article>
  );
}
