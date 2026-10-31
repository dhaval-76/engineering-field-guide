// A frozen value object with validation; defaults cover the optional fields, so no
// builder class is needed.
export function pizza({ size, crust = 'REGULAR', toppings = [], extraCheese = false }) {
  if (!size) throw new Error('size is required');
  if (crust === 'STUFFED' && size !== 'LARGE') throw new Error('stuffed crust is only available in LARGE');
  // defensive, immutable copy
  return Object.freeze({ size, crust, toppings: Object.freeze([...toppings]), extraCheese });
}

const p = pizza({ size: 'LARGE', crust: 'STUFFED', toppings: ['olives'] });
