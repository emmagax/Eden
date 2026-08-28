const CELL_COUNT = 1000;

const symbols = ["▓"];

function AsciiBackground() {
  const cells = Array.from({ length: CELL_COUNT }, (_, index) => {
    const symbol = symbols[index % symbols.length];

    return (
      <span className="ascii-cell" key={index}>
        {symbol}
      </span>
    );
  });

  return (
    <div className="ascii-background" aria-hidden="true">
      <div className="ascii-grid">{cells}</div>
    </div>
  );
}

export default AsciiBackground;
