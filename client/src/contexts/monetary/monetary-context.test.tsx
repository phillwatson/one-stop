import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { renderHook, act } from '@testing-library/react';
import useMonetaryContext, { MonetaryFormatProvider } from './monetary-context';

describe('MonetaryContext', () => {
  describe('useMonetaryContext hook', () => {
    it('should return format function, hidden state, and setHidden function', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      const [format, hidden, setHidden] = result.current;

      expect(typeof format).toBe('function');
      expect(typeof hidden).toBe('boolean');
      expect(typeof setHidden).toBe('function');
    });

    it('should return hidden as false initially', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      const [, hidden] = result.current;
      expect(hidden).toBe(false);
    });
  });

  describe('MonetaryFormatProvider', () => {
    it('should render children', () => {
      render(
        <MonetaryFormatProvider>
          <div data-testid="test-child">Test Child</div>
        </MonetaryFormatProvider>
      );

      expect(screen.getByTestId('test-child')).toBeInTheDocument();
    });
  });

  describe('format function', () => {
    it('should format EUR currency correctly', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      const [format] = result.current;
      const formatted = format(1234.56, 'EUR');

      expect(formatted).toContain('1.234,56');
      expect(formatted).toContain('€');
    });

    it('should format GBP currency correctly', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      const [format] = result.current;
      const formatted = format(1234.56, 'GBP');

      expect(formatted).toContain('£');
      expect(formatted).toContain('1,234.56');
    });

    it('should format USD currency correctly', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      const [format] = result.current;
      const formatted = format(1234.56, 'USD');

      expect(formatted).toContain('$');
      expect(formatted).toContain('1,234.56');
    });

    it('should format zero amount', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      const [format] = result.current;
      const formatted = format(0, 'USD');

      expect(formatted).toContain('$');
      expect(formatted).toContain('0');
    });

    it('should format negative amounts', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      const [format] = result.current;
      const formatted = format(-1234.56, 'USD');

      expect(formatted).toContain('$');
      expect(formatted).toContain('-');
    });
  });

  describe('hidden state and formatting', () => {
    it('should mask amount when hidden is true', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      let [format, hidden] = result.current;
      const normalFormatted = format(1234.56, 'USD');

      act(() => {
        const [, , setHidden] = result.current;
        setHidden(true);
      });

      [format, hidden] = result.current;
      const hiddenFormatted = format(1234.56, 'USD');

      expect(hidden).toBe(true);
      expect(normalFormatted).not.toEqual(hiddenFormatted);
      expect(hiddenFormatted).toContain('#');
      expect(hiddenFormatted).not.toContain('1234');
    });

    it('should show currency symbol even when hidden', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      act(() => {
        const [, , setHidden] = result.current;
        setHidden(true);
      });

      const [format] = result.current;
      const hiddenFormatted = format(1234.56, 'USD');

      expect(hiddenFormatted).toContain('$');
    });

    it('should toggle hidden state', () => {
      const { result } = renderHook(() => useMonetaryContext(), {
        wrapper: MonetaryFormatProvider,
      });

      let [, hidden] = result.current;
      expect(hidden).toBe(false);

      act(() => {
        const [, , setHidden] = result.current;
        setHidden(true);
      });

      [, hidden] = result.current;
      expect(hidden).toBe(true);

      act(() => {
        const [, , setHidden] = result.current;
        setHidden(false);
      });

      [, hidden] = result.current;
      expect(hidden).toBe(false);
    });
  });

  describe('integration with React components', () => {
    function TestComponent() {
      const [format, hidden, setHidden] = useMonetaryContext();
      return (
        <div>
          <div data-testid="formatted">
            {format(1234.56, 'USD')}
          </div>
          <div data-testid="hidden-status">
            {hidden ? 'Hidden' : 'Visible'}
          </div>
          <button
            data-testid="toggle-button"
            onClick={() => setHidden(!hidden)}
          >
            Toggle
          </button>
        </div>
      );
    }

    it('should update component when hidden state changes', () => {
      render(
        <MonetaryFormatProvider>
          <TestComponent />
        </MonetaryFormatProvider>
      );

      expect(screen.getByTestId('hidden-status')).toHaveTextContent('Visible');
      const initialFormatted = screen.getByTestId('formatted').textContent;

      fireEvent.click(screen.getByTestId('toggle-button'));

      expect(screen.getByTestId('hidden-status')).toHaveTextContent('Hidden');
      const hiddenFormatted = screen.getByTestId('formatted').textContent;

      expect(initialFormatted).not.toEqual(hiddenFormatted);
      expect(hiddenFormatted).toContain('#');
    });
  });
});
