const espresso = () => ({ description: 'Espresso', pricePaise: 12_000 });

// Add-ons wrap any beverage — combinations happen at runtime, not as subclasses.
const withMilk = (b) => ({ description: `${b.description}, milk`, pricePaise: b.pricePaise + 2_000 });
const withSyrup = (b, flavour) => ({
  description: `${b.description}, ${flavour} syrup`,
  pricePaise: b.pricePaise + 3_000,
});

const order = withSyrup(withMilk(espresso()), 'hazelnut');
// "Espresso, milk, hazelnut syrup" — 17000 paise (₹170.00)
