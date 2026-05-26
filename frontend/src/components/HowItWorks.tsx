export function HowItWorks() {
  const steps = [
    'Вы указываете цель, горизонт и уровень риска.',
    'Система анализирует доступные инструменты через данные MOEX ISS.',
    'Вы получаете список инструментов, соответствующих выбранным параметрам.',
  ];

  return (
    <section className="panel process-panel" aria-label="Как работает подбор">
      <div className="panel-heading">
        <h2>Как работает подбор</h2>
      </div>
      <ol className="process-list">
        {steps.map((step) => (
          <li key={step}>{step}</li>
        ))}
      </ol>
    </section>
  );
}
