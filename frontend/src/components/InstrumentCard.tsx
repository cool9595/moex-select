import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import type { AssetClass, ConfidenceLevel, DisplayLevel, Instrument, InvestmentScenario, Recommendation } from '../types';

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

const confidenceLabels: Record<ConfidenceLevel, string> = {
  HIGH: 'Высокая полнота данных',
  MEDIUM: 'Средняя полнота данных',
  LOW: 'Ограниченный набор данных',
};

const scenarioLabels: Record<InvestmentScenario, string> = {
  CAPITAL_PRESERVATION: 'Сохранение капитала',
  STABLE_INCOME: 'Стабильный доход',
  CAPITAL_GROWTH: 'Рост капитала',
  SHORT_TERM_LIQUIDITY: 'Краткосрочная ликвидность',
  SPECULATION: 'Спекулятивные идеи',
};

interface InstrumentCardProps {
  instrument: Recommendation | Instrument;
  variant?: 'recommendation' | 'search';
}

function prefersReducedMotion() {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function CountUp({ value, suffix = '' }: { value: number; suffix?: string }) {
  const [display, setDisplay] = useState(prefersReducedMotion() ? value : 0);
  const ref = useRef<HTMLSpanElement | null>(null);
  const started = useRef(false);

  useEffect(() => {
    if (prefersReducedMotion()) {
      setDisplay(value);
      return;
    }
    const node = ref.current;
    if (!node) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && !started.current) {
            started.current = true;
            const duration = 750;
            const start = performance.now();
            const tick = (now: number) => {
              const progress = Math.min((now - start) / duration, 1);
              const eased = 1 - Math.pow(1 - progress, 3);
              setDisplay(value * eased);
              if (progress < 1) {
                requestAnimationFrame(tick);
              } else {
                setDisplay(value);
              }
            };
            requestAnimationFrame(tick);
          }
        });
      },
      { threshold: 0.2 },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [value]);

  return (
    <span ref={ref}>
      {display.toLocaleString('ru-RU', { maximumFractionDigits: 2 })}
      {suffix}
    </span>
  );
}

function formatMaturityDate(value: string | null | undefined) {
  if (!value || value === '0000-00-00') {
    return null;
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toLocaleDateString('ru-RU');
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

type Fact = { label: string; node: ReactNode };

function numericFact(label: string, value: number | null | undefined, suffix = ''): Fact | null {
  if (value == null) {
    return null;
  }
  return { label, node: <CountUp value={value} suffix={suffix} /> };
}

export function InstrumentCard({ instrument, variant = 'recommendation' }: InstrumentCardProps) {
  const cardRef = useRef<HTMLElement | null>(null);
  const maturity = formatMaturityDate(instrument.maturityDate);
  const riskLevel = 'riskLevel' in instrument ? instrument.riskLevel : impliedRisk(instrument.assetClass);
  const liquidity = liquidityLevel(instrument);
  const profileMatch = 'profileMatch' in instrument ? instrument.profileMatch : false;

  const handlePointerMove = (event: React.PointerEvent<HTMLElement>) => {
    if (prefersReducedMotion()) {
      return;
    }
    const node = cardRef.current;
    if (!node) {
      return;
    }
    const rect = node.getBoundingClientRect();
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;
    const rotateY = (x / rect.width - 0.5) * 5;
    const rotateX = (y / rect.height - 0.5) * -5;
    node.style.setProperty('--tilt-x', `${rotateX}deg`);
    node.style.setProperty('--tilt-y', `${rotateY}deg`);
    node.style.setProperty('--spot-x', `${x}px`);
    node.style.setProperty('--spot-y', `${y}px`);
  };

  const resetTilt = () => {
    const node = cardRef.current;
    if (!node) {
      return;
    }
    node.style.setProperty('--tilt-x', '0deg');
    node.style.setProperty('--tilt-y', '0deg');
  };

  const facts: Fact[] = [];
  if (instrument.price != null) {
    facts.push({
      label: 'Цена',
      node: (
        <>
          <CountUp value={instrument.price} />
          {instrument.currency ? ` ${instrument.currency}` : ''}
        </>
      ),
    });
  }
  const optionalFacts = [
    numericFact('Расчетная доходность', instrument.yieldValue, '%'),
    numericFact('Объем', 'volume' in instrument ? instrument.volume : null),
    numericFact('Оборот', 'turnover' in instrument ? instrument.turnover : null),
    numericFact('Волатильность', 'volatility' in instrument ? instrument.volatility : null, '%'),
    'marketCap' in instrument ? numericFact('Капитализация', instrument.marketCap) : null,
    'strikePrice' in instrument ? numericFact('Strike price', instrument.strikePrice) : null,
  ];
  optionalFacts.forEach((fact) => {
    if (fact) {
      facts.push(fact);
    }
  });
  if (maturity) {
    facts.push({ label: 'Погашение', node: maturity });
  }

  return (
    <article
      ref={cardRef}
      className="instrument-card"
      onPointerMove={handlePointerMove}
      onPointerLeave={resetTilt}
    >
      <div className="card-topline">
        <div>
          <div className="ticker-line">
            <h3>{instrument.ticker}</h3>
            <span className={`asset-badge ${instrument.assetClass.toLowerCase()}`}>
              {assetClassLabels[instrument.assetClass]}
            </span>
          </div>
          <p className="instrument-name">{instrument.name}</p>
        </div>
      </div>

      <div className="badge-row">
        <span className={`detail-badge risk-${riskLevel.toLowerCase()}`}>Риск: {riskLabels[riskLevel]}</span>
        <span className="detail-badge">Ликвидность: {liquidityLabels[liquidity]}</span>
        {'scenario' in instrument && <span className="detail-badge scenario-badge">{scenarioLabels[instrument.scenario]}</span>}
        {'confidenceLevel' in instrument && (
          <span className={`detail-badge confidence-${instrument.confidenceLevel.toLowerCase()}`}>
            {confidenceLabels[instrument.confidenceLevel]}
          </span>
        )}
        {profileMatch && <span className="fit-badge">Подходит под профиль</span>}
      </div>

      {'summary' in instrument && instrument.summary && (
        <p className="recommendation-summary">{instrument.summary}</p>
      )}

      {facts.length > 0 && (
        <dl className="facts-grid">
          {facts.map((fact) => (
            <div key={fact.label}>
              <dt>{fact.label}</dt>
              <dd>{fact.node}</dd>
            </div>
          ))}
        </dl>
      )}

      <div className="explanation-block">
        {'summary' in instrument && <h4>Почему попал в подборку</h4>}
        <ul className="explain-list">
          {explanations(instrument, variant).map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </div>

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
