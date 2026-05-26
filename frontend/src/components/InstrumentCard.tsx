import type { Recommendation } from '../types';

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
  instrument: Recommendation;
}

function formatMaturityDate(value: string | null | undefined) {
  if (!value || value === '0000-00-00') {
    return null;
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toLocaleDateString('ru-RU');
}

export function InstrumentCard({ instrument }: InstrumentCardProps) {
  const price =
    instrument.price == null
      ? 'Нет данных'
      : `${instrument.price.toLocaleString('ru-RU', { maximumFractionDigits: 2 })} ${instrument.currency ?? ''}`.trim();
  const maturity = formatMaturityDate(instrument.maturityDate);

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
        <span className={`detail-badge risk-${instrument.riskLevel.toLowerCase()}`}>Риск: {riskLabels[instrument.riskLevel]}</span>
        <span className="detail-badge">Ликвидность: {liquidityLabels[instrument.liquidityLevel]}</span>
        {instrument.profileMatch && <span className="fit-badge">Подходит под профиль</span>}
      </div>

      <dl className="facts-grid">
        <div>
          <dt>Цена</dt>
          <dd>{price}</dd>
        </div>
        {instrument.yieldValue != null && (
          <div>
            <dt>Расчетная доходность</dt>
            <dd>{instrument.yieldValue.toLocaleString('ru-RU', { maximumFractionDigits: 2 })}%</dd>
          </div>
        )}
        {maturity && (
          <div>
            <dt>Погашение</dt>
            <dd>{maturity}</dd>
          </div>
        )}
      </dl>

      <ul className="explain-list">
        {instrument.explanation.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>

      {instrument.warnings.length > 0 && (
        <div className="warning-list">
          {instrument.warnings.map((warning) => (
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
