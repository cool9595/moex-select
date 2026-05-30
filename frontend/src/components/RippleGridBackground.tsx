import { useEffect, useRef } from 'react';

// RippleGridBackground — нативный аналог эффекта Ripple Grid с reactbits.dev.
// Реализован на Canvas без внешних зависимостей (без OGL/WebGL), поэтому
// не добавляет npm-пакетов и не влияет на сборку. Рисуется на фиксированном
// слое позади всего контента и не перехватывает события мыши.
// Под светлую бело-красную тему MOEX: полупрозрачные красные линии,
// фон страницы не затемняется. Отключается при prefers-reduced-motion.

const GRID_COLOR = '227, 6, 19'; // --accent MOEX в формате RGB

export function RippleGridBackground() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }
    const context = canvas.getContext('2d');
    if (!context) {
      return;
    }

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    let width = 0;
    let height = 0;
    let dpr = Math.min(window.devicePixelRatio || 1, 2);
    let frame = 0;
    let animationId = 0;

    const resize = () => {
      width = canvas.clientWidth;
      height = canvas.clientHeight;
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = Math.floor(width * dpr);
      canvas.height = Math.floor(height * dpr);
      context.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    // Перспективная проекция точки сетки в экранные координаты.
    // Линии «уходят» к горизонту, как на оригинальном Ripple Grid.
    const project = (gx: number, gy: number, t: number) => {
      // gx, gy в диапазоне [-1, 1]. Рябь — вертикальное смещение по синусоиде,
      // бегущей по оси X, усиливающейся к краям (искажение по бокам).
      const edge = Math.pow(Math.abs(gx), 1.6);
      const ripple = Math.sin(gx * 7 + t) * 0.05 * (0.35 + edge);
      const ny = gy + ripple;
      // Перспектива: ближние ряды (ny ~ 1) шире, дальние (ny ~ -1) сжаты к центру.
      const depth = (ny + 1.6) / 2.6; // >0
      const perspective = 0.45 + depth * 0.55;
      const sx = width / 2 + gx * (width * 0.62) * perspective;
      const sy = height * 0.5 + ny * (height * 0.6) * perspective;
      return { sx, sy, depth };
    };

    const COLS = 26;
    const ROWS = 18;

    const draw = () => {
      context.clearRect(0, 0, width, height);
      const t = reduceMotion ? 0 : frame * 0.012;

      // Вертикальные линии сетки
      for (let i = 0; i <= COLS; i++) {
        const gx = (i / COLS) * 2 - 1;
        context.beginPath();
        for (let j = 0; j <= ROWS; j++) {
          const gy = (j / ROWS) * 2 - 1;
          const { sx, sy } = project(gx, gy, t);
          if (j === 0) {
            context.moveTo(sx, sy);
          } else {
            context.lineTo(sx, sy);
          }
        }
        // Линии ближе к центру по X — ярче (как «волны» на скрине).
        const centerGlow = 1 - Math.min(Math.abs(gx) * 1.1, 1);
        const alpha = 0.10 + centerGlow * 0.30;
        context.strokeStyle = `rgba(${GRID_COLOR}, ${alpha})`;
        context.lineWidth = 1 + centerGlow * 1.1;
        context.stroke();
      }

      // Горизонтальные линии сетки
      for (let j = 0; j <= ROWS; j++) {
        const gy = (j / ROWS) * 2 - 1;
        context.beginPath();
        for (let i = 0; i <= COLS; i++) {
          const gx = (i / COLS) * 2 - 1;
          const { sx, sy } = project(gx, gy, t);
          if (i === 0) {
            context.moveTo(sx, sy);
          } else {
            context.lineTo(sx, sy);
          }
        }
        const { depth } = project(0, gy, t);
        const alpha = 0.07 + depth * 0.12;
        context.strokeStyle = `rgba(${GRID_COLOR}, ${alpha})`;
        context.lineWidth = 1;
        context.stroke();
      }

      if (!reduceMotion) {
        frame += 1;
        animationId = requestAnimationFrame(draw);
      }
    };

    resize();
    draw();

    let resizeTimer = 0;
    const onResize = () => {
      window.clearTimeout(resizeTimer);
      resizeTimer = window.setTimeout(() => {
        resize();
        if (reduceMotion) {
          draw();
        }
      }, 150);
    };
    window.addEventListener('resize', onResize);

    return () => {
      window.cancelAnimationFrame(animationId);
      window.clearTimeout(resizeTimer);
      window.removeEventListener('resize', onResize);
    };
  }, []);

  return <canvas ref={canvasRef} className="ripple-grid-bg" aria-hidden="true" />;
}
